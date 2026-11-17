package tn.aldokkan.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Le petit serveur (Vercel + Neon) qui garde la liste des apps choisies pour les clients. */
object Backend {
    const val BASE = "https://al-dokkan-api.vercel.app"

    class Result<T>(val value: T?, val error: String?)

    /** Lecture publique : les apps que l'admin a choisies pour les clients. */
    suspend fun clientIds(): Result<Set<String>> = withContext(Dispatchers.IO) {
        try {
            val c = URL("$BASE/api/catalog").openConnection() as HttpURLConnection
            c.connectTimeout = 15_000
            c.readTimeout = 20_000
            c.setRequestProperty("Accept", "application/json")
            if (c.responseCode != 200) return@withContext Result(null, "Erreur du serveur (${c.responseCode})")
            val arr = JSONObject(c.inputStream.bufferedReader().use { it.readText() }).getJSONArray("ids")
            Result((0 until arr.length()).map { arr.getString(it) }.toSet(), null)
        } catch (e: Exception) {
            Result(null, "Pas de connexion")
        }
    }

    /** Publication (admin) : remplace la liste des apps visibles par les clients. */
    suspend fun publish(ids: Set<String>, key: String): String? = withContext(Dispatchers.IO) {
        try {
            val c = URL("$BASE/api/catalog").openConnection() as HttpURLConnection
            c.requestMethod = "PUT"
            c.connectTimeout = 15_000
            c.readTimeout = 20_000
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.setRequestProperty("x-admin-key", key)
            c.outputStream.use { it.write(JSONObject().put("ids", JSONArray(ids.sorted())).toString().toByteArray()) }
            when (val code = c.responseCode) {
                200 -> null
                401 -> "Clé d'administration refusée"
                else -> "Erreur du serveur ($code)"
            }
        } catch (e: Exception) {
            "Pas de connexion"
        }
    }
}
