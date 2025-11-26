package com.web.test.presentation.webview

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.os.Message
import android.util.Log
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import org.json.JSONObject
// ==================== WebView Factory ====================

fun createConfiguredWebView(
    context: Context,
    userAgent: String,
    popupContainer: FrameLayout,
    onPopupCreated: (WebView) -> Unit,
    onPopupClosed: (WebView) -> Unit,
    onFilePicker: (Intent, ValueCallback<Array<Uri>>, Uri?) -> Unit,
    pushTokenState: MutableState<String?>
): WebView {

    // ---------- WebView via reflection ----------
    val handler = WebViewHandler(context)
    val webView = handler.createWebViewViaReflection()
    handler.configureWebViewViaReflection(webView)

    // ---------- Cookies ----------
    CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(webView, true)
    }

    // ---------- Settings ----------
    with(webView.settings) {
        javaScriptEnabled = true
        javaScriptCanOpenWindowsAutomatically = true
        domStorageEnabled = true
        databaseEnabled = true
        cacheMode = WebSettings.LOAD_DEFAULT
        setSupportZoom(true)
        builtInZoomControls = false
        displayZoomControls = false
        setSupportMultipleWindows(true)
        allowFileAccess = true
        allowContentAccess = true
        mediaPlaybackRequiresUserGesture = false
        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        loadWithOverviewMode = true
        useWideViewPort = true
        userAgentString = userAgent
    }

    // ---------- JS Bridge ----------
    webView.addJavascriptInterface(
        WebAppBridge(context) { token ->
            pushTokenState.value = token
        },
        "AndroidBridge"
    )

    // ==================== WebViewClient ====================
    webView.webViewClient = object : WebViewClient() {

        private var isPopupAttached = false

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val targetUri = request?.url ?: return false
            return handleUrlOverride(view, context, targetUri)
        }

        @Suppress("OverridingDeprecatedMember")
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            url: String?
        ): Boolean {
            val targetUri = url?.let { Uri.parse(it) } ?: return false
            return handleUrlOverride(view, context, targetUri)
        }

        // =====================================================
        // =========         MAIN URL HANDLER          =========
        // =====================================================

        private fun handleUrlOverride(
            view: WebView?,
            context: Context,
            targetUri: Uri
        ): Boolean {

            val scheme = targetUri.scheme?.lowercase().orEmpty()
            val host = targetUri.host?.lowercase().orEmpty()
            val url = targetUri.toString()
            val normalizedUrl = url.lowercase()

            // ---------- Identify popup instance ----------
            val isPopup = view != null && view.parent === popupContainer

            // ---------- CRITICAL FIX (popups must load http/https normally) ----------
            if (isPopup && (scheme == "http" || scheme == "https")) {
                return false
            }

            // ---------- External apps ----------
            val externalSchemes = setOf(
                "viber", "tg", "whatsapp", "line", "wechat",
                "kakaotalk", "skype", "facebook", "fb",
                "instagram", "twitter", "x", "tweetie"
            )

            fun killPopupAndOpen(intent: Intent): Boolean {
                if (isPopup && view != null) {
                    popupContainer.removeView(view)
                    onPopupClosed(view)
                    view.destroy()
                }
                return handleExternalIntent(context, intent)
            }

            // ---------- tel: ----------
            if (scheme == "tel") {
                return killPopupAndOpen(Intent(Intent.ACTION_DIAL, targetUri))
            }

            // ---------- sms ----------
            if (scheme == "sms" || scheme == "smsto") {
                return killPopupAndOpen(Intent(Intent.ACTION_SENDTO, targetUri))
            }

            // ---------- mailto ----------
            if (scheme == "mailto") {
                return killPopupAndOpen(Intent(Intent.ACTION_SENDTO, targetUri))
            }

            // ---------- Google Pay ----------
            if (scheme == "googlepay" ||
                host.contains("pay.google.com") ||
                normalizedUrl.contains("gpay")
            ) {
                return killPopupAndOpen(Intent(Intent.ACTION_VIEW, targetUri))
            }

            // ---------- Samsung Pay ----------
            if (scheme == "samsungpay" ||
                normalizedUrl.contains("samsungpay")
            ) {
                return killPopupAndOpen(Intent(Intent.ACTION_VIEW, targetUri))
            }

            // ---------- Intent:// ----------
            if (scheme == "intent") {
                return try {
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    if (killPopupAndOpen(intent)) true
                    else {
                        intent.getStringExtra("browser_fallback_url")?.let {
                            killPopupAndOpen(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                        } ?: false
                    }
                } catch (_: Exception) {
                    false
                }
            }

            // ---------- Direct external schemes ----------
            if (scheme in externalSchemes) {
                return killPopupAndOpen(
                    Intent(Intent.ACTION_VIEW, targetUri)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }

            // ---------- Any non-http scheme ----------
            if (scheme.isNotBlank() && scheme !in listOf("http", "https")) {
                return killPopupAndOpen(
                    Intent(Intent.ACTION_VIEW, targetUri)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }

            // ---------- http/https special hosts ----------
            if (scheme == "http" || scheme == "https") {

                val mapHosts = setOf(
                    "maps.google.com",
                    "maps.app.goo.gl",
                    "maps.google.com.ua",
                    "maps.google.com.tr"
                )

                val openExternally = when {
                    host in mapHosts -> true
                    normalizedUrl.contains("google.com/maps") -> true
                    normalizedUrl.contains("maps.app.goo.gl") -> true
                    else -> false
                }

                if (openExternally) {
                    return killPopupAndOpen(
                        Intent(Intent.ACTION_VIEW, targetUri)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }

                return false
            }

            return false
        }

        // ==================== PAGE LIFECYCLE ====================

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)

            // ---------- Fix for blank popup windows ----------
            if (!isPopupAttached &&
                view != null &&
                view.parent == null &&
                url != null &&
                url != "about:blank"
            ) {
                val params = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                popupContainer.addView(view, params)
                onPopupCreated(view)
                isPopupAttached = true
            }

            // ---------- Push token dispatch ----------
            pushTokenState.value?.let { token ->
                val escaped = JSONObject.quote(token)
                view?.evaluateJavascript(
                    "window.dispatchEvent(new CustomEvent('NativePushToken', { detail: $escaped }));",
                    null
                )
            }
        }

        override fun onReceivedSslError(
            view: WebView?,
            handler: android.webkit.SslErrorHandler?,
            error: android.net.http.SslError?
        ) {
            Log.e("KVWebView", "onReceivedSslError: $error")
            handler?.proceed()
        }
    }

    // ==================== WebChromeClient ====================

    webView.webChromeClient = object : WebChromeClient() {

        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
        ): Boolean {

            val popupWebView = createConfiguredWebView(
                context, userAgent,
                popupContainer, onPopupCreated,
                onPopupClosed, onFilePicker,
                pushTokenState
            )

            val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
            transport.webView = popupWebView
            resultMsg.sendToTarget()
            return true
        }

        override fun onCloseWindow(window: WebView?) {
            window?.let {
                popupContainer.removeView(it)
                onPopupClosed(it)
                it.destroy()
            }
        }

        override fun onPermissionRequest(request: PermissionRequest?) {
            WebPermissionManager.handlePermissionRequest(context as? Activity, request)
        }

        // Alerts, confirm, prompts — unchanged
        override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
            AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                .setOnCancelListener { result?.cancel() }
                .show()
            return true
        }

        override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
            AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> result?.cancel() }
                .setOnCancelListener { result?.cancel() }
                .show()
            return true
        }

        override fun onJsPrompt(
            view: WebView?, url: String?,
            message: String?, defaultValue: String?,
            result: JsPromptResult?
        ): Boolean {
            val input = EditText(context).apply { setText(defaultValue.orEmpty()) }
            AlertDialog.Builder(context)
                .setMessage(message)
                .setView(input)
                .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm(input.text.toString()) }
                .setNegativeButton(android.R.string.cancel) { _, _ -> result?.cancel() }
                .show()
            return true
        }

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            val captureIntent = createCaptureIntent(context)
            val captureUri = captureIntent.second

            val chooser = Intent(Intent.ACTION_CHOOSER).apply {
                putExtra(Intent.EXTRA_INTENT, createFilePickerIntent(fileChooserParams))
                putExtra(Intent.EXTRA_TITLE, "Select or capture file")
                captureIntent.first?.let {
                    putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(it))
                }
            }

            return if (filePathCallback != null) {
                onFilePicker(chooser, filePathCallback, captureUri)
                true
            } else false
        }
    }

    webView.setDownloadListener(createDownloadListener(context, userAgent))

    return webView
}


