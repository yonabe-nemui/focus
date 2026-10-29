package app.focus.personal.ui

import androidx.compose.runtime.Composable

// ブラウザの戻るボタンとは連動させない(画面上部の戻るボタンで遷移する)
@Composable
actual fun BindBackHandler(onBack: () -> Unit) {
}
