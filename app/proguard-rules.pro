# Add project specific ProGuard rules here.

# ─── SQLCipher / Zetetic ─────────────────────────────────────────────────────
-keep class net.zetetic.** { *; }
-keep class net.sqlcipher.** { *; }
-dontwarn net.zetetic.**
-dontwarn net.sqlcipher.**

# ─── Room ────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.**

# ─── Hilt ────────────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel
-dontwarn dagger.hilt.**

# ─── Tink ─────────────────────────────────────────────────────────────────────
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# ─── Kotlin Coroutines ────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ─── Remove log calls in release ─────────────────────────────────────────────
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

# ─── Debugger detection ───────────────────────────────────────────────────────
-keepclassmembers class com.buglist.security.** { *; }

# ─── Argon2Kt ─────────────────────────────────────────────────────────────────
-keep class com.lambdapioneer.argon2kt.** { *; }
-dontwarn com.lambdapioneer.argon2kt.**

# ─── Vico Charts ──────────────────────────────────────────────────────────────
-keep class com.patrykandpatrick.vico.** { *; }
-dontwarn com.patrykandpatrick.vico.**

# ─── Google Fonts ─────────────────────────────────────────────────────────────
-dontwarn androidx.compose.ui.text.googlefonts.**

# ─── Keep application class ───────────────────────────────────────────────────
-keep class com.buglist.BugListApplication { *; }

# ─── SplashScreen / slf4j transitive dep ─────────────────────────────────────
# core-splashscreen pulls in slf4j-api as a transitive dependency, but no
# slf4j implementation is present (nor needed — it's only used by tooling code
# that R8 eliminates). Suppress the missing-class warning to unblock R8.
-dontwarn org.slf4j.**

# ─── Ktor (Auto-Update) ───────────────────────────────────────────────────────
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ─── kotlinx.serialization (GitHub Release Model) ────────────────────────────
-keep class kotlinx.serialization.** { *; }
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep class com.buglist.data.remote.GithubRelease { *; }
-keep class com.buglist.data.remote.GithubAsset { *; }
