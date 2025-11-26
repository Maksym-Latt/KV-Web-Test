package com.web.test.presentation.webview

import android.content.Context
import android.webkit.JavascriptInterface

internal class WebAppBridge(
    private val context: Context,
    private val onTokenSaved: (String) -> Unit
) {
    @JavascriptInterface
    fun savePushToken(token: String) {
        context.getSharedPreferences("web_bridge", Context.MODE_PRIVATE)
            .edit()
            .putString("push_token", token)
            .apply()
        onTokenSaved(token)
    }

    @JavascriptInterface
    fun requestStoredToken(): String? =
        context.getSharedPreferences("web_bridge", Context.MODE_PRIVATE)
            .getString("push_token", null)
}

internal fun loadStoredPushToken(context: Context): String? =
    context.getSharedPreferences("web_bridge", Context.MODE_PRIVATE)
        .getString("push_token", null)
