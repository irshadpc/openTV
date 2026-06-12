package com.opentv.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Launches other installed TV apps (e.g. the official Tubi / Fox Sports apps)
 * by package name. We never restream third-party content — we hand off to the
 * service's own app, which handles login, ads and DRM natively.
 *
 * Note: on Android 11+ the target packages must be declared in a <queries>
 * element in AndroidManifest.xml for [launchApp] to see them.
 */
object ExternalAppLauncher {

    /** @return true if the app was found and launched, false otherwise. */
    fun launchApp(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent); true }.getOrDefault(false)
    }

    /** Opens a URL (Play Store listing or website) in an external handler. */
    fun openUrlExternally(context: Context, url: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent); true }.getOrDefault(false)
    }
}
