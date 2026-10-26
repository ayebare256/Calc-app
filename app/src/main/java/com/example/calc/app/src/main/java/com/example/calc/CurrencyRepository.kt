package com.example.calc

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Rates(val rates: Map<String, Double>, val updated: String)

/** Live rates for ~160 currencies (base USD) from open.er-api.com; cached for offline use. */
object CurrencyRepository {
    private const val URL_STR = "https://open.er-api.com/v6/latest/USD"
    private const val PREFS = "rates"

    fun cached(ctx: Context): Rates? {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = p.getString("json", null) ?: return null
        return parse(json)
    }

    suspend fun fetch(ctx: Context): Result<Rates> = withContext(Dispatchers.IO) {
        runCatching {
            val c = URL(URL_STR).openConnection() as HttpURLConnection
            c.connectTimeout = 10_000; c.readTimeout = 10_000
            val body = c.inputStream.bufferedReader().use { it.readText() }
            val r = parse(body) ?: error("Bad response")
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("json", body).apply()
            r
        }
    }

    private fun parse(body: String): Rates? = runCatching {
        val o = JSONObject(body)
        if (o.optString("result") != "success") return null
        val r = o.getJSONObject("rates")
        Rates(r.keys().asSequence().associateWith { r.getDouble(it) }, o.optString("time_last_update_utc").take(16))
    }.getOrNull()
}
