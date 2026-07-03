package com.gal.myhome

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.gal.myhome.data.SortMode
import com.gal.myhome.data.Svc
import com.gal.myhome.data.T
import com.gal.myhome.data.Target
import com.gal.myhome.data.Weather
import com.gal.myhome.data.asBool
import com.gal.myhome.data.asDouble
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/* ---------- UI models ---------- */

enum class TileKind { LIGHT, AC, PURIFIER, FAN, OUTLET, SWITCH, SENSOR, CURTAIN, OTHER }
enum class SensorKind { TEMP, HUMIDITY, AIR_QUALITY, PM25, OCCUPANCY, MOTION, FILTER }

sealed interface Control
data class SliderCtl(
    val id: String, val label: String, val unit: String,
    val min: Float, val max: Float, val step: Float, val value: Float,
    val warm: Boolean, val targets: List<Target>,
) : Control

data class SegCtl(
    val id: String, val options: List<Pair<Int, String>>, val value: Int?,
    val targets: List<Target>,
) : Control

data class StepCtl(
    val id: String, val label: String, val value: Double,
    val min: Double, val max: Double, val step: Double, val targets: List<Target>,
) : Control

data class SwatchCtl(val hue: List<Target>, val sat: List<Target>) : Control

data class ChipUi(
    val id: String, val label: String, val on: Boolean,
    val isActive: Boolean, val targets: List<Target>,
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
)

data class UiState(
    val tiles: List<TileUi> = emptyList(),
    val weather: Weather? = null,
    val indoorTemp: Double? = null,
    val offline: Boolean = false,
    val loaded: Boolean = false,
)

