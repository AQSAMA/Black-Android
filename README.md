# Black for Android
An unofficial native Android port of the original Xbox version of *Black*, with touch controls, gyroscope aiming, controller support, extended graphics options and a Material 3 **Control Center** for setting it all up.

The game's x86 code is statically recompiled ahead of time to C++ with [XboxXBE-Recomp](https://github.com/TGP482/XboxXBE-Recomp), then built for arm64. Drawing goes through Vulkan, and windowing, input and audio through SDL3. No emulator is involved.

> [!NOTE]
> This project does not include any game assets. You must provide the files from your own legally acquired copy of the game to install or build.

<p align="center">
  <a href="https://youtu.be/x5bczwTuyhw">
    <img src="https://img.youtube.com/vi/x5bczwTuyhw/maxresdefault.jpg" alt="Black Android gameplay on POCO F6" width="720">
  </a>
  <br>
  <sub>▶ Gameplay on POCO F6, max settings + gyro aim</sub>
</p>

## Installation
1. Install the APK.
2. Open **Black Android**. The **Control Center** opens first: allow **all files access** when it asks (on Android 10, the **storage** permission).
3. Copy the game into `/storage/emulated/0/Black`, or into a `Black` folder at the top of your SD card: the USA or Europe release, either as a disc image (`.iso`) or as `default.xbe` with its data folders. The app creates the folder on the phone's storage once access is allowed, and the Control Center shows the game as ready as soon as it finds the files.
4. Tap **Play** to start the game. While playing, the pause menu's **CONTROL CENTER** button brings the settings back over the game.

If you don't grant that access, `Android/data/com.racas.black/files/assets` works too.

Settings (`black.toml`, `android.toml`, `layout.toml`), saves (`hdd`), logs and the shader cache are stored in `Android/data/com.racas.black/files`.

## Features
The **Control Center** is the app's front end: it opens when you launch Black Android and from **CONTROL CENTER** in the pause menu. It edits the same `black.toml`, `android.toml` and `layout.toml` the renderer reads, and a running game picks the changes up without a restart. Rows, sliders, switches and buttons are sized for touch, and the interface itself is adjustable.

### Control Center
- **Overview**: whether the game files were found, importing a disc image or a `default.xbe` folder, **Play**, update checks, and sharing or exporting the settings as TOML.
- **Display**, **Controls**, **Audio** and **Cheats**: the port's settings gathered under large, plain-language pages, each with its own reset button.
- **Interface**: OLED black, dark or system theme; eight accent colours; square or round corners; high contrast; interface and text scale; bottom tabs or a side rail on wide screens; haptics, reduced motion and keep-screen-on.
- **Layout editor**: a full-screen preview of the touch controls. Drag any control, resize it with a slider or a pinch, hide the ones you do not use, and save the layout back to the game. The optional top-left fire zone is a wide rectangle that can be dragged and resized like the round buttons.
- **Back and Play** behave as expected: back leaves the Control Center, and the **Play** button in the top bar returns to the game.

The game's own options remain available in the pause menu, grouped into **Graphics**, **Touch Controls**, **General**, **Mouse and Keyboard** and **Cheats**. Each page has headings and arrows above and below the list to show there is more to scroll to; tap an arrow to move one row, or hold it to keep going.

### Graphics
- **Display**
  - **Render Resolution**: 20% to 200% of the screen's height (67% by default), instead of the original 640x480.
  - **Field of View**: horizontal FOV from 45 to 140 degrees during gameplay. The game's default is 70.
  - **Viewmodel Field of View**: FOV of the first-person weapon and arms.
  - **FMV Scale**: *Fill* zooms the 4:3 videos to fill the screen; *4:3* keeps their original aspect ratio.
- **Performance**
  - **V-Sync**, off by default.
  - **Max Frame Rate**: unlimited by default, or capped at the display's refresh rate or a custom value. This replaces the Xbox's 30 FPS limit.
  - **FPS Counter**.
- **Quality**
  - **Anisotropic Filtering**, applied to all textures.
  - **Draw Distance**: 2x, 4x or 8x the game's level of detail and draw distances.
  - **Reload Blur**: turns the blur while reloading on or off.

