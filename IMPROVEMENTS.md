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

---

## Suggested order

1. **1.1 doorbell shrink + 1.2 baby hero tile** — biggest daily win, app-only.
2. **2.1 wall-dim + 2.2 Nap mode** — protects sleep, app-only.
3. **1.3 trends + 1.4 header chips** — glanceability, app-only.
4. **3.1 scenes + 3.2 alerts** — first server changes; deploy to the Pi
   (`orangepi@192.168.68.75:/home/orangepi/hb-dashboard`).
5. Rest as appetite allows.
