package tn.aldokkan.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.aldokkan.app.AppEntry
import tn.aldokkan.app.AppStatus
import tn.aldokkan.app.Catalog
import tn.aldokkan.app.Remote
import tn.aldokkan.app.UiState
import tn.aldokkan.app.statusOf

/** Message à afficher quand il n'y a rien à montrer : hors connexion, ou vraiment aucune app. */
fun emptyMessage(ui: UiState): Pair<String, String> {
    val offline = Catalog.clientIds == null || Catalog.offeredAll.any { ui.remote[it.id] is Remote.Failed }
    return if (offline) "Pas de connexion" to "Vérifie ta connexion internet, puis réessaie."
    else "Aucune appli pour le moment" to "Réessaie un peu plus tard."
}

/** Accueil de la version Clients, façon Google Play : une liste simple, un bouton par app, « Tout mettre à jour » seulement s'il y a des mises à jour. */
@Composable
fun ClientHome(
    ui: UiState,
    onOpen: (AppEntry) -> Unit,
    onAction: (AppEntry) -> Unit,
    onCancel: (AppEntry) -> Unit,
    onUpdateAll: () -> Unit,
    onRetry: () -> Unit,
) {
    val apps = Catalog.apps
    val updates = apps.count { statusOf(it, ui) == AppStatus.Update }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Row(Modifier.padding(top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Logo(44.dp)
                Text("Al Dokkan", fontSize = 24.sp, fontWeight = FontWeight.Medium, color = Dk.Ink)
            }
        }
        if (updates > 0) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(16.dp)).background(Dk.ActionBg).padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (updates == 1) "1 mise à jour" else "$updates mises à jour",
                        Modifier.weight(1f), color = Color(0xFF0B5E43), fontSize = 15.sp, fontWeight = FontWeight.Medium,
                    )
                    PrimaryButton("Tout mettre à jour", onUpdateAll, height = 36.dp)
                }
            }
        }
        if (apps.isEmpty()) {
            item {
                val (title, sub) = emptyMessage(ui)
                Column(Modifier.fillMaxWidth().padding(vertical = 72.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(title, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = Dk.Ink, textAlign = TextAlign.Center)
                    Text(sub, fontSize = 15.sp, color = Dk.Grey, textAlign = TextAlign.Center)
                    PrimaryButton("Réessayer", onRetry, Modifier.padding(top = 8.dp))
                }
            }
        }
        items(apps, key = { it.id }) { a -> ClientRow(a, ui, onOpen, onAction, onCancel) }
    }
}

@Composable
private fun ClientRow(a: AppEntry, ui: UiState, onOpen: (AppEntry) -> Unit, onAction: (AppEntry) -> Unit, onCancel: (AppEntry) -> Unit) {
    val status = statusOf(a, ui)
    val progress = ui.progress[a.pkg]
    val busy = a.pkg in ui.installing
    val version = ui.installed[a.pkg]?.versionName
    val label = when {
        progress != null -> "Annuler"
        busy -> "En cours…"
        status == AppStatus.Install -> "Installer"
        status == AppStatus.Update -> "Mettre à jour"
        status == AppStatus.Open -> "Ouvrir"
        status == AppStatus.Checking -> "…"
        status == AppStatus.NotPublished -> "Bientôt"
        else -> "Réessayer"
    }
    val enabled = !busy && status != AppStatus.Checking && status != AppStatus.NotPublished
    val click = { if (progress != null) onCancel(a) else onAction(a) }
    Column(Modifier.fillMaxWidth().clickable { onOpen(a) }.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AppIcon(a, 56.dp)
            Column(Modifier.weight(1f)) {
                Text(a.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Dk.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(statusLine(a, status, version), fontSize = 13.sp, color = Dk.Grey, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (status == AppStatus.Open && progress == null) OutlineButton(label, click, height = 36.dp)
            else PrimaryButton(label, click, enabled = enabled || progress != null, height = 36.dp)
        }
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = Dk.Action, trackColor = Dk.Stroke,
            )
        }
    }
}
