# Add project specific ProGuard rules here.
-keep class com.dahua.nmea.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
