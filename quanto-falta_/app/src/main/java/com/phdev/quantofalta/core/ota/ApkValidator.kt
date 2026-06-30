package com.phdev.quantofalta.core.ota

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.security.MessageDigest

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
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_ACTIVITIES or PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_ACTIVITIES or PackageManager.GET_SIGNATURES
            }
            val packageInfo = context.packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                flags
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

            val installedInfo = context.packageManager.getPackageInfo(expectedPackage, flags)
            val archiveSignatures = signatureDigests(packageInfo)
            val installedSignatures = signatureDigests(installedInfo)
            if (archiveSignatures.isEmpty() || !archiveSignatures.all { it in installedSignatures }) {
                Log.e(TAG, "APK signing certificate mismatch")
                return false
            }

            if (PackageInfoCompat.getLongVersionCode(packageInfo) <= PackageInfoCompat.getLongVersionCode(installedInfo)) {
                Log.e(TAG, "APK version is not newer than the installed version")
                return false
            }

            Log.i(TAG, "APK validated: ${packageInfo.packageName} versionCode=${PackageInfoCompat.getLongVersionCode(packageInfo)}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "APK validation failed: ${e.message}")
            false
        }
    }

    private fun signatureDigests(packageInfo: android.content.pm.PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo ?: return emptySet()
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures ?: emptyArray()
        }

        return signatures.map { signature ->
            MessageDigest.getInstance("SHA-256")
                .digest(signature.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }.toSet()
    }
}
