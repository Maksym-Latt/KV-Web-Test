package com.web.test.data.cloak.datasource

import android.os.Build
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

class RootCheckDataSource @Inject constructor() {
    fun isRooted(): Boolean {
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) return true

        val paths = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        if (paths.any { File(it).exists() }) return true

        return canExecuteSu()
    }

    private fun canExecuteSu(): Boolean = try {
        val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            reader.readLine() != null
        }
    } catch (_: Exception) {
        false
    }
}
