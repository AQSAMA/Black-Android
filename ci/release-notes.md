<p align="center">
  <img src="https://raw.githubusercontent.com/RanggaCasper/Black-Android/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="96" alt="Black">
</p>

<h1 align="center">Black for Android {{VERSION}}</h1>

<p align="center">
  An unofficial native Android port of the original Xbox version of <b>Black</b>,<br>
  statically recompiled with <a href="https://github.com/TGP482/XboxXBE-Recomp">XboxXBE-Recomp</a> and drawn with Vulkan.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-11%2B-3DDC84?logo=android&logoColor=white" alt="Android 11+">
  <img src="https://img.shields.io/badge/arch-arm64--v8a-blue" alt="arm64-v8a">
  <img src="https://img.shields.io/badge/Vulkan-1.1-AC162C?logo=vulkan&logoColor=white" alt="Vulkan 1.1">
  <img src="https://img.shields.io/badge/SDL-3-1D4F91" alt="SDL3">
</p>

> [!IMPORTANT]
> This release contains **no game assets**. You need the files from your own legally acquired copy of *Black* (USA or Europe release).

## 📦 Download

| File | Size | SHA-256 |
| --- | --- | --- |
| [`{{APK}}`](https://github.com/RanggaCasper/Black-Android/releases/download/{{TAG}}/{{APK}}) | {{SIZE}} | `{{SHA256}}` |

## 🚀 Installation

1. Install **`{{APK}}`** on your phone.
2. Open **Black** and allow **all files access** when it asks.
3. Copy the game into **`/storage/emulated/0/Black`**: the disc image (`.iso`), or `default.xbe` with its data folders.
4. The game starts as soon as it finds the files.

> [!TIP]
> If you don't grant all files access, put the game in `Android/data/com.racas.black/files/assets` instead. Settings, saves, logs and the shader cache are stored in `Android/data/com.racas.black/files`.

## ✨ Features

| | |
| --- | --- |
| 🖥️ **Display** | Render resolution 20–200%, FOV 45–140°, viewmodel FOV, unlocked frame rate, V-Sync, FPS counter, FMV scale |
| 🎨 **Graphics** | Anisotropic filtering, 2x / 4x / 8x draw distance, toggleable reload blur |
| 🎮 **Input** | Touch controls with a layout editor, gyroscope aiming, SDL3 controllers with rumble, keyboard and mouse |
| ⚙️ **General** | Skip intro, pause while in the background |

The settings are in the game's own options menu. Android's back button opens the pause menu.

## 📱 Requirements

- **Android** 11 or newer, arm64
- **GPU** with a Vulkan 1.1 driver that supports dynamic rendering and timeline semaphores (Adreno 6xx+, Mali-G7x+, Xclipse)
- **4 GB** RAM, **2 GB** free storage

## 🙏 Credits

Built on [XboxXBE-Recomp](https://github.com/TGP482/XboxXBE-Recomp) by **TGP482**, with [SDL](https://github.com/libsdl-org/SDL), [glslang](https://github.com/KhronosGroup/glslang), [toml++](https://github.com/marzer/tomlplusplus) and [Dear ImGui](https://github.com/ocornut/imgui).

---
