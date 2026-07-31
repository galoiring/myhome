# Improvement roadmap (2026-07-13)

Context that shapes every item below: **a newborn just joined the home.** The
panel's job shifts from "control everything" to "glance at the baby room,
operate one-handed while holding a baby, and never light up / make noise at
the wrong moment." The old PLAN.md polish items all shipped (df2df7f); this
plan is the next arc.

Key code touchpoints:
- Tiles & layout: `app/src/main/java/com/gal/myhome/ui/DashboardScreen.kt`
- Tile building & sizing: `app/src/main/java/com/gal/myhome/DashboardViewModel.kt`
  (camera tiles ~line 573; sensor split ~line 597; `setTileWidth/Height` ~365)
- Defaults & prefs: `app/src/main/java/com/gal/myhome/data/Prefs.kt`
  (`TileWidth` S/M/L, `TileHeight` NORMAL/HALF, `defaultTileSizes` ~line 34)
- Backend: `server/server.js` (history sampler ~line 94, doorbell relay ~313)
- Design north star: `design/redesign-mockup.html` (greeting header, room
  chips, scenes row, trend-aware tiles)

Each item is independently shippable; release flow is unchanged (see
PLAN.md "Release & deploy" — bump versionCode/Name, `gh release create`,
update BOTH update.json manifests).

---

## Status (updated 2026-07-26)

Shipped since this plan was written: **1.1** (doorbell defaults to a small
full-height block, +15 % width factor), **1.2** (nursery hero tile — Large,
comfort bands on value + icon circle, humidity pill, sparkline; since
v1.1.12 it stacks with the Living Room reading), **1.5** / **2.1** (night
dark theme + scheduled `screenBrightness` floor with touch-to-restore, in
MainActivity), and part of **2.4** (moon pill at 48 dp in v1.1.10).

Still open: **1.3**, **1.4**, **2.2**, **2.3**, rest of **2.4**, **2.5**,
all of Phase 3.

Phases 0, 4 and 5 below were added 2026-07-26 after an outage that took out
the Living Room and Air Quality tiles for a day. Both failures were real,
but the panel showed neither — it displayed a confident `0 µg/m³ · Good`
for an unreachable purifier and a frozen 27.1 °C for a dead feed. That's
the thread running through the new work: **the panel must not state
something it doesn't know.**

---

## Phase 0 — correctness debt (do first)

Each item below is a verified defect, not a hypothesis.

**All of Phase 0 shipped in v1.1.14 (2026-07-31)**, along with 3.8. Kept here
with the diagnosis intact, because the reasoning is what makes the fixes
reviewable later.

### 0.1 Settings edits clobber other clients — SHIPPED v1.1.14
`serverSettings` is fetched once in `init` (DashboardViewModel.kt:195) and
never refreshed. `editSettings` (:450) mutates that possibly-hours-old copy
and POSTs the whole document, so any change made meanwhile on the web
dashboard or a second client is silently reverted:

```
hide a device on the web dashboard → rename any tile on the tablet → hide is gone
```

