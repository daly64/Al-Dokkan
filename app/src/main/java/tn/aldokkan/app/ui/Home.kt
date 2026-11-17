package tn.aldokkan.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.aldokkan.app.AppEntry
import tn.aldokkan.app.AppStatus
import tn.aldokkan.app.Catalog
import tn.aldokkan.app.Remote
import tn.aldokkan.app.UiState
import tn.aldokkan.app.statusOf

@Composable
fun HomeScreen(
    ui: UiState,
    onOpen: (AppEntry) -> Unit,
    onSearch: () -> Unit,
    onManage: () -> Unit,
    onAction: (AppEntry) -> Unit,
    onCancel: (AppEntry) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val apps = Catalog.apps

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Barre de recherche arrondie, avec le logo à gauche
        Row(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(Dk.Sand).clickable(onClick = onSearch).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Logo(32.dp)
            Text("Rechercher dans Al Dokkan", Modifier.weight(1f), color = Dk.Muted, fontSize = 16.sp)
            Box(Modifier.size(38.dp).clip(CircleShape).background(Dk.Cream), contentAlignment = Alignment.Center) {
                PathIcon(listOf("M12 9m-4 0a4 4 0 1 0 8 0a4 4 0 1 0-8 0", "M4.5 20c1.2-4 4-5.5 7.5-5.5s6.3 1.5 7.5 5.5"), Dk.Esp, 22.dp, viewBox = 24f, stroke = 2f)
            }
        }

        Row(Modifier.fillMaxWidth()) {
            TabLabel("Pour toi", tab == 0) { tab = 0 }
            TabLabel("Récentes", tab == 1) { tab = 1 }
            TabLabel("Catégories", tab == 2) { tab = 2 }
        }

        when (tab) {
            0 -> ForYou(ui, apps, onOpen, onManage, onAction)
            1 -> {
                val recent = apps.mapNotNull { a ->
                    (ui.remote[a.id] as? Remote.Ok)?.let { a to it.release.publishedAt }
                }.sortedByDescending { it.second }.map { it.first }
                if (recent.isEmpty()) CenterText("Les versions publiées apparaîtront ici.")
                recent.forEach { a -> RowFor(a, ui, onOpen, onAction, onCancel) }
            }
            else -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Tout", category == null) { category = null }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Catalog.categories) { c -> Chip(c, category == c) { category = if (category == c) null else c } }
                }
                apps.filter { category == null || it.category == category }.forEach { a -> RowFor(a, ui, onOpen, onAction, onCancel) }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RowFor(a: AppEntry, ui: UiState, onOpen: (AppEntry) -> Unit, onAction: (AppEntry) -> Unit, onCancel: (AppEntry) -> Unit) {
    AppRow(
        app = a, status = statusOf(a, ui), version = ui.installed[a.pkg]?.versionName,
        progress = ui.progress[a.pkg], busy = a.pkg in ui.installing,
        onClick = { onOpen(a) }, onAction = { onAction(a) }, onCancel = { onCancel(a) },
    )
}

@Composable
private fun ForYou(ui: UiState, apps: List<AppEntry>, onOpen: (AppEntry) -> Unit, onManage: () -> Unit, onAction: (AppEntry) -> Unit) {
    val updates = apps.filter { statusOf(it, ui) == AppStatus.Update }
    val installable = apps.filter { statusOf(it, ui) == AppStatus.Install }
    val installed = apps.filter { it.pkg in ui.installed }
    val soon = apps.filter { statusOf(it, ui) in listOf(AppStatus.NotPublished, AppStatus.Failed) }

    // Bannières : d'abord les mises à jour, sinon des apps à découvrir
    val banners = (updates.map { it to true } + installable.map { it to false }).take(5)
    if (banners.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(banners) { (a, isUpdate) ->
                val bg = if (isUpdate) Dk.Terra else Dk.Saffron
                val fg = if (isUpdate) Dk.White else Dk.Esp
                Row(
                    Modifier.width(304.dp).height(176.dp).clip(RoundedCornerShape(24.dp)).background(bg)
                        .clickable { onOpen(a) }.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (isUpdate) "NOUVELLE VERSION" else "À DÉCOUVRIR", color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                        Text(a.name, color = fg, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp, maxLines = 2)
                        Text(a.tagline, color = fg, fontSize = 14.sp)
                        if (isUpdate) {
                            Spacer(Modifier.height(4.dp))
                            Box(
                                Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).background(Dk.Saffron)
                                    .clickable { onAction(a) }.padding(horizontal = 18.dp),
                                contentAlignment = Alignment.Center,
                            ) { Text("Mettre à jour", color = Dk.Esp, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                        }
                    }
                    AppIcon(a, 84.dp)
                }
            }
        }
    }

    if (installed.isNotEmpty()) {
        SectionHeader("Mes apps", onManage)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(installed) { a ->
                val v = ui.installed[a.pkg]?.versionName
                AppCard(a, if (v.isNullOrBlank()) a.tagline else "v$v") { onOpen(a) }
            }
        }
    }
    if (installable.isNotEmpty()) {
        SectionHeader("Prêtes à installer")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(installable) { a ->
                val size = (ui.remote[a.id] as? Remote.Ok)?.release?.assetSize ?: 0L
                AppCard(a, if (size > 0) formatSize(size) else a.tagline) { onOpen(a) }
            }
        }
    }
    if (soon.isNotEmpty()) {
        SectionHeader("Pas encore disponibles")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(soon) { a -> AppCard(a, a.tagline) { onOpen(a) } }
        }
    }
    val failed = apps.firstOrNull { (ui.remote[it.id] as? Remote.Failed) != null }
    if (failed != null && banners.isEmpty() && installed.isEmpty()) {
        InfoNote((ui.remote[failed.id] as Remote.Failed).message)
    }
}
