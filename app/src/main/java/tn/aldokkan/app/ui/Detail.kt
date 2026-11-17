package tn.aldokkan.app.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.aldokkan.app.AppEntry
import tn.aldokkan.app.AppStatus
import tn.aldokkan.app.Installer
import tn.aldokkan.app.Remote
import tn.aldokkan.app.UiState
import tn.aldokkan.app.statusOf

/** Fiche d'une app, dans l'esprit du Play Store. */
@Composable
fun DetailScreen(
    app: AppEntry,
    ui: UiState,
    onBack: () -> Unit,
    onAction: (AppEntry) -> Unit,
    onCancel: (AppEntry) -> Unit,
    onRetry: () -> Unit,
) {
    val ctx: Context = LocalContext.current
    val status = statusOf(app, ui)
    val remote = ui.remote[app.id]
    val release = (remote as? Remote.Ok)?.release
    val installed = ui.installed[app.pkg]
    val progress = ui.progress[app.pkg]
    val busy = app.pkg in ui.installing

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.height(48.dp).width(44.dp).clickable(onClick = onBack), contentAlignment = Alignment.CenterStart) {
            PathIcon(listOf("M15 5l-7 7 7 7"), Dk.Esp, 24.dp, viewBox = 24f, stroke = 2.4f)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
            AppIcon(app, 80.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(app.name, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, color = Dk.Esp)
                Text(app.tagline, fontSize = 14.sp, color = Dk.Muted)
            }
        }

        Row(Modifier.fillMaxWidth()) {
            Stat("Taille", release?.let { formatSize(it.assetSize) } ?: "—", Modifier.weight(1f))
            Stat("Version", release?.tag?.removePrefix("v")?.take(10) ?: "—", Modifier.weight(1f))
            Stat("Publiée", release?.let { formatDate(it.publishedAt) } ?: "—", Modifier.weight(1f))
            Stat("Pour", app.platform.label, Modifier.weight(1f))
        }

        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Dk.Saffron, trackColor = Dk.Line,
            )
            Text("Téléchargement ${(progress.fraction * 100).toInt()} %", fontSize = 14.sp, color = Dk.Muted)
            OutlineButton("Annuler", { onCancel(app) }, Modifier.fillMaxWidth())
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (installed != null && status != AppStatus.Checking) {
                    OutlineButton("Désinstaller", { Installer.uninstall(ctx, app.pkg) }, Modifier.weight(1f))
                }
                if (status == AppStatus.Open) {
                    PrimaryButton("Ouvrir", { Installer.launch(ctx, app.pkg) }, Modifier.weight(1.4f))
                } else if (status == AppStatus.Failed) {
                    PrimaryButton("Réessayer", onRetry, Modifier.weight(1.4f))
                } else {
                    ActionButton(status, busy, { onAction(app) }, Modifier.weight(1.4f), height = 48.dp)
                }
            }
            if (status == AppStatus.Update) {
                PrimaryButton("Ouvrir la version installée", { Installer.launch(ctx, app.pkg) }, Modifier.fillMaxWidth(), height = 44.dp, enabled = true)
            }
        }

        when (remote) {
            is Remote.Failed -> InfoNote(remote.message)
            is Remote.NoRelease -> InfoNote("Cette app n'a pas encore de fichier APK publié. Dès qu'une version est publiée, elle apparaîtra ici.")
            else -> {}
        }

        Screenshots(app)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("À propos de cette app", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Dk.Esp)
            Text(app.description, fontSize = 15.sp, lineHeight = 22.sp, color = Dk.Esp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Chip(app.category) }
        }

        if (release != null) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Nouveautés", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Dk.Esp)
                val notes = release.notes.trim()
                Text(
                    if (notes.isEmpty()) release.name else notes.take(600),
                    fontSize = 15.sp, lineHeight = 22.sp, color = Dk.Muted,
                )
            }
        }

        if (installed != null) {
            Text("Installée : version ${installed.versionName}", fontSize = 14.sp, color = Dk.Muted)
        }
        InfoNote("Une mise à jour s'installe par-dessus la version actuelle. Tes données sont conservées.")
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Dk.Esp, maxLines = 1, textAlign = TextAlign.Center)
        Text(label, fontSize = 12.sp, color = Dk.Muted)
    }
}

/** Les captures d'écran de l'app, en bande qui défile. Rien ne s'affiche s'il n'y en a pas. */
@Composable
private fun Screenshots(app: AppEntry) {
    val urls by androidx.compose.runtime.produceState(emptyList<String>(), app.id) { value = tn.aldokkan.app.GitHub.captures(app) }
    if (urls.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Captures d'écran", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Dk.Esp)
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(urls.size) { i ->
                val bmp by androidx.compose.runtime.produceState<android.graphics.Bitmap?>(null, urls[i]) { value = tn.aldokkan.app.GitHub.bitmap(urls[i]) }
                val b = bmp
                Box(Modifier.height(320.dp).clip(RoundedCornerShape(16.dp)).background(Dk.Sand)) {
                    if (b != null) androidx.compose.foundation.Image(
                        b.asImageBitmap(), "Capture ${i + 1} de ${app.name}",
                        Modifier.height(320.dp), contentScale = androidx.compose.ui.layout.ContentScale.FillHeight,
                    ) else Box(Modifier.height(320.dp).width(150.dp))
                }
            }
        }
    }
}