The README promises the opposite ("the app and web dashboard always
agree"). The server-side merge added on 2026-07-26 does **not** cover this
— the app does send `names`/`hidden`, just stale values.

Priority note (2026-07-31): this was originally called the highest-value item
on the assumption that the app and the web dashboard both get used. They
don't — the Android panel is the only client of this backend, and the phones
in the house are HomeKit. That makes this latent rather than active, and it
shipped mainly because the fix is three lines.

- Fix: re-read `/api/settings` immediately before applying an edit, apply
  the mutation to the fresh copy, then POST. Cheap and sufficient at this
  scale (single-writer-at-a-time, human speed).
- Optional follow-up: refresh settings on the slow loop (alongside
  `historyLoop`) so a rename made elsewhere shows up without a restart.
- Effort: S.

### 0.2 History records unreachable devices as real zeros — SHIPPED v1.1.14
`sampleHistory` (server.js:189) stores any numeric temp/humidity/PM2.5 with
no `StatusActive` check and no hidden-device filter. Measured on the live
`history.json`:

| series | state |
|---|---|
| `Heater|temp`, `Heater|humidity` | 2015 flat `0`s — device unplugged *and* hidden (~25 % of the file) |
| `Mi Air Purifier|pm25` | ~264 zero samples across the 07-25 outage |

So the Air Quality sparkline draws a confident flat line through a
22-hour hole. The v1.1.13 app fix does **not** reach this — sparklines read
history, not the live characteristic, so the falsehood is baked into the
file.

- Fix: skip the sample when `StatusActive` (`75`) is false — judged **per
  accessory, not per service**. The first attempt checked only the service
  carrying the reading, and `Heater|temp` kept recording zeros: an unreachable
  heater also exposes a *thermostat* service, which carries a temperature but
  no `StatusActive` of its own, so it filled the same series straight back in.
  If any service on an accessory reports the flag and all of them are false,
  skip the whole accessory. Same semantics as the app's `notReporting`.
- Effort: S. Pairs with 4.2 (which makes the resulting gaps render honestly).

### 0.3 The whole 7-day history is fetched every 5 min for 24 h sparklines — SHIPPED v1.1.14
`historyLoop` (DashboardViewModel.kt:305) pulls the full dump — 307 KB
today, growing with every sensor added — and the tiles then discard ~86 %
of it client-side via the `dayAgo` filter. `/api/history` (server.js:434)
takes no range parameter.

- Fix: `GET /api/history?hours=24` for the tile sparklines; keep the full
  dump for the history sheet only. Add the param server-side first so old
  clients keep working.
- Effort: S.

### 0.4 Yeelight polling runs at a third of the device quota — SHIPPED v1.1.14
`YeelightClient`'s own header says *"the device quota is ~60
commands/minute"*, and `pollLoop` (DashboardViewModel.kt:259) opens a fresh
TCP connection to every bulb every `pollSeconds` (default 3 s) — 20/min per
bulb purely for state. These bulbs already have a track record of wedging
until Homebridge restarts, so a third of the budget spent on polling is
thin headroom.

- Fix: give bulbs their own slower cadence (10–15 s) rather than sharing
  the HAP poll interval; optionally keep one socket per bulb alive.
- Sliders are fine — they commit on `onDragEnd` / `onValueChangeFinished`,
  not per frame. Verified.
- Effort: S.

### 0.5 `indoorTemp()` has no validity check — SHIPPED v1.1.14
`indoorTemp()` (DashboardViewModel.kt:486) takes the first thermostat's
`CurrentTemperature` with no `StatusActive` or sanity test. It happens to
be right today only because the AC precedes the Heater in the accessories
array; hide the AC or let Homebridge reorder and the hidden, unplugged
Heater's **0 °C** becomes the header's "inside" temperature.

- Fix: skip services reporting `StatusActive` false, and ignore a reading
  of exactly 0 when no other candidate exists.
- Effort: XS.

## Phase 1 — UI improvements

### 1.1 Shrink the front-door doorbell tile  ← requested
The tile mostly shows a stale cached frame (by design — Ring battery), so a
full-size card is wasted space. Purpose-built compact form instead of just
relying on the existing S/Half settings (at HALF the current body cramps the
snapshot under the header):

- New compact `DoorbellTileBody` layout for HALF height: small 16:9
  thumbnail on the start side, name + "2 h ago" age text beside it, ring
  icon accent. Tap behavior unchanged (peek popup).
- Default the doorbell camera to `SMALL` width + `HALF` height in
  `defaultTileSizes` (Prefs.kt) so it stacks under another half tile and
  stops claiming a full cell. User can still resize in Settings.
- Stretch option (can defer to Phase 2): a "chip mode" — no tile at all,
  just a doorbell chip in the header (`🔔 2 h ago`) that opens the peek.

### 1.2 Baby Room hero tile
Make the baby room the most glanceable thing on the wall:

- Promote to `LARGE` width by default.
- Comfort-band coloring on the value + icon circle: temp green in
  18–22 °C, amber outside, red past 16/26; humidity pill green in 40–60 %.
  Reuse the `airQualityPillColor` threshold pattern.
- Show temp + humidity + (if the purifier lives there) PM2.5 in one card so
  no second look is needed. Sparkline already exists — keep it.

### 1.3 Trend-aware sensor tiles (from the mockup)
Small ▲/▼ + delta since 1 h ago next to sensor readings (history data is
already on the server, 5-min samples). Answers "is the nursery warming up?"
without opening the history popup.

### 1.4 Greeting/summary header (from the mockup, incremental)
Adopt just the header band first: weather (exists) + status chips like
`Baby room 21.4° ✓` / `3 lights on` / `🔔 2 h ago`. Chips double as the
home for doorbell "chip mode" (1.1) and alerts (3.2). Skip room filter
chips for now — the grid is already room-grouped.

### 1.5 Night-glance contrast pass
In the night dark theme, fade "off" tiles further and let the baby-room
tile + moon pills be the brightest elements, so a 3 a.m. glance from the
hallway reads instantly.

---

## Phase 2 — UX improvements

### 2.1 Nursery-safe panel brightness ("wall-dim")
A bright tablet in a hallway wakes babies. Add a scheduled auto-dim: after
a configurable hour, drop screen brightness to a floor (WindowManager
attrs) on top of the existing night dark theme; any tap restores full
brightness for N seconds. Settings: schedule + dim level.

### 2.2 Nap mode — doorbell must not blast the screen
Today a ring auto-pops the full-screen live view (DashboardScreen.kt
~258–267). With a sleeping baby that's exactly wrong. Add a **Nap mode**
toggle (header chip): while on, a ring shows only a quiet inline banner/chip
("🔔 Someone's at the door — tap to peek") with no popup, no flash-to-full-
brightness. Nap mode also feeds scenes (3.1) and quiet hours (3.5).

### 2.3 One-thumb quick bar
Holding a baby means operating one-handed, often from an angle. Pin the 3–4
most-used actions (baby moonlight pill, purifier toggle, sound-machine
outlet, Good Night scene) into a fixed quick bar at the bottom edge —
reachable without hunting the grid. Configurable in Settings.

### 2.4 Touch-target audit
Pills/segments are 34 dp; bring interactive targets to ≥48 dp where the
layout allows. One-handed + no-look taps miss small targets.

### 2.5 Combined baby-room history chart
History popup: overlay temp + humidity (and PM2.5 when present) for one
room on a single chart with dual scale, instead of one metric at a time.
"Was the nursery dry last night?" becomes one look.

---

## Phase 3 — Features

### 3.1 Scenes (server-side, buttons in the header row)
`POST /api/scene/:name` on server.js executes a list of HAP/Shelly sets;
app renders a scenes row (mockup already styles it). Starter set:
- **Night feed** — baby moonlight on, hallway light 10 %, everything else
  untouched, panel stays dim.
- **Nap time** — nursery curtain closed, purifier to quiet/auto, sound-
  machine outlet on, Nap mode (2.2) enabled.
- **Good night** — all lights off except moonlights, curtains closed,
  panel wall-dim.
- **Wake up** — curtain open, purifier normal, Nap mode off.
Scene definitions live in `settings.json` on the Pi (per-home, not in git).

### 3.2 Baby-room comfort alerts
Server-side thresholds evaluated in the existing history sampler: temp out
of 18–22 °C for >15 min, humidity <35 %, PM2.5 >35. Expose `GET
/api/alerts`; app shows a persistent amber header chip + optional gentle
tablet chime (suppressed in Nap mode/quiet hours). Thresholds configurable
via `/api/settings`.

### 3.3 Sound-machine timer
If the white-noise machine sits on a Shelly/HomeKit outlet: "on for 45 min"
timer on that outlet tile (server-side timer so it survives app restarts).
Rolled into the Nap time scene.

### 3.4 Nap / feed stopwatch card
Deliberately simple (not a baby-tracker app): one card with "Start nap" /
"Start feed" → shows elapsed ("Nap · 32 min") and keeps the last few
entries for the day. State on the server so phones/web see it too.

### 3.5 Quiet-hours automation
Server cron window (e.g. 20:00–07:00): purifier to quiet, panel wall-dim
on, Nap-mode-style doorbell handling. Single switch in Settings; scenes
can override.

### 3.6 Doorbell ring log
Server keeps the last N rings with their cached snapshots; app shows a
small "while you were napping" list in the peek popup. Complements the
shrunken tile (1.1) — history replaces the need for a big always-visible
frame.

### 3.7 (Later) Away notifications
Push baby-room alerts (3.2) to phones when nobody's near the panel — e.g.
via a self-hosted ntfy on the Pi. Only worth it once 3.2 proves useful.

### 3.8 Tesla battery in the header — SHIPPED v1.1.14  ← requested
TeslaMate already runs on the same Orange Pi, so the panel can show the Model
3's charge next to the weather without touching Tesla's API or the cloud.

Verified live on the Pi (`teslamate-mosquitto`, topics under
`teslamate/cars/1/`):

| topic | sample |
|---|---|
| `battery_level` | `79` |
| `plugged_in` | `false` |
| `charging_state` | `Disconnected` |
| `rated_battery_range_km` | `312.42` |
| `state` | `online` |
| `time_to_full_charge` | `0.0` |

Two routes to it, and the choice matters:

- **`teslamate-api`** (the `tobiasehlert/teslamateapi` container, reachable on
  `127.0.0.1:8080` behind a Caddy basic-auth gate, with a bearer token in that
  container's env — do **not** copy the token into this repo, it is public).
  Plain HTTP, which `server.js` already speaks.
- **MQTT** on `127.0.0.1:1883` — the live push path, but the `mqtt` package
  would be `server.js`'s **first npm dependency ever** (it is currently pure
  Node built-ins: http/https/fs/path/child_process/url). Not worth losing that
  property for one header chip.

So: poll the REST API on the existing weather-ish cadence, cache it, and serve
`GET /api/tesla` → `{ battery, pluggedIn, chargingState, rangeKm, state, ts }`.
Both listeners are localhost-only, so this stays a LAN feature — no Tesla
credentials in the app, and nothing new exposed on the network.

App side: a chip in `WeatherStrip` (DashboardScreen.kt:351), which already
carries indoor temp and power draw. Battery % + a Tesla mark, tinted like the
other status bands (green / amber / red), with a bolt when `plugged_in`.

Two things to get right:
- **Honesty (see 4.1):** a sleeping Tesla stops reporting, and a stale 79 %
  shown as if live is exactly the failure this plan exists to remove. Use
  `state` (`online` / `asleep` / `offline`) plus the reading's age — dim the
  chip and show the age when the car is asleep rather than implying it's live.
- **Asset:** needs a Tesla mark in the drawables. A generic car glyph avoids
  the trademark question entirely if this ever goes beyond this house.

---

## Phase 4 — Trust & legibility (added 2026-07-26)

The panel's failure mode today is confident wrongness. These three items
make "I don't know" a first-class state everywhere a reading appears.

### 4.1 Per-reading freshness, not just a server-level offline flag
The only staleness signal is a small lowercase `offline` in the header
(DashboardScreen.kt:353) — binary and server-wide. Devices fail
*independently*: on 07-25 the server was healthy the whole time while two
feeds died. A frozen reading is pixel-identical to a live one.

- Each sensor tile knows its expected cadence: HAP-polled ≈ poll interval,
  pulled sensors 2 min, pushed sensors up to `PUSH_STALE_MS` (6 h). When the
  newest sample is older than a small multiple of that, dim the hero number
  and append a relative age — `25.3° · 4 h ago`.
- Reuse the `SensorUi.live` / `TileUi.notReporting` plumbing added in
  v1.1.13; this is the age half of the same idea (a device can be reachable
  and still be feeding stale data — the Living Room case, where the reading
  sat frozen at 27.1 °C for 6 h before aging out).

### 4.2 Honest gaps in sparklines and the history sheet
Depends on 0.2. Once the server stops writing zeros for unreachable
devices, the client must render a **gap** rather than interpolating across
it — otherwise a 22-hour hole becomes a clean straight line between two
real points and the lie just moves. Break the polyline whenever consecutive
samples are more than ~3 sample intervals apart (`HistoryView.kt`, plus the
tile `Sparkline`).

### 4.3 Say when a command didn't land
`sendChars`, `toggleTile` and `setYeelight` all swallow failures with
`catch (_: Exception) {}` on the theory that "next poll restores truth".
What the user sees is a tile that obeys, then silently snaps back 5 s later
(`TOUCH_HOLD_MS`) — indistinguishable from a mis-tap.

- On a failed set: drop the override immediately, flash the tile border in
  the error colour once, and show a one-line snackbar naming the device.
- Highest value for Yeelights, which fail wholesale when a DHCP lease
  moves, and where `setYeelight` currently ignores the result entirely.

---

## Phase 5 — Interaction & layout (added 2026-07-26)

### 5.1 Long-press a tile to configure it
Every per-tile setting — rename, hide, room, width, height, order — lives
only in the 959-line `SettingsScreen` (Tiles section, ~:386). But this is a
wall-mounted panel: you are standing in front of the tile you want to
change. Long-press → compact sheet with room / size / rename / hide and
move left-right.

The ViewModel already exposes everything needed (`setRoom`,
`setTileWidth`, `setTileHeight`, `moveTile`, `renameTile`,
`setTileHidden`), so this is UI-only. Biggest ergonomic win per line of
code in the whole list.

### 5.2 Make the row layout declarative before adding to it
`RoomGroupedGrid` now carries five interacting heuristics —
`groupIntoRows` (:532), `bigSingleRoom`, `packRow`'s `allowNormalStack`
(:592), `widthFactor`, and packed-width coalescing against a hardcoded
`maxRowUnits = 11f` (:642). Every release from v1.1.10 to v1.1.12 touched
this code, and the misplaced Air Quality tile fixed in v1.1.13 was a
symptom of it: a derived tile fell out of its room because room assignment
is keyed by tile id.

- Let a room own an explicit row spec (ordered columns, each 1–2 tiles),
  with today's heuristics as the fallback for tiles the user hasn't placed.
- Pairs naturally with 5.1 — long-press editing is how you'd author a spec
  without a config file.
- Do this *before* the next layout feature, not after.

### 5.3 Fold pills and sub-labels into the density scale
`Density` (COMPACT/DEFAULT/LARGE) scales tiles, but status pills and
subtitles stay `labelSmall`/`labelMedium` regardless. From across a room
they're decorative rather than readable — and the pills are exactly where
the comfort bands and the new "No data" state live. Scale them with
density (and consider a floor size in the night theme, per 1.5).

### 5.4 Deferred: trend deltas (existing 1.3) now depend on 4.2
A ▲/▼ delta computed across a fake-zero gap is wrong in a way that looks
authoritative. Land 0.2 + 4.2 first, then 1.3 becomes safe.

### 5.5 Key tile config by device identity, not display name
Every settings key is the Homebridge **display name** — `a:Mi Air Purifier`,
`a:מזגן AC`. Rename a device and all of its config orphans at once: hidden
state, room, size, order, custom name.

Live example, on the wall today: `מזגן AC` was renamed to `AC`, so
`"a:מזגן AC"` sits uselessly in `hidden` while the tile is now `a:AC` — **a
stray AC tile appeared on the panel**, and its `WHOLE_HOME` room and `LARGE`
size defaults quietly stopped applying. It has happened before, too: Prefs.kt
still carries `"a:Ceeling light" to Room.BEDROOM, // pre-rename spelling`.

HAP already serves a stable serial (characteristic `30`) for every accessory
here — the AC's is `ac-failover-1`, unchanged straight through the rename;
purifier `583824103`, nursery sensor `blt.1.1pe9vdb194k03`, bulbs `a01f7d` /
`4dc114`. Only `HomebridgeLogCleaner` (hidden anyway) and the synthetic
`Living Room` sensor lack one, and the latter is minted by our own server.js
so it can be given one.

- Key on serial, with a one-time migration mapping existing name-keyed
  settings across. Keep the name as the fallback for accessories with no
  usable serial.
- Effort: M — the migration is the risky part and wants a real-device test,
  which is why it didn't ride along with v1.1.14.

### 5.6 Device health / outage history view
Three times in the week of 2026-07-25 the question was "why isn't X showing",
and each time answering it needed an SSH session into the Pi. The data was
already there: `history.json` holds 7 days of 5-minute samples, which is how
both purifier outages (21.6 h and 6.0 h) and the exact minute the Living Room
feed died were dated after the fact.

- A Health screen listing every sensor: last good reading, current age, and a
  7-day uptime bar. Mostly aggregation over data already stored.
- 4.1 makes a single tile honest about *now*; this shows the **pattern**.
  "The purifier has dropped twice this week" is a different signal from "the
  purifier is offline" — the first says it's the network, not a fluke.
- Now that 0.2 stops writing filler zeros, a gap in a series *is* the outage
  record, so this gets easier rather than harder.
- Effort: M.

### 5.7 Not doing: automated Pi backups — declined 2026-07-31
Raised because nothing on the Pi is backed up: single 29.8 GB **SD** card,
75 % full, 202 days uptime, no backup job of any kind, and Homebridge's
pairing state has already broken once on this machine
(`AccessoryInfo.*.json.broken-20260502`, `.pre-restore-20260529`).

Declined — Homebridge backs itself up to the dev Mac, and the rest is on
GitHub. Two things genuinely aren't covered by either, noted so the decision
stays an informed one rather than an assumption:

- `hb-dashboard/settings.json` — gitignored **by design** (it's personal
  runtime state), so GitHub will never hold it. It is the only copy of every
  tile name, group, hidden device and `pullSensors` entry.
- TeslaMate's Postgres volume — the entire drive/charge history of the car.

An in-app *Export / Restore settings* pair would cover the first one from the
panel itself, without any Pi-side cron.


---

## Suggested order

1. ~~**0.1 + 0.2 + 0.3 + 0.4 + 0.5**~~ — all shipped v1.1.14, with 3.8.
2. **4.2 then 4.1** — completes the outage work: gaps render as gaps, stale
   readings admit their age. 0.2 is done, so 4.2 is unblocked — and there are
   now real gaps in `history.json` waiting to be drawn correctly.
3. **5.5** — the stray "AC" tile is a live symptom; do it before more tiles
   accumulate settings to migrate.
4. **4.3 + 5.1** — the two biggest day-to-day UX wins, app-only, no server
   changes. 5.1 is mostly wiring to existing ViewModel functions.
5. **5.2** — before any further layout work, and before 1.3/1.4.
6. **2.2 Nap mode + 2.3 quick bar** — the remaining sleep-protection items
   from the original plan (2.1 already shipped).
7. **5.3 + rest of 2.4** — legibility and touch targets in one pass.
8. **3.1 scenes + 3.2 alerts** — first substantial server work; deploy to
   the Pi (`orangepi@192.168.68.75:/home/orangepi/hb-dashboard`).
9. Rest as appetite allows.

Deployment note (2026-07-26): the Pi's `server.js` is byte-identical to git
`HEAD` apart from the settings-merge fix, which is already deployed there.
Keep it that way — diverging copies were how the `pullSensors` loss went
unnoticed.
