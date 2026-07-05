package com.gal.myhome.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class HomeApi {

    // overwritten from Prefs before any real request; this initial value is
    // never actually hit
    @Volatile
    var baseUrl: String = "http://192.168.1.100:8090"

    private val json = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private fun url(path: String) = baseUrl.trimEnd('/') + path

    private fun getBody(path: String): String {
        client.newCall(Request.Builder().url(url(path)).build()).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} for $path")
            return resp.body?.string() ?: throw IOException("empty body for $path")
        }
    }

    private fun postBody(path: String, body: String) {
        client.newCall(
            Request.Builder().url(url(path)).post(body.toRequestBody(json)).build()
        ).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} for $path")
        }
    }

    suspend fun accessories(): List<Acc> = withContext(Dispatchers.IO) {
        val root = JSONObject(getBody("/api/accessories"))
        val arr = root.getJSONArray("accessories")
        (0 until arr.length()).map { parseAcc(arr.getJSONObject(it)) }
    }

    suspend fun shelly(): List<ShellyDevice> = withContext(Dispatchers.IO) {
        val arr = JSONArray(getBody("/api/shelly"))
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            if (o.has("error") || !o.has("components")) return@mapNotNull null
            val comps = o.getJSONArray("components")
            ShellyDevice(
                ip = o.getString("ip"),
                name = o.optString("name", "").ifEmpty { null },
                comps = (0 until comps.length()).map { j ->
                    val c = comps.getJSONObject(j)
                    ShellyComp(
                        id = c.getInt("id"),
                        type = c.optString("type", "switch"),
                        name = c.optString("name", "").ifEmpty { null },
                        state = c.optBoolean("state", false),
                        apower = c.optDouble("apower", 0.0),
                    )
                },
            )
        }
    }

    suspend fun weather(): Weather = withContext(Dispatchers.IO) {
        val o = JSONObject(getBody("/api/weather"))
        val cur = o.getJSONObject("current")
        val daily = o.getJSONObject("daily")

        // hourly forecast: everything after the current hour ("time" entries
        // are local ISO like 2026-07-05T14:00 — string compare works). Absent
        // until the server is updated to request hourly data; optJSONObject
        // keeps old-server responses parsing fine.
        val hours = mutableListOf<HourForecast>()
        o.optJSONObject("hourly")?.let { hourly ->
            val times = hourly.getJSONArray("time")
            val temps = hourly.getJSONArray("temperature_2m")
            val codes = hourly.getJSONArray("weather_code")
            val rains = hourly.optJSONArray("precipitation_probability")
            val nowIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:00", java.util.Locale.US)
                .format(java.util.Date())
            for (i in 0 until times.length()) {
                val t = times.getString(i)
                if (t <= nowIso || hours.size >= 6) continue
                hours.add(HourForecast(
                    hour = t.substring(11, 13).toInt(),
                    temp = temps.getDouble(i),
                    code = codes.getInt(i),
                    rain = rains?.optInt(i) ?: 0,
                ))
            }
        }

        Weather(
            temp = cur.getDouble("temperature_2m"),
            feels = cur.getDouble("apparent_temperature"),
            code = cur.getInt("weather_code"),
            humidity = cur.getInt("relative_humidity_2m"),
            wind = cur.getDouble("wind_speed_10m"),
            hi = daily.getJSONArray("temperature_2m_max").getDouble(0),
            lo = daily.getJSONArray("temperature_2m_min").getDouble(0),
            rainToday = daily.optJSONArray("precipitation_probability_max")?.optInt(0),
            hours = hours,
        )
    }

    /** Epoch ms of the last doorbell press (0 = never); set by Scrypted
     * calling the server's /api/doorbell/ring webhook. */
    suspend fun doorbellRing(): Long = withContext(Dispatchers.IO) {
        JSONObject(getBody("/api/doorbell")).optLong("ring", 0L)
    }

    // "<accessory name>|<temp|humidity|pm25>" -> [(epoch ms, value)]
    suspend fun history(): Map<String, List<Pair<Long, Double>>> = withContext(Dispatchers.IO) {
        val o = JSONObject(getBody("/api/history"))
        buildMap {
            o.keys().forEach { k ->
                val arr = o.getJSONArray(k)
                put(k, (0 until arr.length()).map { i ->
                    val p = arr.getJSONArray(i)
                    p.getLong(0) to p.getDouble(1)
                })
            }
        }
    }

    suspend fun settings(): ServerSettings = withContext(Dispatchers.IO) {
        val o = JSONObject(getBody("/api/settings"))
        val s = ServerSettings()
        o.optJSONObject("names")?.let { n ->
            n.keys().forEach { k -> s.names[k] = n.getString(k) }
        }
        o.optJSONArray("groups")?.let { g ->
            for (i in 0 until g.length()) {
                val go = g.getJSONObject(i)
                val members = go.optJSONArray("members") ?: JSONArray()
                s.groups.add(Group(
                    name = go.optString("name", "Group"),
                    members = (0 until members.length()).map { members.getString(it) },
                ))
            }
        }
        o.optJSONArray("hidden")?.let { h ->
            for (i in 0 until h.length()) s.hidden.add(h.getString(i))
        }
        o.optJSONArray("shellies")?.let { sh -> s.shelliesRaw = sh.toString() }
        s
    }

    suspend fun saveSettings(s: ServerSettings) = withContext(Dispatchers.IO) {
        val o = JSONObject()
        o.put("names", JSONObject(s.names as Map<*, *>))
        o.put("groups", JSONArray(s.groups.map { g ->
            JSONObject().put("name", g.name).put("members", JSONArray(g.members))
        }))
        o.put("hidden", JSONArray(s.hidden))
        o.put("shellies", JSONArray(s.shelliesRaw))
        postBody("/api/settings", o.toString())
    }

    suspend fun setChars(targets: List<Target>, value: Any) = withContext(Dispatchers.IO) {
        val chars = JSONArray(targets.map { t ->
            JSONObject().put("aid", t.aid).put("iid", t.iid).put("value", value)
        })
        postBody("/api/set", JSONObject().put("characteristics", chars).toString())
    }

    suspend fun setShelly(ip: String, id: Int, type: String, state: Boolean) =
        withContext(Dispatchers.IO) {
            postBody("/api/shelly/set", JSONObject()
                .put("ip", ip).put("id", id).put("type", type).put("state", state)
                .toString())
        }

    private fun parseAcc(o: JSONObject): Acc {
        val services = o.getJSONArray("services")
        return Acc(
            aid = o.getInt("aid"),
            services = (0 until services.length()).map { i ->
                val s = services.getJSONObject(i)
                val chars = s.getJSONArray("characteristics")
                Svc(
                    type = s.getString("type"),
                    chars = (0 until chars.length()).map { j -> parseChr(chars.getJSONObject(j)) },
                )
            },
        )
    }

    private fun parseChr(c: JSONObject): Chr {
        val valid = c.optJSONArray("valid-values")?.let { v ->
            (0 until v.length()).map { v.getInt(it) }
        }
        return Chr(
            iid = c.getInt("iid"),
            type = c.getString("type"),
            value = if (c.isNull("value")) null else c.get("value"),
            minValue = if (c.has("minValue")) c.optDouble("minValue") else null,
            maxValue = if (c.has("maxValue")) c.optDouble("maxValue") else null,
            minStep = if (c.has("minStep")) c.optDouble("minStep") else null,
            validValues = valid,
        )
    }
}
