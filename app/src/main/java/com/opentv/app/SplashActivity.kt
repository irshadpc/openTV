package com.opentv.app

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

/**
 * Branded, animated splash. The logo springs in, the title/tagline fade up, and
 * a 3-dot loader pulses while the app prepares — then hands off to [MainActivity].
 */
class SplashActivity : AppCompatActivity() {

    private val splashDurationMs = 1700L

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        animateIn()

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, splashDurationMs)
    }

    private fun animateIn() {
        val logo = findViewById<View>(R.id.splashLogo)
        val title = findViewById<View>(R.id.splashTitle)
        val tagline = findViewById<View>(R.id.splashTagline)
        val loader = findViewById<View>(R.id.splashLoader)

        // Logo: fade + spring-scale in, then a gentle continuous pulse.
        logo.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(560).setInterpolator(OvershootInterpolator(2.2f))
            .withEndAction {
                ObjectAnimator.ofFloat(logo, View.SCALE_X, 1f, 1.05f, 1f).apply {
                    duration = 1600; repeatCount = ObjectAnimator.INFINITE; start()
                }
                ObjectAnimator.ofFloat(logo, View.SCALE_Y, 1f, 1.05f, 1f).apply {
                    duration = 1600; repeatCount = ObjectAnimator.INFINITE; start()
                }
            }
            .start()

        // Title + tagline: fade up with a slight stagger.
        fadeUp(title, startOffset = 280)
        fadeUp(tagline, startOffset = 420)

        // Loader: fade in, then animate the three dots.
        loader.animate().alpha(1f).setStartDelay(560).setDuration(300).start()
        startDotAnimation()
    }

    private fun fadeUp(view: View, startOffset: Long) {
        view.translationY = 24f
        view.animate()
            .alpha(1f).translationY(0f)
            .setStartDelay(startOffset).setDuration(420)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun startDotAnimation() {
        val dots = listOf(
            findViewById<View>(R.id.dot1),
            findViewById<View>(R.id.dot2),
            findViewById<View>(R.id.dot3)
        )
        dots.forEachIndexed { i, dot ->
            ObjectAnimator.ofFloat(dot, View.TRANSLATION_Y, 0f, -16f, 0f).apply {
                duration = 720
                startDelay = 560L + i * 140L
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
            ObjectAnimator.ofFloat(dot, View.ALPHA, 0.4f, 1f, 0.4f).apply {
                duration = 720
                startDelay = 560L + i * 140L
                repeatCount = ObjectAnimator.INFINITE
                start()
            }
        }
    }
}
