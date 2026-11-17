package tn.aldokkan.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Le petit serveur (Vercel + Neon) qui garde, pour chaque app, si elle est proposée aux clients sur mobile et/ou TV. */
object Backend {
    const val BASE = "https://al-dokkan-api.vercel.app"

    class Choice(val mobile: Set<String>, val tv: Set<String>)
    class Result<T>(val value: T?, val error: String?)

    private fun ids(o: JSONObject, name: String): Set<String> {
        val a = o.optJSONArray(name) ?: return emptySet()
        return (0 until a.length()).map { a.getString(it) }.toSet()
    }

    /** Lecture publique du choix de l'admin. */
    suspend fun choice(): Result<Choice> = withContext(Dispatchers.IO) {
        try {
            val c = URL("$BASE/api/catalog").openConnection() as HttpURLConnection
            c.connectTimeout = 15_000
            c.readTimeout = 20_000
            c.setRequestProperty("Accept", "application/json")
            if (c.responseCode != 200) return@withContext Result(null, "Erreur du serveur (${c.responseCode})")
            val o = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
            Result(Choice(ids(o, "mobile"), ids(o, "tv")), null)
        } catch (e: Exception) {
            Result(null, "Pas de connexion")
        }
    }

    /** Version Clients : les apps choisies pour cet appareil. */
    suspend fun clientIds(tv: Boolean): Result<Set<String>> {
        val r = choice()
        return Result(r.value?.let { if (tv) it.tv else it.mobile }, r.error)
    }

    private val publishLock = Mutex()

    /** Admin : enregistre le choix. Les envois partent dans l'ordre, le dernier gagne. */
    suspend fun publish(mobile: Set<String>, tv: Set<String>): String? = publishLock.withLock {
        withContext(Dispatchers.IO) {
            try {
                val c = URL("$BASE/api/catalog").openConnection() as HttpURLConnection
                c.requestMethod = "PUT"
                c.connectTimeout = 15_000
                c.readTimeout = 20_000
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.setRequestProperty("x-admin-key", BuildConfig.ADMIN_KEY)
                val body = JSONObject().put("mobile", JSONArray(mobile.sorted())).put("tv", JSONArray(tv.sorted()))
                c.outputStream.use { it.write(body.toString().toByteArray()) }
                when (val code = c.responseCode) {
                    200 -> null
                    401 -> "Refusé par le serveur"
                    else -> "Erreur du serveur ($code)"
                }
            } catch (e: Exception) {
                "Pas de connexion"
            }
        }
    }
}
