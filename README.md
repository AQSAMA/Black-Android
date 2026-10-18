# Black for Android
An unofficial native Android port of the original Xbox version of *Black*, with touch controls, gyroscope aiming, controller support and extended graphics options.

The game's x86 code is statically recompiled ahead of time to C++ with [XboxXBE-Recomp](https://github.com/TGP482/XboxXBE-Recomp), then built for arm64. Drawing goes through Vulkan, and windowing, input and audio through SDL3. No emulator is involved.

> [!NOTE]
> This project does not include any game assets. You must provide the files from your own legally acquired copy of the game to install or build.

## Installation
1. Install the APK.
2. Open **Black Android** and allow **all files access** when it asks.
3. Copy the game into `/storage/emulated/0/Black`: the USA or Europe release, either as a disc image (`.iso`) or as `default.xbe` with its data folders. The app creates the folder once access is allowed, and starts the game as soon as it finds the files.

If you don't grant that access, `Android/data/com.racas.black/files/assets` works too.

Settings (`black.toml`, `android.toml`, `layout.toml`), saves (`hdd`), logs and the shader cache are stored in `Android/data/com.racas.black/files`.

## Features
### Display
- **Render Resolution**: 20% to 200% of the screen's height (67% by default), instead of the original 640x480.
- **Field of View**: horizontal FOV from 45 to 140 degrees during gameplay. The game's default is 70.
- **Viewmodel Field of View**: FOV of the first-person weapon and arms.
- **Max Frame Rate**: caps the frame rate at the display's refresh rate or a custom value, or removes the cap. This replaces the Xbox's 30 FPS limit.
- **V-Sync** and an **FPS Counter**.
- **FMV Scale**: *Fill* zooms the 4:3 videos to fill the screen; *4:3* keeps their original aspect ratio.

### Graphics
- **Anisotropic Filtering**, applied to all textures.
- **Draw Distance**: 2x, 4x or 8x the game's level of detail and draw distances.
- **Reload Blur**: turns the blur while reloading on or off.

### Input
- **Touch**: the left side of the screen is a movement stick, and dragging on the right turns the view. On-screen buttons cover fire, aim, reload, use, melee, grenades, crouch, weapon switching and the suppressor. Dragging from a fire button turns the view while firing.
- **Controls Layout**: in the options menu under Recomp Settings: pick a preset (*Default*, *3 Finger* or *4 Finger*), set the controls' opacity and size, and drag each control where you want it with *Edit Controls Position*.
- **Gyroscope**: aim by turning the phone, either always or only while firing, with adjustable sensitivity.
- **Controller**: any controller SDL3 supports, with rumble. The touch buttons hide while a controller is in use.
- **Keyboard and Mouse**, with rebindable controls.

### General
- **Skip Intro** skips the intro logos.
- **Background Pause**: the game and its sound pause while the app is in the background.

These settings are in the game's own options menu. Android's back button opens the pause menu.

## System Requirements
- **Android** 11 or newer, arm64.
- **GPU** with a Vulkan 1.1 driver that supports dynamic rendering and timeline semaphores, for example Adreno 6xx or newer, Mali-G7x or newer, or Xclipse.
- **Memory**: 4 GB.
- **Storage**: 2 GB for the game files and saves.

## Building
Building requires a Windows PC.

### 1. Clone the repository
```
git clone https://github.com/RanggaCasper/Black-Android.git
```

### 2. Add the game files
Put `default.xbe` from the USA release in `assets/`, or put the USA disc image (`.iso`) in `assets/` or `iso/`. The build extracts `default.xbe` from the image.

### 3. Install the dependencies
- [Visual Studio 2022](https://visualstudio.microsoft.com/downloads/) with *Desktop development with C++*. The recompiler runs on the PC.
- [Android Studio](https://developer.android.com/studio). In its SDK Manager, install Android SDK Platform 34, NDK `28.2.13676358` and CMake `3.31.6`.
- [Python](https://www.python.org/), needed only to extract `default.xbe` from a disc image.

### 4. Build
Run `build.bat`. It builds the recompiler, recompiles the xbe into `generated/game`, then builds `app/build/outputs/apk/release/app-release.apk` with Gradle. The first build compiles the whole game for arm64 and takes 30 to 90 minutes.

Install the APK with:
```
adb install -r app/build/outputs/apk/release/app-release.apk
```

## Credits
- [XboxXBE-Recomp](https://github.com/TGP482/XboxXBE-Recomp) by TGP482: the static recompiler and the portable runtime (Xbox kernel, XDK libraries and Direct3D 8) this port is built on.
- [SDL](https://github.com/libsdl-org/SDL), [glslang](https://github.com/KhronosGroup/glslang), [toml++](https://github.com/marzer/tomlplusplus) and [Dear ImGui](https://github.com/ocornut/imgui).
- [xemu](https://github.com/xemu-project/xemu) and [Cxbx-Reloaded](https://github.com/Cxbx-Reloaded/Cxbx-Reloaded), which XboxXBE-Recomp used as references for Xbox hardware and API behavior.

*Black* is a trademark of its respective owners. This project is not affiliated with or endorsed by them.
