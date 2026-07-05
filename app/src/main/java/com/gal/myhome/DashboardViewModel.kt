package com.gal.myhome

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gal.myhome.data.AIRQ_LABELS
import com.gal.myhome.data.Acc
import com.gal.myhome.data.Chr
import com.gal.myhome.data.Group
import com.gal.myhome.data.HomeApi
import com.gal.myhome.data.Prefs
import com.gal.myhome.data.PrefsRepo
import com.gal.myhome.data.SVC
import com.gal.myhome.data.ServerSettings
import com.gal.myhome.data.ShellyDevice
import com.gal.myhome.data.CameraCfg
import com.gal.myhome.data.Room
import com.gal.myhome.data.TileHeight
import com.gal.myhome.data.TileWidth
import com.gal.myhome.data.SortMode
import com.gal.myhome.data.Svc
import com.gal.myhome.data.T
import com.gal.myhome.data.Target
import com.gal.myhome.data.UpdateClient
import com.gal.myhome.data.UpdateInfo
import com.gal.myhome.data.Weather
import com.gal.myhome.data.YeelightClient
import com.gal.myhome.data.YlFound
import com.gal.myhome.data.YlState
import com.gal.myhome.data.asBool
import com.gal.myhome.data.asDouble
import com.gal.myhome.data.kelvinToWarmth
import com.gal.myhome.data.warmthToKelvin
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.roundToInt

/* ---------- UI models ---------- */

enum class TileKind { LIGHT, AC, PURIFIER, FAN, OUTLET, SWITCH, SENSOR, CURTAIN, CAMERA, OTHER }
enum class SensorKind { TEMP, HUMIDITY, AIR_QUALITY, PM25, OCCUPANCY, MOTION, FILTER }

// routes a control to a LAN Yeelight instead of the HomeKit API
data class YlRef(val ip: String, val prop: String) // "bright" | "ct" | "moon"

sealed interface Control
data class SliderCtl(
    val id: String, val label: String, val unit: String,
    val min: Float, val max: Float, val step: Float, val value: Float,
    val warm: Boolean, val targets: List<Target>,
    val yl: YlRef? = null,
) : Control

data class SegCtl(
    val id: String, val options: List<Pair<Int, String>>, val value: Int?,
    val targets: List<Target>,
) : Control

data class StepCtl(
    val id: String, val label: String, val value: Double,
    val min: Double, val max: Double, val step: Double, val targets: List<Target>,
) : Control

// window covering position; value is % open, fabric drawn anchored left (opens right→left)
data class CurtainCtl(val id: String, val value: Float, val targets: List<Target>) : Control

data class ChipUi(
    val id: String, val label: String, val on: Boolean,
    val isActive: Boolean, val targets: List<Target>,
    val yl: YlRef? = null,
)

// moonlight/nightlight mode — HAP tiles drive the light's mode switch
// services, pure-LAN Yeelight tiles go through YlRef instead
data class MoonUi(
    val on: Boolean,
    val onTargets: List<Target>,
    val offTargets: List<Target>,
    val yl: YlRef? = null,
)

data class SensorUi(val kind: SensorKind, val value: String, val unit: String)

data class ShellyRef(val ip: String, val compId: Int, val compType: String)

data class TileUi(
    val key: String,
    val name: String,
    val sub: String,
    val kind: TileKind,
    val isOn: Boolean,
    val canToggle: Boolean,
    val hidden: Boolean,
    val isGroup: Boolean,
    val origNames: List<String>,
    val controls: List<Control>,
    val chips: List<ChipUi>,
    val sensors: List<SensorUi>,
    val shelly: ShellyRef?,
    val toggleTargets: List<Target>,
    val toggleIsActive: Boolean,
    val yeelight: String? = null,
    val camera: CameraCfg? = null,
    val room: Room? = null,
    val width: TileWidth = TileWidth.MEDIUM,
    val height: TileHeight = TileHeight.NORMAL,
    val modeControl: SegCtl? = null,
    // brightness pulled out of `controls`: the whole card acts as the dimmer
    val dimmer: SliderCtl? = null,
    val moon: MoonUi? = null,
)

data class UiState(
    val tiles: List<TileUi> = emptyList(),
    val weather: Weather? = null,
    val indoorTemp: Double? = null,
    val powerW: Double? = null,
    // epoch ms of the last doorbell press, 0 = never
    val ringAt: Long = 0L,
    val offline: Boolean = false,
    val loaded: Boolean = false,
)

