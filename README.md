# Naik VNC

Naik VNC is an independent modified fork of **bVNC**, based on the open-source
[`iiordanov/remote-desktop-clients`](https://github.com/iiordanov/remote-desktop-clients)
project.

The fork focuses on improving VNC use in desktop-style Android environments,
especially Samsung DeX, while preserving the underlying bVNC functionality.

## Project identity

- **App name:** Naik VNC
- **Android package:** `com.naiklabs.dexvnc`
- **Maintainer / Play developer name:** Naik_Labs
- **Source repository:** https://github.com/OptimusPrime100/naik-vnc
- **Upstream project:** https://github.com/iiordanov/remote-desktop-clients
- **Upstream base used for this fork:** bVNC v6.4.9 / build 116490
- **Upstream base commit:** `2440c8e7`

Naik VNC is an independent fork. It is **not affiliated with or endorsed by**
the original bVNC developer or Samsung Electronics.

## What is different in Naik VNC

The fork currently adds or changes the following areas:

- Samsung DeX-oriented Android pointer capture.
- A local software cursor that remains responsive while pointer capture is active.
- Android-style software mouse acceleration for captured relative mouse input.
- User-adjustable mouse speed and acceleration settings.
- Persistent DeX mouse settings.
- `Ctrl + Shift + Alt + Q` shortcut to release or recapture the pointer.
- DeX Mouse Settings integrated into the existing VNC session menu.
- Custom Naik VNC application identity and package.
- Security hardening to avoid logging plaintext/decrypted credential material.
- Build compatibility adjustments required by the current Android/Gradle toolchain.

A more detailed modification history is available in
[`CHANGES-NAIK.md`](CHANGES-NAIK.md).

## Repository structure

This repository is a fork of the full upstream `remote-desktop-clients` project,
so it still contains upstream modules for several remote-desktop clients.

The Android application distributed as **Naik VNC** is built from:

```text
CustomVnc-app
    |
    +-- bVNC
    +-- pubkeyGenerator
    +-- remoteClientLib
    +-- common
    +-- other transitive project dependencies
```

`CustomVnc-app` is the final application module, while much of the VNC
implementation and the Naik VNC DeX modifications live in the shared `bVNC`
module.

## Building Naik VNC

### Requirements

- Git
- JDK 21
- Android SDK / Android Studio
- Android SDK platform required by the project (currently target API 36)

The Gradle wrapper is included in the repository and uses Gradle 8.13.

### Clone

```bash
git clone https://github.com/OptimusPrime100/naik-vnc.git
cd naik-vnc
```

### Windows

```powershell
.\gradlew.bat :CustomVnc-app:assembleDebug
```

### Linux / macOS

```bash
./gradlew :CustomVnc-app:assembleDebug
```

Debug APK outputs are generated under:

```text
CustomVnc-app/build/outputs/apk/debug/
```

The project currently builds ABI-specific APKs for:

- `arm64-v8a`
- `armeabi-v7a`
- `x86`
- `x86_64`
- universal

The local SQLCipher 4.5.4 AAR required by this source tree is included under
`common/aars/` so that the checked-in source matches the build configuration
used for Naik VNC.

## Signing

Release signing keys, keystores, passwords, and private signing configuration
are intentionally **not** stored in this repository.

Anyone building a release version must provide their own signing credentials.

## Licensing and attribution

Naik VNC is derived from bVNC and the broader
`iiordanov/remote-desktop-clients` project.

The original upstream copyright and licensing notices are retained in this
repository, including:

- [`LICENSE`](LICENSE)
- [`COPYRIGHT-bVNC`](COPYRIGHT-bVNC)
- [`COPYRIGHT-Opaque`](COPYRIGHT-Opaque)
- license headers within individual source files

The bVNC-derived code is distributed under the terms described by the upstream
project, including the GNU General Public License version 3 where applicable.
Some bundled or referenced components use their own compatible licenses; their
existing notices and source headers remain authoritative.

Naik VNC modifications are distributed under the same applicable open-source
license terms as the files and components they modify.

## Upstream project

For the original project, documentation, history, and upstream development,
visit:

https://github.com/iiordanov/remote-desktop-clients

Issues specifically related to the Naik VNC fork should be reported in this
repository rather than to the upstream bVNC project.
