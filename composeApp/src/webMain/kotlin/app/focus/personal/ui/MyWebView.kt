package app.focus.personal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.browser.window

@Composable
actual fun MyWebView(
    url: String,
    modifier: Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text("Opening in new tab...")
    }
    
    LaunchedEffect(url) {
        window.open(url, "_blank")
    }
}
