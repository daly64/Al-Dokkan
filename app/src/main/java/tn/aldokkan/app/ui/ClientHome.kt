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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/** Accueil de la version Clients : une grille d'apps, un gros bouton sur chacune, et « Tout mettre à jour » bien visible. */
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

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Logo(40.dp)
                Text("Al Dokkan", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Dk.Esp)
            }
        }
        if (updates > 0) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Dk.Terra).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        if (updates == 1) "1 mise à jour disponible" else "$updates mises à jour disponibles",
                        color = Dk.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                    )
                    Box(
                        Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(25.dp)).background(Dk.Saffron).clickable(onClick = onUpdateAll),
                        contentAlignment = Alignment.Center,
                    ) { Text("Tout mettre à jour", color = Dk.Esp, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
                }
            }
        }
        if (apps.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                val (title, sub) = emptyMessage(ui)
                Column(Modifier.fillMaxWidth().padding(vertical = 56.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Dk.Esp, textAlign = TextAlign.Center)
                    Text(sub, fontSize = 16.sp, color = Dk.Muted, textAlign = TextAlign.Center)
                    PrimaryButton("Réessayer", onRetry, Modifier.padding(top = 8.dp))
                }
            }
        }
        items(apps, key = { it.id }) { a -> ClientCard(a, ui, onOpen, onAction, onCancel) }
    }
}

@Composable
private fun ClientCard(a: AppEntry, ui: UiState, onOpen: (AppEntry) -> Unit, onAction: (AppEntry) -> Unit, onCancel: (AppEntry) -> Unit) {
    val status = statusOf(a, ui)
    val progress = ui.progress[a.pkg]
    val busy = a.pkg in ui.installing
    val version = ui.installed[a.pkg]?.versionName
    val label = when {
        progress != null -> "Annuler"
        busy -> "Installation…"
        status == AppStatus.Install -> "Installer"
        status == AppStatus.Update -> "Mettre à jour"
        status == AppStatus.Open -> "Ouvrir"
        status == AppStatus.Checking -> "…"
        status == AppStatus.NotPublished -> "Bientôt"
        else -> "Réessayer"
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).border(1.dp, Dk.Line, RoundedCornerShape(22.dp))
            .clickable { onOpen(a) }.padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppIcon(a, 72.dp)
        Text(a.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Dk.Esp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(statusLine(a, status, version), fontSize = 13.sp, color = Dk.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = Dk.Saffron, trackColor = Dk.Line,
            )
        }
        PrimaryButton(
            label,
            { if (progress != null) onCancel(a) else onAction(a) },
            Modifier.fillMaxWidth(),
            enabled = !busy && status != AppStatus.Checking && status != AppStatus.NotPublished,
            height = 46.dp,
        )
    }
}
