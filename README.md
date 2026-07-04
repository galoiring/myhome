# My Home

A native Android wall-panel dashboard for Homebridge, plus the tiny backend
that bridges Homebridge's local HAP API into a simple REST API the app (and
an optional web dashboard) can poll. Built for a Samsung Galaxy Tab mounted
in landscape as a fixed smart-home control panel, but the backend/app pair
works for any Homebridge setup.

```
app/       Android app (Kotlin + Jetpack Compose, Material 3)
server/    Backend (server.js) + web dashboard frontend (public/index.html)
```

## How it fits together

```
Homebridge (insecure-mode HAP API, localhost only)
        │
        ▼
server/server.js  ──  small REST API, LAN-only
        │                  GET  /api/accessories
        │                  POST /api/set
        │                  GET  /api/weather
        │                  GET/POST /api/settings
        │                  GET  /api/shelly, POST /api/shelly/set
        ▼
Android app  (and/or the web dashboard at server/public/index.html)
```

Homebridge's HAP API only listens on `127.0.0.1` and needs a PIN for every
request — `server.js` is what turns that into a plain LAN-reachable JSON API
the app can poll every few seconds, and adds a couple of things HomeKit
doesn't do out of the box (Shelly device control, weather, per-tile
names/groups/hidden state shared between every client).

## Quick start for Homebridge owners

1. **Enable Homebridge's insecure mode** — Homebridge UI → Settings → check
   "Homebridge Insecure Mode" (or run the process with `-I`), then restart
   Homebridge. Note the **setup PIN** shown in the Homebridge UI (also in
   `config.json`) — you'll need it below. Insecure mode is what lets a local
   script talk to the HAP API directly without a full HomeKit pairing
   handshake; it only listens on `127.0.0.1` by default, so it isn't exposed
   beyond the machine Homebridge runs on.
2. **Copy `server/` onto that same machine** (wherever Homebridge already
   runs — a Raspberry Pi, an Orange Pi, anywhere with Node.js):
   ```bash
   scp -r server/ youruser@your-homebridge-host:~/hb-dashboard
   ```
3. **Run it**, with your Homebridge PIN and (optionally) your location for
   the weather card:
   ```bash
   cd ~/hb-dashboard
   HAP_PIN="031-45-154" WEATHER_LAT="40.71" WEATHER_LON="-74.01" node server.js
   ```
   It listens on `:8090` by default (`PORT` to change it). To keep it running
   across reboots, adapt `server/hb-dashboard.service.example` into a systemd
   unit (`sudo systemctl enable --now hb-dashboard`).
4. **Point a browser at it** — `http://<that-host>:8090` serves the web
   dashboard directly, no app required.
5. **Or install the Android app** on a tablet (see [Install on the
   tablet](#install-on-the-tablet) below) and set the server URL in
   Settings to `http://<that-host>:8090`.

Tile names, groups, and hidden devices are configured from either client
(tap "Edit" on the web dashboard, or Settings → Tiles in the app) and are
stored server-side in `server/settings.json` (gitignored — it's your data,
not source code; starts empty and is created on first save), so the app and
web dashboard always agree.

### Backend environment variables

| Variable | Default | Meaning |
|---|---|---|
| `PORT` | `8090` | port the dashboard listens on |
| `HAP_HOST` | `127.0.0.1` | where Homebridge's insecure API listens |
| `HAP_PORT` | `51705` | Homebridge's insecure API port |
| `HAP_PIN` | *(required, no default)* | your Homebridge setup PIN |
| `WEATHER_LAT`, `WEATHER_LON` | example coordinates | for the weather card (open-meteo, no API key needed) |

### Shelly devices (optional)

The dashboard can also control Shelly Gen2+ devices directly over LAN
(outside of HomeKit). Add their IPs to `shellies` in `server/settings.json`
(or via `POST /api/settings`):
```json
{ "shellies": [{ "ip": "192.168.1.50" }] }
```

## App features

- Adaptive **no-scroll** grid: rooms group into their own rows, sparse rows
  don't stretch to fill the screen, and each tile has a configurable
  width (Small/Medium/Large) and height (Normal/Half — two same-width Half
  tiles pair into one stacked column)
- Tap a tile to toggle it — the tile tint (and a soft glow when active) is
  the state indicator, no separate switch
- Native controls: brightness/warmth/speed/position sliders, AC mode (as a
  compact header dropdown) with cool/heat steppers, discrete fan-speed
  segmented buttons, purifier modes, a drawn curtain/window visual, sensor
  readouts with a colored air-quality pill, weather strip with inside temp
- Direct Yeelight LAN control (bypassing Homebridge) and an on-demand RTSP
  camera live view (e.g. via Scrypted), independent of the backend above
- Night mode (scheduled black screen, tap to wake), in-app update checking
- Optimistic updates with a 5s hold (poll every 1–10s, configurable)
- Settings: server URL, poll rate, theme (system/light/dark, Material You or
  fixed accent), text size, clock format, tile sort/order/size/room, and
  tile management (rename/hide/group — stored on the server)

## Requirements

- App name: **My Home**, package `com.gal.myhome`
- minSdk 26, targetSdk/compileSdk 36, Kotlin 2.1, Compose (BOM 2025.05)
- JDK 17
- Android SDK with platform 36 and build-tools 36.x
- Backend: Node.js (no npm dependencies), on the same host as Homebridge

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
  -dname "CN=My Home, O=Your Name, C=US"
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
If you're building it just for yourself, grab the signed APK from this
repo's [Releases](../../releases) page instead of generating your own key.

### Known issue: corporate/local TLS interception breaks first-time Gradle sync

If `./gradlew assembleRelease` fails to resolve plugins from `dl.google.com`
with a silent failure, a network security/filtering tool on the Mac is
intercepting TLS with a custom root CA trusted by the System keychain but
not by Java's `cacerts`. Build on a clean network or import the CA chain
into the JDK. Relatedly, reverse-DNS lookups are black-holed on some
networks — Python's `HTTPServer` hangs in `socket.getfqdn()` at startup
unless `server_bind` is overridden (see `server/preview-proxy.py`).

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
  "versionCode": 6,
  "versionName": "1.0.0",
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

## Web dashboard (`server/public/`)

`server/public/index.html` is a no-scroll adaptive grid with tap-to-toggle
tiles and a weather header strip — the same design as the Android app, just
served directly by `server.js`, no app required. Preview it locally against
a live backend with:

```bash
python3 server/preview-proxy.py   # http://localhost:8091, proxies /api to UPSTREAM
```

(`UPSTREAM` env var to point at a different backend host; defaults to this
project's original deployment.)
