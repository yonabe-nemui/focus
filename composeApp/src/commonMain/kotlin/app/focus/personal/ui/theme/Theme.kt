package app.focus.personal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun FocusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) FocusDarkColorScheme else FocusLightColorScheme,
        typography  = FocusTypography,
        shapes      = FocusShapeScheme,
        content     = content
    )
}
