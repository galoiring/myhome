# Dashboard polish plan (2026-07-04)

Work items from the tablet screenshot review. All UI code is in
`app/src/main/java/com/gal/myhome/ui/DashboardScreen.kt` unless noted;
tile data is built in `app/src/main/java/com/gal/myhome/DashboardViewModel.kt`.

## 1. BUG — segmented buttons don't show the selected option

**Symptom:** purifier is on Auto and the AC fan is on Low, but neither
"Auto" nor "Low" looks selected.

**Root cause (found):** the selection state *is* correct in the data —
`SegRow` (DashboardScreen.kt ~line 911) renders the selected segment with
`activeContainerColor = MaterialTheme.colorScheme.surfaceBright`, which on
the light theme is nearly the same white as an unselected segment sitting
on a light tile. The check icon is also suppressed (`icon = {}`), so there
is no visual signal at all. It's a styling bug, not a state bug.

**Fix:**
- Pass the tile's tint into `SegRow` — `ControlView` already receives
  `onColor`/`subColor` but `SegRow` ignores them. Use a clearly contrasting
  selected style, e.g. `activeContainerColor = onColor` (the tile's content
  color) with `activeContentColor` = the tile background, or the `TintSet.iconCircle`
  color for the tile kind. Must read clearly on BOTH light and dark themes
  and on tinted (blue purifier / amber light) tiles.
- Restore a selected checkmark: `icon = { SegmentedButtonDefaults.Icon(selected) }`
  (or keep icons off and rely on strong fill — but fill must be obvious).
- While here: soften the segmented border with
  `border = BorderStroke(1.dp, subColor.copy(alpha = .35f))` so it stops
  looking like a stark black outline (visible in the screenshot on AC + purifier).

**Related data quirks to double-check while testing (secondary):**
- AC speed → segment mapping is `DashboardViewModel.kt` ~line 649: it snaps
  the current speed to the nearest of 33/66/100 with `cur ?: 0` → Low. Verify
  with the real AC that "Low" reports a value that snaps correctly (if the
  characteristic is 0–3 instead of 0–100, the snap still lands on 33/Low, fine).
- Purifier `TGT_AP` seg (~line 683): value can be null if the characteristic
  hasn't polled yet — with the styling fix, null simply shows nothing selected,
  which is correct.
- Screenshot also shows purifier "Speed 0%" while "Purifying" (auto mode) —
  the device likely reports rotation speed 0 in auto. Optional: hide/disable
  the speed slider when mode == Auto.

## 2. Curtain — nicer draggable

`CurtainRow`, DashboardScreen.kt ~lines 994–1106. The window scene + fabric
stays; improve the affordance:

- **Grip handle:** replace the thin 4dp bar with a proper pill handle
  (~10–12dp wide, rounded, `fillMaxHeight(0.5f)`), colored `gripColor` with a
  soft white inner highlight and a small drop shadow so it reads as grabbable.
  Optionally 3 tiny dots or a `⋮`-style pattern inside. Let it overhang the
  fabric's leading edge by a few dp so it's visible even fully closed.
- **Animate position:** when not dragging, drive the fabric fraction through
  `animateFloatAsState` so poll updates / taps glide instead of jumping.
  (Keep raw value while `drag != null` so the finger stays 1:1.)
- **Drag feedback:** while dragging show a small floating "% open" badge above
  the grip (or make the header % text bolder/accented during drag), and fire a
  light haptic on drag end (respect `prefs.hapticFeedback`, see TileCard for
  the pattern).
- **Fabric edge:** round the leading edge (top-end/bottom-end corners of the
  fabric box, ~6dp) and add a subtle edge shadow so fabric visually sits above
  the window.

## 3. Tile ("cube") borders

`TileCard`, DashboardScreen.kt ~line 594. Cards are flat translucent fills
with no edge definition.

- Add a hairline border on the card `Surface`:
  - light theme: `Color.White.copy(alpha = .55f)` (glass edge highlight) or
    `outlineVariant.copy(alpha = .4f)` — try both, pick what looks best;
  - dark theme: `Color.White.copy(alpha = .08f)`;
  - tinted (on) tiles: tint's content color at low alpha (e.g.
    `onContent.copy(alpha = .15f)`) so lit tiles get a matching warm/cool edge.
- Optionally add `shadowElevation = 1.dp`–`2.dp` for gentle lift; verify it
  doesn't look muddy with the 0.90 alpha glass background.
- Keep the animated bg (`animateColorAsState`) — animate the border color too
  so on/off transitions stay smooth.

## 4. Sensor tiles — show temp & air quality differently

Two places: the standalone SENSOR tile body (DashboardScreen.kt ~lines
653–697, e.g. "Baby Room", "Mi Air Purifier Air Quality") and the inline
`SensorsRow` (~line 1147).

- **PM2.5 / air-quality tile** (the split tile built in DashboardViewModel.kt
  ~line 510): currently a big empty card with a tiny "1 µg/m³". Redesign as a
  hero reading like the temp tile: large number, "µg/m³" unit small beside it,
  plus a color-coded status pill (thresholds e.g. ≤12 "Good" green, ≤35
  "Moderate" amber, >35 "Poor" red — reuse/extend `airQualityPillColor`).
  Tint the tile's icon circle with the status color. Optionally a thin arc/ring
  gauge around the value.
- **Temp sensor tile** ("Baby Room"): keep the big degrees, but add a subtle
  temperature accent (cool blue ≤18°, neutral, warm amber ≥26°) on the icon
  circle, and show humidity as a small pill (droplet icon + %) instead of the
  plain icon+text row.
- Give SENSOR tiles a `TintSet`-style treatment so they don't look like "off"
  gray tiles — e.g. a faint teal/green tint via `alwaysTinted`, or the
  status-based tint above.
- Rename the split AQ tile: "Mi Air Purifier Air Quality" is long — the split
  in DashboardViewModel.kt can name it "Air Quality" (room label already gives
  context) — but keep `serverSettings.names[key]` override working.

## Verify & ship

1. Build: `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ANDROID_HOME=/opt/homebrew/share/android-commandlinetools ./gradlew assembleRelease`
2. Check light AND dark theme, and Half-height tiles (curtain + purifier are half-height in this layout).
3. Release: bump `versionCode`/`versionName` in `app/build.gradle`, update
   repo-root `update.json` AND `app/build/outputs/apk/release/update.json`,
   then the tablet self-updates via Settings > Updates.

Suggested order: 1 (bug) → 3 (borders, small) → 2 (curtain) → 4 (sensors) —
each is independently shippable.
