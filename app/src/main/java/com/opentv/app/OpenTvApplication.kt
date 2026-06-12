package com.opentv.app

import android.app.Application
import android.webkit.CookieManager

/**
 * Application class. Used to enable persistent cookies app-wide before any
 * WebView is created, so login/session state survives across launches.
 */
class OpenTvApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Accept and persist cookies (needed for sessions / local storage flows).
        CookieManager.getInstance().setAcceptCookie(true)
    }
}
