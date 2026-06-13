package com.opentv.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.opentv.app.databinding.ActivityMainBinding
import com.opentv.app.player.ChannelsActivity
import com.opentv.app.util.ExternalAppLauncher
import com.opentv.app.util.NetworkMonitor
import com.opentv.app.web.TvWebChromeClient
import com.opentv.app.web.TvWebViewClient

/**
 * Hosts the full-screen streaming WebView and all TV-specific behavior:
 * immersive mode, D-pad / remote handling, double-back-to-exit, a settings
 * panel, connectivity-aware reloading, loading spinner, and an offline screen.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var webView: WebView
    private lateinit var chromeClient: TvWebChromeClient
    private lateinit var networkMonitor: NetworkMonitor

    private val prefs by lazy {
        getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)
    }

    private var backPressedTime = 0L
    private var hasLoadedOnce = false

    // --- Virtual D-pad pointer state ---
    // Cursor mode is ON by default because the streamed website is not built
    // for a D-pad: the pointer lets the user move freely and click anything.
    private var cursorMode = true
    private var cursorX = 0f
    private var cursorY = 0f
    private val cursorBaseStep = 60f      // px per key press (fast)
    private val cursorEdgeScroll = 260f   // page scroll when pointer hits an edge
    private val cursorIdleMs = 3000L      // hide the pointer after this idle time
    private val hideCursorRunnable = Runnable { binding.cursorView.visibility = View.GONE }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draw behind system bars so we can hide them for an immersive view.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Keep the screen awake during streaming (no sleep mid-match).
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setupWebView()
        setupOverlayControls()
        setupNetworkMonitor()

        // Restore the last visited page, or fall back to the configured start URL.
        val startUrl = prefs.getString(AppConfig.KEY_LAST_URL, null) ?: AppConfig.START_URL
        loadUrlOrOffline(startUrl)
    }

    // ---------------------------------------------------------------------
    // WebView configuration
    // ---------------------------------------------------------------------

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView = binding.webView

        webView.settings.apply {
            javaScriptEnabled = true                 // required for the player
            domStorageEnabled = true                 // HTML5 localStorage
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false // allow autoplay of streams
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT      // cache web content
            allowContentAccess = true
            allowFileAccess = false                  // security: no local file URIs
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            // Identify as a TV browser; some players gate features on UA.
            userAgentString = userAgentString + " OpenTV/1.0 AndroidTV"
            // Mixed content can be required when an https page embeds an http
            // HLS stream. Allow it so streams are not silently blocked.
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }

        // Force a dark WebView surface where supported (matches the dark theme).
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, true)
        }

        // Hardware-accelerated rendering for smooth video.
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.setBackgroundColor(0xFF000000.toInt())

        // Persist cookies / sessions across launches.
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        // Custom clients (security, fullscreen video, progress, errors).
        webView.webViewClient = TvWebViewClient(
            onPageLoadStarted = { showSpinner(true) },
            onPageLoadFinished = { url ->
                showSpinner(false)
                hasLoadedOnce = true
                rememberLastUrl(url)
            },
            onLoadError = { description ->
                showSpinner(false)
                if (!networkMonitor.isOnline()) {
                    showOffline(true)
                } else {
                    Toast.makeText(this, description, Toast.LENGTH_SHORT).show()
                }
            }
        )

        chromeClient = TvWebChromeClient(
            fullScreenContainer = binding.fullscreenContainer,
            webViewContainer = binding.webView,
            onProgress = { progress -> binding.progressBar.progress = progress },
            onEnterFullScreen = {
                // During video playback hide the pointer + control bar so they
                // don't sit on top of the picture.
                binding.controlBar.visibility = View.GONE
                binding.cursorView.visibility = View.GONE
                hideSystemUi()
            },
            onExitFullScreen = {
                binding.controlBar.visibility = View.VISIBLE
                applyCursorVisibility()
                hideSystemUi()
            }
        )
        webView.webChromeClient = chromeClient
    }

    // ---------------------------------------------------------------------
    // Overlay controls (spinner, refresh, offline retry)
    // ---------------------------------------------------------------------

    private fun setupOverlayControls() {
        binding.refreshButton.setOnClickListener { webView.reload() }
        binding.menuButton.setOnClickListener { showSettingsPanel() }
        binding.cursorButton.setOnClickListener { toggleCursorMode() }
        binding.retryButton.setOnClickListener {
            showOffline(false)
            loadUrlOrOffline(webView.url ?: AppConfig.START_URL)
        }

        // Centre the pointer once the root has been measured, then show it.
        binding.root.post {
            cursorX = binding.root.width / 2f
            cursorY = binding.root.height / 2f
            applyCursorVisibility()
        }
    }

    // ---------------------------------------------------------------------
    // Virtual D-pad pointer
    // ---------------------------------------------------------------------

    /**
     * When the pointer is on we intercept the D-pad here (before the WebView
     * sees it): arrows move the pointer, OK "clicks" wherever it sits. This is
     * what makes a non-TV website usable with a plain remote.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Don't hijack the D-pad while the offline screen or a video is up, or
        // when the pointer is disabled — let normal focus handle those.
        val pointerActive = cursorMode &&
            binding.offlineLayout.visibility != View.VISIBLE &&
            !chromeClient.isInFullScreen()

        if (pointerActive && event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    // If the pointer was hidden (idle), the first press just
                    // reveals it; otherwise it moves.
                    if (binding.cursorView.visibility == View.VISIBLE) {
                        moveCursor(event.keyCode, event.repeatCount)
                    } else {
                        showCursor()
                    }
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_BUTTON_A -> {
                    // Don't click blind: if hidden, reveal first.
                    if (binding.cursorView.visibility == View.VISIBLE) {
                        clickAtCursor()
                        scheduleCursorHide()
                    } else {
                        showCursor()
                    }
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun moveCursor(keyCode: Int, repeatCount: Int) {
        // Hold an arrow to accelerate, so crossing the screen isn't tedious.
        val step = cursorBaseStep * (1f + minOf(repeatCount, 12) * 0.6f)
        val w = binding.root.width.toFloat()
        val h = binding.root.height.toFloat()

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> cursorX -= step
            KeyEvent.KEYCODE_DPAD_RIGHT -> cursorX += step
            KeyEvent.KEYCODE_DPAD_UP -> {
                cursorY -= step
                if (cursorY <= 0f) webView.scrollBy(0, -cursorEdgeScroll.toInt())
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                cursorY += step
                if (cursorY >= h) webView.scrollBy(0, cursorEdgeScroll.toInt())
            }
        }
        cursorX = cursorX.coerceIn(0f, w)
        cursorY = cursorY.coerceIn(0f, h)
        showCursor()
    }

    private fun updateCursorPosition() {
        // Anchor the pointer's tip (top-left of the arrow) at the position.
        binding.cursorView.x = cursorX
        binding.cursorView.y = cursorY
    }

    /** Make the pointer visible at its current spot and (re)start the idle timer. */
    private fun showCursor() {
        updateCursorPosition()
        binding.cursorView.visibility = View.VISIBLE
        scheduleCursorHide()
    }

    private fun scheduleCursorHide() {
        binding.cursorView.removeCallbacks(hideCursorRunnable)
        binding.cursorView.postDelayed(hideCursorRunnable, cursorIdleMs)
    }

    /** "Clicks" at the pointer: an on-screen control if hit, else the WebView. */
    private fun clickAtCursor() {
        val controls = listOf(binding.menuButton, binding.cursorButton, binding.refreshButton)
        val bar = binding.controlBar
        for (btn in controls) {
            // Control buttons live inside controlBar, so add its offset.
            val left = bar.x + btn.x
            val top = bar.y + btn.y
            if (cursorX in left..(left + btn.width) && cursorY in top..(top + btn.height)) {
                btn.performClick()
                return
            }
        }
        // Otherwise synthesize a tap into the WebView at the pointer location.
        val downTime = SystemClock.uptimeMillis()
        val x = cursorX - webView.x
        val y = cursorY - webView.y
        MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0).also {
            webView.dispatchTouchEvent(it); it.recycle()
        }
        MotionEvent.obtain(downTime, downTime + 60, MotionEvent.ACTION_UP, x, y, 0).also {
            webView.dispatchTouchEvent(it); it.recycle()
        }
    }

    private fun toggleCursorMode() {
        cursorMode = !cursorMode
        applyCursorVisibility()
        Toast.makeText(
            this,
            if (cursorMode) R.string.cursor_on else R.string.cursor_off,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun applyCursorVisibility() {
        if (cursorMode && !chromeClient.isInFullScreen() &&
            binding.offlineLayout.visibility != View.VISIBLE
        ) {
            showCursor()   // briefly show, then auto-hide when idle
        } else {
            binding.cursorView.removeCallbacks(hideCursorRunnable)
            binding.cursorView.visibility = View.GONE
        }
    }

    private fun showSpinner(show: Boolean) {
        binding.loadingSpinner.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showOffline(show: Boolean) {
        binding.offlineLayout.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            showSpinner(false)
            binding.cursorView.visibility = View.GONE   // use normal focus here
            binding.retryButton.requestFocus()
        } else {
            applyCursorVisibility()
        }
    }

    // ---------------------------------------------------------------------
    // Connectivity
    // ---------------------------------------------------------------------

    private fun setupNetworkMonitor() {
        networkMonitor = NetworkMonitor(
            context = this,
            onAvailable = {
                // Connection came back: hide offline screen and reload if needed.
                if (binding.offlineLayout.visibility == View.VISIBLE || !hasLoadedOnce) {
                    showOffline(false)
                    loadUrlOrOffline(webView.url ?: AppConfig.START_URL)
                }
            },
            onLost = { /* Don't blank a running stream; offline screen shows on next error. */ }
        )
        networkMonitor.register()
    }

    private fun loadUrlOrOffline(url: String) {
        if (networkMonitor.isOnline()) {
            showOffline(false)
            webView.loadUrl(url)
        } else {
            showOffline(true)
        }
    }

    private fun rememberLastUrl(url: String) {
        // Don't remember error/blank URLs.
        if (url.startsWith("http")) {
            prefs.edit().putString(AppConfig.KEY_LAST_URL, url).apply()
        }
    }

    // ---------------------------------------------------------------------
    // Immersive mode
    // ---------------------------------------------------------------------

    private fun hideSystemUi() {
        WindowInsetsControllerCompat(window, binding.root).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    // ---------------------------------------------------------------------
    // Remote / D-pad handling
    // ---------------------------------------------------------------------

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            // MENU button opens the settings panel.
            KeyEvent.KEYCODE_MENU -> {
                showSettingsPanel()
                return true
            }
            // Dedicated remote refresh keys, where present.
            KeyEvent.KEYCODE_R -> {
                webView.reload()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * BACK button logic:
     *  1. If a video is full-screen -> exit full screen.
     *  2. Else if the WebView can go back -> go back.
     *  3. Else double-press to exit the app.
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            chromeClient.isInFullScreen() -> chromeClient.exitFullScreen()
            webView.canGoBack() -> webView.goBack()
            else -> handleDoubleBackExit()
        }
    }

    private fun handleDoubleBackExit() {
        val now = System.currentTimeMillis()
        if (now - backPressedTime < AppConfig.DOUBLE_BACK_EXIT_MS) {
            super.onBackPressed()
        } else {
            backPressedTime = now
            Toast.makeText(this, R.string.press_back_again, Toast.LENGTH_SHORT).show()
        }
    }

    // ---------------------------------------------------------------------
    // Settings panel
    // ---------------------------------------------------------------------

    private fun showSettingsPanel() {
        // Each entry pairs a visible label with the action to run when chosen.
        // External streaming apps (Tubi, Fox Sports) are listed first, built
        // from AppConfig.EXTERNAL_APPS so adding more is a one-line change.
        val entries = mutableListOf<Pair<String, () -> Unit>>()

        AppConfig.EXTERNAL_APPS.forEach { app ->
            entries += app.label to { openExternalApp(app) }
        }
        entries += getString(R.string.action_live_channels) to {
            startActivity(Intent(this, ChannelsActivity::class.java))
        }
        entries += getString(R.string.action_refresh) to { webView.reload() }
        entries += getString(R.string.action_go_home) to { webView.loadUrl(AppConfig.START_URL) }
        entries += getString(R.string.action_clear_cache) to {
            webView.clearCache(true)
            Toast.makeText(this, R.string.cache_cleared, Toast.LENGTH_SHORT).show()
        }
        entries += getString(R.string.action_exit) to { finish() }

        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_OpenTV_Dialog)
            .setTitle(R.string.settings_title)
            .setItems(entries.map { it.first }.toTypedArray()) { _, which ->
                entries[which].second.invoke()
            }
            .show()
    }

    /**
     * Hand off to an official streaming app. If it's installed we launch it
     * (it handles login/ads/DRM natively); otherwise we offer the Play Store,
     * and as a last resort load the service's website in our own WebView.
     */
    private fun openExternalApp(app: AppConfig.ExternalApp) {
        if (ExternalAppLauncher.launchApp(this, app.packageName)) return

        Toast.makeText(
            this,
            getString(R.string.app_not_installed, app.label),
            Toast.LENGTH_LONG
        ).show()

        if (ExternalAppLauncher.openUrlExternally(this, app.playStoreUrl)) return
        // No Play Store handler (rare): fall back to the site in the WebView.
        webView.loadUrl(app.webUrl)
    }

    // ---------------------------------------------------------------------
    // Lifecycle: keep WebView/video state consistent
    // ---------------------------------------------------------------------

    override fun onPause() {
        super.onPause()
        webView.onPause()
        webView.pauseTimers()
    }

    override fun onResume() {
        super.onResume()
        webView.resumeTimers()
        webView.onResume()
        hideSystemUi()
    }

    override fun onDestroy() {
        networkMonitor.unregister()
        // Detach and destroy the WebView to avoid leaks.
        (webView.parent as? android.view.ViewGroup)?.removeView(webView)
        webView.destroy()
        super.onDestroy()
    }
}
