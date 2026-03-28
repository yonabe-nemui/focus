package app.focus.personal.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import platform.WebKit.WKWebView
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest

@Composable
actual fun MyWebView(
    url: String,
    modifier: Modifier
) {
    UIKitView(
        factory = {
            WKWebView().apply {
                loadRequest(NSURLRequest(NSURL(string = url)))
            }
        },
        update = { webView ->
            webView.loadRequest(NSURLRequest(NSURL(string = url)))
        },
        modifier = modifier
    )
}
