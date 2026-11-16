package tn.aldokkan.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.aldokkan.app.AppEntry
import tn.aldokkan.app.Catalog
import tn.aldokkan.app.UiState
import tn.aldokkan.app.statusOf

@Composable
fun SearchScreen(
    ui: UiState,
    onOpen: (AppEntry) -> Unit,
    onAction: (AppEntry) -> Unit,
    onCancel: (AppEntry) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim().lowercase()
    val results = Catalog.apps.filter {
        q.isEmpty() || it.name.lowercase().contains(q) || it.tagline.lowercase().contains(q) ||
            it.category.lowercase().contains(q) || it.description.lowercase().contains(q)
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Rechercher dans Al Dokkan") },
            singleLine = true,
            shape = RoundedCornerShape(26.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Dk.Terra, unfocusedBorderColor = Dk.Line,
                cursorColor = Dk.Terra, focusedTextColor = Dk.Esp, unfocusedTextColor = Dk.Esp,
            ),
        )
        Text(if (q.isEmpty()) "TOUTES LES APPS" else "RÉSULTATS", fontSize = 14.sp, color = Dk.Muted, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        if (results.isEmpty()) CenterText("Aucune app ne correspond.")
        results.forEach { a ->
            AppRow(
                app = a, status = statusOf(a, ui), version = ui.installed[a.pkg]?.versionName,
                progress = ui.progress[a.pkg], busy = a.pkg in ui.installing,
                onClick = { onOpen(a) }, onAction = { onAction(a) }, onCancel = { onCancel(a) },
            )
        }
    }
}
