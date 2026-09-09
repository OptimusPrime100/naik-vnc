# Naik VNC modification history

This document records the principal changes made for the Naik VNC fork.

## Fork baseline

- Upstream repository: `iiordanov/remote-desktop-clients`
- Upstream branch: `master`
- Base commit: `2440c8e7`
- Upstream version at the fork point: `v6.4.9`
- Upstream build number at the fork point: `116490`

The upstream Git history is preserved in this repository.

## Public application identity

Naik VNC uses a separate application identity from upstream bVNC:

- App name: `Naik VNC`
- Package ID: `com.naiklabs.dexvnc`
- Maintainer / Play developer name: `Naik_Labs`

The custom application is built from the `CustomVnc-app` module.

A custom configuration asset is provided at:

```text
bVNC/src/main/assets/com.naiklabs.dexvnc.yaml
```

## Samsung DeX pointer-capture support

A custom VNC canvas activity was added:

```text
bVNC/src/main/java/com/iiordanov/bVNC/DexRemoteCanvasActivity.java
```

The custom activity adds Android pointer capture for desktop-style use.

This prevents the captured pointer from reaching the physical display edges
while the VNC session is active, which avoids unintended desktop-shell edge
behaviour in Samsung DeX.

### Pointer-capture toggle

The fork provides:

```text
Ctrl + Shift + Alt + Q
```

to switch between:

- pointer captured by Naik VNC; and
- pointer released back to the Android / DeX desktop.

## Local software cursor

While pointer capture is active, the Android system pointer is not used for
normal absolute movement.

Naik VNC therefore implements a local software cursor whose screen position is
maintained independently from the remote VNC pointer.

This provides immediate visual mouse feedback while relative captured input is
being sent to the remote computer.

## Mouse acceleration and settings

The fork adds software mouse acceleration for captured relative mouse movement.

The implementation uses Android motion/velocity information to provide a more
natural desktop-mouse feel than raw relative input alone.

User-adjustable settings include:

- mouse speed;
- acceleration strength.

These values are persisted locally using Android `SharedPreferences`.

The DeX mouse settings are integrated into the existing VNC session menu.

## Custom activity routing

The fork modifies the custom-app launch path so that the Naik VNC package uses
`DexRemoteCanvasActivity`, while preserving the normal upstream activity path
for other project variants.

Relevant source:

```text
bVNC/src/main/java/com/undatech/opaque/IntentHelper.kt
CustomVnc-app/src/main/AndroidManifest.xml
```

## Session menu integration

The existing VNC session menu was extended with a `DeX Mouse Settings` entry.

Relevant resource:

```text
bVNC/src/main/res/menu/canvasactivitymenu.xml
```

## Security hardening

Credential-related diagnostic logging was removed from:

```text
bVNC/src/main/java/com/iiordanov/bVNC/PasswordManager.java
```

The fork no longer deliberately logs:

- plaintext passed to the encryption function;
- decrypted plaintext;
- encrypted credential blobs produced by that function.

The encryption/decryption behaviour itself was otherwise left unchanged by
this specific hardening change.

## Android / Gradle build adjustments

The `CustomVnc-app` build configuration was adjusted for the current build
environment and Naik VNC package identity.

The source is currently built with JDK 21 compatibility settings.

The repository's Gradle wrapper uses Gradle 8.13.

## SQLCipher dependency

The project build expects:

```text
android-database-sqlcipher-4.5.4.aar
```

as a local AAR dependency.

For reproducibility, the AAR used by the Naik VNC build is included at:

```text
common/aars/android-database-sqlcipher-4.5.4.aar
```

## FreeRDP compatibility adjustment

A compile-time compatibility issue in the shared RDP code was handled in:

```text
remoteClientLib/src/main/java/com/undatech/opaque/RdpCommunicator.java
```

The incompatible glyph-cache performance flag call is left disabled so that
the complete multi-module source tree can build with the FreeRDP sources and
toolchain used by this fork.

This adjustment is build-related and is not part of the Naik VNC VNC/DeX input
feature itself.

## Upstream attribution

Naik VNC remains derived from bVNC and the broader
`iiordanov/remote-desktop-clients` project.

The fork retains the upstream Git history, `LICENSE`, copyright files, and
source-file license notices.

Naik VNC is an independent modified fork and is not affiliated with or endorsed
by the original bVNC developer or Samsung Electronics.
