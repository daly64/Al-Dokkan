package tn.aldokkan.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.aldokkan.app.AppEntry
import tn.aldokkan.app.AppStatus
import tn.aldokkan.app.Catalog
import tn.aldokkan.app.Installer
import tn.aldokkan.app.Prefs
import tn.aldokkan.app.Remote
import tn.aldokkan.app.StoreViewModel
import tn.aldokkan.app.statusOf

/** Une zone sélectionnable à la télécommande : bordure épaisse quand elle a le focus. */
@Composable
private fun Focusable(
    modifier: Modifier = Modifier,
    radius: Int = 28,
    requester: FocusRequester? = null,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(radius.dp)
    Box(
        modifier
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .clip(shape)
            .background(if (focused) Dk.Cream else Dk.White)
            .border(if (focused) 4.dp else 1.dp, if (focused) Dk.Terra else Dk.Line, shape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick),
    ) { content() }
}

@Composable
fun TvWelcome(onDone: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Row(
        Modifier.fillMaxSize().background(Dk.White).padding(64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(64.dp),
    ) {
        Logo(220.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Bienvenue dans Al Dokkan", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Dk.Esp)
            Text("Ta boutique d'apps, sur ton téléviseur.", fontSize = 22.sp, color = Dk.Muted)
            Text("• Toutes tes apps TV au même endroit", fontSize = 20.sp, color = Dk.Esp)
            Text("• Installation en un clic", fontSize = 20.sp, color = Dk.Esp)
            Text("• Mises à jour par-dessus la version actuelle", fontSize = 20.sp, color = Dk.Esp)
            Spacer(Modifier.height(12.dp))
            Focusable(Modifier.width(280.dp).height(64.dp), 32, focus, onClick = onDone) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Commencer", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Dk.Terra)
                }
            }
        }
    }
}

@Composable
fun TvStore(vm: StoreViewModel, prefs: Prefs) {
    val ctx = LocalContext.current
    val ui by vm.state.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    val apps = Catalog.apps
    val updates = apps.count { statusOf(it, ui) == AppStatus.Update }

    Column(Modifier.fillMaxSize().background(Dk.White).padding(horizontal = 56.dp, vertical = 36.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Logo(64.dp)
            Column(Modifier.weight(1f)) {
                Text("Al Dokkan TV", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Dk.Esp)
                Text("Apps pour la TV", fontSize = 18.sp, color = Dk.Muted)
            }
            if (updates > 0) {
                Focusable(Modifier.height(56.dp), 28, onClick = { vm.updateAll() }) {
                    Box(Modifier.padding(horizontal = 26.dp).height(56.dp), contentAlignment = Alignment.Center) {
                        Text("Tout mettre à jour ($updates)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Dk.Terra)
                    }
                }
            }
            Focusable(Modifier.height(56.dp), 28, onClick = { showSettings = true }) {
                Box(Modifier.padding(horizontal = 26.dp).height(56.dp), contentAlignment = Alignment.Center) {
                    Text("Réglages", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Dk.Esp)
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(300.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(apps, key = { it.id }) { app ->
                val status = statusOf(app, ui)
                TvCard(
                    app, ui, status,
                    requester = if (app == apps.first()) first else null,
                    onClick = {
                        when (status) {
                            AppStatus.Open -> Installer.launch(ctx, app.pkg)
                            AppStatus.Install, AppStatus.Update -> vm.install(app)
                            AppStatus.Failed -> vm.refreshAll()
                            else -> {}
                        }
                    },
                )
            }
        }
    }

    if (showSettings) {
        var token by remember { mutableStateOf(prefs.token) }
        AlertDialog(
            onDismissRequest = { showSettings = false },
            confirmButton = {
                TextButton({ vm.saveToken(token); showSettings = false }) { Text("Enregistrer", color = Dk.Terra, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            },
            dismissButton = { TextButton({ showSettings = false }) { Text("Fermer", color = Dk.Muted, fontSize = 18.sp) } },
            title = { Text("Accès aux apps", fontWeight = FontWeight.Bold, color = Dk.Esp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Colle ici le jeton d'accès en lecture seule si tes apps sont privées.", fontSize = 16.sp, color = Dk.Muted)
                    OutlinedTextField(
                        value = token, onValueChange = { token = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text("Jeton d'accès") },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Dk.Terra, unfocusedBorderColor = Dk.Line, cursorColor = Dk.Terra,
                            focusedLabelColor = Dk.Terra, focusedTextColor = Dk.Esp, unfocusedTextColor = Dk.Esp,
                        ),
                    )
                }
            },
            containerColor = Dk.White,
        )
    }

    StoreDialogs(vm, ui)
}

@Composable
private fun TvCard(app: AppEntry, ui: tn.aldokkan.app.UiState, status: AppStatus, requester: FocusRequester?, onClick: () -> Unit) {
    val progress = ui.progress[app.pkg]
    val busy = app.pkg in ui.installing
    val version = ui.installed[app.pkg]?.versionName
    val remote = ui.remote[app.id]
    val label = when {
        progress != null || busy -> "Téléchargement…"
        status == AppStatus.Install -> "Installer"
        status == AppStatus.Update -> "Mettre à jour"
        status == AppStatus.Open -> "Ouvrir"
        status == AppStatus.Checking -> "…"
        status == AppStatus.NotPublished -> "Bientôt"
        else -> "Réessayer"
    }
    Focusable(Modifier.fillMaxWidth(), 28, requester, onClick) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app, 84.dp)
                Column(Modifier.weight(1f)) {
                    Text(app.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Dk.Esp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(statusLine(app, status, version), fontSize = 16.sp, color = Dk.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Text(app.description, fontSize = 15.sp, color = Dk.Esp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress.fraction },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = Dk.Saffron, trackColor = Dk.Line,
                )
            } else if (remote is Remote.Failed) {
                Text(remote.message, fontSize = 14.sp, color = Dk.Terra, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Box(
                Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp))
                    .background(if (status == AppStatus.Open) Dk.Sand else Dk.Terra),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (status == AppStatus.Open) Dk.Esp else Dk.White)
            }
        }
    }
}
