package com.opentv.worldcup

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

/**
 * Lightweight launcher / splash activity.
 *
 * Uses the AndroidX SplashScreen API (with a themed fallback for Android 9–11)
 * to show the app logo immediately on cold start, then hands off to
 * [MainActivity]. Kept deliberately minimal for fast startup.
 */
class SplashActivity : AppCompatActivity() {

    // How long to display the branded splash before launching the WebView.
    private val splashDurationMs = 1200L

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate / setContentView.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            // No transition animation -> feels instant on TV.
            overridePendingTransition(0, 0)
            finish()
        }, splashDurationMs)
    }
}
