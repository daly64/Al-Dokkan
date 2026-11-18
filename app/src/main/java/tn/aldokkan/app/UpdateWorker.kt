package tn.aldokkan.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** Vérifie de temps en temps (en arrière-plan) si une app installée a une nouvelle version, et prévient par une notification. */
class UpdateWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val prefs = Prefs(ctx)
        val fromServer = Backend.clientIds(BuildConfig.IS_TV).value
        if (fromServer != null) prefs.clientIds = fromServer
        val ids = fromServer ?: prefs.clientIds ?: emptySet()

        val pm = ctx.packageManager
        val keys = mutableSetOf<String>()
        val names = mutableListOf<String>()
        for (a in Catalog.all.filter { it.id == Catalog.SELF_ID || it.id in ids }) {
            val info = runCatching { pm.getPackageInfo(a.pkg, 0) }.getOrNull() ?: continue
            val r = GitHub.latest(a) as? Remote.Ok ?: continue
            val stamp = prefs.stamp(a.pkg)
            val newer = if (stamp != null) stamp != r.release.stamp else r.release.assetUpdatedAt > info.lastUpdateTime
            if (newer) {
                keys += "${a.pkg}:${r.release.stamp}"
                names += a.name
            }
        }

        // On ne prévient qu'une fois par nouvelle version.
        val fresh = keys - prefs.notified
        prefs.notified = keys
        if (fresh.isNotEmpty() && NotificationManagerCompat.from(ctx).areNotificationsEnabled()) {
            notify(ctx, names)
        }
        return Result.success()
    }

    private fun notify(ctx: Context, names: List<String>) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel("updates", "Mises à jour", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = if (names.size == 1) "Mise à jour disponible" else "${names.size} mises à jour disponibles"
        val n = NotificationCompat.Builder(ctx, "updates")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(names.joinToString(", "))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(1, n) }
    }
}
