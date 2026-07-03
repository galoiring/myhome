# My Home

Native Android WebView wrapper around the LAN smart-home dashboard at
`http://192.168.68.75:8090`. Sideload-only (no Play Store) — built for a
Samsung Galaxy Tab wall display.

The app just loads the live dashboard URL in a fullscreen WebView, so
dashboard updates never require rebuilding the app.

## Features

- Fullscreen, edge-to-edge WebView with a themed status/nav bar
  (`#eef1f6` light / `#0f1216` dark, following system theme)
- Splash screen using the dashboard's app icon
- Hardware back button navigates WebView history, exits app when there's none
- Keeps the screen on while the app is in the foreground
- Offline/error screen with a native Retry button (dashboard is LAN-only,
  plain HTTP — cleartext traffic is allowlisted only for `192.168.68.75`
  via `app/src/main/res/xml/network_security_config.xml`)

## Requirements

- App name: **My Home**, package `com.gal.myhome`
- minSdk 26, targetSdk/compileSdk 36
- JDK 17
- Android SDK with platform 36 and build-tools 36.x

## Build

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=$HOME/Library/Android/sdk   # adjust if different
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### Signing

The release build is signed with a locally generated keystore that is
**intentionally not committed** (this repo is public). You need:

- `keystore/myhome-release.jks`
- `keystore.properties` (storeFile/storePassword/keyAlias/keyPassword)

at the repo root, both gitignored. Regenerate them with:

```bash
mkdir -p keystore
keytool -genkeypair -v -keystore keystore/myhome-release.jks -alias myhome \
  -keyalg RSA -keysize 2048 -validity 10950 \
  -dname "CN=My Home, O=Gal, C=IL"
```

then write `keystore.properties`:

```
storeFile=keystore/myhome-release.jks
storePassword=<password used above>
keyAlias=myhome
keyPassword=<same password — PKCS12 requires store/key passwords to match>
```

**Back these up.** Losing them means future rebuilds can't update the app
in place on the tablet — it would need to be uninstalled and reinstalled
(losing WebView local storage/cache).

### Known issue: corporate/local TLS interception breaks first-time Gradle sync

If `./gradlew assembleRelease` fails to resolve `com.android.application`
from `dl.google.com` with a silent failure (no clear SSL error, just
"could not resolve plugin artifact"), some network security/filtering tool
on your Mac is intercepting TLS to `dl.google.com` with a custom root CA
that's trusted by curl/Safari (via the macOS System keychain) but not by
Java's own `cacerts` trust store. Check `security find-certificate -a -p
/Library/Keychains/System.keychain | grep -i "forward trust"` — if that
matches, that's the culprit. Either build on a network/machine without that
interception, or import the CA chain into the JDK's `cacerts`.

## Icon

`assets-src/icon-512.png` is the source icon (downloaded from the dashboard
at `/icon-512.png`); launcher mipmaps and the adaptive icon
(`app/src/main/res/mipmap-anydpi-v26/`, `drawable/ic_launcher_foreground*`)
are generated from it.

## Install on the tablet

Easiest, no cable: serve the built APK over LAN and download it in the
tablet's browser:

```bash
python3 -m http.server 8000 --directory app/build/outputs/apk/release
```

then on the tablet open `http://<this-machine's-LAN-IP>:8000/app-release.apk`,
download it, tap to install, and allow "install unknown apps" for the
browser when prompted.

Alternative: `adb install -r app/build/outputs/apk/release/app-release.apk`
over USB or Wireless debugging.
