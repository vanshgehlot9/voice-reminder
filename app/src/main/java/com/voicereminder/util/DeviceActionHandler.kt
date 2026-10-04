package com.voicereminder.util

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log

object DeviceActionHandler {
    private const val TAG = "DeviceActionHandler"

    /**
     * Open an application by name (e.g. "google", "youtube", "whatsapp", "camera", "spotify", etc.)
     */
    fun openApp(context: Context, appName: String): Boolean {
        val target = appName.lowercase().trim()
        Log.i(TAG, "Attempting to open app: '$target'")

        try {
            // 1. Specific Well-Known App Handlers
            when {
                target.contains("camera") -> {
                    val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                        return true
                    }
                }
                target.contains("youtube") -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        setPackage("com.google.android.youtube")
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                        return true
                    }
                }
                target.contains("whatsapp") -> {
                    val intent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                    if (intent != null) {
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                        return true
                    }
                }
                target.contains("spotify") -> {
                    val intent = context.packageManager.getLaunchIntentForPackage("com.spotify.music")
                    if (intent != null) {
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                        return true
                    }
                }
                target.contains("setting") -> {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    return true
                }
                target.contains("calculator") -> {
                    val calculatorPackages = listOf(
                        "com.google.android.calculator",
                        "com.android.calculator2",
                        "com.sec.android.app.popupcalculator",
                        "com.miui.calculator"
                    )
                    for (pkg in calculatorPackages) {
                        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                        if (intent != null) {
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                            return true
                        }
                    }
                }
                target.contains("google") || target.contains("chrome") || target.contains("browser") -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    return true
                }
                target.contains("map") -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    return true
                }
                target.contains("instagram") -> {
                    val intent = context.packageManager.getLaunchIntentForPackage("com.instagram.android")
                    if (intent != null) {
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                        return true
                    }
                }
            }

            // 2. Dynamic Search through installed apps
            val pm = context.packageManager
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in installedApps) {
                val label = pm.getApplicationLabel(app).toString().lowercase()
                if (label == target || label.contains(target) || target.contains(label)) {
                    val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                    if (launchIntent != null) {
                        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(launchIntent)
                        return true
                    }
                }
            }

            // 3. Fallback: Search on Google Play Store or Google Web
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(target))).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Error opening app: ${e.message}", e)
            return false
        }
    }

    /**
     * Make a phone call or open the dialer with a contact/number
     */
    fun makeCall(context: Context, target: String): Boolean {
        try {
            val cleanTarget = target.trim()
            val uri = if (cleanTarget.matches(Regex("^[0-9+()\\-\\s]+$"))) {
                Uri.parse("tel:${cleanTarget.replace(Regex("[^0-9+]"), "")}")
            } else {
                // If it's a name, open dialer (user can select contact or press call)
                Uri.parse("tel:")
            }
            val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error making call: ${e.message}", e)
            return false
        }
    }

    /**
     * Play music or a specific song on YouTube / default player
     */
    fun playMusic(context: Context, query: String): Boolean {
        try {
            val q = query.ifBlank { "music" }
            val youtubeUrl = "https://www.youtube.com/results?search_query=" + Uri.encode(q)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error playing music: ${e.message}", e)
            return false
        }
    }

    /**
     * Search Google for a query
     */
    fun searchWeb(context: Context, query: String): Boolean {
        try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error searching web: ${e.message}", e)
            return false
        }
    }
}
