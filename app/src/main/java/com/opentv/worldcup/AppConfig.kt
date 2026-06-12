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

    val PLAYLIST_SOURCES: List<PlaylistSource> = listOf(
        PlaylistSource("Sports — Global", "https://iptv-org.github.io/iptv/categories/sports.m3u"),
        PlaylistSource("United States", "https://iptv-org.github.io/iptv/countries/us.m3u"),
        PlaylistSource("Canada", "https://iptv-org.github.io/iptv/countries/ca.m3u"),
        PlaylistSource("Mexico", "https://iptv-org.github.io/iptv/countries/mx.m3u"),
        PlaylistSource("United Kingdom", "https://iptv-org.github.io/iptv/countries/uk.m3u"),
        PlaylistSource("Bundled (offline)", "")
    )

    // -----------------------------------------------------------------------
    // External streaming apps (official, licensed services).
    //
    // The app does NOT restream their content. It hands off to the installed
    // official app (which handles login/ads/DRM natively), and only falls back
    // to the Play Store / website if that app isn't installed.
    //
    //   - Tubi: free, ad-supported, Fox-owned; carries Fox's free World Cup
    //           coverage in the US (no login required).
    //   - Fox Sports: official US World Cup rights-holder (TV-provider login).
    //
    // Verify a package on your device with:
    //   adb shell pm list packages | grep -iE "tubi|fox"
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
            label = "Tubi",
            packageName = "com.tubitv",
            webUrl = "https://tubitv.com",
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.tubitv"
        ),
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
