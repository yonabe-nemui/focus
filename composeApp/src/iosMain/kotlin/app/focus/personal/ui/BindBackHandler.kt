package app.focus.personal.ui

import androidx.compose.runtime.Composable

@Composable
actual fun BindBackHandler(onBack: () -> Unit) {
    // iOS gestures are usually handled by the system or a navigation controller.
    // If you need to handle app-level back gestures in Compose, you could 
    // add some custom detection here.
}
