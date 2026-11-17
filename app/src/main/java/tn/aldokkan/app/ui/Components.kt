package tn.aldokkan.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.aldokkan.app.AppEntry
import tn.aldokkan.app.AppStatus
import tn.aldokkan.app.Progress

/** Dessine des tracés (même format que les SVG de la maquette) en trait épais et arrondi. */
@Composable
fun PathIcon(
    paths: List<String>,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    viewBox: Float = 96f,
    stroke: Float = 5f,
) {
    val parsed = remember(paths) { paths.map { PathParser().parsePathString(it).toPath() } }
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / viewBox
        scale(s, s, pivot = Offset.Zero) {
            parsed.forEach {
                drawPath(it, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

@Composable
fun AppIcon(app: AppEntry, size: Dp) {
    val shape = RoundedCornerShape(size * 0.23f)
    when {
        app.id == "aldokkan" -> Box(Modifier.size(size).clip(shape)) { Logo(size) }
        app.iconImg != null -> androidx.compose.foundation.Image(
            androidx.compose.ui.res.painterResource(app.iconImg), app.name,
            Modifier.size(size).clip(shape), contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        )
        app.iconBg != null && app.iconFg != null -> Box(Modifier.size(size).clip(shape)) {
            androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(app.iconBg), null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            // Premier plan d'une icône adaptative : 108 dp dessinés, 72 dp visibles.
            androidx.compose.foundation.Image(
                androidx.compose.ui.res.painterResource(app.iconFg), app.name,
                Modifier.fillMaxSize().graphicsLayer(scaleX = 1.5f, scaleY = 1.5f),
            )
        }
        else -> Box(Modifier.size(size).clip(shape).background(app.bg), contentAlignment = Alignment.Center) { PathIcon(app.glyph, app.fg, size) }
    }
}

/** Le logo d'Al Dokkan : une échoppe avec auvent rayé et porte en arche. */
@Composable
fun Logo(size: Dp, tile: Color = Dk.Terra, stripe1: Color = Dk.Saffron, stripe2: Color = Dk.Cream, arch: Color = Dk.Esp, base: Color = Dk.Cream) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension / 96f
        scale(s, s, pivot = Offset.Zero) {
            drawRoundRect(tile, size = Size(96f, 96f), cornerRadius = CornerRadius(22f, 22f))
            drawRect(stripe1, Offset(18f, 26f), Size(20f, 10f))
            drawRect(stripe2, Offset(38f, 26f), Size(20f, 10f))
            drawRect(stripe1, Offset(58f, 26f), Size(20f, 10f))
            drawCircle(stripe1, 10f, Offset(28f, 36f))
            drawCircle(stripe2, 10f, Offset(48f, 36f))
            drawCircle(stripe1, 10f, Offset(68f, 36f))
            val door = Path().apply {
                moveTo(38f, 74f)
                lineTo(38f, 60f)
                arcTo(Rect(38f, 50f, 58f, 70f), 180f, 180f, false)
                lineTo(58f, 74f)
                close()
            }
            drawPath(door, arch)
            drawRoundRect(base, Offset(30f, 74f), Size(36f, 4f), CornerRadius(2f, 2f))
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, height: Dp = 48.dp) {
    val bg = if (enabled) Dk.Terra else Dk.Line
    val fg = if (enabled) Dk.White else Dk.Muted
    Box(
        modifier.height(height).clip(RoundedCornerShape(50)).background(bg)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = fg, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1) }
}

@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 48.dp) {
    Box(
        modifier.height(height).clip(RoundedCornerShape(50)).border(2.dp, Dk.Terra, RoundedCornerShape(50))
            .clickable(onClick = onClick).padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Dk.Terra, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1) }
}

/** Le bouton d'action d'une app selon son état : Installer, Mettre à jour, Ouvrir… */
@Composable
fun ActionButton(status: AppStatus, busy: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 40.dp) {
    when {
        busy -> PrimaryButton("En cours…", {}, modifier, enabled = false, height = height)
        status == AppStatus.Install -> PrimaryButton("Installer", onClick, modifier, height = height)
        status == AppStatus.Update -> PrimaryButton("Mettre à jour", onClick, modifier, height = height)
        status == AppStatus.Open -> OutlineButton("Ouvrir", onClick, modifier, height = height)
        status == AppStatus.Checking -> PrimaryButton("…", {}, modifier, enabled = false, height = height)
        status == AppStatus.NotPublished -> PrimaryButton("Bientôt", {}, modifier, enabled = false, height = height)
        else -> PrimaryButton("Indisponible", {}, modifier, enabled = false, height = height)
    }
}

fun statusLine(app: AppEntry, status: AppStatus, version: String?): String = when (status) {
    AppStatus.Update -> "${app.tagline} · mise à jour disponible"
    AppStatus.Open -> "${app.tagline}" + if (!version.isNullOrBlank()) " · v$version" else ""
    else -> app.tagline
}

/** Une ligne d'app, comme dans la liste du Play Store. */
@Composable
fun AppRow(
    app: AppEntry,
    status: AppStatus,
    version: String?,
    progress: Progress?,
    onClick: () -> Unit,
    onAction: () -> Unit,
    onCancel: () -> Unit,
    busy: Boolean,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AppIcon(app, 56.dp)
        Column(Modifier.weight(1f)) {
            Text(app.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Dk.Esp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(statusLine(app, status, version), fontSize = 14.sp, color = Dk.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (progress != null) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress.fraction },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Dk.Saffron, trackColor = Dk.Line,
                )
            }
        }
        if (progress != null) OutlineButton("Annuler", onCancel, height = 40.dp)
        else ActionButton(status, busy, onAction)
    }
}

/** Une carte d'app pour les rangées qui défilent. */
@Composable
fun AppCard(app: AppEntry, sub: String, onClick: () -> Unit) {
    Column(Modifier.width(88.dp).clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AppIcon(app, 80.dp)
        Text(app.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Dk.Esp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
        Text(sub, fontSize = 13.sp, color = Dk.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SectionHeader(title: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().height(36.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Dk.Esp)
        if (onClick != null) PathIcon(listOf("M5 12h14", "M13 6l6 6-6 6"), Dk.Esp, 24.dp, viewBox = 24f, stroke = 2.2f)
    }
}

@Composable
fun TabLabel(text: String, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick).padding(end = 22.dp)) {
        Text(
            text, fontSize = 16.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Dk.Terra else Dk.Muted, modifier = Modifier.padding(vertical = 12.dp),
        )
        Box(Modifier.height(3.dp).width(28.dp).background(if (selected) Dk.Terra else Color.Transparent))
    }
}

@Composable
fun InfoNote(text: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Dk.GreenBg).padding(16.dp)) {
        Text(text, fontSize = 14.sp, color = Dk.GreenTx, lineHeight = 20.sp)
    }
}

@Composable
fun Chip(text: String, selected: Boolean = false, onClick: () -> Unit = {}) {
    Box(
        Modifier.height(40.dp).clip(RoundedCornerShape(20.dp))
            .background(if (selected) Dk.Esp else Dk.Sand).clickable(onClick = onClick).padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontSize = 15.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) Dk.Cream else Dk.Esp) }
}

@Composable
fun Dot(on: Boolean) {
    Box(
        Modifier.height(8.dp).width(if (on) 28.dp else 8.dp).clip(CircleShape).background(if (on) Dk.Saffron else Dk.Line),
    )
}

@Composable
fun CenterText(text: String) {
    Text(text, fontSize = 15.sp, color = Dk.Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp))
}
