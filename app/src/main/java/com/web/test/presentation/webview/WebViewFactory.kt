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

internal fun createConfiguredWebView(
    context: Context,
    userAgent: String,
    popupContainer: FrameLayout,
    onPopupCreated: (WebView) -> Unit,
    onPopupClosed: (WebView) -> Unit,
    onFilePicker: (Intent, ValueCallback<Array<Uri>>, Uri?) -> Unit,
    pushTokenState: MutableState<String?>
): WebView {
    val handler = WebViewHandler(context)
    val webView = handler.createWebViewViaReflection()
    handler.configureWebViewViaReflection(webView)

    CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(webView, true)
    }

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

    webView.addJavascriptInterface(
        WebAppBridge(context) { token ->
            pushTokenState.value = token
        },
        "AndroidBridge"
    )

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

        private fun handleUrlOverride(
            view: WebView?,
            context: Context,
            targetUri: Uri
        ): Boolean {
            val scheme = targetUri.scheme?.lowercase().orEmpty()
            val host = targetUri.host?.lowercase().orEmpty()
            val url = targetUri.toString()
            val normalizedUrl = url.lowercase()

            val externalSchemes = setOf(
                "viber",
                "tg",
                "whatsapp",
                "line",
                "wechat",
                "kakaotalk",
                "skype",
                "facebook",
                "fb",
                "instagram",
                "twitter",
                "x",
                "tweetie"
            )

            fun isPopupView(): Boolean {
                val parent = view?.parent
                return parent === popupContainer && view is WebView
            }

            fun openExternalAndKillPopupIfNeeded(intent: Intent): Boolean {
                if (isPopupView()) {
                    popupContainer.removeView(view)
                    onPopupClosed(view as WebView)
                    view.destroy()
                }
                return handleExternalIntent(context, intent)
            }

            if (scheme == "tel") {
                val intent = Intent(Intent.ACTION_DIAL, targetUri)
                return openExternalAndKillPopupIfNeeded(intent)
            }

            if (scheme == "sms" || scheme == "smsto") {
                val intent = Intent(Intent.ACTION_SENDTO, targetUri)
                return openExternalAndKillPopupIfNeeded(intent)
            }

            if (scheme == "mailto") {
                val intent = Intent(Intent.ACTION_SENDTO, targetUri)
                return openExternalAndKillPopupIfNeeded(intent)
            }

            val isGooglePay = scheme == "googlepay" ||
                    host.contains("pay.google.com") ||
                    normalizedUrl.contains("gpay")
            if (isGooglePay) {
                val intent = Intent(Intent.ACTION_VIEW, targetUri)
                return openExternalAndKillPopupIfNeeded(intent)
            }

            val isSamsungPay = scheme == "samsungpay" ||
                    normalizedUrl.contains("samsungpay")
            if (isSamsungPay) {
                val intent = Intent(Intent.ACTION_VIEW, targetUri)
                return openExternalAndKillPopupIfNeeded(intent)
            }

            val whatsappHosts = setOf("wa.me", "api.whatsapp.com")
            val telegramHosts = setOf("t.me", "telegram.me")
            val viberHosts = setOf("viber.com", "www.viber.com", "invite.viber.com")
            val facebookHosts = setOf("m.me", "facebook.com", "www.facebook.com")
            val instagramHosts = setOf("instagram.com", "www.instagram.com")
            val twitterHosts = setOf("twitter.com", "www.twitter.com", "x.com", "www.x.com")

            if (scheme == "intent") {
                return try {
                    val parsedIntent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    if (openExternalAndKillPopupIfNeeded(parsedIntent)) {
                        true
                    } else {
                        parsedIntent.getStringExtra("browser_fallback_url")?.let { fallbackUrl ->
                            val fallbackIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(fallbackUrl)
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            openExternalAndKillPopupIfNeeded(fallbackIntent)
                        } ?: false
                    }
                } catch (_: Exception) {
                    false
                }
            }

            if (scheme in externalSchemes) {
                val intent = Intent(Intent.ACTION_VIEW, targetUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return openExternalAndKillPopupIfNeeded(intent)
            }

            if (scheme.isNotBlank() && scheme != "http" && scheme != "https") {
                val intent = Intent(Intent.ACTION_VIEW, targetUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return openExternalAndKillPopupIfNeeded(intent)
            }

            if (scheme == "http" || scheme == "https") {
                val mapHosts = setOf(
                    "maps.google.com",
                    "www.google.com",
                    "maps.app.goo.gl",
                    "maps.google.com.ua",
                    "maps.google.com.tr"
                )

                val openExternally = when {
                    host in whatsappHosts -> true
                    host in telegramHosts -> true
                    host in viberHosts -> true
                    host in facebookHosts -> true
                    host in instagramHosts -> true
                    host in twitterHosts -> true
                    host in mapHosts -> true
                    normalizedUrl.contains("maps.app.goo.gl") -> true
                    normalizedUrl.contains("google.com/maps") -> true
                    else -> false
                }

                if (openExternally) {
                    val intent = Intent(Intent.ACTION_VIEW, targetUri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    return openExternalAndKillPopupIfNeeded(intent)
                }

                return false
            }

            return false
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)

            if (!isPopupAttached &&
                view != null &&
                view.parent == null &&
                url != null &&
                (url.startsWith("http://") || url.startsWith("https://"))
            ) {
                val params = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                popupContainer.addView(view, params)
                onPopupCreated(view)
                isPopupAttached = true
            }

            pushTokenState.value?.let { token ->
                val escapedToken = JSONObject.quote(token)
                view?.evaluateJavascript(
                    "window.dispatchEvent(new CustomEvent('NativePushToken', { detail: $escapedToken }));",
                    null
                )
            }
        }

        @Suppress("DEPRECATION")
        override fun onReceivedError(
            view: WebView?,
            errorCode: Int,
            description: String?,
            failingUrl: String?
        ) {
            super.onReceivedError(view, errorCode, description, failingUrl)
            Log.e(
                "KVWebView",
                "onReceivedError code=$errorCode, desc=$description, url=$failingUrl"
            )
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            super.onReceivedError(view, request, error)
            Log.e(
                "KVWebView",
                "onReceivedError code=${error.errorCode}, desc=${error.description}, url=${request.url}"
            )
        }
    }

    webView.webChromeClient = object : WebChromeClient() {
        private var customView: FrameLayout? = null
        private var customViewCallback: CustomViewCallback? = null

        override fun onPermissionRequest(request: PermissionRequest?) {
            WebPermissionManager.handlePermissionRequest(context as? Activity, request)
        }

        override fun onPermissionRequestCanceled(request: PermissionRequest?) {
            super.onPermissionRequestCanceled(request)
            if (request == WebPermissionManager.pendingRequest) {
                WebPermissionManager.pendingRequest = null
                WebPermissionManager.pendingResources = emptyArray()
            }
        }

        override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
            if (customView != null) {
                callback?.onCustomViewHidden()
                return
            }
            val activity = context as? Activity ?: return
            val decor = activity.window.decorView as? FrameLayout ?: return
            customView = view as? FrameLayout
            customViewCallback = callback
            decor.addView(customView)
            activity.window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                )
        }

        override fun onHideCustomView() {
            val activity = context as? Activity ?: return
            val decor = activity.window.decorView as? FrameLayout ?: return
            customView?.let { decor.removeView(it) }
            customViewCallback?.onCustomViewHidden()
            customView = null
            customViewCallback = null
            activity.window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
        }

        override fun onJsAlert(
            view: WebView?,
            url: String?,
            message: String?,
            result: JsResult?
        ): Boolean {
            AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                .setOnCancelListener { result?.cancel() }
                .show()
            return true
        }

        override fun onJsConfirm(
            view: WebView?,
            url: String?,
            message: String?,
            result: JsResult?
        ): Boolean {
            AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> result?.cancel() }
                .setOnCancelListener { result?.cancel() }
                .show()
            return true
        }

        override fun onJsPrompt(
            view: WebView?,
            url: String?,
            message: String?,
            defaultValue: String?,
            result: JsPromptResult?,
        ): Boolean {
            val input = EditText(context).apply { setText(defaultValue.orEmpty()) }
            AlertDialog.Builder(context)
                .setMessage(message)
                .setView(input)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    result?.confirm(input.text.toString())
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> result?.cancel() }
                .setOnCancelListener { result?.cancel() }
                .show()
            return true
        }

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?,
        ): Boolean {
            val captureIntent = createCaptureIntent(context)
            val captureUri = captureIntent.second
            val chooser = Intent(Intent.ACTION_CHOOSER).apply {
                putExtra(Intent.EXTRA_INTENT, createFilePickerIntent(fileChooserParams))
                putExtra(Intent.EXTRA_TITLE, "Select or capture file")
                captureIntent.first?.let { cameraIntent ->
                    putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
                }
            }

            return if (filePathCallback != null) {
                onFilePicker(chooser, filePathCallback, captureUri)
                true
            } else {
                false
            }
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
