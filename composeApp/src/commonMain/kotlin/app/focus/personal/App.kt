package app.focus.personal

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import app.focus.personal.ui.RssListScreen
import app.focus.personal.viewmodel.RssViewModel
import androidx.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(
    viewModel: RssViewModel,
    onLinkClick: (String) -> Unit
) {
    MaterialTheme {
        RssListScreen(
            viewModel = viewModel,
            onLinkClick = onLinkClick
        )
    }
}
