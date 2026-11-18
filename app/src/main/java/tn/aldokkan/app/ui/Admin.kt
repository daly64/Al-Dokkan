package tn.aldokkan.app.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tn.aldokkan.app.DeviceKey
import tn.aldokkan.app.GitHub
import tn.aldokkan.app.Remote
import tn.aldokkan.app.Backend
import tn.aldokkan.app.Catalog

private const val AUTH = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

/** Version Admin : un seul écran, ouvert par empreinte. Chaque interrupteur est publié tout de suite. */
@Composable
fun AdminRoot() {
    val activity = LocalContext.current as FragmentActivity
    var unlocked by rememberSaveable { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    fun ask() {
        // Sans empreinte ni verrouillage d'écran sur le téléphone, on ne peut rien demander : on ouvre.
        if (BiometricManager.from(activity).canAuthenticate(AUTH) != BiometricManager.BIOMETRIC_SUCCESS) {
            unlocked = true
            return
        }
        val prompt = BiometricPrompt(
            activity, ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { unlocked = true }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { failed = true }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Al Dokkan Admin")
                .setSubtitle("Pose ton doigt pour ouvrir")
                .setAllowedAuthenticators(AUTH)
                .build(),
        )
    }

    LaunchedEffect(unlocked) { if (!unlocked) ask() }

    if (unlocked) {
        AdminScreen()
    } else {
        Column(Modifier.fillMaxSize().padding(40.dp), verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
            Logo(96.dp, tile = Dk.Esp)
            Text("Al Dokkan Admin", fontWeight = FontWeight.Medium, fontSize = 24.sp, color = Dk.Ink)
            Box(
                Modifier.padding(top = 28.dp).size(88.dp).clip(CircleShape).background(Dk.ActionBg).clickable { failed = false; ask() },
                contentAlignment = Alignment.Center,
            ) {
                PathIcon(
                    listOf("M12 11a3 3 0 0 0-3 3c0 2 .5 3.5 1.5 5", "M12 7a7 7 0 0 0-7 7c0 1.5.3 3 1 4.5", "M12 7a7 7 0 0 1 7 7c0 2-.4 3.5-1.2 5", "M15 14c0 2.5-.7 4.5-2 6"),
                    Dk.Action, 48.dp, viewBox = 24f, stroke = 1.8f,
                )
            }
            Text(if (failed) "Touche l'icône pour réessayer" else "Empreinte pour entrer", fontSize = 15.sp, color = Dk.Grey)
        }
    }
}

@Composable
private fun AdminScreen() {
    val scope = rememberCoroutineScope()
    var mobile by remember { mutableStateOf<Set<String>?>(null) }
    var tv by remember { mutableStateOf<Set<String>?>(null) }
    var status by remember { mutableStateOf("Chargement…") }
    var readOnly by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var undo by remember { mutableStateOf<Pair<Set<String>, Set<String>>?>(null) }
    var undoTick by remember { mutableStateOf(0) }
    val remote = remember { mutableStateMapOf<String, Remote>() }

    // Chargement du choix actuel + première utilisation : l'appareil s'enregistre comme administrateur.
    LaunchedEffect(Unit) {
        val r = Backend.choice()
        val c = r.value
        if (c == null) {
            status = (r.error ?: "Erreur") + " — impossible de charger la liste"
            readOnly = true
            return@LaunchedEffect
        }
        mobile = c.mobile; tv = c.tv
        when {
            !c.enrolled -> {
                val e = Backend.enroll()
                status = e ?: "Cet appareil est maintenant l'administrateur"
                readOnly = e != null
            }
            c.device != DeviceKey.id() -> {
                status = "Cet appareil n'est pas l'administrateur enregistré : lecture seule"
                readOnly = true
            }
            else -> status = "Tout est publié"
        }
    }

    // État de version de chaque app (GitHub), pour ne pas cocher une app qui n'apparaîtrait pas.
    LaunchedEffect(Unit) {
        Catalog.choosable.forEach { a -> launch { remote[a.id] = GitHub.latest(a) } }
    }

    LaunchedEffect(undoTick) {
        if (undo != null) { delay(6000); undo = null }
    }

    fun apply(m: Set<String>, t: Set<String>) {
        mobile = m; tv = t
        status = "Publication…"
        scope.launch { status = Backend.publish(m, t) ?: "Publié pour les clients" }
    }

    fun change(m: Set<String>, t: Set<String>) {
        val om = mobile ?: return
        val ot = tv ?: return
        undo = om to ot
        undoTick++
        apply(m, t)
    }

    val m = mobile
    val t = tv
    val all = Catalog.choosable
    var tab by rememberSaveable { mutableStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Vitrine des clients", fontWeight = FontWeight.Medium, fontSize = 24.sp, color = Dk.Ink)
            Text(
                if (readOnly) status else "Touche une app pour l'afficher ou la masquer",
                fontSize = 14.sp, color = if (readOnly) Dk.Terra else Dk.Grey,
            )
        }
        Row(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp).fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).background(Dk.Fill).padding(4.dp),
        ) {
            listOf("Mobile", "TV").forEachIndexed { i, label ->
                val on = tab == i
                Box(
                    Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(18.dp)).background(if (on) Dk.White else Color.Transparent).clickable { tab = i },
                    contentAlignment = Alignment.Center,
                ) { Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (on) Dk.Action else Dk.Grey) }
            }
        }
        if (m != null && t != null) {
            val cur = if (tab == 0) m else t
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                all.chunked(3).forEach { rowApps ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowApps.forEach { a ->
                            val on = a.id in cur
                            val noRelease = remote[a.id] is Remote.NoRelease
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                    .then(if (readOnly) Modifier else Modifier.clickable {
                                        val next = if (on) cur - a.id else cur + a.id
                                        if (tab == 0) change(next, t) else change(m, next)
                                    }),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(Modifier.padding(6.dp)) {
                                    Box(
                                        Modifier.clip(RoundedCornerShape(22.dp))
                                            .then(if (on) Modifier.border(3.dp, Dk.Action, RoundedCornerShape(22.dp)) else Modifier.alpha(0.4f))
                                            .padding(if (on) 3.dp else 0.dp),
                                    ) { AppIcon(a, 78.dp) }
                                    if (on) Box(
                                        Modifier.align(Alignment.TopEnd).size(26.dp).clip(CircleShape).background(Dk.Action).border(2.dp, Dk.White, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) { PathIcon(listOf("M5 12.5l4.5 4.5L19 7.5"), Dk.White, 14.dp, viewBox = 24f, stroke = 3.5f) }
                                }
                                Text(a.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Dk.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (noRelease) Text("Pas de version", fontSize = 11.sp, color = Dk.Terra)
                            }
                        }
                        repeat(3 - rowApps.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        } else {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(status, fontSize = 15.sp, color = Dk.Grey) }
        }

        // Rattrape une fausse manip : la barre disparaît toute seule après quelques secondes.
        val u = undo
        if (u != null) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF313033)).padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(status.ifBlank { "Modification publiée" }, Modifier.weight(1f), color = Dk.White, fontSize = 14.sp)
                Text(
                    "Annuler", color = Color(0xFF8AB4F8), fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    modifier = Modifier.clickable { undo = null; apply(u.first, u.second) }.padding(12.dp),
                )
            }
        }
    }
}
