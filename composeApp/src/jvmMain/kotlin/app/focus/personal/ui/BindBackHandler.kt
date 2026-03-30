package app.focus.personal.ui

import androidx.compose.runtime.Composable

@Composable
actual fun BindBackHandler(onBack: () -> Unit) {
    // Other platforms might not have a hardware back button
    // Optionally handle Escape key or other navigation events here
}
