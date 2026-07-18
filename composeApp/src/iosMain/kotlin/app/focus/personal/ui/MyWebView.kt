package app.focus.personal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.darwin.NSObject

/** 読み込み完了時にタイトルと進捗 1f を通知する最小実装のデリゲート。 */
private class FocusNavigationDelegate(
    private val onProgress: (Float) -> Unit,
    private val onTitle: (String) -> Unit,
) : NSObject(), WKNavigationDelegateProtocol {
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        onProgress(1f)
        webView.title?.takeIf { it.isNotBlank() }?.let(onTitle)
    }
}

@Composable
actual fun MyWebView(
    url: String,
    modifier: Modifier,
    onProgress: (Float) -> Unit,
    onTitle: (String) -> Unit,
) {
    val delegate = remember { FocusNavigationDelegate(onProgress, onTitle) }
    UIKitView(
        factory = {
            WKWebView().apply {
                navigationDelegate = delegate
                loadRequest(NSURLRequest(NSURL(string = url)))
            }
        },
        update = { webView ->
            webView.loadRequest(NSURLRequest(NSURL(string = url)))
        },
        modifier = modifier
    )
}
