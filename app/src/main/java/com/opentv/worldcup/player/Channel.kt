package com.opentv.worldcup.player

/**
 * A single playable entry parsed from an M3U playlist.
 *
 * @param name  display name (from the text after the #EXTINF comma).
 * @param url   the stream URL (HLS .m3u8, or any URL ExoPlayer can play).
 * @param logo  optional logo URL (tvg-logo); not loaded by default to keep the
 *              app dependency-free, but preserved for future use.
 * @param group optional category (group-title), e.g. "Sports".
 */
data class Channel(
    val name: String,
    val url: String,
    val logo: String? = null,
    val group: String? = null
)
