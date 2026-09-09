/*
 * bVNC DeX pointer-capture activity.
 *
 * DeX-specific behaviour:
 *   - Android pointer capture prevents the Samsung DeX system pointer from
 *     reaching screen edges and revealing the DeX taskbar/header.
 *   - A completely independent local overlay cursor follows physical mouse
 *     movement immediately in SCREEN coordinates.
 *   - Android-style mouse acceleration is recreated because Pointer Capture
 *     intentionally disables the system's normal pointer ballistics.
 *   - Acceleration strength and overall speed are independently tunable.
 *   - DeX Mouse Settings is integrated into bVNC's existing session menu.
 *   - That local screen position is then converted into VNC framebuffer
 *     coordinates and sent to the remote PC.
 *   - Ctrl + Shift + Alt + Q toggles pointer capture on/off.
 */

package com.iiordanov.bVNC;

import android.content.SharedPreferences;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.undatech.opaque.input.RemotePointer;
import com.undatech.remoteClientUi.R;

public class DexRemoteCanvasActivity extends RemoteCanvasActivity {
    private static final String TAG = "DexRemoteCanvas";

    /*
     * Android deliberately disables mouse acceleration while Pointer Capture
     * is active. These constants recreate Android's current AOSP mouse
     * ballistics so captured movement feels much closer to the normal DeX
     * system cursor.
     */
    private static final float AOSP_MOUSE_CPI = 800.0f;
    private static final float COUNTS_TO_MM = 25.4f / AOSP_MOUSE_CPI;

    private static final float[] SEGMENT_MAX_SPEED = {
            32.002f, 52.83f, 119.124f, Float.POSITIVE_INFINITY
    };
    private static final float[] SEGMENT_BASE_GAIN = {
            3.19f, 3.79f + 1.0f, 7.28f, 15.04f
    };
    private static final float[] SEGMENT_RECIPROCAL = {
            0.0f, -51.254f, -182.0f - 0.737f, -1107.0f - 0.556f
    };
    private static final float[] SENSITIVITY_FACTORS = {
            1.0f, 2.0f, 4.0f, 6.0f, 7.0f,
            8.0f, 9.0f, 10.0f, 11.0f, 12.0f,
            13.0f, 14.0f, 16.0f, 18.0f, 20.0f
    };

    /*
     * bVNC DeX tuning defaults.
     *
     * dexAccelerationStrength:
     *   0.00 = no acceleration (pure 1:1 movement)
     *   0.25 = very mild
     *   0.50 = medium / v6 default
     *   0.75 = strong
     *   1.00 = full v5 / Android-style curve
     *
     * dexBaseSpeed changes overall cursor speed without changing the shape
     * of the acceleration curve.
     *
     * DEX_MAX_GAIN caps very fast movement so the pointer never "takes off".
     */
    private static final float DEFAULT_DEX_ACCELERATION_STRENGTH = 0.50f;
    private static final float DEFAULT_DEX_BASE_SPEED = 1.00f;
    private static final float DEX_MAX_GAIN = 2.20f;
    private static final float DEX_MIN_GAIN = 0.45f;

    private static final String DEX_MOUSE_PREFS = "bvnc_dex_mouse_settings";
    private static final String PREF_DEX_ACCELERATION = "acceleration_strength";
    private static final String PREF_DEX_SPEED = "base_speed";


    /*
     * Runtime values loaded from SharedPreferences.
     * These can be changed from the DeX Mouse Settings GUI without rebuilding.
     */
    private float dexAccelerationStrength = DEFAULT_DEX_ACCELERATION_STRENGTH;
    private float dexBaseSpeed = DEFAULT_DEX_BASE_SPEED;

    private boolean pointerCaptureWanted = true;
    private boolean swallowEscapeQUp = false;

    private FrameLayout dexCanvasLayout;
    private ImageView dexLocalCursor;

    /*
     * IMPORTANT:
     * These are LOCAL screen-space coordinates. They are deliberately NOT
     * derived from RemotePointer.getX()/getY() after initialization.
     *
     * This is what makes the overlay independent of remote/VNC cursor latency.
     */
    private float localCursorX;
    private float localCursorY;
    private boolean localCursorInitialized = false;

