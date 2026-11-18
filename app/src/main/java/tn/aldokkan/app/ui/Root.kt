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
import androidx.compose.runtime.LaunchedEffect
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

enum class Tab { Home, Settings }

@Composable
fun DokkanRoot(vm: StoreViewModel, prefs: Prefs, onFullscreen: (Boolean) -> Unit) {
    if (BuildConfig.IS_ADMIN) { AdminRoot(); return }
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
    // Tant que l'écran est ouvert, on reprend le choix de l'admin toutes les 20 secondes.
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(20_000)
            vm.reloadClientIds()
        }
    }
    val updateCount = Catalog.apps.count { statusOf(it, ui) == AppStatus.Update }

    Column(Modifier.fillMaxSize().background(Dk.White)) {
        Box(Modifier.weight(1f)) {
            if (selected != null) {
                DetailScreen(selected, ui, onBack = { selectedId = null }, onAction = action, onCancel = cancel, onRetry = { vm.refreshAll() })
            } else {
                when (tab) {
                    Tab.Home -> ClientHome(ui, open, action, cancel, onUpdateAll = { vm.updateAll() }, onRetry = { vm.reloadClientIds() })
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
    Tab.Settings to listOf("M4 7h10", "M18 7h2", "M4 17h2", "M10 17h10", "M16 7m-2.5 0a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0", "M8 17m-2.5 0a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0"),
)
private val navLabels = mapOf(Tab.Home to "Accueil", Tab.Settings to "Réglages")

@Composable
private fun BottomBar(current: Tab, badge: Int, onSelect: (Tab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Dk.White)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Dk.Stroke))
        Row(Modifier.fillMaxWidth().height(76.dp)) {
            listOf(Tab.Home, Tab.Settings).forEach { t ->
                val on = t == current
                val color = if (on) Dk.Action else Dk.Grey
                Column(
                    Modifier.weight(1f).fillMaxSize().clickable { onSelect(t) },
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.width(56.dp).height(30.dp).clip(RoundedCornerShape(15.dp)).background(if (on) Dk.ActionBg else androidx.compose.ui.graphics.Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        PathIcon(navIcons.getValue(t), color, 22.dp, viewBox = 24f, stroke = 2f)
                        if (t == Tab.Home && badge > 0) {
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
