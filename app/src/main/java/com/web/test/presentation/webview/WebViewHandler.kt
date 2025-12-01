package com.web.test.presentation.webview

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

// ==================== WebView handler ====================

class WebViewHandler(
    private val context: Context
) {

    fun createWebViewViaReflection(): WebView {
        return try {
            val webViewClass = Class.forName("android.webkit.WebView")
            val ctor = webViewClass.getConstructor(Context::class.java)
            ctor.newInstance(context) as WebView
        } catch (_: Throwable) {
            WebView(context)
        }
    }

    fun configureWebViewViaReflection(webView: WebView) {
        val settings = webView.settings
        val settingsClass = settings.javaClass

        try {
            val jsMethod = settingsClass.getMethod(
                "setJavaScriptEnabled",
                Boolean::class.javaPrimitiveType
            )
            jsMethod.invoke(settings, true)
        } catch (_: Throwable) {
        }

        try {
            val domMethod = settingsClass.getMethod(
                "setDomStorageEnabled",
                Boolean::class.javaPrimitiveType
            )
            domMethod.invoke(settings, true)
        } catch (_: Throwable) {
        }

        try {
            val uaMethod = settingsClass.getMethod(
                "setUserAgentString",
                String::class.java
            )
            val baseUa = WebSettings.getDefaultUserAgent(context)
            uaMethod.invoke(settings, baseUa.replace("wv", "") + " KVWebTest/1.0")
        } catch (_: Throwable) {
        }
    }

    fun loadUrlViaReflection(webView: WebView, url: String) {
        try {
            CookieManager.getInstance().flush()

            val method = WebView::class.java.getMethod("loadUrl", String::class.java)
            method.invoke(webView, url)
        } catch (_: Throwable) {
            webView.loadUrl(url)
        }
    }
}