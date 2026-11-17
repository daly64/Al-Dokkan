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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import tn.aldokkan.app.Catalog

/** Trois écrans de bienvenue, au premier lancement seulement. */
@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    val pager = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == 2

    Column(Modifier.fillMaxSize().background(Dk.White).padding(horizontal = 28.dp, vertical = 24.dp)) {
        Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Passer", color = Dk.Muted, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onDone).padding(12.dp),
            )
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.fillMaxWidth().height(340.dp), contentAlignment = Alignment.Center) {
                    when (page) {
                        0 -> TilesArt()
                        1 -> UpdateArt()
                        else -> ShieldArt()
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    when (page) {
                        0 -> "Toutes tes apps, au même endroit"
                        1 -> "Installe et mets à jour en un geste"
                        else -> "100 % perso, sans compte"
                    },
                    fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 35.sp, color = Dk.Esp,
                )
                Text(
                    when (page) {
                        0 -> "Al Dokkan range les applications que tu as créées, comme sur les étagères d'une petite boutique."
                        1 -> "Une nouvelle version s'installe par-dessus l'ancienne. Tes données restent en place."
                        else -> "Pas de pub, pas d'inscription. Ta boutique ne sert que toi."
                    },
                    fontSize = 17.sp, lineHeight = 25.sp, color = Dk.Muted,
                )
            }
        }
        Row(Modifier.fillMaxWidth().height(24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            repeat(3) { Dot(it == pager.currentPage) }
        }
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp)).background(Dk.Terra).clickable {
                if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            },
            contentAlignment = Alignment.Center,
        ) { Text(if (last) "Commencer" else "Suivant", color = Dk.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
    }
}

@Composable
private fun TilesArt() {
    val apps = Catalog.apps.take(8)
    Column(Modifier.width(288.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(3) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(3) { col ->
                    val i = row * 3 + col
                    if (i == 4) {
                        Box(Modifier.size(86.dp), contentAlignment = Alignment.Center) { Logo(86.dp) }
                    } else {
                        val a = apps[if (i < 4) i else i - 1]
                        AppIcon(a, 86.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateArt() {
    Column(
        Modifier.width(310.dp).clip(RoundedCornerShape(28.dp)).background(Dk.Cream).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(Catalog.apps[0], 56.dp)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.height(14.dp).width(120.dp).clip(CircleShape).background(Dk.Esp))
                Box(Modifier.height(10.dp).width(80.dp).clip(CircleShape).background(Dk.Line))
            }
        }
        Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Dk.Line)) {
            Box(Modifier.fillMaxWidth(0.68f).height(10.dp).clip(CircleShape).background(Dk.Saffron))
        }
        Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(Dk.Terra), contentAlignment = Alignment.Center) {
            Text("Mettre à jour", color = Dk.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun ShieldArt() {
    Box(Modifier.size(220.dp).clip(CircleShape).background(Dk.Esp), contentAlignment = Alignment.Center) {
        PathIcon(listOf("M48 12 78 24v24c0 18-12 30-30 36C30 78 18 66 18 48V24Z"), Dk.Saffron, 120.dp, stroke = 6f)
        PathIcon(listOf("M36 48l9 9 17-18"), Color(0xFF58A97D), 120.dp, stroke = 7f)
    }
}
