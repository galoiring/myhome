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
    // opaque passthrough — the shelly device list is managed by the server;
    // round-trip it untouched so app-side saves never corrupt it
    var shelliesRaw: String = "[]",
)

data class Weather(
    val temp: Double,
    val feels: Double,
    val code: Int,
    val humidity: Int,
    val wind: Double,
    val hi: Double,
    val lo: Double,
)

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
