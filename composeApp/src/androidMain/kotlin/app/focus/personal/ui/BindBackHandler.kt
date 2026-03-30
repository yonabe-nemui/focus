package app.focus.personal.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun BindBackHandler(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
}
