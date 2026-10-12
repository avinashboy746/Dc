package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Reusable WebViewContainer Composable that loads the Discord web URL and handles:
 * - Basic navigation (Back, Forward, Refresh, LoadUrl)
 * - JavaScript, DOM Storage, Database and Hardware Acceleration
 * - Touch and typing stabilization to prevent unwanted repressing / accidental reloading
 * - Discord file chooser and media permissions
 * - Session cookie persistence
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    url: String,
    modifier: Modifier = Modifier,
    useDesktopMode: Boolean = false,
    onCanGoBackChange: (Boolean) -> Unit = {},
    onCanGoForwardChange: (Boolean) -> Unit = {},
    onLoadingChange: (Boolean) -> Unit = {},
    onProgressChange: (Int) -> Unit = {},
    onErrorChange: (Boolean, String) -> Unit = { _, _ -> },
    onUrlChange: (String) -> Unit = {},
    onWebViewReady: (WebView) -> Unit = {},
    onShowFileChooser: (ValueCallback<Array<Uri>>?) -> Unit = {}
) {
    val mobileUserAgent = remember {
        "Mozilla/5.0 (Linux; Android 14; Mobile; rv:128.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36 DiscordBrowser-bcsupportOG/1.0"
    }
    val desktopUserAgent = remember {
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 DiscordBrowser-bcsupportOG/1.0"
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // 1. Prevent accidental pull-to-refresh & jitter while typing
                overScrollMode = View.OVER_SCROLL_NEVER
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                isFocusable = true
                isFocusableInTouchMode = true

                // 2. Configure WebSettings
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    allowFileAccess = true
                    allowContentAccess = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = false
                    displayZoomControls = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    userAgentString = if (useDesktopMode) desktopUserAgent else mobileUserAgent
                }

                // 3. CookieManager setup for Discord session persistence
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

                // 4. WebChromeClient for Discord progress, attachments & media
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgressChange(newProgress)
                        onLoadingChange(newProgress < 100)
                        onCanGoBackChange(view?.canGoBack() ?: false)
                        onCanGoForwardChange(view?.canGoForward() ?: false)
                    }

                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        onShowFileChooser(filePathCallback)
                        return true
                    }

                    override fun onPermissionRequest(request: PermissionRequest?) {
                        request?.let {
                            val grantedResources = mutableListOf<String>()
                            for (res in it.resources) {
                                if (res == PermissionRequest.RESOURCE_AUDIO_CAPTURE ||
                                    res == PermissionRequest.RESOURCE_VIDEO_CAPTURE
                                ) {
                                    grantedResources.add(res)
                                }
                            }
                            if (grantedResources.isNotEmpty()) {
                                it.grant(grantedResources.toTypedArray())
                            } else {
                                it.deny()
                            }
                        }
                    }
                }

                // 5. WebViewClient for Discord domain protection & input stabilization
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val targetUrl = request?.url?.toString() ?: return false
                        onUrlChange(targetUrl)

                        val isDiscordDomain = targetUrl.contains("discord.com") ||
                                targetUrl.contains("discordapp.com") ||
                                targetUrl.contains("discord.gg") ||
                                targetUrl.contains("media.discordapp.net") ||
                                targetUrl.contains("google.com/accounts") ||
                                targetUrl.contains("appleid.apple.com")

                        return if (isDiscordDomain) {
                            false // Load within Discord Browser
                        } else {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                            }
                            true
                        }
                    }

                    override fun onPageStarted(view: WebView?, targetUrl: String?, favicon: android.graphics.Bitmap?) {
                        onLoadingChange(true)
                        onErrorChange(false, "")
                        targetUrl?.let { onUrlChange(it) }
                        onCanGoBackChange(view?.canGoBack() ?: false)
                        onCanGoForwardChange(view?.canGoForward() ?: false)
                    }

                    override fun onPageFinished(view: WebView?, targetUrl: String?) {
                        onLoadingChange(false)
                        targetUrl?.let { onUrlChange(it) }
                        onCanGoBackChange(view?.canGoBack() ?: false)
                        onCanGoForwardChange(view?.canGoForward() ?: false)
                        CookieManager.getInstance().flush()

                        // Inject CSS for typing stability (removes accidental overscroll refresh)
                        val fixTypingScript = """
                            (function() {
                                document.documentElement.style.overscrollBehaviorY = 'none';
                                document.body.style.overscrollBehaviorY = 'none';
                                document.body.style.touchAction = 'manipulation';
                            })();
                        """.trimIndent()
                        view?.evaluateJavascript(fixTypingScript, null)
                    }

                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        if (request?.isForMainFrame == true) {
                            onErrorChange(true, error?.description?.toString() ?: "Network error")
                            onLoadingChange(false)
                        }
                    }
                }

                loadUrl(url)
                onWebViewReady(this)
            }
        },
        update = { webView ->
            val targetUA = if (useDesktopMode) desktopUserAgent else mobileUserAgent
            if (webView.settings.userAgentString != targetUA) {
                webView.settings.userAgentString = targetUA
                webView.reload()
            }
        }
    )
}
