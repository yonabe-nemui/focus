package app.focus.personal.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun MyWebView(
    url: String,
    modifier: Modifier = Modifier
)
