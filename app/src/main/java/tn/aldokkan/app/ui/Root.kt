package tn.aldokkan.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.aldokkan.app.AppEntry
import tn.aldokkan.app.BuildConfig
import tn.aldokkan.app.AppStatus
import tn.aldokkan.app.Catalog
import tn.aldokkan.app.Installer
import tn.aldokkan.app.Prefs
import tn.aldokkan.app.StoreViewModel
import tn.aldokkan.app.statusOf

enum class Tab { Home, Search, Manage, Clients, Settings }

@Composable
fun DokkanRoot(vm: StoreViewModel, prefs: Prefs, onFullscreen: (Boolean) -> Unit) {
    // Premier lancement : bienvenue, puis splash. Ensuite : splash, puis la boutique.
    var stage by rememberSaveable { mutableStateOf(if (prefs.onboarded) "splash" else "welcome") }
    when (stage) {
        "welcome" -> if (BuildConfig.IS_TV) TvWelcome(onDone = { prefs.onboarded = true; stage = "splash" })
        else WelcomeScreen(onDone = { prefs.onboarded = true; stage = "splash" })
        "splash" -> SplashScreen(onDone = { stage = "main" })
        else -> if (BuildConfig.IS_TV) TvStore(vm, prefs) else MainScreen(vm, prefs, onFullscreen)
    }
}

@Composable
private fun MainScreen(vm: StoreViewModel, prefs: Prefs, onFullscreen: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    val ui by vm.state.collectAsState()
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = Catalog.byId(selectedId)

    BackHandler(enabled = selected != null) { selectedId = null }
    BackHandler(enabled = selected == null && tab != Tab.Home) { tab = Tab.Home }

    val open: (AppEntry) -> Unit = { selectedId = it.id }
    val action: (AppEntry) -> Unit = { a ->
        when (statusOf(a, ui)) {
            AppStatus.Open -> Installer.launch(ctx, a.pkg)
            AppStatus.Install, AppStatus.Update -> vm.install(a)
            else -> open(a)
        }
    }
    val cancel: (AppEntry) -> Unit = { vm.cancel(it) }
    val updateCount = Catalog.apps.count { statusOf(it, ui) == AppStatus.Update }

    Column(Modifier.fillMaxSize().background(Dk.White)) {
        Box(Modifier.weight(1f)) {
            if (selected != null) {
                DetailScreen(selected, ui, onBack = { selectedId = null }, onAction = action, onCancel = cancel, onRetry = { vm.refreshAll() })
            } else {
                when (tab) {
                    Tab.Home -> HomeScreen(ui, open, onSearch = { tab = Tab.Search }, onManage = { tab = Tab.Manage }, onAction = action, onCancel = cancel)
                    Tab.Search -> SearchScreen(ui, open, action, cancel)
                    Tab.Manage -> ManageScreen(ui, open, action, cancel, onUpdateAll = { vm.updateAll() })
                    Tab.Clients -> ClientsScreen(ui, hasKey = prefs.adminKey.isNotBlank(), onPublish = { vm.publishClientIds(it) })
                    Tab.Settings -> SettingsScreen(vm, prefs, onFullscreen)
                }
            }
        }
        if (selected == null) BottomBar(tab, updateCount) { tab = it }
    }

    StoreDialogs(vm, ui)
}

@Composable
fun StoreDialogs(vm: StoreViewModel, ui: tn.aldokkan.app.UiState) {
    val ctx = LocalContext.current
    ui.message?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.dismissMessage() },
            confirmButton = { TextButton({ vm.dismissMessage() }) { Text("OK", color = Dk.Terra, fontWeight = FontWeight.Bold) } },
            text = { Text(msg, fontSize = 16.sp, color = Dk.Esp) },
            containerColor = Dk.White,
        )
    }
    if (ui.askInstallPermission) {
        AlertDialog(
            onDismissRequest = { vm.dismissPermission() },
            confirmButton = {
                TextButton({ vm.dismissPermission(); Installer.openInstallSettings(ctx) }) {
                    Text("Autoriser", color = Dk.Terra, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton({ vm.dismissPermission() }) { Text("Plus tard", color = Dk.Muted) } },
            title = { Text("Autoriser l'installation", fontWeight = FontWeight.Bold, color = Dk.Esp) },
            text = { Text("Pour installer tes apps, Android demande d'autoriser Al Dokkan à installer des applications. Active l'option, puis reviens ici.", fontSize = 16.sp, color = Dk.Esp) },
            containerColor = Dk.White,
        )
    }
}

private val navIcons = mapOf(
    Tab.Home to listOf("M4 11l8-7 8 7v9h-5v-6H9v6H4Z"),
    Tab.Search to listOf("M11 11m-7 0a7 7 0 1 0 14 0a7 7 0 1 0-14 0", "M16.5 16.5L21 21"),
    Tab.Manage to listOf("M6 4h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z", "M15 4h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2h-3a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z", "M6 13h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2Z", "M15 13h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2h-3a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2Z"),
    Tab.Clients to listOf("M9 8m-3 0a3 3 0 1 0 6 0a3 3 0 1 0-6 0", "M3.5 19c.8-3.2 2.8-4.5 5.5-4.5s4.7 1.3 5.5 4.5", "M16 6.2a3 3 0 0 1 0 5.6", "M17.5 14.7c1.6.5 2.6 1.8 3 4.3"),
    Tab.Settings to listOf("M4 7h10", "M18 7h2", "M4 17h2", "M10 17h10", "M16 7m-2.5 0a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0", "M8 17m-2.5 0a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0"),
)
private val navLabels = mapOf(Tab.Home to "Accueil", Tab.Search to "Rechercher", Tab.Manage to "Mes apps", Tab.Clients to "Clients", Tab.Settings to "Réglages")

/** La version Clients n'a pas l'onglet d'administration. */
private val visibleTabs get() = Tab.entries.filter { BuildConfig.IS_ADMIN || it != Tab.Clients }

@Composable
private fun BottomBar(current: Tab, badge: Int, onSelect: (Tab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Dk.White)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Dk.Line))
        Row(Modifier.fillMaxWidth().height(76.dp)) {
            visibleTabs.forEach { t ->
                val on = t == current
                val color = if (on) Dk.Esp else Dk.Muted
                Column(
                    Modifier.weight(1f).fillMaxSize().clickable { onSelect(t) },
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.width(56.dp).height(30.dp).clip(RoundedCornerShape(15.dp)).background(if (on) Dk.Pill else androidx.compose.ui.graphics.Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        PathIcon(navIcons.getValue(t), color, 22.dp, viewBox = 24f, stroke = 2f)
                        if (t == Tab.Manage && badge > 0) {
                            Box(
                                Modifier.align(Alignment.TopEnd).size(18.dp).clip(CircleShape).background(Dk.Terra),
                                contentAlignment = Alignment.Center,
                            ) { Text("$badge", color = Dk.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                    Text(navLabels.getValue(t), fontSize = 12.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, color = color)
                }
            }
        }
    }
}
