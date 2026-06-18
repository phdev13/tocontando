package com.phdev.quantofalta.core.ota

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.io.File

/**
 * Validates a downloaded APK before offering installation.
 * Checks package name to prevent installing wrong apps.
 */
class ApkValidator(private val context: Context) {

    companion object {
        private const val TAG = "ApkValidator"
    }

    /** Returns true if the APK is valid and matches the expected package */
    fun validate(apkFile: File): Boolean {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            Log.e(TAG, "APK file does not exist or is empty")
            return false
        }

        // Verify it's a valid APK and extract package info
        return try {
            val packageInfo = context.packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.GET_ACTIVITIES
            )
            if (packageInfo == null) {
                Log.e(TAG, "Could not read APK package info")
                return false
            }

            // Must match our own package name — prevents installing wrong APKs
            val expectedPackage = context.packageName
            if (packageInfo.packageName != expectedPackage) {
                Log.e(TAG, "Package name mismatch: expected $expectedPackage, got ${packageInfo.packageName}")
                return false
            }

            Log.i(TAG, "APK validated: ${packageInfo.packageName} versionCode=${packageInfo.longVersionCode}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "APK validation failed: ${e.message}")
            false
        }
    }
}
