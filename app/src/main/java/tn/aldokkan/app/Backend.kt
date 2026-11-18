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

    class Choice(val mobile: Set<String>, val tv: Set<String>, val enrolled: Boolean = false, val device: String? = null)
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
            Result(Choice(ids(o, "mobile"), ids(o, "tv"), o.optBoolean("enrolled"), o.optString("device").ifBlank { null }), null)
        } catch (e: Exception) {
            Result(null, "Pas de connexion")
        }
    }

    /** Version Clients : les apps choisies pour cet appareil. */
    suspend fun clientIds(tv: Boolean): Result<Set<String>> {
        val r = choice()
        return Result(r.value?.let { if (tv) it.tv else it.mobile }, r.error)
    }

    /** Admin : enregistre la clé de cet appareil (accepté une seule fois par le serveur). */
    suspend fun enroll(): String? = withContext(Dispatchers.IO) {
        try {
            val c = URL("$BASE/api/enroll").openConnection() as HttpURLConnection
            c.requestMethod = "POST"
            c.connectTimeout = 15_000
            c.readTimeout = 20_000
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(JSONObject().put("publicKey", DeviceKey.publicKey()).toString().toByteArray()) }
            when (val code = c.responseCode) {
                200 -> null
                else -> "Erreur du serveur ($code)"
            }
        } catch (e: Exception) {
            "Pas de connexion"
        }
    }

    private val publishLock = Mutex()

    private fun list(l: Set<String>) = l.sorted().joinToString(",") { "\"$it\"" }

    /** Admin : enregistre le choix, signé avec la clé de l'appareil. Les envois partent dans l'ordre, le dernier gagne. */
    suspend fun publish(mobile: Set<String>, tv: Set<String>): String? = publishLock.withLock {
        withContext(Dispatchers.IO) {
            try {
                // Même texte compact que sur le serveur : listes triées.
                val body = "{\"mobile\":[${list(mobile)}],\"tv\":[${list(tv)}]}"
                val ts = System.currentTimeMillis()
                val c = URL("$BASE/api/catalog").openConnection() as HttpURLConnection
                c.requestMethod = "PUT"
                c.connectTimeout = 15_000
                c.readTimeout = 20_000
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.setRequestProperty("x-ts", ts.toString())
                c.setRequestProperty("x-sig", DeviceKey.sign("$ts.$body".toByteArray()))
                c.outputStream.use { it.write(body.toByteArray()) }
                when (val code = c.responseCode) {
                    200 -> null
                    401 -> "Refusé : rouvre l'app (ou vérifie l'heure du téléphone)"
                    else -> "Erreur du serveur ($code)"
                }
            } catch (e: Exception) {
                "Pas de connexion"
            }
        }
    }
}
