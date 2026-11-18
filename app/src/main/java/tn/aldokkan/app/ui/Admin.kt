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
import androidx.compose.foundation.layout.width
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
        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
            Logo(72.dp)
            Text("Al Dokkan Admin", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = Dk.Esp)
            if (failed) {
                Text("Déverrouillage annulé", fontSize = 15.sp, color = Dk.Muted)
                PrimaryButton("Réessayer", { failed = false; ask() })
            }
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
    val allIds = all.map { it.id }.toSet()

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Apps des clients", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = Dk.Esp)
            Text(status, fontSize = 14.sp, color = if (status.startsWith("Publié") || status.startsWith("Tout") || status.startsWith("Cet appareil est")) Dk.GreenTx else if (readOnly) Dk.Terra else Dk.Muted)

            if (m != null && t != null) {
                val nm = m.count { it in allIds }
                val nt = t.count { it in allIds }
                val orphans = all.count { (it.id in m || it.id in t) && remote[it.id] is Remote.NoRelease }
                Text("Mobile : $nm app(s) · TV : $nt app(s)", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Dk.Esp)
                if (orphans > 0) Text("$orphans app(s) cochée(s) sans version publiée : les clients ne les verront pas.", fontSize = 13.sp, color = Dk.Terra)

                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("Rechercher une app") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Dk.Terra, cursorColor = Dk.Terra),
                )

                if (!readOnly) {
                    val allM = allIds.all { it in m }
                    val allT = allIds.all { it in t }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(if (allM) "Aucune sur mobile" else "Tout sur mobile", false) { change(if (allM) m - allIds else m + allIds, t) }
                        Chip(if (allT) "Aucune sur TV" else "Tout sur TV", false) { change(m, if (allT) t - allIds else t + allIds) }
                    }
                }

                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                    Text("Mobile", Modifier.width(76.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Dk.Terra, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("TV", Modifier.width(76.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Dk.Terra, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                val shown = all.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
                if (shown.isEmpty()) Text("Aucune app ne correspond.", fontSize = 14.sp, color = Dk.Muted)
                shown.forEach { a ->
                    val r = remote[a.id]
                    val line = when (r) {
                        is Remote.Ok -> "Version ${r.release.tag.removePrefix("v")} publiée"
                        is Remote.NoRelease -> "Pas encore de version"
                        is Remote.Failed -> "Version inconnue"
                        else -> "Vérification…"
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppIcon(a, 44.dp)
                        Column(Modifier.weight(1f)) {
                            Text(a.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Dk.Esp)
                            Text(line, fontSize = 13.sp, color = if (r is Remote.NoRelease) Dk.Terra else Dk.Muted, maxLines = 1)
                        }
                        Box2(a.id in m, !readOnly) { on -> change(if (on) m + a.id else m - a.id, t) }
                        Box2(a.id in t, !readOnly) { on -> change(m, if (on) t + a.id else t - a.id) }
                    }
                    HorizontalDivider(color = Dk.Line)
                }
            }
        }

        // Rattrape une fausse manip : la barre disparaît toute seule après quelques secondes.
        val u = undo
        if (u != null) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp).clip(RoundedCornerShape(16.dp)).background(Dk.Esp).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Modification publiée", Modifier.weight(1f), color = Dk.White, fontSize = 15.sp)
                Text(
                    "ANNULER", color = Dk.Saffron, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    modifier = Modifier.clickable { undo = null; apply(u.first, u.second) }.padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun Box2(checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.width(76.dp), horizontalArrangement = Arrangement.Center) {
        Switch(
            checked = checked, onCheckedChange = onChange, enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Dk.Sage, checkedThumbColor = Dk.White, checkedBorderColor = Dk.Sage,
                uncheckedTrackColor = Color(0xFFCDBA9E), uncheckedThumbColor = Dk.White, uncheckedBorderColor = Color(0xFFCDBA9E),
            ),
        )
    }
}
