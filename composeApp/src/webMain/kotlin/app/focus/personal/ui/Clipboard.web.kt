package app.focus.personal.ui

import androidx.compose.ui.platform.ClipEntry

actual fun plainTextClipEntry(text: String): ClipEntry = ClipEntry.withPlainText(text)
