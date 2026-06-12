# Keep JavaScript interface methods (none used currently, but safe for future).
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep WebView client/chrome client classes intact.
-keep class com.opentv.worldcup.web.** { *; }

# Standard Kotlin metadata.
-keep class kotlin.Metadata { *; }
