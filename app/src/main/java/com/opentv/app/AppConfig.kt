package com.opentv.app

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
        "githubusercontent.com",
        // Allowed so the WebView fallback can open these official sites.
        "tubitv.com",
        "foxsports.com"
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

    // -----------------------------------------------------------------------
    // Live Channels (M3U playlist) configuration.
    //
    // The native Media3 player plays direct HLS (.m3u8) streams listed in an
    // M3U playlist. Populate the playlist with sources YOU are licensed to use
    // (your own paid IPTV subscription's M3U, official/free streams, etc.).
    // -----------------------------------------------------------------------

    /**
     * Optional remote M3U playlist URL. If left blank, the app loads the bundled
     * asset playlist named [PLAYLIST_ASSET] from app/src/main/assets instead.
     *
     * Example: "https://my-provider.example/playlist.m3u"
     */
    const val PLAYLIST_URL: String = ""

    /** Bundled fallback playlist in app/src/main/assets/. */
    const val PLAYLIST_ASSET: String = "playlist.m3u"

    /**
     * Selectable M3U sources shown in the Live Channels screen.
     *
     * These point at the open-source **iptv-org/iptv** project
     * (https://github.com/iptv-org/iptv), which catalogs **publicly available,
     * free** streams — it hosts no content itself. For the World Cup the most
     * relevant lists are the global Sports category plus the three 2026 host
     * countries (USA, Canada, Mexico), where free-to-air channels often carry
     * matches. Availability/quality varies and some links may break over time.
     *
     * An empty [url] means "load the bundled [PLAYLIST_ASSET]" (offline).
     */
    data class PlaylistSource(val name: String, val url: String)

    /**
     * Keywords used by the Live Channels "World Cup" filter to trim a large
     * iptv-org list down to sports / known 2026-broadcaster channels. A channel
     * matches if its name or group contains any of these (case-insensitive).
     * Covers generic sports terms plus major US/CA/MX/UK rights-holders.
     */
    val RELEVANT_KEYWORDS: List<String> = listOf(
        "sport", "fifa", "world cup", "mundial", "football", "soccer", "futbol",
        "fútbol", "deportes", "fox", "espn", "fs1", "fs2", "fubo", "tnt", "bein",
        "dazn", "telemundo", "univision", "tudn", "azteca", "tsn", "rds", "cbc",
        "bbc", "itv", "channel 4", "sbs", "optus"
    )

    val PLAYLIST_SOURCES: List<PlaylistSource> = listOf(
        PlaylistSource("Sports — Global", "https://iptv-org.github.io/iptv/categories/sports.m3u"),
        PlaylistSource("United States", "https://iptv-org.github.io/iptv/countries/us.m3u"),
        PlaylistSource("Canada", "https://iptv-org.github.io/iptv/countries/ca.m3u"),
        PlaylistSource("Mexico", "https://iptv-org.github.io/iptv/countries/mx.m3u"),
        PlaylistSource("United Kingdom", "https://iptv-org.github.io/iptv/countries/uk.m3u"),
        PlaylistSource("Bundled (offline)", "")
    )

    // -----------------------------------------------------------------------
    // Web shortcuts — open an official site INSIDE the app's WebView (so the
    // user can sign in; cookies/DOM storage persist the session across launches).
    //
    //   - Tubi: free, ad-supported, Fox-owned service. Opens its login page so
    //           the user can sign in and watch within the app.
    // -----------------------------------------------------------------------
    data class WebShortcut(val label: String, val url: String)

    val WEB_SHORTCUTS: List<WebShortcut> = listOf(
        WebShortcut("Tubi (Login)", "https://tubitv.com/login")
    )

    // -----------------------------------------------------------------------
    // External streaming apps (official, licensed services).
    //
    // The app does NOT restream their content. It hands off to the installed
    // official app (which handles login/ads/DRM natively), and only falls back
    // to the Play Store / website if that app isn't installed.
    //
    //   - Fox Sports: official US World Cup rights-holder (TV-provider login).
    //
    // Verify a package on your device with:
    //   adb shell pm list packages | grep -i fox
    // and update [packageName] if it differs.
    // -----------------------------------------------------------------------
    data class ExternalApp(
        val label: String,
        val packageName: String,
        val webUrl: String,
        val playStoreUrl: String
    )

    val EXTERNAL_APPS: List<ExternalApp> = listOf(
        ExternalApp(
            label = "Fox Sports",
            packageName = "com.foxsports.videogo",
            webUrl = "https://www.foxsports.com/soccer/fifa-world-cup",
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.foxsports.videogo"
        )
    )

    // --- Keys for persisted preferences. ---
    const val PREFS_NAME: String = "open_tv_prefs"
    const val KEY_LAST_URL: String = "last_visited_url"
}
