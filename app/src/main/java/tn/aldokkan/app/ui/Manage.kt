package tn.aldokkan.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.aldokkan.app.AppEntry
import tn.aldokkan.app.AppStatus
import tn.aldokkan.app.Catalog
import tn.aldokkan.app.UiState
import tn.aldokkan.app.statusOf

@Composable
fun ManageScreen(
    ui: UiState,
    onOpen: (AppEntry) -> Unit,
    onAction: (AppEntry) -> Unit,
    onCancel: (AppEntry) -> Unit,
    onUpdateAll: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val updates = Catalog.apps.filter { statusOf(it, ui) == AppStatus.Update }
    val installed = Catalog.apps.filter { it.pkg in ui.installed }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Gérer les apps", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = Dk.Esp)
        Row {
            TabLabel("Mises à jour · ${updates.size}", tab == 0) { tab = 0 }
            TabLabel("Installées · ${installed.size}", tab == 1) { tab = 1 }
        }
        val list = if (tab == 0) updates else installed
        if (tab == 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (updates.isEmpty()) "Tout est à jour" else "${updates.size} app(s) ont une nouvelle version",
                    fontSize = 15.sp, color = Dk.Muted, modifier = Modifier.weight(1f),
                )
                if (updates.isNotEmpty()) PrimaryButton("Tout mettre à jour", onUpdateAll, height = 40.dp)
            }
            if (updates.isNotEmpty()) InfoNote("Une mise à jour s'installe par-dessus la version actuelle. Tes données sont conservées.")
        }
        if (list.isEmpty()) CenterText(if (tab == 0) "Rien à mettre à jour pour l'instant." else "Aucune app installée depuis cette liste.")
        list.forEach { a ->
            AppRow(
                app = a, status = statusOf(a, ui), version = ui.installed[a.pkg]?.versionName,
                progress = ui.progress[a.pkg], busy = a.pkg in ui.installing,
                onClick = { onOpen(a) }, onAction = { onAction(a) }, onCancel = { onCancel(a) },
            )
        }
    }
}
