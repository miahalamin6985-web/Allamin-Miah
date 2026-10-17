package com.example.engine

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings

class AppLauncher(private val context: Context) {

    // Common app mapping
    private val appMap = mapOf(
        "youtube" to "com.google.android.youtube",
        "whatsapp" to "com.whatsapp",
        "settings" to "com.android.settings",
        "maps" to "com.google.android.apps.maps",
        "google maps" to "com.google.android.apps.maps",
        "spotify" to "com.spotify.music",
        "clock" to "com.google.android.deskclock",
        "calculator" to "com.google.android.calculator",
        "chrome" to "com.android.chrome",
        "gmail" to "com.google.android.gm",
        "camera" to "camera_intent"
    )

    fun launchAppByName(name: String): AppLaunchResult {
        val cleanName = name.trim().lowercase()

        if (cleanName == "settings") {
            return try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                AppLaunchResult.Success("Opening Settings")
            } catch (e: Exception) {
                AppLaunchResult.Failure("Could not open Settings: ${e.message}")
            }
        }

        if (cleanName == "camera") {
            return try {
                val intent = Intent("android.media.action.IMAGE_CAPTURE").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                AppLaunchResult.Success("Opening Camera")
            } catch (e: Exception) {
                AppLaunchResult.Failure("Could not open Camera: ${e.message}")
            }
        }

        val targetPackage = appMap[cleanName]
        if (targetPackage != null) {
            val intent = context.packageManager.getLaunchIntentForPackage(targetPackage)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return AppLaunchResult.Success("Opening ${cleanName.replaceFirstChar { it.uppercase() }}")
            }
        }

        // Search installed apps by label
        try {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (pkg in packages) {
                val label = pm.getApplicationLabel(pkg).toString().lowercase()
                if (label == cleanName || label.contains(cleanName)) {
                    val intent = pm.getLaunchIntentForPackage(pkg.packageName)
                    if (intent != null) {
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                        return AppLaunchResult.Success("Opening ${pm.getApplicationLabel(pkg)}")
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback for YouTube / Web search if app not installed
        if (cleanName == "youtube") {
            return try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                AppLaunchResult.Success("Opening YouTube in browser")
            } catch (e: Exception) {
                AppLaunchResult.Failure("Could not open YouTube")
            }
        }

        return AppLaunchResult.NotFound(
            message = "App '$name' is not installed on this device.",
            queryName = name
        )
    }

    fun openPlayStore(query: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$query")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=$query")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    }
}

sealed class AppLaunchResult {
    data class Success(val message: String) : AppLaunchResult()
    data class Failure(val error: String) : AppLaunchResult()
    data class NotFound(val message: String, val queryName: String) : AppLaunchResult()
}
