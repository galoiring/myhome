package com.gal.myhome.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

/**
 * Direct Yeelight LAN protocol (TCP :55443, newline-delimited JSON-RPC).
 * Requires "LAN Control" enabled per device in the Yeelight app.
 * Short-lived socket per request; the device quota is ~60 commands/minute.
 */
data class YlState(
    val power: Boolean,
    val bright: Int,
    val ctK: Int,
    val moonSupported: Boolean,
    val moonlight: Boolean,
)

data class YlFound(val ip: String, val model: String)

class YeelightClient {

    private fun request(ip: String, method: String, params: JSONArray): JSONObject? {
        Socket().use { sock ->
            sock.connect(InetSocketAddress(ip, 55443), 2500)
            sock.soTimeout = 2500
            val cmd = JSONObject().put("id", 1).put("method", method).put("params", params)
            sock.getOutputStream().write((cmd.toString() + "\r\n").toByteArray())
            val reader = sock.getInputStream().bufferedReader()
            // the device may interleave unsolicited "props" notifications
            repeat(5) {
                val line = reader.readLine() ?: return null
                val o = try { JSONObject(line) } catch (_: Exception) { return@repeat }
                if (o.optInt("id", -1) == 1) return o
            }
        }
        return null
    }

    suspend fun state(ip: String): YlState? = withContext(Dispatchers.IO) {
        try {
            val r = request(ip, "get_prop",
                JSONArray(listOf("power", "bright", "ct", "active_mode")))
            val res = r?.optJSONArray("result") ?: return@withContext null
            val activeMode = res.optString(3)
            YlState(
                power = res.optString(0) == "on",
                bright = res.optString(1).toIntOrNull() ?: 0,
                ctK = res.optString(2).toIntOrNull() ?: 4000,
                moonSupported = activeMode.isNotEmpty(),
                moonlight = activeMode == "1",
            )
        } catch (_: Exception) { null }
    }

    suspend fun setPower(ip: String, on: Boolean) =
        cmd(ip, "set_power", listOf(if (on) "on" else "off", "smooth", 400))

    /** mode 5 = nightlight (moonlight), 1 = normal */
    suspend fun setMoonlight(ip: String, moon: Boolean) =
        cmd(ip, "set_power", listOf("on", "smooth", 400, if (moon) 5 else 1))

    suspend fun setBright(ip: String, v: Int) =
        cmd(ip, "set_bright", listOf(v.coerceIn(1, 100), "smooth", 400))

    suspend fun setCtKelvin(ip: String, k: Int) =
        cmd(ip, "set_ct_abx", listOf(k.coerceIn(1700, 6500), "smooth", 400))

    private suspend fun cmd(ip: String, method: String, params: List<Any>) =
        withContext(Dispatchers.IO) {
            try { request(ip, method, JSONArray(params)) } catch (_: Exception) { null }
        }

    /** SSDP discovery (multicast M-SEARCH on :1982). Best-effort; some networks block it. */
    suspend fun discover(timeoutMs: Long = 2500): List<YlFound> = withContext(Dispatchers.IO) {
        val results = LinkedHashMap<String, YlFound>()
        try {
            DatagramSocket().use { sock ->
                sock.soTimeout = 500
                val msg = ("M-SEARCH * HTTP/1.1\r\n" +
                        "HOST: 239.255.255.250:1982\r\n" +
                        "MAN: \"ssdp:discover\"\r\n" +
                        "ST: wifi_bulb\r\n\r\n").toByteArray()
                sock.send(DatagramPacket(msg, msg.size,
                    InetAddress.getByName("239.255.255.250"), 1982))
                val end = System.currentTimeMillis() + timeoutMs
                val buf = ByteArray(4096)
                while (System.currentTimeMillis() < end) {
                    val packet = DatagramPacket(buf, buf.size)
                    try { sock.receive(packet) } catch (_: SocketTimeoutException) { continue }
                    val txt = String(packet.data, 0, packet.length)
                    if (!txt.contains("yeelight", ignoreCase = true)) continue
                    val ip = packet.address.hostAddress ?: continue
                    val model = Regex("model:\\s*(\\S+)", RegexOption.IGNORE_CASE)
                        .find(txt)?.groupValues?.get(1) ?: "?"
                    results[ip] = YlFound(ip, model)
                }
            }
        } catch (_: Exception) { /* return what we have */ }
        results.values.toList()
    }
}

/** UI slider shows "warmth" 0..100 (right = warmer); device wants Kelvin 1700..6500. */
fun warmthToKelvin(warmth: Int): Int = 6500 - (warmth.coerceIn(0, 100) * 48)
fun kelvinToWarmth(k: Int): Int = ((6500 - k.coerceIn(1700, 6500)) / 48.0).toInt().coerceIn(0, 100)