    /*
     * Last VNC framebuffer position calculated from the independent local
     * cursor. Buttons and scrolling use this target position as well.
     */
    private int targetRemoteX;
    private int targetRemoteY;

    private VelocityTracker dexVelocityTracker;
    private float cumulativeRawX = 0.0f;
    private float cumulativeRawY = 0.0f;
    private int androidPointerSpeed = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadDexMouseSettings();
        androidPointerSpeed = readAndroidPointerSpeed();
        setupDexVelocityTracker();
        setupDexLocalCursor();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            RemoteCanvas canvas = getCanvas();
            if (canvas != null) {
                canvas.setFocusable(true);
                canvas.setFocusableInTouchMode(true);
                canvas.setOnCapturedPointerListener(
                        (view, event) -> handleCapturedPointerEvent(event)
                );

                canvas.post(this::requestDexPointerCapture);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (dexVelocityTracker != null) {
            dexVelocityTracker.recycle();
            dexVelocityTracker = null;
        }
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus
                && pointerCaptureWanted
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            RemoteCanvas canvas = getCanvas();
            if (canvas != null) {
                canvas.post(this::requestDexPointerCapture);
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && event.getKeyCode() == KeyEvent.KEYCODE_Q) {

            if (event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getRepeatCount() == 0
                    && event.isCtrlPressed()
                    && event.isShiftPressed()
                    && event.isAltPressed()) {

                swallowEscapeQUp = true;
                toggleDexPointerCapture();
                return true;
            }

            if (event.getAction() == KeyEvent.ACTION_UP && swallowEscapeQUp) {
                swallowEscapeQUp = false;
                return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.itemDexMouseSettings) {
            showDexMouseSettingsDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadDexMouseSettings() {
        SharedPreferences prefs = getSharedPreferences(
                DEX_MOUSE_PREFS,
                MODE_PRIVATE
        );

        dexAccelerationStrength = clampFloat(
                prefs.getFloat(
                        PREF_DEX_ACCELERATION,
                        DEFAULT_DEX_ACCELERATION_STRENGTH
                ),
                0.0f,
                1.0f
        );

        dexBaseSpeed = clampFloat(
                prefs.getFloat(
                        PREF_DEX_SPEED,
                        DEFAULT_DEX_BASE_SPEED
                ),
                0.50f,
                2.00f
        );
    }

    private void saveDexMouseSettings() {
        getSharedPreferences(
                DEX_MOUSE_PREFS,
                MODE_PRIVATE
        ).edit()
                .putFloat(
                        PREF_DEX_ACCELERATION,
                        dexAccelerationStrength
                )
                .putFloat(
                        PREF_DEX_SPEED,
                        dexBaseSpeed
                )
                .apply();
    }

    private float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }

    private void showDexMouseSettingsDialog() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(
                dp(24),
                dp(12),
                dp(24),
                dp(8)
        );

        TextView explanation = new TextView(this);
        explanation.setText(
                "Changes apply immediately and are saved automatically."
        );
        explanation.setPadding(0, 0, 0, dp(16));
        content.addView(explanation);

        TextView speedLabel = new TextView(this);
        speedLabel.setTextSize(16);
        content.addView(speedLabel);

        SeekBar speedSeekBar = new SeekBar(this);
        /*
         * Progress 0..150 maps to 50%..200%.
         */
        speedSeekBar.setMax(150);
        speedSeekBar.setProgress(
                Math.round((dexBaseSpeed - 0.50f) * 100.0f)
        );
        content.addView(speedSeekBar);

        TextView accelerationLabel = new TextView(this);
        accelerationLabel.setTextSize(16);
        accelerationLabel.setPadding(0, dp(14), 0, 0);
        content.addView(accelerationLabel);

        SeekBar accelerationSeekBar = new SeekBar(this);
        accelerationSeekBar.setMax(100);
        accelerationSeekBar.setProgress(
                Math.round(dexAccelerationStrength * 100.0f)
        );
        content.addView(accelerationSeekBar);

        TextView accelerationHint = new TextView(this);
        accelerationHint.setText(
                "0% = no acceleration    •    100% = full Android-style curve"
        );
        accelerationHint.setPadding(0, dp(4), 0, dp(18));
        content.addView(accelerationHint);

        Button resetButton = new Button(this);
        resetButton.setText("Reset to defaults");
        content.addView(resetButton);

        updateDexMouseSettingLabels(
                speedLabel,
                accelerationLabel
        );

        speedSeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {
                        dexBaseSpeed = 0.50f + progress / 100.0f;

                        updateDexMouseSettingLabels(
                                speedLabel,
                                accelerationLabel
                        );
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        saveDexMouseSettings();
                    }
                }
        );

        accelerationSeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {
                        dexAccelerationStrength = progress / 100.0f;

                        updateDexMouseSettingLabels(
                                speedLabel,
                                accelerationLabel
                        );
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        saveDexMouseSettings();
                    }
                }
        );

        resetButton.setOnClickListener(view -> {
            dexBaseSpeed = DEFAULT_DEX_BASE_SPEED;
            dexAccelerationStrength =
                    DEFAULT_DEX_ACCELERATION_STRENGTH;

            speedSeekBar.setProgress(
                    Math.round(
                            (dexBaseSpeed - 0.50f) * 100.0f
                    )
            );

            accelerationSeekBar.setProgress(
                    Math.round(
                            dexAccelerationStrength * 100.0f
                    )
            );

            updateDexMouseSettingLabels(
                    speedLabel,
                    accelerationLabel
            );

            saveDexMouseSettings();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("DeX Mouse Settings")
                .setView(content)
                .setPositiveButton(
                        "Done",
                        (dialogInterface, which) -> {
                            saveDexMouseSettings();
                            resetDexVelocityTracker();
                        }
                )
                .create();

        dialog.setOnDismissListener(dialogInterface -> {
            saveDexMouseSettings();
            resetDexVelocityTracker();
        });

        dialog.show();
    }

    private void updateDexMouseSettingLabels(
            TextView speedLabel,
            TextView accelerationLabel
    ) {
        speedLabel.setText(
                "Mouse speed: "
                        + Math.round(dexBaseSpeed * 100.0f)
                        + "%"
        );

        accelerationLabel.setText(
                "Acceleration: "
                        + Math.round(
                                dexAccelerationStrength * 100.0f
                        )
                        + "%"
        );
    }

    private void setupDexVelocityTracker() {
        if (dexVelocityTracker != null) {
            dexVelocityTracker.recycle();
        }
        dexVelocityTracker = VelocityTracker.obtain();
        cumulativeRawX = 0.0f;
        cumulativeRawY = 0.0f;
    }

    private void resetDexVelocityTracker() {
        if (dexVelocityTracker == null) {
            dexVelocityTracker = VelocityTracker.obtain();
        } else {
            dexVelocityTracker.clear();
        }
        cumulativeRawX = 0.0f;
        cumulativeRawY = 0.0f;
    }

    private int readAndroidPointerSpeed() {
        int speed = Settings.System.getInt(
                getContentResolver(),
                "pointer_speed",
                0
        );
        return Math.max(-7, Math.min(7, speed));
    }

    private float calculateAndroidMouseGain(
            float rawDx,
            float rawDy,
            long eventTimeMs
    ) {
        if (dexVelocityTracker == null) {
            setupDexVelocityTracker();
        }

        cumulativeRawX += rawDx;
        cumulativeRawY += rawDy;

        MotionEvent synthetic = MotionEvent.obtain(
                eventTimeMs,
                eventTimeMs,
                MotionEvent.ACTION_MOVE,
                cumulativeRawX,
                cumulativeRawY,
                0
        );
        dexVelocityTracker.addMovement(synthetic);
        synthetic.recycle();

        dexVelocityTracker.computeCurrentVelocity(1000);
        float vx = dexVelocityTracker.getXVelocity();
        float vy = dexVelocityTracker.getYVelocity();

        float speedMmPerS =
                (float) Math.hypot(vx, vy) * COUNTS_TO_MM;

        int factorIndex = Math.max(
                0,
                Math.min(SENSITIVITY_FACTORS.length - 1, androidPointerSpeed + 7)
        );
        float commonFactor =
                0.64f * SENSITIVITY_FACTORS[factorIndex] / 10.0f;

        if (speedMmPerS <= 0.001f) {
            return commonFactor * SEGMENT_BASE_GAIN[0];
        }

        int segment = SEGMENT_MAX_SPEED.length - 1;
        for (int i = 0; i < SEGMENT_MAX_SPEED.length; i++) {
            if (speedMmPerS <= SEGMENT_MAX_SPEED[i]) {
                segment = i;
                break;
            }
        }

        float gain =
                SEGMENT_BASE_GAIN[segment]
                        + SEGMENT_RECIPROCAL[segment] / speedMmPerS;
        gain *= commonFactor;

        /*
         * v5 returned the full Android/AOSP gain here.
         *
         * v6 blends that gain toward neutral 1.0 movement. This gives us an
         * explicit acceleration-strength control without changing ordinary
         * pointer sensitivity.
         *
         * strength = 0.0 -> gain is always 1.0
         * strength = 1.0 -> full v5 curve
         */
        float softenedGain =
                1.0f
                        + (gain - 1.0f) * dexAccelerationStrength;

        /*
         * Cap both extremes so very fast movements do not become excessive
         * and very slow movements do not feel artificially heavy.
         */
        softenedGain = Math.max(
                DEX_MIN_GAIN,
                Math.min(DEX_MAX_GAIN, softenedGain)
        );

        return softenedGain * dexBaseSpeed;
    }

    private void setupDexLocalCursor() {
        dexCanvasLayout = findViewById(R.id.canvasLayout);

        if (dexCanvasLayout == null) {
            Log.w(TAG, "Could not find canvasLayout; local overlay cursor disabled");
            return;
        }

        dexLocalCursor = new ImageView(this);
        dexLocalCursor.setImageResource(R.drawable.cursor);
        dexLocalCursor.setScaleType(ImageView.ScaleType.CENTER);
        dexLocalCursor.setClickable(false);
        dexLocalCursor.setFocusable(false);
        dexLocalCursor.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        dexLocalCursor.setVisibility(View.INVISIBLE);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.TOP | Gravity.START;

        dexCanvasLayout.addView(dexLocalCursor, params);
        dexLocalCursor.bringToFront();

        /*
         * We only use the remote pointer once to choose a sensible initial
         * location. After this, the local cursor owns its position.
         */
        dexCanvasLayout.post(this::initializeLocalCursorFromRemotePointer);
    }

    private void toggleDexPointerCapture() {
        RemoteCanvas canvas = getCanvas();
        if (canvas == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        if (canvas.hasPointerCapture()) {
            releaseDexPointerCapture();
        } else {
            requestDexPointerCapture();
        }
    }

    private void requestDexPointerCapture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !hasWindowFocus()) {
            return;
        }

        RemoteCanvas canvas = getCanvas();
        if (canvas == null) {
            return;
        }

        pointerCaptureWanted = true;
        androidPointerSpeed = readAndroidPointerSpeed();
        resetDexVelocityTracker();
        canvas.setFocusable(true);
        canvas.setFocusableInTouchMode(true);
        canvas.requestFocus();

        if (!localCursorInitialized) {
            initializeLocalCursorFromRemotePointer();
        }

        setDexLocalCursorVisible(true);

        if (!canvas.hasPointerCapture()) {
            Log.i(TAG, "Requesting pointer capture");
            canvas.requestPointerCapture();
        }
    }

    private void releaseDexPointerCapture() {
        pointerCaptureWanted = false;
        setDexLocalCursorVisible(false);
        resetDexVelocityTracker();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        RemoteCanvas canvas = getCanvas();
        if (canvas != null && canvas.hasPointerCapture()) {
            Log.i(TAG, "Releasing pointer capture");
            canvas.releasePointerCapture();
        }
    }

    private void setDexLocalCursorVisible(boolean visible) {
        if (dexLocalCursor != null) {
            dexLocalCursor.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);

            if (visible) {
                dexLocalCursor.bringToFront();
            }
        }
    }

    /**
     * Initial synchronization only.
     *
     * Convert the current VNC pointer position to screen coordinates so the
     * overlay starts in a sensible place. Once initialized, ordinary movement
     * NEVER reads pointer.getX()/getY() again.
     */
    private void initializeLocalCursorFromRemotePointer() {
        RemoteCanvas canvas = getCanvas();

        if (canvas == null
                || dexCanvasLayout == null
                || getRemoteConnection() == null
                || getRemoteConnection().getPointer() == null
                || canvas.getWidth() <= 0
                || canvas.getHeight() <= 0) {
            return;
        }

        RemotePointer pointer = getRemoteConnection().getPointer();

        targetRemoteX = pointer.getX();
        targetRemoteY = pointer.getY();

        float[] point = new float[] {targetRemoteX, targetRemoteY};

        Matrix imageMatrix = canvas.getImageMatrix();
        if (imageMatrix != null) {
            imageMatrix.mapPoints(point);
        }

        localCursorX = canvas.getLeft() + point[0] - canvas.getScrollX();
        localCursorY = canvas.getTop() + point[1] - canvas.getScrollY();

        clampLocalCursorToLayout();
        positionLocalCursorView();

        localCursorInitialized = true;
    }

    /**
     * Move the independent local cursor using Android-style mouse ballistics.
     *
     * Pointer Capture supplies raw/unaccelerated mouse deltas, so we recreate
     * Android's normal acceleration curve before updating the cursor.
     */
    private void moveIndependentLocalCursor(
            float rawDx,
            float rawDy,
            long eventTimeMs
    ) {
        if (!localCursorInitialized) {
            initializeLocalCursorFromRemotePointer();
        }

        if (!localCursorInitialized) {
            return;
        }

        float gain = calculateAndroidMouseGain(rawDx, rawDy, eventTimeMs);

        localCursorX += rawDx * gain;
        localCursorY += rawDy * gain;

        clampLocalCursorToLayout();
        positionLocalCursorView();
        updateRemoteTargetFromLocalCursor();
    }

    private void clampLocalCursorToLayout() {
        if (dexCanvasLayout == null) {
            return;
        }

        float maxX = Math.max(0, dexCanvasLayout.getWidth() - 1);
        float maxY = Math.max(0, dexCanvasLayout.getHeight() - 1);

        localCursorX = Math.max(0, Math.min(maxX, localCursorX));
        localCursorY = Math.max(0, Math.min(maxY, localCursorY));
    }

    private void positionLocalCursorView() {
        if (dexLocalCursor == null) {
            return;
        }

        dexLocalCursor.setX(localCursorX);
        dexLocalCursor.setY(localCursorY);
        dexLocalCursor.bringToFront();
    }

    /**
     * Convert the independent screen-space cursor to VNC framebuffer
     * coordinates. This ensures the delayed remote cursor eventually lands
     * underneath the instantaneous overlay cursor.
     */
    private void updateRemoteTargetFromLocalCursor() {
        RemoteCanvas canvas = getCanvas();

        if (canvas == null) {
            return;
        }

        float canvasX =
                localCursorX - canvas.getLeft() + canvas.getScrollX();
        float canvasY =
                localCursorY - canvas.getTop() + canvas.getScrollY();

        float[] point = new float[] {canvasX, canvasY};

        Matrix imageMatrix = canvas.getImageMatrix();
        if (imageMatrix != null) {
            Matrix inverse = new Matrix();

            if (imageMatrix.invert(inverse)) {
                inverse.mapPoints(point);
            }
        }

        int imageWidth = canvas.getImageWidth();
        int imageHeight = canvas.getImageHeight();

        int x = Math.round(point[0]);
        int y = Math.round(point[1]);

        if (imageWidth > 0) {
            x = Math.max(0, Math.min(imageWidth - 1, x));
        }

        if (imageHeight > 0) {
            y = Math.max(0, Math.min(imageHeight - 1, y));
        }

        targetRemoteX = x;
        targetRemoteY = y;
    }

    private boolean handleCapturedPointerEvent(MotionEvent event) {
        if (getRemoteConnection() == null
                || getRemoteConnection().getPointer() == null) {
            return false;
        }

        RemotePointer pointer = getRemoteConnection().getPointer();
        int metaState = event.getMetaState();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                moveCapturedPointer(pointer, event, metaState);
                return true;

            case MotionEvent.ACTION_BUTTON_PRESS:
                pressCapturedButton(pointer, event, metaState);
                return true;

            case MotionEvent.ACTION_BUTTON_RELEASE:
                pointer.releaseButton(
                        targetRemoteX,
                        targetRemoteY,
                        metaState
                );
                return true;

            case MotionEvent.ACTION_SCROLL:
                scrollCapturedPointer(pointer, event, metaState);
                return true;

            default:
                return false;
        }
    }

    private void moveCapturedPointer(
            RemotePointer pointer,
            MotionEvent event,
            int metaState
    ) {
        /*
         * Preserve any batched historical mouse samples. This also gives the
         * velocity tracker better input for Android-style acceleration.
         */
        for (int i = 0; i < event.getHistorySize(); i++) {
            moveIndependentLocalCursor(
                    event.getHistoricalX(i),
                    event.getHistoricalY(i),
                    event.getHistoricalEventTime(i)
            );
        }

        /*
         * Move the local overlay first, with recreated Android ballistics.
         */
        moveIndependentLocalCursor(
                event.getX(),
                event.getY(),
                event.getEventTime()
        );

        /*
         * Then send the same accelerated target to the remote VNC server.
         */
        pointer.moveMouse(targetRemoteX, targetRemoteY, metaState);
    }

    private void pressCapturedButton(
            RemotePointer pointer,
            MotionEvent event,
            int metaState
    ) {
        int button = event.getActionButton();

        if ((button & MotionEvent.BUTTON_PRIMARY) != 0) {
            pointer.leftButtonDown(
                    targetRemoteX,
                    targetRemoteY,
                    metaState
            );
        } else if ((button & MotionEvent.BUTTON_SECONDARY) != 0) {
            pointer.rightButtonDown(
                    targetRemoteX,
                    targetRemoteY,
                    metaState
            );
        } else if ((button & MotionEvent.BUTTON_TERTIARY) != 0) {
            pointer.middleButtonDown(
                    targetRemoteX,
                    targetRemoteY,
                    metaState
            );
        }
    }

    private void scrollCapturedPointer(
            RemotePointer pointer,
            MotionEvent event,
            int metaState
    ) {
        float vertical = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
        float horizontal = event.getAxisValue(MotionEvent.AXIS_HSCROLL);

        if (vertical != 0.0f) {
            int ticks = Math.max(
                    1,
                    Math.min(7, Math.round(Math.abs(vertical)))
            );

            for (int i = 0; i < ticks; i++) {
                if (vertical > 0.0f) {
                    pointer.scrollUp(
                            targetRemoteX,
                            targetRemoteY,
                            metaState
                    );
                } else {
                    pointer.scrollDown(
                            targetRemoteX,
                            targetRemoteY,
                            metaState
                    );
                }

                pointer.releaseButton(
                        targetRemoteX,
                        targetRemoteY,
                        metaState
                );
            }
        }

        if (horizontal != 0.0f) {
            int ticks = Math.max(
                    1,
                    Math.min(7, Math.round(Math.abs(horizontal)))
            );

            for (int i = 0; i < ticks; i++) {
                if (horizontal > 0.0f) {
                    pointer.scrollRight(
                            targetRemoteX,
                            targetRemoteY,
                            metaState
                    );
                } else {
                    pointer.scrollLeft(
                            targetRemoteX,
                            targetRemoteY,
                            metaState
                    );
                }

                pointer.releaseButton(
                        targetRemoteX,
                        targetRemoteY,
                        metaState
                );
            }
        }
    }
}