private const val TOUCH_HOLD_MS = 5000L

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val api = HomeApi()
    val prefsRepo = PrefsRepo(app)
    val prefs = prefsRepo.flow.stateIn(viewModelScope, SharingStarted.Eagerly, Prefs())

    var ui by mutableStateOf(UiState())
        private set

    private var accs: List<Acc> = emptyList()
    private var shellyDevs: List<ShellyDevice> = emptyList()
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
                    accs = a.await()
                    shellyDevs = s.await()
                }
                if (!settingsLoaded.value) refreshServerSettings()
                pruneOverrides()
                rebuild(offline = false)
            } catch (_: Exception) {
                ui = ui.copy(offline = true)
            }
            delay(prefs.value.pollSeconds * 1000L)
        }
    }

    private suspend fun weatherLoop() {
        while (true) {
            api.baseUrl = prefs.value.serverUrl
            try {
                ui = ui.copy(weather = api.weather())
            } catch (_: Exception) { /* retry next cycle */ }
            delay(10 * 60 * 1000L)
        }
    }

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

    fun setColor(hue: List<Target>, sat: List<Target>, h: Int, s: Int) {
        for (t in hue) override("${t.aid}.${t.iid}", h)
        for (t in sat) override("${t.aid}.${t.iid}", s)
        viewModelScope.launch {
            try {
                api.setChars(hue, h)
                api.setChars(sat, s)
            } catch (_: Exception) { }
        }
    }

    fun toggleTile(tile: TileUi) {
        if (tile.shelly != null) {
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
            offline = offline,
            loaded = true,
        )
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

        val weight: (TileUi) -> Int = { t ->
            when {
                t.isGroup -> -1
                t.kind == TileKind.LIGHT -> 0
                t.kind == TileKind.AC || t.kind == TileKind.PURIFIER -> 1
                t.kind == TileKind.SWITCH || t.kind == TileKind.OUTLET -> 2
                else -> 3
            }
        }
        return when (prefs.value.sortMode) {
            SortMode.AUTO -> list.sortedBy(weight)
            SortMode.NAME -> list.sortedBy { it.name.lowercase() }
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
        var mainToggleSet = false
        var onAny = false
        val subParts = mutableListOf<String>()
        val occCount = mutableMapOf<String, Int>()

        for (svc in acc.services) {
            if (svc.type == SVC.INFO || svc.type == SVC.PROTO) continue
            val svcOcc = occCount[svc.type] ?: 0
            occCount[svc.type] = svcOcc + 1
            fun tg(chType: String) = targetsFor(members, svc.type, svcOcc, chType)
            fun cid(c: Chr) = "${acc.aid}.${c.iid}"

            val on = svc.ch(T.ON)
            val active = svc.ch(T.ACTIVE)

            if ((svc.type == SVC.SWITCH || svc.type == SVC.OUTLET || svc.type == SVC.FAN) && mainToggleSet && on != null) {
                chips.add(ChipUi(cid(on), svcName(svc, "Switch"),
                    chrValue(acc.aid, on).asBool(), false, tg(T.ON)))
                continue
            }

            val tgl = on ?: active
            if (tgl != null && !mainToggleSet) {
                toggleTargets = tg(if (on != null) T.ON else T.ACTIVE)
                toggleIsActive = active != null && on == null
                mainToggleSet = true
            } else if (tgl != null && mainToggleSet && svc.type == SVC.LIGHT) {
                chips.add(ChipUi(cid(tgl), svcName(svc, "Light"),
                    chrValue(acc.aid, tgl).asBool(), false, tg(T.ON)))
            }

            svc.ch(T.BRIGHT)?.let { c ->
                controls.add(SliderCtl(cid(c), "Brightness", "%",
                    (c.minValue ?: 0.0).toFloat(), (c.maxValue ?: 100.0).toFloat(),
                    (c.minStep ?: 1.0).toFloat(),
                    (chrValue(acc.aid, c).asDouble() ?: 0.0).toFloat(), false, tg(T.BRIGHT)))
            }
            svc.ch(T.SPEED)?.let { c ->
                controls.add(SliderCtl(cid(c), "Speed", "%",
                    (c.minValue ?: 0.0).toFloat(), (c.maxValue ?: 100.0).toFloat(),
                    (c.minStep ?: 1.0).toFloat(),
                    (chrValue(acc.aid, c).asDouble() ?: 0.0).toFloat(), false, tg(T.SPEED)))
            }
            svc.ch(T.CT)?.let { c ->
                controls.add(SliderCtl(cid(c), "Warmth", "",
                    (c.minValue ?: 140.0).toFloat(), (c.maxValue ?: 500.0).toFloat(),
                    (c.minStep ?: 1.0).toFloat(),
                    (chrValue(acc.aid, c).asDouble() ?: 0.0).toFloat(), true, tg(T.CT)))
            }
            svc.ch(T.TGT_POS)?.let { c ->
                controls.add(SliderCtl(cid(c), "Position", "%",
                    (c.minValue ?: 0.0).toFloat(), (c.maxValue ?: 100.0).toFloat(),
                    (c.minStep ?: 1.0).toFloat(),
                    (chrValue(acc.aid, c).asDouble() ?: 0.0).toFloat(), false, tg(T.TGT_POS)))
            }

            val hue = svc.ch(T.HUE)
            val sat = svc.ch(T.SAT)
            if (hue != null && sat != null) controls.add(SwatchCtl(tg(T.HUE), tg(T.SAT)))

            svc.ch(T.TGT_HC)?.let { c ->
                val labels = listOf(0 to "Auto", 1 to "Heat", 2 to "Cool")
                val valid = c.validValues ?: labels.map { it.first }
                controls.add(SegCtl(cid(c), labels.filter { it.first in valid },
                    chrValue(acc.aid, c).asDouble()?.toInt(), tg(T.TGT_HC)))
            }
            svc.ch(T.TGT_AP)?.let { c ->
                val labels = listOf(0 to "Manual", 1 to "Auto")
                val valid = c.validValues ?: labels.map { it.first }
                controls.add(SegCtl(cid(c), labels.filter { it.first in valid },
                    chrValue(acc.aid, c).asDouble()?.toInt(), tg(T.TGT_AP)))
            }

            svc.ch(T.COOL_TH)?.let { c ->
                controls.add(StepCtl(cid(c), "Cool to", chrValue(acc.aid, c).asDouble() ?: 0.0,
                    c.minValue ?: 10.0, c.maxValue ?: 35.0, stepOf(c), tg(T.COOL_TH)))
            }
            svc.ch(T.HEAT_TH)?.let { c ->
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

        val hasToggle = toggleTargets.isNotEmpty()
        val parts = subParts.take(3).joinToString(" · ")
        val sub = if (hasToggle) (if (onAny) parts.ifEmpty { "On" } else "Off") else parts

        return TileUi(
            key = key,
            name = name,
            sub = sub,
            kind = tileKind(acc),
            isOn = onAny && hasToggle,
            canToggle = hasToggle,
            hidden = key in serverSettings.hidden,
            isGroup = isGroup,
            origNames = members.map { it.origName },
            controls = controls,
            chips = chips,
            sensors = sensors,
            shelly = null,
            toggleTargets = toggleTargets,
            toggleIsActive = toggleIsActive,
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