### Touch Controls
- **Touch**: the left side of the screen is a floating movement stick, and dragging on the right turns the view. On-screen buttons cover fire, aim, reload, use, melee, grenades, crouch, weapon switching and the suppressor. Dragging from a fire button turns the view while firing. The controls hide over loading screens and cutscenes.
- **Modern controls**: the default button style is dark glass with a thin rim and warm ink, a soft shadow, and an accent fill while a button is held. The movement stick is a floating ring with a small knob and compass ticks. *Xbox* (the skin's icons with the Xbox controller's marks), *Clean* and *Classic* remain available.
- **Fire zone**: an optional wide fire rectangle in the top-left corner for the left index finger. It is off by default, switched on under **Controls → Top-left fire zone**, and then dragged or resized in the editor like any other control.
- **Menus**: a D-pad and a back button move through the menus and the options; tap an arrow to step one row, or hold it to keep going.
- **Touch Sensitivity**: how fast a drag turns the view, separate from the mouse's sensitivity.
- **Controls Layout**: pick a preset (*Default* for two thumbs, *3 Finger* or *4 Finger*) and set the controls' opacity and overall size. Each preset only shows the controls that grip needs, and every position remains editable.
- **Button Style**: *Modern* (the default glass controls), *Xbox* (the skin's icons with the Xbox controller's A, B, X, Y and other button marks), *Clean* (the same icons without them) or *Classic* (the original drawn icons).
- **Edit Controls Position**: drag each control where you want it. To resize one, select it and tap **−** or **+**, or pinch it with a second finger.
- **Motion Controls**: aim with the gyroscope by turning the phone, or a DualShock 4 or DualSense on Android 12 and newer, either always or only while firing, with adjustable sensitivity. The controller's gyroscope is used while you play with the controller, the phone's while you use the touch screen.

### Other Input
- **Controller**: any controller SDL3 supports, with rumble. The touch buttons hide while a controller is in use. Controllers that some phones pass on as a virtual device (a DualShock 4 on MIUI, for example) work too.
  - **Stick Deadzone and Sensitivity**, under *Touch Controls → Controller*, for each stick: a radial dead zone from 0% to 50% of the travel (0% leaves the game's own), past which the travel runs from rest to full again, scaled by a sensitivity from 0.1 to 3.0.
- **Keyboard and Mouse**, with rebindable controls, mouse sensitivity and invert look. In play the mouse is captured as on a PC: no pointer on the screen, and the view keeps turning past the screen's edge.

### General
- **Language**: English, French, German, Italian or Spanish where the disc has them, plus Indonesian for the port's own text.
- **Skip Intros** skips the intro logos.
- **Audio Output**: automatic, mono, stereo or 5.1 surround.
- **Check for Updates**: the app checks GitHub for a new release each time it opens. This button checks right away.
- **Background Pause**: the game and its sound pause while the app is in the background.

### Cheats
- **Weapons**: infinite ammo, infinite grenades, rapid fire (2x to 4x) and no recoil.
- **Player**: god mode and one-hit kills.
- **Game**: game speed from 25% to 200%.

## System Requirements
- **Android** 10 or newer, arm64.
- **GPU** with a Vulkan 1.1 driver, for example Adreno 6xx or newer, Mali-G7x or newer, or Xclipse. Drivers without dynamic rendering or timeline semaphores are supported.
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

### 5. Build the Control Center preview (any OS)
The Control Center can be built as a standalone preview APK, `com.racas.black.preview`, without Windows, the NDK, the recompiled game or any game data:
```
./gradlew -PpreviewOnly :uipreview:assembleRelease
```
The APK is at `uipreview/build/outputs/apk/release/uipreview-release.apk`. It runs against demo settings: the screens, theme and layout editor work, but **Play** only explains that the preview does not include the game. It installs beside the full app.

Pushing a tag that starts with `preview-v` runs `.github/workflows/preview.yml`, which builds the APK and publishes it as a prerelease. Tag releases require all four signing secrets: `PREVIEW_KEYSTORE_BASE64`, `PREVIEW_KEYSTORE_PASSWORD`, `PREVIEW_KEY_ALIAS` and `PREVIEW_KEY_PASSWORD`. A manual workflow run without them still produces a debug-signed artifact for testing, but it is not released.

## Credits
- [XboxXBE-Recomp](https://github.com/TGP482/XboxXBE-Recomp) by TGP482: the static recompiler and the portable runtime (Xbox kernel, XDK libraries and Direct3D 8) this port is built on.
- [SDL](https://github.com/libsdl-org/SDL), [glslang](https://github.com/KhronosGroup/glslang), [toml++](https://github.com/marzer/tomlplusplus) and [Dear ImGui](https://github.com/ocornut/imgui).
- [bagasromadon](https://github.com/bagasromadon): the touch controls' button skin.
- [xemu](https://github.com/xemu-project/xemu) and [Cxbx-Reloaded](https://github.com/Cxbx-Reloaded/Cxbx-Reloaded), which XboxXBE-Recomp used as references for Xbox hardware and API behavior.

*Black* is a trademark of its respective owners. This project is not affiliated with or endorsed by them.
