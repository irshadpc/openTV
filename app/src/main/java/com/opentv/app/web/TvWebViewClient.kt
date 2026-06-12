package com.opentv.app.web

import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.opentv.app.AppConfig

/**
 * Custom [WebViewClient] responsible for:
 *  - restricting top-level navigation to trusted domains,
 *  - reporting page load start/finish (for the spinner & last-URL memory),
 *  - graceful handling of network and SSL errors.
 */
class TvWebViewClient(
    private val onPageLoadStarted: (url: String) -> Unit,
    private val onPageLoadFinished: (url: String) -> Unit,
    private val onLoadError: (description: String) -> Unit
) : WebViewClient() {

    /**
     * Decide whether a navigation should proceed. Returning true means "I handled
     * it / block it"; false means "let the WebView load it".
     */
    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest
    ): Boolean {
        val url = request.url
        return if (isTrusted(url)) {
            false // allow the WebView to load trusted top-level navigation
        } else {
            // Block navigation to untrusted hosts (prevents external redirects,
            // ad popups, app-store deep links, etc.). We intentionally do NOT
            // open it in an external browser.
            true
        }
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageLoadStarted(url)
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        onPageLoadFinished(url)
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        super.onReceivedError(view, request, error)
        // Only surface errors for the MAIN frame; sub-resource failures (ads,
        // analytics, optional CDNs) should not blank the whole screen.
        if (request.isForMainFrame) {
            onLoadError(error.description?.toString() ?: "Failed to load page")
        }
    }

    override fun onReceivedSslError(
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        // Only proceed through SSL errors for trusted hosts; otherwise cancel.
        val host = runCatching { Uri.parse(error.url).host }.getOrNull()
        if (host != null && isTrustedHost(host)) {
            // Streaming CDNs occasionally present cert chains the WebView trust
            // store is strict about; allow them only for our allow-listed hosts.
            handler.proceed()
        } else {
            handler.cancel()
            onLoadError("Secure connection failed")
        }
    }

    // --- Trust helpers -------------------------------------------------------

    private fun isTrusted(uri: Uri): Boolean {
        val host = uri.host ?: return false
        return isTrustedHost(host)
    }

    private fun isTrustedHost(host: String): Boolean {
        return AppConfig.ALLOWED_HOSTS.any { allowed ->
            host == allowed || host.endsWith(".$allowed")
        }
    }
}
