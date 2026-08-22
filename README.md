# App Lock

A fully offline app-locker for Android. No internet permission, no ads, no
analytics — every credential and setting stays only on your device inside
`EncryptedSharedPreferences` (AES-256, key held in the Android Keystore).

## Features
- **Lock any installed app** behind your choice of:
  - PIN
  - Pattern (custom-built 3x3 grid, no external library)
  - Password
  - Fingerprint / face unlock (via `BiometricPrompt` — same code path covers
    both; the device decides which biometric it offers)
- **Different app, different lock**: every locked app can either use the
  shared master lock, or you can tap **Custom lock** on that app's row to
  give it its own separate PIN/Pattern/Password + its own biometric toggle
- **The App Lock app itself is locked too** — opening it always demands the
  master credential first, so someone can't just disable your locks from
  inside the app
- Detects when a locked app is opened **from the home screen, Recents, or a
  notification** (via an optional Accessibility Service), not only when
  launched a certain way
- Search + a settings screen to change the master credential and toggle
  biometrics globally

## About "Hide" — please read this before relying on it
Hidden apps disappear only from **this app's own list** (toggle "Show hidden
apps" to bring them back). This is a deliberate, honest limitation, not a
bug: **a normal Android app cannot remove another app's home-screen icon.**
Only two things can do that on a non-rooted phone:
1. Being the **default launcher** itself (build your own home screen — a
   separate kind of project from this one)
2. Being **Device Owner** (an enterprise-style management mode, provisioned
   with a one-time `adb shell dpm set-device-owner ...` command — this still
   needs `adb`, so it isn't a purely no-PC/no-Termux path)

If real icon-hiding matters more to you than a standalone app-locker, the
launcher-based approach hides apps genuinely because the launcher controls
what's drawn on the home screen at all.

## Build
Same as before — open in Android Studio, or push to GitHub and let
`.github/workflows/build.yml` build the debug APK in the cloud (Actions tab
→ download the `AppLock-debug-apk` artifact once the run finishes green).

minSdk 26 (Android 8.0+).

## First run
1. Set your master PIN/Pattern/Password when prompted.
2. Tick **Lock** next to any app you want protected. Optionally tap
   **Custom lock** to give that one app its own separate credential.
3. Settings → Accessibility → enable **App Lock** so locks also catch apps
   reopened from Recents/notifications.
4. Settings (in-app, gear icon) → toggle fingerprint/face if you want a
   faster unlock alongside your credential.

## Limitations, honestly
- "Hide" is soft/in-app only (see above).
- The lock triggers on foreground-app change; it can't intercept something
  that happens entirely inside an already-open, already-unlocked app.
- Package name is `com.example.applock` — rename it before publishing.
