package tn.aldokkan.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import kotlin.coroutines.coroutineContext

/** La dernière version publiée d'une app sur GitHub (une « release » avec un fichier .apk). */
data class Release(
    val tag: String,
    val name: String,
    val notes: String,
    val publishedAt: Long,
    val assetId: Long,
    val assetName: String,
    val assetSize: Long,
    val assetUpdatedAt: Long,
) {
    val stamp: String get() = "$assetId:$assetUpdatedAt"
}

sealed class Remote {
    data object Loading : Remote()
    data class Ok(val release: Release) : Remote()
    data class NoRelease(val reason: String) : Remote()
    data class Failed(val message: String) : Remote()
}

object GitHub {
    private const val API = "https://api.github.com"

    private fun connect(url: String, token: String?, accept: String): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 30_000
        c.instanceFollowRedirects = false
        c.setRequestProperty("Accept", accept)
        c.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        c.setRequestProperty("User-Agent", "Al-Dokkan")
        if (!token.isNullOrBlank()) c.setRequestProperty("Authorization", "Bearer $token")
        return c
    }

    private fun time(s: String?): Long =
        runCatching { Instant.parse(s).toEpochMilli() }.getOrDefault(0L)

    suspend fun latest(app: AppEntry, token: String): Remote = withContext(Dispatchers.IO) {
        try {
            val c = connect("$API/repos/${app.repo}/releases?per_page=10", token, "application/vnd.github+json")
            when (val code = c.responseCode) {
                200 -> {
                    val arr = JSONArray(c.inputStream.bufferedReader().use { it.readText() })
                    for (i in 0 until arr.length()) {
                        val r = arr.getJSONObject(i)
                        if (r.optBoolean("draft")) continue
                        val assets = r.optJSONArray("assets") ?: continue
                        var pick = -1
                        for (j in 0 until assets.length()) {
                            val n = assets.getJSONObject(j).optString("name")
                            if (!n.endsWith(".apk", ignoreCase = true)) continue
                            if (pick == -1) pick = j
                            if (app.apkHint != null && n.contains(app.apkHint, ignoreCase = true)) { pick = j; break }
                        }
                        if (pick == -1) continue
                        val a = assets.getJSONObject(pick)
                        return@withContext Remote.Ok(
                            Release(
                                tag = r.optString("tag_name"),
                                name = r.optString("name").ifBlank { r.optString("tag_name") },
                                notes = r.optString("body"),
                                publishedAt = time(r.optString("published_at")),
                                assetId = a.getLong("id"),
                                assetName = a.optString("name"),
                                assetSize = a.optLong("size"),
                                assetUpdatedAt = time(a.optString("updated_at")),
                            )
                        )
                    }
                    Remote.NoRelease("Pas encore de version publiée")
                }
                401 -> Remote.Failed("Jeton d'accès refusé : vérifie-le dans Réglages")
                403, 429 -> Remote.Failed("Le serveur a refusé la demande (limite atteinte ?). Réessaie plus tard")
                404 -> Remote.Failed(
                    if (token.isBlank()) "App privée : ajoute ton jeton d'accès dans Réglages"
                    else "App introuvable : le jeton n'y donne pas accès"
                )
                else -> Remote.Failed("Erreur du serveur ($code)")
            }
        } catch (e: IOException) {
            Remote.Failed("Pas de connexion")
        } catch (e: Exception) {
            Remote.Failed("Réponse illisible")
        }
    }

    /** Télécharge l'APK. GitHub redirige vers un autre serveur : le jeton ne doit pas le suivre. */
    suspend fun download(
        app: AppEntry,
        release: Release,
        token: String,
        dest: File,
        onProgress: (Long, Long) -> Unit,
    ) = withContext(Dispatchers.IO) {
        var url = "$API/repos/${app.repo}/releases/assets/${release.assetId}"
        var auth: String? = token
        var accept = "application/octet-stream"
        var hops = 0
        while (true) {
            val c = connect(url, auth, accept)
            val code = c.responseCode
            if (code in intArrayOf(301, 302, 303, 307, 308)) {
                url = c.getHeaderField("Location") ?: throw IOException("Redirection sans adresse")
                c.disconnect()
                auth = null
                accept = "*/*"
                if (++hops > 5) throw IOException("Trop de redirections")
                continue
            }
            if (code != 200) {
                c.disconnect()
                throw IOException(if (code == 404 || code == 401) "Accès refusé au fichier : vérifie ton jeton d'accès" else "Téléchargement refusé ($code)")
            }
            val total = c.contentLengthLong.takeIf { it > 0 } ?: release.assetSize
            dest.parentFile?.mkdirs()
            val tmp = File(dest.path + ".part")
            c.inputStream.use { input ->
                tmp.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    var lastReport = 0L
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        val now = System.currentTimeMillis()
                        if (now - lastReport > 150) { onProgress(done, total); lastReport = now }
                    }
                    onProgress(done, total)
                }
            }
            c.disconnect()
            if (dest.exists()) dest.delete()
            if (!tmp.renameTo(dest)) throw IOException("Impossible d'enregistrer le fichier")
            return@withContext
        }
    }
}
