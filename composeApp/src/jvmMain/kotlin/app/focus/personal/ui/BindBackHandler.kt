package app.focus.personal.ui

import androidx.compose.runtime.Composable

// システムの戻る操作がないため何もしない(画面上部の戻るボタンで遷移する)
@Composable
actual fun BindBackHandler(onBack: () -> Unit) {
}