private const val TOUCH_HOLD_MS = 5000L

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data object Downloading : UpdateState
    data class Error(val message: String) : UpdateState
}

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val api = HomeApi()
    private val ylClient = YeelightClient()
    private val updateClient = UpdateClient()
    val prefsRepo = PrefsRepo(app)
    val prefs = prefsRepo.flow.stateIn(viewModelScope, SharingStarted.Eagerly, Prefs())

    var ui by mutableStateOf(UiState())
        private set

    var updateState by mutableStateOf<UpdateState>(UpdateState.Idle)
        private set

    // latest full history dump (name|kind -> [ts, value]), refreshed every
    // few minutes for the sensor-tile sparklines
    var histories by mutableStateOf<Map<String, List<Pair<Long, Double>>>>(emptyMap())
        private set

    private var accs: List<Acc> = emptyList()
    private var shellyDevs: List<ShellyDevice> = emptyList()
    private var ylStates: Map<String, YlState?> = emptyMap()
    private var ringAt = 0L
    var serverSettings = ServerSettings()
        private set

    // optimistic values applied on top of server state; key -> (value, timestamp)
    private val overrides = ConcurrentHashMap<String, Pair<Any, Long>>()
    private val settingsLoaded = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            refreshServerSettings()
            launch { pollLoop() }
            launch { weatherLoop() }
            launch { historyLoop() }
            // prefs changes (rooms, sort, yeelights, cameras) reshape tiles immediately
            launch { prefs.collect { if (ui.loaded) rebuild(ui.offline) } }
            launch { checkForUpdate() }
        }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            updateState = UpdateState.Checking
            val info = updateClient.checkForUpdate(prefs.value.updateCheckUrl)
            updateState = when {
                info == null -> UpdateState.Error("Couldn't reach the update server")
                info.versionCode <= BuildConfig.VERSION_CODE -> UpdateState.Idle
                else -> UpdateState.Available(info)
            }
        }
    }

    fun downloadAndInstallUpdate() {
        val available = updateState as? UpdateState.Available ?: return
        viewModelScope.launch {
            updateState = UpdateState.Downloading
            val context = getApplication<Application>()
            val file = updateClient.downloadApk(available.info.url, context.cacheDir)
            if (file == null) {
                updateState = UpdateState.Error("Download failed")
                return@launch
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            updateState = UpdateState.Idle
        }
    }

    fun updatePrefs(p: Prefs) {
        viewModelScope.launch { prefsRepo.update(p) }
    }

    private suspend fun refreshServerSettings() {
        api.baseUrl = prefs.value.serverUrl
        try {
            serverSettings = api.settings()
            settingsLoaded.value = true
        } catch (_: Exception) { /* defaults; retried after next save */ }
    }

    private suspend fun pollLoop() {
        while (true) {
            api.baseUrl = prefs.value.serverUrl
            try {
                coroutineScope {
                    val a = async { api.accessories() }
                    val s = async { try { api.shelly() } catch (_: Exception) { shellyDevs } }
                    val d = async { try { api.doorbellRing() } catch (_: Exception) { ringAt } }
                    // Yeelights are independent of the server; state() returns null on error
                    val y = prefs.value.yeelights.map { cfg ->
                        async { cfg.ip to ylClient.state(cfg.ip) }
                    }
                    accs = a.await()
                    shellyDevs = s.await()
                    ringAt = d.await()
                    ylStates = y.awaitAll().toMap()
                }
                if (!settingsLoaded.value) refreshServerSettings()
                pruneOverrides()
                rebuild(offline = false)
                // after a doze the 10-min weather loop may not have ticked for
                // hours — the fast poll nudges it so the header recovers
                // seconds after the tablet wakes instead of showing 2am data
                if (System.currentTimeMillis() - lastWeatherAt > 10 * 60 * 1000L) {
                    viewModelScope.launch { fetchWeatherNow() }
                }
            } catch (_: Exception) {
                ui = ui.copy(offline = true)
            }
            delay(prefs.value.pollSeconds * 1000L)
        }
    }

    @Volatile
    private var lastWeatherAt = 0L

    private suspend fun fetchWeatherNow() {
        try {
            ui = ui.copy(weather = api.weather())
            lastWeatherAt = System.currentTimeMillis()
        } catch (_: Exception) { /* retry next cycle */ }
    }

    private suspend fun weatherLoop() {
        while (true) {
            api.baseUrl = prefs.value.serverUrl
            fetchWeatherNow()
            delay(10 * 60 * 1000L)
        }
    }

    suspend fun history(): Map<String, List<Pair<Long, Double>>> = api.history()

    // periodic snapshot of the same history the HistorySheet fetches on
    // demand — feeds the always-visible sparklines on sensor tiles
    private suspend fun historyLoop() {
        while (true) {
            if (!ui.loaded) {
                delay(5_000L)
                continue
            }
            api.baseUrl = prefs.value.serverUrl
            try {
                histories = api.history()
            } catch (_: Exception) { /* older server — retry next cycle */ }
            delay(5 * 60 * 1000L)
        }
    }

    suspend fun cameraSnapshot(rtspUrl: String): ByteArray = api.snapshot(rtspUrl)

    suspend fun cameraSnapshotCached(rtspUrl: String): HomeApi.Snapshot? =
        api.snapshotCached(rtspUrl)

    private fun pruneOverrides() {
        val now = System.currentTimeMillis()
        overrides.entries.removeIf { now - it.value.second > TOUCH_HOLD_MS }
    }

    /* ---------- actions ---------- */

    private fun override(key: String, value: Any) {
        overrides[key] = value to System.currentTimeMillis()
    }

    fun sendChars(targets: List<Target>, value: Any) {
        for (t in targets) override("${t.aid}.${t.iid}", value)
        rebuild(ui.offline)
        viewModelScope.launch {
            try { api.setChars(targets, value) } catch (_: Exception) { /* next poll restores truth */ }
        }
    }

    fun toggleMoon(tile: TileUi) {
        val m = tile.moon ?: return
        if (m.yl != null) {
            setYeelight(m.yl, !m.on)
            return
        }
        when {
            !m.on -> sendChars(m.onTargets, true)
            // day/night mode pairs: leaving moonlight means turning day mode
            // on; the pill reads the night switch, so pin it off optimistically
            m.offTargets.isNotEmpty() -> {
                for (t in m.onTargets) override("${t.aid}.${t.iid}", false)
                sendChars(m.offTargets, true)
            }
            else -> sendChars(m.onTargets, false)
        }
    }

    fun setYeelight(ref: YlRef, value: Any) {
        override("ylk:${ref.ip}:${ref.prop}", value)
        rebuild(ui.offline)
        viewModelScope.launch {
            when (ref.prop) {
                "bright" -> ylClient.setBright(ref.ip, (value as Number).toInt())
                "ct" -> ylClient.setCtKelvin(ref.ip, warmthToKelvin((value as Number).toInt()))
                "moon" -> ylClient.setMoonlight(ref.ip, value as Boolean)
            }
        }
    }

    suspend fun discoverYeelights(): List<YlFound> = ylClient.discover()

    fun setRoom(key: String, room: Room) {
        val p = prefs.value
        updatePrefs(p.copy(rooms = p.rooms + (key to room)))
        rebuild(ui.offline)
    }

    fun setTileWidth(key: String, width: TileWidth) {
        val p = prefs.value
        val cfg = p.sizeFor(key).copy(width = width)
        updatePrefs(p.copy(tileSizes = p.tileSizes + (key to cfg)))
        rebuild(ui.offline)
    }

    fun setTileHeight(key: String, height: TileHeight) {
        val p = prefs.value
        val cfg = p.sizeFor(key).copy(height = height)
        updatePrefs(p.copy(tileSizes = p.tileSizes + (key to cfg)))
        rebuild(ui.offline)
    }

    /** Reorders tiles for manual layout control; captures the current full
     * order on first use so every tile has an explicit position from then on. */
    fun moveTile(key: String, delta: Int) {
        val order = ui.tiles.map { it.key }.toMutableList()
        val from = order.indexOf(key)
        if (from < 0) return
        val to = (from + delta).coerceIn(0, order.size - 1)
        if (to == from) return
        order.removeAt(from)
        order.add(to, key)
        updatePrefs(prefs.value.copy(tileOrder = order))
    }

    fun toggleTile(tile: TileUi) {
        if (tile.yeelight != null) {
            val on = !tile.isOn
            override("ylk:${tile.yeelight}:power", on)
            rebuild(ui.offline)
            viewModelScope.launch { ylClient.setPower(tile.yeelight, on) }
        } else if (tile.shelly != null) {
            val on = !tile.isOn
            override("sk:${tile.key}", on)
            rebuild(ui.offline)
            viewModelScope.launch {
                try {
                    api.setShelly(tile.shelly.ip, tile.shelly.compId, tile.shelly.compType, on)
                } catch (_: Exception) { }
            }
        } else if (tile.toggleTargets.isNotEmpty()) {
            val on = !tile.isOn
            override("main:${tile.key}", on)
            sendChars(tile.toggleTargets, if (tile.toggleIsActive) (if (on) 1 else 0) else on)
        }
    }

    /* ---------- server-settings edits (kept in sync with the web dashboard) ---------- */

    fun renameTile(tile: TileUi, newName: String) = editSettings {
        if (newName.isBlank()) it.names.remove(tile.key) else it.names[tile.key] = newName.trim()
    }

    fun setTileHidden(tile: TileUi, hidden: Boolean) = editSettings {
        if (hidden) { if (!it.hidden.contains(tile.key)) it.hidden.add(tile.key) }
        else it.hidden.remove(tile.key)
    }

    fun createGroup(name: String, memberOrigNames: List<String>) = editSettings {
        it.groups.add(Group(name.ifBlank { "Group" }, memberOrigNames))
    }

    fun ungroup(tile: TileUi) = editSettings { s ->
        s.groups.removeIf { g -> "g:" + g.members.sorted().joinToString("|") == tile.key }
        s.names.remove(tile.key)
    }

    private fun editSettings(edit: (ServerSettings) -> Unit) {
        edit(serverSettings)
        rebuild(ui.offline)
        viewModelScope.launch {
            try {
                api.saveSettings(serverSettings)
                serverSettings = api.settings()
            } catch (_: Exception) { }
            rebuild(ui.offline)
        }
    }

    /* ---------- building tiles ---------- */

    private fun chrValue(aid: Int, c: Chr): Any? =
        overrides["$aid.${c.iid}"]?.first ?: c.value

    private fun rebuild(offline: Boolean) {
        val tiles = buildTiles()
        ui = ui.copy(
            tiles = tiles,
            indoorTemp = indoorTemp(),
            powerW = totalPowerW(),
            ringAt = ringAt,
            offline = offline,
            loaded = true,
        )
    }

    // only Shelly devices report power, so this is "everything metered", not
    // a true whole-home number — still useful as a live activity signal
    private fun totalPowerW(): Double? {
        if (shellyDevs.isEmpty()) return null
        return shellyDevs.sumOf { d -> d.comps.sumOf { if (it.state) it.apower else 0.0 } }
    }

    private fun indoorTemp(): Double? {
        var visible: Double? = null
        var any: Double? = null
        val hiddenKeys = serverSettings.hidden.toSet()
        for (acc in accs) {
            val key = "a:${acc.origName}"
            for (svc in acc.services) {
                if (svc.type != SVC.HC) continue
                val v = svc.ch(T.CUR_TEMP)?.value.asDouble() ?: continue
                if (any == null) any = v
                if (visible == null && key !in hiddenKeys) visible = v
            }
        }
        return visible ?: any
    }

    private fun buildTiles(): List<TileUi> {
        val list = mutableListOf<TileUi>()
        val accsFiltered = accs.filter { it.aid != 1 }
        val byOrig = accsFiltered.associateBy { it.origName }
        val used = mutableSetOf<Int>()

        for (g in serverSettings.groups) {
            val members = g.members.mapNotNull { byOrig[it] }
            if (members.size < 2) continue
            members.forEach { used.add(it.aid) }
            val key = "g:" + g.members.sorted().joinToString("|")
            list.add(buildDeviceTile(key, serverSettings.names[key] ?: g.name, members, isGroup = true))
        }
        for (acc in accsFiltered) {
            if (acc.aid in used) continue
            val key = "a:${acc.origName}"
            list.add(buildDeviceTile(key, serverSettings.names[key] ?: acc.origName, listOf(acc), isGroup = false))
        }
        for (dev in shellyDevs) {
            for (c in dev.comps) {
                val key = "s:${dev.ip}:${c.id}"
                val on = (overrides["sk:$key"]?.first as? Boolean) ?: c.state
                val sub = if (on) {
                    "On" + (if (c.apower > 1) " · ${c.apower.roundToInt()} W" else "")
                } else "Off"
                list.add(TileUi(
                    key = key,
                    name = serverSettings.names[key] ?: c.name ?: dev.name ?: "Shelly",
                    sub = sub,
                    kind = TileKind.LIGHT,
                    isOn = on,
                    canToggle = true,
                    hidden = key in serverSettings.hidden,
                    isGroup = false,
                    origNames = emptyList(),
                    controls = emptyList(),
                    chips = emptyList(),
                    sensors = emptyList(),
                    shelly = ShellyRef(dev.ip, c.id, c.type),
                    toggleTargets = emptyList(),
                    toggleIsActive = false,
                ))
            }
        }

        val p = prefs.value
        for (cfg in p.yeelights) {
            val key = "y:${cfg.ip}"
            val st = ylStates[cfg.ip]
            val on = (overrides["ylk:${cfg.ip}:power"]?.first as? Boolean) ?: st?.power ?: false
            val bright = (overrides["ylk:${cfg.ip}:bright"]?.first as? Number)?.toInt()
                ?: st?.bright ?: 0
            val warmth = (overrides["ylk:${cfg.ip}:ct"]?.first as? Number)?.toInt()
                ?: st?.let { kelvinToWarmth(it.ctK) } ?: 50
            val moon = (overrides["ylk:${cfg.ip}:moon"]?.first as? Boolean)
                ?: st?.moonlight ?: false
            list.add(TileUi(
                key = key,
                name = serverSettings.names[key] ?: cfg.name,
                sub = when {
                    st == null -> "Unreachable · enable LAN Control?"
                    on -> "$bright%"
                    else -> "Off"
                },
                kind = TileKind.LIGHT,
                isOn = on,
                canToggle = true,
                hidden = key in serverSettings.hidden,
                isGroup = false,
                origNames = listOf(cfg.name),
                dimmer = SliderCtl("$key:bright", "Brightness", "%", 1f, 100f, 1f,
                    bright.toFloat(), false, emptyList(), YlRef(cfg.ip, "bright")),
                controls = listOf(
                    SliderCtl("$key:ct", "Warmth", "", 0f, 100f, 1f,
                        warmth.toFloat(), true, emptyList(), YlRef(cfg.ip, "ct")),
                ),
                chips = emptyList(),
                moon = if (st?.moonSupported == true)
                    MoonUi(moon, emptyList(), emptyList(), YlRef(cfg.ip, "moon"))
                else null,
                sensors = emptyList(),
                shelly = null,
                toggleTargets = emptyList(),
                toggleIsActive = false,
                yeelight = cfg.ip,
            ))
        }
        for (cfg in p.cameras) {
            val key = "c:${cfg.name}"
            list.add(TileUi(
                key = key,
                name = serverSettings.names[key] ?: cfg.name,
                sub = if (cfg.doorbell) "Opens on ring · tap to peek" else "Tap for live view",
                kind = TileKind.CAMERA,
                isOn = false,
                canToggle = false,
                hidden = key in serverSettings.hidden,
                isGroup = false,
                origNames = listOf(cfg.name),
                controls = emptyList(),
                chips = emptyList(),
                sensors = emptyList(),
                shelly = null,
                toggleTargets = emptyList(),
                toggleIsActive = false,
                camera = cfg,
            ))
        }

        // split the purifier's air-quality reading into its own tile so the
        // controls tile stays short — a combined tile needed inside-scrolling
        // to reach the reading, which defeats the point of a compact size
        val split = mutableListOf<TileUi>()
        for (t in list) {
            if (t.kind == TileKind.PURIFIER && t.sensors.isNotEmpty()) {
                split.add(t.copy(sensors = emptyList()))
                split.add(TileUi(
                    key = "${t.key}:aq",
                    // the row-level room label already gives context, so the
                    // parent device name would just make this wrap
                    name = serverSettings.names["${t.key}:aq"] ?: "Air Quality",
                    sub = "",
                    kind = TileKind.SENSOR,
                    isOn = false,
                    canToggle = false,
                    hidden = t.hidden,
                    isGroup = false,
                    origNames = t.origNames,
                    controls = emptyList(),
                    chips = emptyList(),
                    sensors = t.sensors,
                    shelly = null,
                    toggleTargets = emptyList(),
                    toggleIsActive = false,
                ))
            } else {
                split.add(t)
            }
        }

        val tagged = split.map {
            val size = p.sizeFor(it.key)
            it.copy(room = p.roomFor(it.key), width = size.width, height = size.height)
        }
        // an explicit user reorder always wins; new tiles not yet placed sort to the end
        if (p.tileOrder.isNotEmpty()) {
            val orderIndex = p.tileOrder.withIndex().associate { (i, k) -> k to i }
            return tagged.sortedBy { orderIndex[it.key] ?: Int.MAX_VALUE }
        }
        val weight: (TileUi) -> Int = { t ->
            when {
                t.isGroup -> -1
                t.kind == TileKind.LIGHT -> 0
                t.kind == TileKind.AC || t.kind == TileKind.PURIFIER -> 1
                t.kind == TileKind.SWITCH || t.kind == TileKind.OUTLET -> 2
                else -> 3
            }
        }
        return when (p.sortMode) {
            SortMode.AUTO -> tagged.sortedWith(
                compareBy({ it.room?.priority ?: 5 }, { weight(it) })
            )
            SortMode.NAME -> tagged.sortedBy { it.name.lowercase() }
        }
    }

    private fun tileKind(acc: Acc): TileKind {
        val types = acc.services.map { it.type }
        return when {
            SVC.LIGHT in types -> TileKind.LIGHT
            SVC.HC in types -> TileKind.AC
            SVC.AP in types -> TileKind.PURIFIER
            SVC.FAN in types || SVC.FAN2 in types -> TileKind.FAN
            SVC.OUTLET in types -> TileKind.OUTLET
            SVC.SWITCH in types -> TileKind.SWITCH
            SVC.WC in types -> TileKind.CURTAIN
            SVC.TEMP in types || SVC.HUM in types -> TileKind.SENSOR
            else -> TileKind.OTHER
        }
    }

    // matching characteristic in each member: same service type, same occurrence
    // index of that service type, same characteristic type
    private fun targetsFor(members: List<Acc>, svcType: String, svcOcc: Int, chType: String): List<Target> {
        val out = mutableListOf<Target>()
        for (m in members) {
            var occ = 0
            for (s in m.services) {
                if (s.type != svcType) continue
                if (occ == svcOcc) {
                    s.ch(chType)?.let { out.add(Target(m.aid, it.iid)) }
                    break
                }
                occ++
            }
        }
        return out
    }

    private fun svcName(svc: Svc, fallback: String): String =
        svc.ch(T.NAME)?.value as? String ?: fallback

    private fun buildDeviceTile(key: String, name: String, members: List<Acc>, isGroup: Boolean): TileUi {
        val acc = members[0]
        val controls = mutableListOf<Control>()
        val chips = mutableListOf<ChipUi>()
        val sensors = mutableListOf<SensorUi>()
        var toggleTargets = emptyList<Target>()
        var toggleIsActive = false
        var modeCtl: SegCtl? = null
        var mainToggleSet = false
        var moonOn = false
        var moonOnTargets = emptyList<Target>()
        var moonOffTargets = emptyList<Target>()
        var onAny = false
        val subParts = mutableListOf<String>()
        val occCount = mutableMapOf<String, Int>()
        val hasMultipleLights = acc.services.count { it.type == SVC.LIGHT } > 1

        for (svc in acc.services) {
            if (svc.type == SVC.INFO || svc.type == SVC.PROTO) continue
            val svcOcc = occCount[svc.type] ?: 0
            occCount[svc.type] = svcOcc + 1
            fun tg(chType: String) = targetsFor(members, svc.type, svcOcc, chType)
            fun cid(c: Chr) = "${acc.aid}.${c.iid}"

            val on = svc.ch(T.ON)
            val active = svc.ch(T.ACTIVE)

            if ((svc.type == SVC.SWITCH || svc.type == SVC.OUTLET || svc.type == SVC.FAN) && mainToggleSet && on != null) {
                // Yeelight ceiling lights expose moonlight as extra switch
                // services ("Moonlight Mode", or "Mode Day"/"Mode Night") —
                // fold those into the moon control instead of generic chips
                val nm = svcName(svc, "Switch").trim().lowercase().replace(Regex("\\s+"), " ")
                if (svc.type == SVC.SWITCH && (nm.contains("moonlight") || nm == "mode night")) {
                    moonOn = chrValue(acc.aid, on).asBool()
                    moonOnTargets = tg(T.ON)
                    continue
                }
                if (svc.type == SVC.SWITCH && nm == "mode day") {
                    moonOffTargets = tg(T.ON)
                    continue
                }
                chips.add(ChipUi(cid(on), svcName(svc, "Switch"),
                    chrValue(acc.aid, on).asBool(), false, tg(T.ON)))
                continue
            }

            val tgl = on ?: active
            if (tgl != null && !mainToggleSet) {
                toggleTargets = tg(if (on != null) T.ON else T.ACTIVE)
                toggleIsActive = active != null && on == null
                mainToggleSet = true
            } else if (tgl != null && mainToggleSet && svc.type == SVC.LIGHT
                && svcName(svc, "") != acc.origName) {
                // a chip named like the accessory duplicates the card toggle — skip it
                chips.add(ChipUi(cid(tgl), svcName(svc, "Light"),
                    chrValue(acc.aid, tgl).asBool(), false, tg(T.ON)))
            }

            svc.ch(T.BRIGHT)?.let { c ->
                // multi-light accessories (main + moonlight): label sliders by service
                val label = svcName(svc, "Brightness")
                    .takeIf { hasMultipleLights && it != acc.origName } ?: "Brightness"
                controls.add(SliderCtl(cid(c), label, "%",
                    (c.minValue ?: 0.0).toFloat(), (c.maxValue ?: 100.0).toFloat(),
                    (c.minStep ?: 1.0).toFloat(),
                    (chrValue(acc.aid, c).asDouble() ?: 0.0).toFloat(), false, tg(T.BRIGHT)))
            }
            svc.ch(T.SPEED)?.let { c ->
                if (svc.type == SVC.HC) {
                    // this AC's hardware only has 3 real fan speeds; a 0-100
                    // slider misrepresents it as continuous. Until the first
                    // poll lands the value is unknown — no segment selected
                    val steps = listOf(33 to "Low", 66 to "Medium", 100 to "High")
                    val cur = chrValue(acc.aid, c).asDouble()?.roundToInt()
                    val nearest = cur?.let { cv -> steps.minByOrNull { abs(it.first - cv) }?.first }
                    controls.add(SegCtl(cid(c), steps, nearest, tg(T.SPEED)))
                } else {
                    // in Auto the purifier drives its own fan (and reports
                    // rotation speed 0) — the slider only means anything in
                    // Manual, so it's hidden until the mode says so
                    val apAuto = svc.type == SVC.AP &&
                        svc.ch(T.TGT_AP)?.let { chrValue(acc.aid, it).asDouble()?.toInt() } == 1
                    if (!apAuto) {
                        controls.add(SliderCtl(cid(c), "Speed", "%",
                            (c.minValue ?: 0.0).toFloat(), (c.maxValue ?: 100.0).toFloat(),
                            (c.minStep ?: 1.0).toFloat(),
                            (chrValue(acc.aid, c).asDouble() ?: 0.0).toFloat(), false, tg(T.SPEED)))
                    }
                }
            }
            svc.ch(T.CT)?.let { c ->
                controls.add(SliderCtl(cid(c), "Warmth", "",
                    (c.minValue ?: 140.0).toFloat(), (c.maxValue ?: 500.0).toFloat(),
                    (c.minStep ?: 1.0).toFloat(),
                    (chrValue(acc.aid, c).asDouble() ?: 0.0).toFloat(), true, tg(T.CT)))
            }
            svc.ch(T.TGT_POS)?.let { c ->
                controls.add(CurtainCtl(cid(c),
                    (chrValue(acc.aid, c).asDouble() ?: 0.0).toFloat(), tg(T.TGT_POS)))
            }

            val hcMode = svc.ch(T.TGT_HC)?.let { chrValue(acc.aid, it).asDouble()?.toInt() }
            svc.ch(T.TGT_HC)?.let { c ->
                // mode changes rarely (seasonal) — kept out of the everyday
                // card body and surfaced instead as a tap on the header text
                val labels = listOf(0 to "Auto", 1 to "Heat", 2 to "Cool")
                val valid = c.validValues ?: labels.map { it.first }
                modeCtl = SegCtl(cid(c), labels.filter { it.first in valid }, hcMode, tg(T.TGT_HC))
            }
            svc.ch(T.TGT_AP)?.let { c ->
                val labels = listOf(0 to "Manual", 1 to "Auto")
                val valid = c.validValues ?: labels.map { it.first }
                controls.add(SegCtl(cid(c), labels.filter { it.first in valid },
                    chrValue(acc.aid, c).asDouble()?.toInt(), tg(T.TGT_AP)))
            }

            // only show the setpoint that matters for the current mode (0=auto,1=heat,2=cool)
            if (hcMode != 1) svc.ch(T.COOL_TH)?.let { c ->
                controls.add(StepCtl(cid(c), "Cool to", chrValue(acc.aid, c).asDouble() ?: 0.0,
                    c.minValue ?: 10.0, c.maxValue ?: 35.0, stepOf(c), tg(T.COOL_TH)))
            }
            if (hcMode != 2) svc.ch(T.HEAT_TH)?.let { c ->
                controls.add(StepCtl(cid(c), "Heat to", chrValue(acc.aid, c).asDouble() ?: 0.0,
                    c.minValue ?: 0.0, c.maxValue ?: 25.0, stepOf(c), tg(T.HEAT_TH)))
            }
            svc.ch(T.TGT_TEMP)?.let { c ->
                controls.add(StepCtl(cid(c), "Target", chrValue(acc.aid, c).asDouble() ?: 0.0,
                    c.minValue ?: 10.0, c.maxValue ?: 38.0, stepOf(c), tg(T.TGT_TEMP)))
            }

            svc.ch(T.SWING)?.let { c ->
                chips.add(ChipUi(cid(c), "Swing", chrValue(acc.aid, c).asBool(), true, tg(T.SWING)))
            }

            svc.ch(T.CUR_TEMP)?.let { c ->
                sensors.add(SensorUi(SensorKind.TEMP, fmtNum(chrValue(acc.aid, c)), "°C"))
            }
            svc.ch(T.HUMID)?.let { c ->
                sensors.add(SensorUi(SensorKind.HUMIDITY, fmtNum(chrValue(acc.aid, c)), "%"))
            }
            svc.ch(T.AIRQ)?.let { c ->
                val v = chrValue(acc.aid, c).asDouble()?.toInt() ?: 0
                sensors.add(SensorUi(SensorKind.AIR_QUALITY, AIRQ_LABELS.getOrElse(v) { "—" }, ""))
            }
            svc.ch(T.PM25)?.let { c ->
                sensors.add(SensorUi(SensorKind.PM25, fmtNum(chrValue(acc.aid, c)), "µg/m³"))
            }
            svc.ch(T.OCC)?.let { c ->
                sensors.add(SensorUi(SensorKind.OCCUPANCY,
                    if (chrValue(acc.aid, c).asBool()) "Active" else "Idle", ""))
            }
            svc.ch(T.MOTION)?.let { c ->
                sensors.add(SensorUi(SensorKind.MOTION,
                    if (chrValue(acc.aid, c).asBool()) "Active" else "Idle", ""))
            }
            svc.ch(T.FILTER)?.let { c ->
                sensors.add(SensorUi(SensorKind.FILTER, fmtNum(chrValue(acc.aid, c)), "% filter"))
            }
        }

        // on-state and subtitle from live (override-merged) values across all members
        for (m in members) {
            for (svc in m.services) {
                if (svc.type == SVC.INFO) continue
                for (c in svc.chars) {
                    val v = chrValue(m.aid, c)
                    if ((c.type == T.ON || c.type == T.ACTIVE) && v.asBool()) onAny = true
                    if (m === members[0]) {
                        when (c.type) {
                            T.CUR_TEMP -> v.asDouble()?.let { subParts.add("${fmtNum(it)}°C") }
                            T.CUR_HC -> v.asDouble()?.toInt()?.let { s ->
                                listOf("Off", "Idle", "Heating", "Cooling").getOrNull(s)
                                    ?.let { if (s >= 2) subParts.add(it) }
                            }
                            T.CUR_AP -> if (v.asDouble()?.toInt() == 2) subParts.add("Purifying")
                            T.BRIGHT -> v.asDouble()?.takeIf { it > 0 }
                                ?.let { subParts.add("${it.roundToInt()}%") }
                        }
                    }
                }
            }
        }
        // a fresh head-tap wins over polled state until the server catches up
        (overrides["main:$key"]?.first as? Boolean)?.let { onAny = it }

        val kind = tileKind(acc)
        if (kind == TileKind.PURIFIER) {
            // keep the wall view simple: auto/manual + speed + PM2.5 only
            chips.clear()
            sensors.retainAll { it.kind == SensorKind.PM25 }
        }
        if (kind == TileKind.AC) {
            // the header's "inside" pill already shows this same reading —
            // a second copy on the tile is noise
            sensors.removeAll { it.kind == SensorKind.TEMP }
        }

        // lights: the main brightness leaves the control list — the whole
        // card becomes the dimmer (drag across it); extra sliders (e.g. a
        // moonlight brightness on multi-light accessories) stay as rows
        var dimmer: SliderCtl? = null
        if (kind == TileKind.LIGHT) {
            dimmer = controls.filterIsInstance<SliderCtl>().firstOrNull { !it.warm }
            dimmer?.let { controls.remove(it) }
        }

        val hasToggle = toggleTargets.isNotEmpty()
        val parts = subParts.take(3).joinToString(" · ")
        val sub = when {
            hasToggle -> if (onAny) parts.ifEmpty { "On" } else "Off"
            kind == TileKind.SENSOR -> "" // the tile body shows the readings large
            else -> parts
        }

        return TileUi(
            key = key,
            name = name,
            sub = sub,
            kind = kind,
            isOn = onAny && hasToggle,
            canToggle = hasToggle,
            hidden = key in serverSettings.hidden,
            isGroup = isGroup,
            origNames = members.map { it.origName },
            controls = controls,
            chips = chips,
            // accessories often expose the same reading through several services
            sensors = sensors.distinctBy { it.kind },
            shelly = null,
            toggleTargets = toggleTargets,
            toggleIsActive = toggleIsActive,
            modeControl = modeCtl,
            dimmer = dimmer,
            moon = if (moonOnTargets.isNotEmpty()) MoonUi(moonOn, moonOnTargets, moonOffTargets) else null,
        )
    }

    private fun stepOf(c: Chr): Double =
        c.minStep?.takeIf { it >= 0.5 } ?: 1.0

    private fun fmtNum(v: Any?): String {
        val d = v.asDouble() ?: return "—"
        val r = (d * 10).roundToInt() / 10.0
        return if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
    }
}
