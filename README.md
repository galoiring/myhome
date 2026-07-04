# My Home

Native Android app (Kotlin + Jetpack Compose, Material 3) for a LAN
smart-home dashboard. Sideload-only (no Play Store) — built for a Samsung
Galaxy Tab wall display mounted in landscape.

The app talks directly to the dashboard server's HTTP API (default
`http://192.168.68.75:8090`, changeable in Settings):

- `GET /api/accessories` — HomeKit accessory tree (via Homebridge)
- `GET /api/shelly`, `POST /api/shelly/set` — Shelly devices
- `GET /api/weather` — open-meteo payload
- `GET/POST /api/settings` — tile names / groups / hidden (shared with the
  web dashboard, so both stay in sync)
- `POST /api/set` — write HomeKit characteristics

`dashboard/` holds the redesigned **web** dashboard (`index.html`) served by
the Pi, plus `preview-proxy.py` to preview it locally against live data.

## App features

- Adaptive **no-scroll** grid: picks the best columns×rows split for the
  screen so every tile fits; falls back to a scrolling grid in portrait
- Tap a tile's header to toggle it — the tile tint is the state indicator
- Native controls: brightness/warmth/speed/position sliders, color swatches,
  AC mode segmented buttons + cool/heat steppers, purifier modes, chips for
  secondary switches, sensor readouts, weather strip with inside temperature
- Optimistic updates with a 5s hold (poll every 1–10s, configurable)
- Settings screen: server URL, poll rate, theme (system/light/dark),
  Material You dynamic color or fixed accent, text size, clock format,
  weather/clock visibility, tile sort, keep-screen-on, fullscreen, haptics,
  and tile management (rename / hide / group — stored on the server)
- Splash screen, edge-to-edge, keeps screen on while in the foreground

## Requirements

- App name: **My Home**, package `com.gal.myhome`
- minSdk 26, targetSdk/compileSdk 36, Kotlin 2.1, Compose (BOM 2025.05)
- JDK 17
- Android SDK with platform 36 and build-tools 36.x

## Build

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools   # adjust if different
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk` (R8-minified, signed)

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
in place on the tablet — it would need to be uninstalled and reinstalled.

### Known issue: corporate/local TLS interception breaks first-time Gradle sync

If `./gradlew assembleRelease` fails to resolve plugins from `dl.google.com`
with a silent failure, a network security/filtering tool on the Mac is
intercepting TLS with a custom root CA trusted by the System keychain but
not by Java's `cacerts`. Build on a clean network or import the CA chain
into the JDK. Relatedly, reverse-DNS lookups are black-holed on this
network — Python's `HTTPServer` hangs in `socket.getfqdn()` at startup
unless `server_bind` is overridden (see `dashboard/preview-proxy.py`).

## Icon

`assets-src/icon-512.png` is the source icon; launcher mipmaps and the
adaptive icon (`app/src/main/res/mipmap-anydpi-v26/`,
`drawable/ic_launcher_foreground*`) are generated from it.

## Install on the tablet

First install (or if the in-app updater can't reach the server): serve the
built APK over LAN and download it in the tablet's browser:

```bash
python3 -m http.server 8000 --directory app/build/outputs/apk/release
```

then on the tablet open `http://<this-machine's-LAN-IP>:8000/app-release.apk`,
download it, tap to install, and allow "install unknown apps" for the
browser when prompted. Because the signing key is unchanged, it updates the
existing install in place.

### In-app updates

After the first install, Settings → Updates checks a small `update.json`
manifest served next to the APK and can download + trigger the install
itself (no browser round-trip). **Every time you build a new release**,
bump `versionCode`/`versionName` in `app/build.gradle` and drop an
`update.json` next to the APK before serving it:

```json
{
  "versionCode": 5,
  "versionName": "2.3",
  "url": "http://<this-machine's-LAN-IP>:8000/app-release.apk",
  "notes": "short changelog line"
}
```

The app checks this once on launch and via the "Check now" button in
Settings; if `versionCode` is higher than the running build, it downloads
the APK (via `REQUEST_INSTALL_PACKAGES` + a `FileProvider`) and opens the
system installer. The first update still needs the one-time "install
unknown apps" grant, this time for My Home itself rather than the browser.

Alternative: `adb install -r app/build/outputs/apk/release/app-release.apk`
over USB or Wireless debugging.

## Web dashboard (`dashboard/`)

`dashboard/index.html` is the redesigned page for the Pi's web server
(no-scroll adaptive grid, tap-to-toggle tiles, weather header strip). Deploy
by copying it over the served file on `192.168.68.75`. Preview locally with:

```bash
python3 dashboard/preview-proxy.py   # http://localhost:8091, proxies /api to the Pi
```
