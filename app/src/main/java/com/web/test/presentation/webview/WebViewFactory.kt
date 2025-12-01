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
import android.provider.MediaStore
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

    // ==================== WebViewClient ====================
    webView.webViewClient = object : WebViewClient() {

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
            val url = targetUri.toString()

            // ---------- POPUP должен работать как нормальный браузер ----------
            // важнейшая строка: popup не перехватываем
            val isPopup = view != null && view.parent === popupContainer
            if (isPopup) return false


            // ---------- tel: ----------
            if (scheme == "tel") {
                context.startActivity(Intent(Intent.ACTION_DIAL, targetUri))
                return true
            }

            // ---------- sms ----------
            if (scheme == "sms" || scheme == "smsto") {
                context.startActivity(Intent(Intent.ACTION_SENDTO, targetUri))
                return true
            }

            // ---------- mailto ----------
            if (scheme == "mailto") {
                context.startActivity(Intent(Intent.ACTION_SENDTO, targetUri))
                return true
            }

            // ---------- intent:// ----------
            if (scheme == "intent") {
                return try {
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    context.startActivity(intent)
                    true
                } catch (_: Exception) {
                    false
                }
            }

            // ---------- viber / tg / whatsapp ----------
            if (scheme in setOf("viber", "tg", "whatsapp")) {
                return try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, targetUri))
                    true
                } catch (_: Exception) {
                    false
                }
            }

            // ---------- googlepay, samsungpay ----------
            if (scheme == "googlepay" ||
                url.contains("gpay") ||
                scheme == "samsungpay"
            ) {
                return try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, targetUri))
                    true
                } catch (_: Exception) {
                    false
                }
            }

            // ---------- Any other non-http scheme ----------
            if (scheme.isNotBlank() && scheme !in listOf("http", "https")) {
                return try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, targetUri))
                    true
                } catch (_: Exception) {
                    false
                }
            }

            // ---------- http/https — всегда грузим сами ----------
            return false
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
    return webView
}

internal const val WEB_PERMISSION_REQUEST_CODE = 1001

internal object WebPermissionManager {

    fun handlePermissionRequest(activity: Activity?, request: PermissionRequest?) {
        if (activity == null || request == null) {
            request?.deny()
            return
        }

        val needsCamera = request.resources?.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE) == true

        if (!needsCamera) {
            request.grant(request.resources)
            return
        }

        val hasCameraPermission =
            ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            request.grant(request.resources)
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.CAMERA),
                WEB_PERMISSION_REQUEST_CODE
            )
            pendingRequest = request
        }
    }

    private var pendingRequest: PermissionRequest? = null

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode != WEB_PERMISSION_REQUEST_CODE) return

        pendingRequest?.let { req ->
            val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
            if (granted) req.grant(req.resources) else req.deny()
        }

        pendingRequest = null
    }
}


internal fun createCaptureIntent(context: Context): Pair<Intent?, Uri?> {
    return try {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.cacheDir
        val imageFile = File.createTempFile("capture_", ".jpg", dir)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
        }

        intent to uri
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