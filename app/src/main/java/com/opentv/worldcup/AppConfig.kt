package com.opentv.worldcup

/**
 * Single source of truth for app-wide configuration.
 *
 * To point the app at a different site in the future, change [START_URL] and
 * (optionally) [ALLOWED_HOSTS] — nothing else needs to be touched.
 */
object AppConfig {

    /** The page the app loads on startup. Change this to retarget the app. */
    const val START_URL: String = "https://yinkyade.github.io/open_tv_site/world-cup"

    /**
     * Hosts the WebView is allowed to navigate to directly. Any navigation to a
     * host NOT in this list is blocked (prevents unwanted external redirects).
     *
     * Note: matching is suffix-based, so "github.io" also matches
     * "yinkyade.github.io". Add streaming CDN domains here if a provider's
     * embedded player navigates the top frame to another host.
     */
    val ALLOWED_HOSTS: List<String> = listOf(
        "github.io",
        "githubusercontent.com"
    )

    /**
     * If true, sub-resources (iframes, video segments, scripts) from ANY host
     * are allowed to load — only top-level page navigation is restricted to
     * [ALLOWED_HOSTS]. Streaming sites almost always need this because the
     * actual video/HLS comes from third-party CDNs.
     */
    const val ALLOW_THIRD_PARTY_SUBRESOURCES: Boolean = true

    /** Milliseconds within which a second BACK press exits the app. */
    const val DOUBLE_BACK_EXIT_MS: Long = 2000L

    // --- Keys for persisted preferences. ---
    const val PREFS_NAME: String = "open_tv_prefs"
    const val KEY_LAST_URL: String = "last_visited_url"
}
