package com.gal.myhome.data

// HomeKit characteristic type codes (short hex form, as served by the dashboard API)
object T {
    const val NAME = "23"; const val ON = "25"; const val BRIGHT = "8"
    const val HUE = "13"; const val SAT = "2F"; const val CT = "CE"
    const val ACTIVE = "B0"; const val CUR_HC = "B1"; const val TGT_HC = "B2"
    const val CUR_TEMP = "11"; const val COOL_TH = "D"; const val HEAT_TH = "12"; const val TGT_TEMP = "35"
    const val HUMID = "10"; const val AIRQ = "95"; const val PM25 = "C6"
    const val TGT_AP = "A8"; const val CUR_AP = "A9"; const val SPEED = "29"; const val SWING = "B6"
    const val OCC = "71"; const val MOTION = "22"; const val CONTACT = "6A"; const val FILTER = "AB"
    const val CUR_POS = "6D"; const val TGT_POS = "7C"
    // StatusActive: sensor services set this false when the device behind them
    // is unreachable — homebridge-miot then reports every reading as 0, which
    // is indistinguishable from a real reading unless this flag is consulted
    const val STATUS_ACTIVE = "75"
}

// HomeKit service type codes
object SVC {
    const val INFO = "3E"; const val PROTO = "A2"; const val LIGHT = "43"; const val SWITCH = "49"
    const val FAN = "40"; const val FAN2 = "B7"; const val HC = "BC"; const val AP = "BB"
    const val AIRQ = "8D"; const val TEMP = "8A"; const val HUM = "82"; const val OCC = "86"
    const val MOTION = "85"; const val CONTACT = "80"; const val FILTER = "BA"; const val OUTLET = "47"
    const val WC = "8C"
}

val AIRQ_LABELS = listOf("—", "Excellent", "Good", "Fair", "Inferior", "Poor")

data class Chr(
    val iid: Int,
    val type: String,
    val value: Any?,
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val minStep: Double? = null,
    val validValues: List<Int>? = null,
)

data class Svc(val type: String, val chars: List<Chr>) {
    fun ch(type: String): Chr? = chars.firstOrNull { it.type == type }
}

data class Acc(val aid: Int, val services: List<Svc>) {
    val origName: String
        get() = services.firstOrNull { it.type == SVC.INFO }?.ch(T.NAME)?.value as? String
            ?: "Accessory $aid"
}

data class ShellyComp(
    val id: Int,
    val type: String,
    val name: String?,
    val state: Boolean,
    val apower: Double,
)

data class ShellyDevice(val ip: String, val name: String?, val comps: List<ShellyComp>)

data class Group(var name: String, val members: List<String>)

data class ServerSettings(
    val names: MutableMap<String, String> = mutableMapOf(),
    val groups: MutableList<Group> = mutableListOf(),
    val hidden: MutableList<String> = mutableListOf(),
    // opaque passthrough — the shelly device list and the extra HAP bridges
    // polled for climate readings are managed by the server; round-trip them
    // untouched so app-side saves never corrupt them
    var shelliesRaw: String = "[]",
    var pullSensorsRaw: String = "[]",
)

data class HourForecast(
    val hour: Int, // 0-23, local time
    val temp: Double,
    val code: Int,
    val rain: Int, // precipitation probability %
)

data class Weather(
    val temp: Double,
    val feels: Double,
    val code: Int,
    val humidity: Int,
    val wind: Double,
    val hi: Double,
    val lo: Double,
    // empty / null while the server still runs the pre-forecast API
    val rainToday: Int? = null,
    val hours: List<HourForecast> = emptyList(),
)

// Model 3 charge, relayed by the dashboard server from TeslaMate on the same
// host. `state` is TeslaMate's own ("online" / "asleep" / "offline"): anything
// but online means the reading is last-known rather than live, so the header
// shows it dimmed with its age instead of implying the car just reported.
data class Tesla(
    val battery: Int,
    val rangeKm: Double?,
    val pluggedIn: Boolean,
    val chargingState: String,
    val state: String,
    // when the server polled — NOT when the car last reported. Using this for
    // the age label made it permanently read "0m"
    val ts: Long,
    // when the car entered its current state; this is what "last heard from"
    // actually means, and it's null on servers too old to send it
    val stateSince: Long?,
) {
    val live: Boolean get() = state.equals("online", ignoreCase = true)
    val charging: Boolean get() = chargingState.equals("charging", ignoreCase = true)

    /** How long the reading has been standing still, in minutes. */
    val ageMinutes: Long?
        get() = stateSince?.let { (System.currentTimeMillis() - it) / 60000L }

    /** A parked Tesla sleeps within minutes and stays asleep for days, and its
     * charge doesn't drift while it does — so "asleep" is not a reason to
     * distrust the number, only to say how old it is. Reserve the faded
     * treatment for a feed that has genuinely stopped. */
    val stale: Boolean get() = (ageMinutes ?: 0L) > 12 * 60
}

data class Target(val aid: Int, val iid: Int)

fun Any?.asDouble(): Double? = when (this) {
    is Number -> toDouble()
    is Boolean -> if (this) 1.0 else 0.0
    else -> null
}

fun Any?.asBool(): Boolean = when (this) {
    is Boolean -> this
    is Number -> toDouble() != 0.0
    else -> false
}

fun wmoInfo(code: Int): Pair<String, String> = when {
    code == 0 -> "☀️" to "Clear"
    code <= 2 -> "🌤️" to "Partly cloudy"
    code == 3 -> "☁️" to "Overcast"
    code == 45 || code == 48 -> "🌫️" to "Fog"
    code <= 57 -> "🌦️" to "Drizzle"
    code <= 67 -> "🌧️" to "Rain"
    code <= 77 -> "🌨️" to "Snow"
    code <= 82 -> "🌧️" to "Showers"
    code <= 86 -> "🌨️" to "Snow showers"
    else -> "⛈️" to "Thunderstorm"
}
