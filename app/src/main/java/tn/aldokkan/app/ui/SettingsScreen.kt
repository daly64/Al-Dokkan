package tn.aldokkan.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.aldokkan.app.Prefs
import tn.aldokkan.app.StoreViewModel

@Composable
fun SettingsScreen(vm: StoreViewModel, prefs: Prefs, onFullscreen: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    var token by remember { mutableStateOf(prefs.token) }
    var show by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf(prefs.fullscreen) }
    var wifiOnly by remember { mutableStateOf(prefs.wifiOnly) }
    val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "" }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Réglages", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = Dk.Esp)
        Label("ACCÈS AUX APPS")
        Text(
            "Tes apps sont privées : colle ici ton jeton d'accès en lecture seule. Il reste sur cet appareil.",
            fontSize = 14.sp, color = Dk.Muted, lineHeight = 20.sp,
        )
        OutlinedTextField(
            value = token, onValueChange = { token = it; saved = false },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Jeton d'accès") },
            singleLine = true,
            visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Dk.Terra, unfocusedBorderColor = Dk.Line, cursorColor = Dk.Terra,
                focusedLabelColor = Dk.Terra, focusedTextColor = Dk.Esp, unfocusedTextColor = Dk.Esp,
            ),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlineButton(if (show) "Masquer" else "Afficher", { show = !show }, height = 44.dp)
            PrimaryButton(if (saved) "Enregistré" else "Enregistrer", { vm.saveToken(token); saved = true }, height = 44.dp)
        }
        HorizontalDivider(color = Dk.Line, modifier = Modifier.padding(top = 8.dp))

        Label("APPS")
        SwitchRow("Wi-Fi uniquement", "Ne pas télécharger en données mobiles", wifiOnly) { wifiOnly = it; prefs.wifiOnly = it }
        HorizontalDivider(color = Dk.Line)

        Label("AFFICHAGE")
        SwitchRow("Plein écran automatique", "Masquer les barres du système", fullscreen) { fullscreen = it; prefs.fullscreen = it; onFullscreen(it) }
        HorizontalDivider(color = Dk.Line)

        Label("À PROPOS")
        Column(Modifier.height(64.dp), verticalArrangement = Arrangement.Center) {
            Text("Al Dokkan", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Dk.Esp)
            Text("Version $version", fontSize = 14.sp, color = Dk.Muted)
        }
    }
}

@Composable
private fun Label(t: String) {
    Text(t, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Dk.Terra, modifier = Modifier.padding(top = 14.dp))
}

@Composable
private fun SwitchRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Dk.Esp)
            Text(sub, fontSize = 14.sp, color = Dk.Muted)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Dk.Sage, checkedThumbColor = Dk.White, checkedBorderColor = Dk.Sage, uncheckedTrackColor = androidx.compose.ui.graphics.Color(0xFFCDBA9E), uncheckedThumbColor = Dk.White, uncheckedBorderColor = androidx.compose.ui.graphics.Color(0xFFCDBA9E)),
        )
    }
}
