package com.web.test.data.cloak

import android.os.Build
import java.io.File
import java.io.IOException

class RootCheckDataSource {
    fun isRooted(): Boolean {
        return checkBuildTags() || checkRootBinaries() || canExecuteSu()
    }

    private fun checkBuildTags(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkRootBinaries(): Boolean {
        val paths = listOf(
            "/system/app/Superuser.apk",
            "/system/bin/su",
            "/system/xbin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/system/su",
            "/system/bin/.ext/su",
        )
        return paths.any { File(it).exists() }
    }

    private fun canExecuteSu(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val result = process.inputStream.bufferedReader().readText()
            result.isNotEmpty()
        } catch (io: IOException) {
            false
        }
    }
}
