package com.voicereminder.network

import android.content.Context
import android.content.SharedPreferences
import com.voicereminder.BuildConfig

/**
 * Manages JWT token + server base URL persistence across app restarts.
 * Uses SharedPreferences so no token is ever stored in memory only.
 */
object AuthManager {
    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_BASE_URL = "base_url"
    val DEFAULT_BASE_URL: String = BuildConfig.BASE_URL

    private var prefs: SharedPreferences? = null

    var accessToken: String?
        get() = prefs?.getString(KEY_ACCESS_TOKEN, "lightweight_dummy_token")
        set(value) {
            prefs?.edit()?.putString(KEY_ACCESS_TOKEN, value)?.apply()
        }

    /** The server URL — settable at runtime so no rebuild is needed. */
    var baseUrl: String
        get() {
            val saved = prefs?.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
            // If old stale URLs or temporary tunnels are stuck in prefs, reset to live Render default URL
            if (saved.isBlank() || saved.contains("ngrok") || saved.contains("192.168.") || saved.contains("YOUR_SERVER_IP") || !saved.startsWith("http")) {
                prefs?.edit()?.putString(KEY_BASE_URL, DEFAULT_BASE_URL)?.apply()
                return DEFAULT_BASE_URL
            }
            return saved
        }
        set(value) {
            val normalised = if (value.endsWith("/")) value else "$value/"
            prefs?.edit()?.putString(KEY_BASE_URL, normalised)?.apply()
            // Reset the Retrofit client so the new URL takes effect immediately
            VoiceboxApiClient.reset()
        }

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Clean out any stale ngrok or local IPs immediately on launch
        val saved = prefs?.getString(KEY_BASE_URL, null)
        if (saved == null || saved.contains("ngrok") || saved.contains("192.168.") || saved.contains("YOUR_SERVER_IP")) {
            prefs?.edit()?.putString(KEY_BASE_URL, DEFAULT_BASE_URL)?.apply()
        }
        // Auto-provision token so first-time users go directly to Home Dashboard without seeing a blocking login screen
        if (prefs?.getString(KEY_ACCESS_TOKEN, null) == null) {
            prefs?.edit()?.putString(KEY_ACCESS_TOKEN, "lightweight_dummy_token")?.apply()
        }
    }

    fun clear() {
        prefs?.edit()
            ?.remove(KEY_ACCESS_TOKEN)
            ?.apply()
    }
}
