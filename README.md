# Open TV — World Cup (Android TV App)

A modern, native **Android TV / Google TV** application that wraps the
streaming site
[`https://yinkyade.github.io/open_tv_site/world-cup`](https://yinkyade.github.io/open_tv_site/world-cup)
in a full-screen, remote-friendly, immersive WebView experience.

Written in **Kotlin**, built with **Android Studio / Gradle**, targeting
**Android TV 9 (API 28) and above**.

---

## ✨ Features

| Area | What's implemented |
|------|--------------------|
| **Platform** | Native Android TV, Kotlin, Leanback launcher, Google TV compatible, API 28+ |
| **UI** | Dark Material3 theme, full-screen immersive mode, custom splash screen + logo, large D-pad focus rings |
| **WebView** | JavaScript, DOM storage, HTML5 video, full-screen video, cookies/localStorage, hardware acceleration, screen-on lock |
| **Video** | HLS (`.m3u8`), embedded players, full-screen playback, no-sleep during playback |
| **Navigation** | BACK = previous page / exit full-screen, double-BACK to exit, MENU = settings panel, D-pad optimized |
| **Connectivity** | Live network detection, offline retry screen, auto-reload on reconnect |
| **TV** | TV banner, custom adaptive launcher icon, home-screen entry, 1080p & 4K friendly |
| **Performance** | Web content caching, fast splash → WebView handoff, R8 shrinking for small APK |
| **Security** | Trusted-domain navigation allow-list, blocked external redirects, SSL error handling |
| **Extras** | Landscape lock, hidden system UI, loading spinner, refresh button, last-page memory, single-variable URL config |

---

## 🗂 Project structure

```
opentv/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
├── keystore.properties.template      # copy → keystore.properties to sign
├── gradle/wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts              # dependencies + signing + release config
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml       # Leanback, banner, permissions, activities
        ├── java/com/opentv/worldcup/
        │   ├── AppConfig.kt          # ⭐ single place to change the URL
        │   ├── OpenTvApplication.kt  # app-wide cookie setup
        │   ├── SplashActivity.kt     # branded splash → MainActivity
        │   ├── MainActivity.kt       # WebView host, immersive, D-pad, settings
        │   ├── util/NetworkMonitor.kt
        │   └── web/
        │       ├── TvWebViewClient.kt    # security + errors + SSL
        │       └── TvWebChromeClient.kt  # HTML5 full-screen video + progress
        └── res/
            ├── layout/activity_main.xml, activity_splash.xml
            ├── values/colors.xml, strings.xml, themes.xml
            ├── drawable/   (logo, banner, icons, focus ring)
            ├── mipmap-anydpi-v26/  (adaptive launcher icon)
            └── xml/network_security_config.xml
```

---

## 📡 Live Channels (native HLS player + M3U)

Alongside the WebView, the app includes a **native Media3 / ExoPlayer** path for
direct HLS (`.m3u8`) streams, driven by an **M3U playlist**:

- Open it from the WebView's **MENU → Live Channels**.
- Channels are listed in a D-pad-navigable screen; selecting one opens the
  full-screen native player.
- In the player, **CHANNEL +/-** (or media next/prev) switches channels.

### Adding your own sources (important)

The app ships an empty-by-design template at
**`app/src/main/assets/playlist.m3u`** containing only public *test* streams.
Add **only sources you are legally entitled to use** (your own paid IPTV
subscription's M3U entries, official/free HLS streams, etc.):

```m3u
#EXTINF:-1 group-title="Sports",My Licensed Channel
https://my-provider.example/stream/master.m3u8
```

To use a **remote** playlist instead of the bundled file, set one variable in
`AppConfig.kt`:

```kotlin
const val PLAYLIST_URL = "https://my-provider.example/playlist.m3u"
```

> This project provides the playback **engine** only. It does not include or
> endorse any unauthorized streams of licensed content (e.g. World Cup
> broadcasts). You are responsible for the legality of the sources you add.

## 🔧 Changing the streamed site (future-proofing)

Open **`app/src/main/java/com/opentv/worldcup/AppConfig.kt`** and edit a single
constant:

```kotlin
const val START_URL = "https://your-new-site.example/page"
```

If the new site navigates to other top-level hosts, also add them to
`ALLOWED_HOSTS`. That's the only change required.

---

## 🛠 Build instructions

### A. Build with Android Studio (recommended)

1. Install **Android Studio** (Hedgehog or newer) with the Android SDK
   (API 34) and Android TV system image.
2. **File → Open** → select this `opentv/` folder.
3. Let Gradle sync finish (this also generates `gradle-wrapper.jar`).
4. Pick a device:
   - **Run on emulator:** Tools → Device Manager → create an **Android TV
     (1080p)** virtual device (API 28+), then press ▶ **Run**.
   - **Run on real TV:** enable Developer Options + ADB debugging on the TV,
     connect with `adb connect <tv-ip>`, then press ▶ **Run**.

### B. Build a debug APK from the command line

A prebuilt debug APK is already included at the project root:
**`OpenTV-WorldCup-debug.apk`** (ready to install — see below).

To rebuild it yourself, use the bundled helper script (sets JDK 17 + SDK env,
installs the needed SDK packages, then builds):

```bash
cd opentv
./build-apk.sh
# → app/build/outputs/apk/debug/app-debug.apk
```

Or invoke Gradle directly if your environment is already configured:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools \
./gradlew assembleDebug
```

#### Install on an Android TV / Google TV device

```bash
# 1. On the TV: Settings → System → About → click "Build" 7× to unlock
#    Developer options, then enable "Network debugging" / "USB debugging".
# 2. From your computer (replace with your TV's IP shown in Network debugging):
adb connect 192.168.1.50:5555
adb install -r OpenTV-WorldCup-debug.apk
# 3. The app appears on the Android TV home row with its banner.
```

> Note: Android TV requires **JDK 17** and Gradle **8.7** (AGP 8.5). The bundled
> `gradle-wrapper.jar` already pins Gradle 8.7, so `./gradlew` fetches the right
> version automatically — even if your system Gradle is newer.

---

## 📦 Producing a SIGNED RELEASE APK

1. **Create a keystore** (one-time):

   ```bash
   keytool -genkey -v -keystore release.keystore \
     -alias opentv -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Configure signing** — copy the template and fill in your values:

   ```bash
   cp keystore.properties.template keystore.properties
   # edit keystore.properties: storeFile, storePassword, keyAlias, keyPassword
   ```

3. **Build the signed release APK:**

   ```bash
   ./gradlew assembleRelease
   # → app/build/outputs/apk/release/app-release.apk  (signed & shrunk)
   ```

   If `keystore.properties` is absent, the release APK is built **unsigned**;
   you can sign it later with `apksigner`.

4. **(Optional) Build an Android App Bundle** for Play Store / Google TV:

   ```bash
   ./gradlew bundleRelease
   # → app/build/outputs/bundle/release/app-release.aab
   ```

5. **Install / verify:**

   ```bash
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```

---

## 📺 Publishing to Google Play (Android TV)

- The manifest already declares `LEANBACK_LAUNCHER`, an `android:banner`, and
  `touchscreen` as not required — the three things Play checks for TV.
- Upload the **`.aab`** to Play Console and opt the release into the
  **Android TV** form factor.
- Provide store assets: TV banner (320×180), screenshots (1920×1080), etc.

---

## 🎮 Remote controls

| Key | Action |
|-----|--------|
| **D-pad** | Navigate page elements / focusable buttons |
| **Center / Enter** | Select |
| **BACK** | Exit full-screen video → page back → (double-press) exit app |
| **MENU** | Open settings panel (Refresh / Home / Clear cache / Exit) |

---

## ⚠️ Notes & tuning

- **Cleartext streams:** `network_security_config.xml` permits HTTP because some
  HLS CDNs still serve over cleartext. Tighten it to specific domains if you
  prefer HTTPS-only.
- **Subresource hosts:** top-level navigation is restricted to `ALLOWED_HOSTS`,
  but third-party video/CDN subresources are allowed (streaming needs this).
  See `AppConfig.ALLOW_THIRD_PARTY_SUBRESOURCES`.
- **Icons/banner** are vector placeholders. Replace `ic_logo.xml` and
  `tv_banner.xml` with your branded artwork (banner must be 16:9, 320×180+).
```
