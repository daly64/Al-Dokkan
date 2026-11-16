package tn.aldokkan.app.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatSize(bytes: Long): String =
    if (bytes >= 1_000_000) String.format(Locale.getDefault(), "%.1f Mo", bytes / 1_048_576.0)
    else "${bytes / 1024} Ko"

fun formatDate(ms: Long): String =
    if (ms <= 0) "—"
    else DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(ms))
