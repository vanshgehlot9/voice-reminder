package com.voicereminder.network

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages JWT token + server base URL persistence across app restarts.
 * Uses SharedPreferences so no token is ever stored in memory only.
 */
import com.voicereminder.BuildConfig

object AuthManager {
    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_BASE_URL = "base_url"
    private val DEFAULT_BASE_URL = BuildConfig.BASE_URL

    private var prefs: SharedPreferences? = null

    var accessToken: String?
        get() = prefs?.getString(KEY_ACCESS_TOKEN, null)
        set(value) {
            prefs?.edit()?.putString(KEY_ACCESS_TOKEN, value)?.apply()
        }

    /** The server URL — settable at runtime so no rebuild is needed. */
    var baseUrl: String
        get() {
            val saved = prefs?.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
            // If the old stale local IP is stuck in prefs, use the live default URL
            if (saved.contains("192.168.31.197")) {
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
    }

    fun clear() {
        prefs?.edit()
            ?.remove(KEY_ACCESS_TOKEN)
            ?.apply()
    }
}
