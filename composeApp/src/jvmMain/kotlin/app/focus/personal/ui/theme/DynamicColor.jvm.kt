package app.focus.personal.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

actual val supportsDynamicColor: Boolean = false

@Composable
actual fun dynamicColorSchemeOrNull(darkTheme: Boolean): ColorScheme? = null
