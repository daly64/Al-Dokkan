package tn.aldokkan.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.aldokkan.app.Catalog
import tn.aldokkan.app.Remote
import tn.aldokkan.app.UiState

/** Version Admin : on choisit ici les apps que les clients verront, puis on publie. */
@Composable
fun ClientsScreen(ui: UiState, hasKey: Boolean, onPublish: (Set<String>) -> Unit) {
    val saved = Catalog.clientIds ?: emptySet()
    var chosen by remember(saved) { mutableStateOf(saved) }
    val changed = chosen != saved

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Apps des clients", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = Dk.Esp)
        Text(
            "Choisis les apps que la version Clients affiche, puis publie. Les clients voient le changement à leur prochaine ouverture.",
            fontSize = 15.sp, color = Dk.Muted,
        )
        if (!hasKey) InfoNote("Pour publier, colle d'abord la clé d'administration dans Réglages.")

        Catalog.choosable.forEach { a ->
            val published = ui.remote[a.id] is Remote.Ok
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppIcon(a, 48.dp)
                Column(Modifier.weight(1f)) {
                    Text(a.name, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Dk.Esp)
                    Text(if (published) a.tagline else "Pas encore de version publiée", fontSize = 14.sp, color = Dk.Muted)
                }
                Switch(
                    checked = a.id in chosen,
                    onCheckedChange = { on -> chosen = if (on) chosen + a.id else chosen - a.id },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Dk.Sage, checkedThumbColor = Dk.White, checkedBorderColor = Dk.Sage,
                        uncheckedTrackColor = Color(0xFFCDBA9E), uncheckedThumbColor = Dk.White, uncheckedBorderColor = Color(0xFFCDBA9E),
                    ),
                )
            }
            HorizontalDivider(color = Dk.Line)
        }

        Text(
            if (changed) "${chosen.size} app(s) choisie(s) · modifications non publiées" else "${chosen.size} app(s) visibles par les clients",
            fontSize = 14.sp, color = Dk.Muted,
        )
        PrimaryButton("Publier pour les clients", { onPublish(chosen) }, Modifier.fillMaxWidth(), enabled = changed || Catalog.clientIds == null)
    }
}
