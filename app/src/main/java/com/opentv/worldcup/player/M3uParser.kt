package com.opentv.worldcup.player

/**
 * Minimal, dependency-free parser for extended M3U (`#EXTM3U`) playlists.
 *
 * Supported format:
 * ```
 * #EXTM3U
 * #EXTINF:-1 tvg-logo="https://.../logo.png" group-title="Sports",Channel Name
 * https://example.com/stream/master.m3u8
 * ```
 *
 * Lines starting with `#` (other than `#EXTINF`) are ignored, as are blanks.
 * Any non-comment line is treated as the URL for the most recent `#EXTINF`.
 */
object M3uParser {

    fun parse(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()

        var pendingName: String? = null
        var pendingLogo: String? = null
        var pendingGroup: String? = null

        content.lineSequence().forEach { raw ->
            val line = raw.trim()
            when {
                line.isEmpty() -> { /* skip */ }

                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    pendingName = extractDisplayName(line)
                    pendingLogo = extractAttribute(line, "tvg-logo")
                    pendingGroup = extractAttribute(line, "group-title")
                }

                // Other directives (#EXTM3U, #EXTGRP, comments) are ignored.
                line.startsWith("#") -> { /* skip */ }

                // A bare line is the stream URL for the last #EXTINF.
                else -> {
                    val name = pendingName ?: deriveNameFromUrl(line)
                    channels.add(
                        Channel(
                            name = name,
                            url = line,
                            logo = pendingLogo,
                            group = pendingGroup
                        )
                    )
                    pendingName = null
                    pendingLogo = null
                    pendingGroup = null
                }
            }
        }
        return channels
    }

    /** Text after the last comma on an #EXTINF line is the channel name. */
    private fun extractDisplayName(extinf: String): String {
        val commaIndex = extinf.lastIndexOf(',')
        return if (commaIndex in 0 until extinf.length - 1) {
            extinf.substring(commaIndex + 1).trim()
        } else {
            "Unnamed"
        }
    }

    /** Pulls a `key="value"` attribute out of an #EXTINF line. */
    private fun extractAttribute(extinf: String, key: String): String? {
        val pattern = "$key=\"([^\"]*)\"".toRegex(RegexOption.IGNORE_CASE)
        return pattern.find(extinf)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    private fun deriveNameFromUrl(url: String): String =
        url.substringAfterLast('/').ifBlank { url }
}