internal fun handleExternalIntent(context: Context, intent: Intent): Boolean {
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Required app is not installed", Toast.LENGTH_SHORT).show()
        false
    } catch (_: Exception) {
        false
    }
}

internal const val WEB_PERMISSION_REQUEST_CODE = 1001

internal object WebPermissionManager {
    internal var pendingRequest: PermissionRequest? = null
    internal var pendingResources: Array<String> = emptyArray()

    fun handlePermissionRequest(activity: Activity?, request: PermissionRequest?) {
        if (request == null) return
        if (activity == null) {
            request.deny()
            return
        }

        val requestedResources = request.resources ?: run {
            request.deny()
            return
        }
        val requiredPermissions = mutableSetOf<String>()

        if (requestedResources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
            requiredPermissions.add(Manifest.permission.CAMERA)
        }
        if (requestedResources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
            requiredPermissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (requiredPermissions.isEmpty()) {
            request.grant(requestedResources)
            return
        }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            request.grant(requestedResources)
        } else {
            pendingRequest = request
            pendingResources = requestedResources
            ActivityCompat.requestPermissions(
                activity,
                missingPermissions.toTypedArray(),
                WEB_PERMISSION_REQUEST_CODE
            )
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode != WEB_PERMISSION_REQUEST_CODE) return
        val request = pendingRequest ?: return
        val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

        if (allGranted) {
            request.grant(pendingResources)
        } else {
            request.deny()
        }

        pendingRequest = null
        pendingResources = emptyArray()
    }
}

