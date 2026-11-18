package tn.aldokkan.app

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

data class InstalledInfo(val versionName: String, val lastUpdate: Long)

data class Progress(val done: Long, val total: Long) {
    val fraction: Float get() = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f
}

data class UiState(
    val remote: Map<String, Remote> = emptyMap(),
    val installed: Map<String, InstalledInfo> = emptyMap(),
    val stamps: Map<String, String> = emptyMap(),
    val progress: Map<String, Progress> = emptyMap(),
    val installing: Set<String> = emptySet(),
    val message: String? = null,
    val askInstallPermission: Boolean = false,
)

enum class AppStatus { Checking, Failed, NotPublished, Install, Update, Open }

fun statusOf(app: AppEntry, ui: UiState): AppStatus {
    val inst = ui.installed[app.pkg]
    return when (val r = ui.remote[app.id]) {
        null, Remote.Loading -> if (inst != null) AppStatus.Open else AppStatus.Checking
        is Remote.Failed -> if (inst != null) AppStatus.Open else AppStatus.Failed
        is Remote.NoRelease -> if (inst != null) AppStatus.Open else AppStatus.NotPublished
        is Remote.Ok -> {
            if (inst == null) {
                AppStatus.Install
            } else {
                val stamp = ui.stamps[app.pkg]
                val newer = if (stamp != null) stamp != r.release.stamp else r.release.assetUpdatedAt > inst.lastUpdate
                if (newer) AppStatus.Update else AppStatus.Open
            }
        }
    }
}

class StoreViewModel(application: Application) : AndroidViewModel(application) {
    val prefs = Prefs(application)
    val state = MutableStateFlow(UiState())
    private val jobs = HashMap<String, Job>()

    init {
        refreshInstalled()
        if (!BuildConfig.IS_ADMIN) {
            Catalog.clientIds = prefs.clientIds
            viewModelScope.launch { loadClientIds(); refreshAll() }
        }
        viewModelScope.launch {
            InstallEvents.flow.collect {
                state.update { s -> s.copy(installing = s.installing - it.pkg) }
                refreshInstalled()
            }
        }
    }

    fun refreshInstalled() {
        val pm = getApplication<Application>().packageManager
        val map = HashMap<String, InstalledInfo>()
        val stamps = HashMap<String, String>()
        for (a in Catalog.all) {
            val info = runCatching { pm.getPackageInfo(a.pkg, 0) }.getOrNull() ?: continue
            map[a.pkg] = InstalledInfo(info.versionName ?: "", info.lastUpdateTime)
            prefs.stamp(a.pkg)?.let { stamps[a.pkg] = it }
        }
        state.update { it.copy(installed = map, stamps = stamps) }
    }

    fun refreshAll() {
        for (a in Catalog.offeredAll) refresh(a)
    }

    /** Récupère la liste des apps choisies pour les clients (le dernier choix connu est gardé hors connexion). */
    private suspend fun loadClientIds() {
        val r = Backend.clientIds(BuildConfig.IS_TV)
        r.value?.let {
            Catalog.clientIds = it
            prefs.clientIds = it
        }
    }

    /** Au retour dans l'app : les clients reprennent le dernier choix de l'admin et vérifient les nouvelles apps. */
    fun reloadClientIds() {
        viewModelScope.launch {
            loadClientIds()
            // Nouvelles apps, ou apps dont la vérification a échoué la dernière fois : on réessaie.
            for (a in Catalog.offeredAll) {
                val r = state.value.remote[a.id]
                if (r == null || r is Remote.Failed || r is Remote.NoRelease) refresh(a)
            }
        }
    }


    private fun refresh(a: AppEntry) {
        state.update { it.copy(remote = it.remote + (a.id to Remote.Loading)) }
        viewModelScope.launch {
            val r = GitHub.latest(a)
            state.update { it.copy(remote = it.remote + (a.id to r)) }
            Catalog.setPublished(a.id, r is Remote.Ok)
        }
    }

    fun dismissMessage() = state.update { it.copy(message = null) }

    fun dismissPermission() = state.update { it.copy(askInstallPermission = false) }

    private fun onWifi(): Boolean {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    fun cancel(app: AppEntry) {
        jobs.remove(app.id)?.cancel()
        state.update { it.copy(progress = it.progress - app.pkg, installing = it.installing - app.pkg) }
    }

    /** Télécharge puis lance l'installation. Retourne la tâche pour pouvoir l'attendre. */
    fun install(app: AppEntry): Job? {
        val ctx = getApplication<Application>()
        if (!Installer.canInstall(ctx)) {
            state.update { it.copy(askInstallPermission = true) }
            return null
        }
        if (prefs.wifiOnly && !onWifi()) {
            state.update { it.copy(message = "Connecte-toi en Wi-Fi, ou désactive « Wi-Fi uniquement » dans Réglages.") }
            return null
        }
        val release = (state.value.remote[app.id] as? Remote.Ok)?.release
        if (release == null) {
            state.update { it.copy(message = "Cette app n'a pas encore de version à installer. Réessaie dans un instant.") }
            return null
        }
        jobs[app.id]?.let { if (it.isActive) return it }
        val job = viewModelScope.launch {
            try {
                state.update { it.copy(progress = it.progress + (app.pkg to Progress(0, release.assetSize))) }
                val file = File(ctx.cacheDir, "apks/${app.pkg}.apk")
                GitHub.download(app, release, file) { done, total ->
                    state.update { it.copy(progress = it.progress + (app.pkg to Progress(done, total))) }
                }
                state.update { it.copy(progress = it.progress - app.pkg, installing = it.installing + app.pkg) }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { Installer.install(ctx, file, app.pkg, release.stamp) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.update {
                    it.copy(
                        progress = it.progress - app.pkg,
                        installing = it.installing - app.pkg,
                        message = e.message ?: "Téléchargement impossible",
                    )
                }
            } finally {
                jobs.remove(app.id)
            }
        }
        jobs[app.id] = job
        return job
    }

    /** Met à jour toutes les apps qui ont une nouvelle version, l'une après l'autre. */
    fun updateAll() {
        viewModelScope.launch {
            val todo = Catalog.apps.filter { statusOf(it, state.value) == AppStatus.Update }
            for (a in todo) {
                // On s'abonne avant de lancer, pour ne pas rater la fin de l'installation.
                val waiter = async(start = CoroutineStart.UNDISPATCHED) {
                    withTimeoutOrNull(120_000) { InstallEvents.flow.first { it.pkg == a.pkg } }
                }
                val job = install(a)
                if (job == null) {
                    waiter.cancel()
                    break
                }
                job.join()
                waiter.await()
            }
        }
    }
}
