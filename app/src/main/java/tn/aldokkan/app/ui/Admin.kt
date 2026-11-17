package tn.aldokkan.app.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch
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

    LaunchedEffect(Unit) {
        val r = Backend.choice()
        val c = r.value
        if (c != null) { mobile = c.mobile; tv = c.tv; status = "Tout est publié" }
        else status = (r.error ?: "Erreur") + " — impossible de charger la liste"
    }

    fun save(m: Set<String>, t: Set<String>) {
        mobile = m; tv = t
        status = "Publication…"
        scope.launch { status = Backend.publish(m, t) ?: "Publié pour les clients" }
    }

    val m = mobile
    val t = tv
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Apps des clients", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = Dk.Esp)
        Text(status, fontSize = 14.sp, color = if (status.startsWith("Publié") || status.startsWith("Tout")) Dk.GreenTx else Dk.Muted)
        if (m != null && t != null) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
                Text("Mobile", Modifier.width(76.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Dk.Terra)
                Text("TV", Modifier.width(76.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Dk.Terra)
            }
            Catalog.choosable.forEach { a ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppIcon(a, 44.dp)
                    Column(Modifier.weight(1f)) {
                        Text(a.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Dk.Esp)
                        Text(a.tagline, fontSize = 13.sp, color = Dk.Muted, maxLines = 1)
                    }
                    Box2(a.id in m) { on -> save(if (on) m + a.id else m - a.id, t) }
                    Box2(a.id in t) { on -> save(m, if (on) t + a.id else t - a.id) }
                }
                HorizontalDivider(color = Dk.Line)
            }
        }
    }
}

@Composable
private fun Box2(checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.width(76.dp), horizontalArrangement = Arrangement.Center) {
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Dk.Sage, checkedThumbColor = Dk.White, checkedBorderColor = Dk.Sage,
                uncheckedTrackColor = Color(0xFFCDBA9E), uncheckedThumbColor = Dk.White, uncheckedBorderColor = Color(0xFFCDBA9E),
            ),
        )
    }
}