internal fun createCaptureIntent(context: Context): Pair<Intent?, Uri?> {
    return try {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: context.cacheDir
        val imageFile = File.createTempFile("capture_", ".jpg", dir)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
        val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        cameraIntent to uri
    } catch (_: Exception) {
        null to null
    }
}

internal fun createFilePickerIntent(fileChooserParams: WebChromeClient.FileChooserParams?): Intent {
    val mimeTypes = fileChooserParams?.acceptTypes?.filter { it.isNotBlank() }?.toTypedArray()
    return Intent(Intent.ACTION_GET_CONTENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = mimeTypes?.firstOrNull() ?: "*/*"
        if (mimeTypes != null && mimeTypes.isNotEmpty()) {
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        }
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    }
}

internal fun createDownloadListener(
    context: Context,
    userAgent: String
): DownloadListener {
    return DownloadListener { url, ua, contentDisposition, mimeType, _ ->

        Thread {
            var finalFileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            var finalMimeType = mimeType

            try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "HEAD"
                    connectTimeout = 5000
                    readTimeout = 5000
                }

                connection.connect()

                val headDisposition = connection.getHeaderField("Content-Disposition")
                val headMime = connection.contentType

                val fileNameFromHead = getFileNameFromContentDisposition(headDisposition)
                if (!fileNameFromHead.isNullOrBlank()) {
                    finalFileName = fileNameFromHead
                } else {
                    finalFileName = URLUtil.guessFileName(
                        url,
                        headDisposition ?: contentDisposition,
                        headMime ?: mimeType
                    )
                }

                if (!headMime.isNullOrBlank()) {
                    finalMimeType = headMime
                }

                connection.disconnect()
            } catch (_: Exception) {
            }

            val activity = context as? Activity ?: return@Thread

            activity.runOnUiThread {
                AlertDialog.Builder(context)
                    .setTitle("Download file")
                    .setMessage("Download $finalFileName?")
                    .setPositiveButton("Download") { _, _ ->
                        try {
                            val request = DownloadManager.Request(Uri.parse(url)).apply {
                                addRequestHeader("User-Agent", ua ?: userAgent)
                                setNotificationVisibility(
                                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                                )
                                setMimeType(finalMimeType)
                                setAllowedOverMetered(true)
                                setAllowedOverRoaming(true)

                                setDestinationInExternalPublicDir(
                                    Environment.DIRECTORY_DOWNLOADS,
                                    finalFileName
                                )
                            }

                            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            dm.enqueue(request)

                            Toast.makeText(context, "Downloading…", Toast.LENGTH_SHORT).show()
                        } catch (_: Exception) {
                            Toast.makeText(context, "Download failed", Toast.LENGTH_LONG).show()
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }.start()
    }
}

internal fun getFileNameFromContentDisposition(
    contentDisposition: String?,
): String? {
    if (contentDisposition.isNullOrBlank()) return null

    val utf8Regex = Regex("filename\\*=UTF-8''([^;]+)")
    val utf8Match = utf8Regex.find(contentDisposition)
    if (utf8Match != null) {
        return try {
            URLDecoder.decode(utf8Match.groupValues[1], "UTF-8")
        } catch (_: Exception) {
            null
        }
    }

    val fallbackRegex = Regex("filename=\"?([^\";]+)\"?")
    val fallbackMatch = fallbackRegex.find(contentDisposition)
    return fallbackMatch?.groupValues?.getOrNull(1)
}
