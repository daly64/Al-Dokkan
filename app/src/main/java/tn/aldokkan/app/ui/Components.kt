package tn.aldokkan.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import androidx.compose.ui.graphics.drawscope.translate
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

/** L'icône réelle de l'app (fond + premier plan comme sur l'écran d'accueil), sinon un pictogramme. */
@Composable
fun AppIcon(app: AppEntry, size: Dp) {
    val shape = RoundedCornerShape(size * 0.23f)
    when {
        app.id.startsWith("aldokkan") -> Box(Modifier.size(size).clip(shape)) { Logo(size) }
        app.iconImg != null -> Image(
            painterResource(app.iconImg), app.name,
            Modifier.size(size).clip(shape), contentScale = ContentScale.Crop,
        )
        app.iconBg != null && app.iconFg != null -> Box(Modifier.size(size).clip(shape)) {
            Image(painterResource(app.iconBg), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            // Le premier plan d'une icône adaptative est dessiné sur 108 dp mais seuls 72 dp se voient.
            Image(
                painterResource(app.iconFg), app.name,
                Modifier.fillMaxSize().graphicsLayer(scaleX = 1.5f, scaleY = 1.5f),
            )
        }
        else -> Box(Modifier.size(size).clip(shape).background(app.bg), contentAlignment = Alignment.Center) {
            PathIcon(app.glyph, app.fg, size)
        }
    }
}

/** Le logo d'Al Dokkan : une échoppe avec auvent rayé et porte en arche. */
@Composable
fun Logo(size: Dp, admin: Boolean = tn.aldokkan.app.BuildConfig.IS_ADMIN) {
    // Même dessin que l'icône du lanceur (repère 108), recadré sur la partie visible.
    val tile = if (admin) Dk.Esp else Dk.Terra
    val door = if (admin) Dk.Terra else Dk.Esp
    val bar = if (admin) Dk.Saffron else Dk.Esp
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension / 78f
        drawRoundRect(tile, size = this.size, cornerRadius = CornerRadius(this.size.minDimension * 0.23f))
        scale(s, s, pivot = Offset.Zero) {
            translate(-15f, -15f) {
                drawRect(Dk.Sand, Offset(30f, 38f), Size(48f, 42f))
                drawRoundRect(Dk.Esp.copy(alpha = 0.85f), Offset(34f, 50f), Size(9f, 10f), CornerRadius(1.5f))
                drawRoundRect(Dk.Esp.copy(alpha = 0.85f), Offset(65f, 50f), Size(9f, 10f), CornerRadius(1.5f))
                val d = Path().apply {
                    moveTo(46f, 80f); lineTo(46f, 64f)
                    arcTo(Rect(46f, 56f, 62f, 72f), 180f, 180f, false)
                    lineTo(62f, 80f); close()
                }
                drawPath(d, door)
                drawRoundRect(bar, Offset(24f, 80f), Size(60f, 4f), CornerRadius(2f))
                listOf(Dk.Saffron, Dk.Cream, Dk.Saffron, Dk.Cream).forEachIndexed { i, c ->
                    val x = 26f + 14f * i
                    drawRect(c, Offset(x, 26f), Size(14f, 12f))
                    drawArc(c, 0f, 180f, true, Offset(x, 31f), Size(14f, 14f))
                }
                drawRoundRect(bar, Offset(24f, 23f), Size(60f, 4f), CornerRadius(2f))
                if (admin) {
                    drawCircle(Dk.Esp, 14f, Offset(77f, 75f))
                    drawCircle(Color(0xFF2F8F5B), 11f, Offset(77f, 75f))
                    val ck = Path().apply { moveTo(71.5f, 75f); lineTo(75.3f, 78.8f); lineTo(82.5f, 71.6f) }
                    drawPath(ck, Dk.White, style = androidx.compose.ui.graphics.drawscope.Stroke(3.2f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                }
            }
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, height: Dp = 48.dp) {
    val bg = if (enabled) Dk.Action else Dk.Fill
    val fg = if (enabled) Dk.White else Dk.Grey
    Box(
        modifier.height(height).clip(RoundedCornerShape(50)).background(bg)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = fg, fontWeight = FontWeight.Medium, fontSize = 15.sp, maxLines = 1) }
}

@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 48.dp) {
    Box(
        modifier.height(height).clip(RoundedCornerShape(50)).border(1.dp, Dk.Stroke, RoundedCornerShape(50))
            .clickable(onClick = onClick).padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Dk.Action, fontWeight = FontWeight.Medium, fontSize = 15.sp, maxLines = 1) }
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
