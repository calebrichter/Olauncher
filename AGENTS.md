# Olauncher Flow Codex Guidelines

This document outlines the architecture, coding practices, and verification steps for developer agents working on the **Olauncher Flow** codebase.

---

## 1. Project Overview

Olauncher Flow is a custom fork of Olauncher (a minimalist Android launcher) that implements strict time-based and constraint-based application blocking ("flows").

### Core Rules
- **Whitelists**: Certain core apps are always allowed (e.g. Phone, Messages, Engage) and hidden from the main favorites list.
- **Phases**: The day is split into phases (Morning, Noon, Evening, Free, Night) loaded from a local configuration file.
- **Lock/Unlock Conditions**: A phase may require a trigger app (e.g., Bible app, Telegram) to be used for a minimum duration (e.g., 10 minutes) before general apps are unlocked.
- **Telegram Bypass**: An active Telegram Bot (`@olflowbot`) listener polls for bypass commands. If a message is received in a designated group chat, all apps are unlocked for a temporary period (e.g., 15 minutes).
- **Hiding & Blocking**: Blocked apps are completely hidden from the home screen, app drawer list, and search results. If launched through other means, the launch is intercepted.

---

## 2. Key Code Components

All customized flow logic lives in the package `app.olauncher.flow`:

- **[FlowConfig.kt](file:///Users/calebric/code/olauncher-flow/app/src/main/java/app/olauncher/flow/FlowConfig.kt)**: Defines the JSON structures and loads configurations from local storage:
  - Configuration Path: `/sdcard/Android/data/app.olauncher.flow.debug/files/olauncher-flow.json` (for debug builds).
- **[FlowEngine.kt](file:///Users/calebric/code/olauncher-flow/app/src/main/java/app/olauncher/flow/FlowEngine.kt)**: Handles the state machine logic, checking active phases, tracking trigger app foreground usage, evaluating if a package is allowed, and resolving remaining bypass durations.
- **[FlowApplication.kt](file:///Users/calebric/code/olauncher-flow/app/src/main/java/app/olauncher/flow/FlowApplication.kt)**: Plugs into the Android Application lifecycle, initializing the `FlowEngine` and starting a background scheduler to poll the Telegram Bot API every 15 seconds.

### UI Integration Hooks

- **[MainViewModel.kt](file:///Users/calebric/code/olauncher-flow/app/src/main/java/app/olauncher/MainViewModel.kt)**:
  - `launchApp` / `launchShortcut`: Intercepts attempts to launch blocked applications.
  - `getAppList` / `getPrivateSpaceAppList`: Filters out blocked packages so they don't show up in search or the drawer.
- **[HomeFragment.kt](file:///Users/calebric/code/olauncher-flow/app/src/main/java/app/olauncher/ui/HomeFragment.kt)**:
  - Periodically refreshes the active status label `tvFlowStatus` (which displays phase info/countdowns).
  - Hides blocked favorite apps (`View.GONE`) rather than deleting them from preferences, restoring them when allowed.
- **[SettingsFragment.kt](file:///Users/calebric/code/olauncher-flow/app/src/main/java/app/olauncher/ui/SettingsFragment.kt)**:
  - Integrates the **Flow Schedule** card and detail view dialog overlay.
  - Resolves package names to friendly app names dynamically via `PackageManager`.

---

## 3. Style & Layout Guidelines

- **Minimalist Aesthetic**: Olauncher utilizes pure black/white/gray and transparent backgrounds. Never introduce colorful card backgrounds, heavy shadows, or complex gradients.
- **Overlay Customization**: Instead of default Android `AlertDialogs` which clash with the launcher, use full-screen overlay dialogs defined directly in layouts (such as `accessibilityLayout` or `flowScheduleLayout`) and dim the background ScrollView using:
  ```kotlin
  binding.scrollView.animateAlpha(if (show) 0.5f else 1f)
  ```
- **Responsive Layouts**: Always modify layout files in both `app/src/main/res/layout/` (portrait) and `app/src/main/res/layout-land/` (landscape) to maintain package compile safety and prevent nullability in View Binding.

---

## 4. Verification Protocols

### A. Environment Paths
For command line compilation on the Mac Mini host:
- **Java Home**: `JAVA_HOME=/opt/homebrew/opt/openjdk@21`
- **Android Home**: `ANDROID_HOME=/opt/homebrew/share/android-commandlinetools`
- **ADB Command**: `/opt/homebrew/bin/adb`

### B. Commands
1. **Running Unit Tests**:
   ```bash
   JAVA_HOME=/opt/homebrew/opt/openjdk@21 ANDROID_HOME=/opt/homebrew/share/android-commandlinetools ./gradlew test
   ```
2. **Assembling Debug Build**:
   ```bash
   JAVA_HOME=/opt/homebrew/opt/openjdk@21 ANDROID_HOME=/opt/homebrew/share/android-commandlinetools ./gradlew assembleDebug
   ```
3. **Deploying to target device**:
   ```bash
   /opt/homebrew/bin/adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
4. **Launching Olauncher Flow**:
   ```bash
   /opt/homebrew/bin/adb shell am start -n app.olauncher.flow.debug/app.olauncher.MainActivity
   ```
5. **Checking Error Logs**:
   ```bash
   /opt/homebrew/bin/adb logcat -d '*:E' | grep "app.olauncher"
   ```
