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
import android.webkit.JavascriptInterface
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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.io.File
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder

@Composable
fun WebViewScreen(
    viewModel: WebViewViewModel = hiltViewModel()
) {
    val callbackState = remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val captureUriState = remember { mutableStateOf<Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = callbackState.value
        val captureUri = captureUriState.value
        val data = result.data
        val isSuccess = result.resultCode == Activity.RESULT_OK
        val clipData = data?.clipData
        val dataUri = data?.data

        val uris: Array<Uri>? = when {
            !isSuccess -> null
            clipData != null -> {
                (0 until clipData.itemCount).mapNotNull { index ->
                    clipData.getItemAt(index).uri
                }.toTypedArray()
            }
            dataUri != null -> arrayOf(dataUri)
            captureUri != null -> arrayOf(captureUri)
            else -> null
        }

        callback?.onReceiveValue(uris)
        callbackState.value = null
        captureUriState.value = null
    }

    // ==================== Root container ====================

    Surface(color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            AndroidWebView(
                url = viewModel.initialUrl,
                onFilePicker = { intent, fileCallback, captureUri ->
                    callbackState.value = fileCallback
                    captureUriState.value = captureUri
                    filePickerLauncher.launch(intent)
                }
            )
        }
    }
}

@Composable
private fun AndroidWebView(
    url: String,
    onFilePicker: (Intent, ValueCallback<Array<Uri>>, Uri?) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val customUserAgent = remember {
        WebSettings.getDefaultUserAgent(context).replace("wv", "") + " KVWebTest/1.0"
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val mainWebViewState = remember { mutableStateOf<WebView?>(null) }
    val popupWebViewState = remember { mutableStateOf<WebView?>(null) }
    val pushTokenState = remember { mutableStateOf(loadStoredPushToken(context)) }
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        when {
            // 1) Закрыть попап, если есть
            popupWebViewState.value != null -> {
                val popup = popupWebViewState.value
                val parent = popup?.parent as? FrameLayout
                parent?.removeView(popup)
                popup?.destroy()
                popupWebViewState.value = null
            }

            // 2) Если WebView может назад — идём назад
            mainWebViewState.value?.canGoBack() == true -> {
                mainWebViewState.value?.goBack()
            }

            // 3) Иначе — отдаём системный back в Activity/NavHost
            else -> {
                showExitDialog = true
            }
        }
    }

    if (showExitDialog) {
        AlertDialog.Builder(context)
            .setTitle("Exit")
            .setMessage("Do you want to exit the app?")
            .setPositiveButton("Exit") { _, _ ->
                activity?.finish()
            }
            .setNegativeButton("Stay") { dialog, _ ->
                dialog.dismiss()
            }
            .setOnDismissListener { showExitDialog = false }
            .show()
    }


    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            val popupContainer = FrameLayout(context)
            val root = FrameLayout(context)

            val mainWebView = createConfiguredWebView(
                context = context,
                userAgent = customUserAgent,
                popupContainer = popupContainer,
                onPopupCreated = { popup -> popupWebViewState.value = popup },
                onPopupClosed = { popup ->
                    popupContainer.removeView(popup)
                    popupWebViewState.value = null
                },
                onFilePicker = onFilePicker,
                pushTokenState = pushTokenState
            )

            mainWebViewState.value = mainWebView

            root.addView(
                mainWebView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
            root.addView(
                popupContainer,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

            WebViewHandler(context).loadUrlViaReflection(mainWebView, url)
            root
        },
        update = {

        }
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mainWebViewState.value?.onResume()
                    mainWebViewState.value?.resumeTimers()
                    popupWebViewState.value?.onResume()
                    popupWebViewState.value?.resumeTimers()
                }

                Lifecycle.Event.ON_PAUSE -> {
                    mainWebViewState.value?.onPause()
                    mainWebViewState.value?.pauseTimers()
                    popupWebViewState.value?.onPause()
                    popupWebViewState.value?.pauseTimers()
                }

                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

// ==================== WebView factory ====================

private fun createConfiguredWebView(
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

    // ==================== Settings ====================

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

    // ==================== WebViewClient ====================

    webView.webViewClient = object : WebViewClient() {

        // важливо: для попапів додаємо у popupContainer тільки тоді,
        // коли реально грузимо http/https, а не зовнішні схеми
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
                // Messaging
                "viber",
                "tg",
                "whatsapp",
                "line",
                "wechat",
                "kakaotalk",
                "skype",

                // Social
                "facebook",
                "fb",
                "instagram",

                // Twitter / X
                "twitter",
                "x",
                "tweetie"
            )

            fun isPopupView(): Boolean {
                val parent = view?.parent
                return parent === popupContainer && view is WebView
            }

            fun openExternalAndKillPopupIfNeeded(intent: Intent): Boolean {
                // якщо це попап — навіть не показуємо його, а прибираємо
                if (isPopupView()) {
                    popupContainer.removeView(view)
                    onPopupClosed(view as WebView)
                    view.destroy()
                }
                return handleExternalIntent(context, intent)
            }

            // ---------- tel: ----------
            if (scheme == "tel") {
                val intent = Intent(Intent.ACTION_DIAL, targetUri)
                return openExternalAndKillPopupIfNeeded(intent)
            }

            // ---------- sms: / smsto: ----------
            if (scheme == "sms" || scheme == "smsto") {
                val intent = Intent(Intent.ACTION_SENDTO, targetUri)
                return openExternalAndKillPopupIfNeeded(intent)
            }

            // ---------- mailto: ----------
            if (scheme == "mailto") {
                val intent = Intent(Intent.ACTION_SENDTO, targetUri)
                return openExternalAndKillPopupIfNeeded(intent)
            }

            // ---------- Google Pay ----------
            val isGooglePay = scheme == "googlepay" ||
                    host.contains("pay.google.com") ||
                    normalizedUrl.contains("gpay")
            if (isGooglePay) {
                val intent = Intent(Intent.ACTION_VIEW, targetUri)
                return openExternalAndKillPopupIfNeeded(intent)
            }

            // ---------- Samsung Pay ----------
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

            // ---------- intent:// ----------
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

            // ---------- прямые схемы: whatsapp://, tg://, fb:// и т.д. ----------
            if (scheme in externalSchemes) {
                val intent = Intent(Intent.ACTION_VIEW, targetUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return openExternalAndKillPopupIfNeeded(intent)
            }

            // ---------- любые другие нестандартные схемы ----------
            if (scheme.isNotBlank() && scheme != "http" && scheme != "https") {
                val intent = Intent(Intent.ACTION_VIEW, targetUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return openExternalAndKillPopupIfNeeded(intent)
            }

            // ---------- http/https с особыми хостами ----------
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

        // ==================== Page lifecycle ====================

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)

            // ленивая привязка popup-WebView к контейнеру
            if (!isPopupAttached &&
                view != null &&
                view.parent == null &&              // ещё не в layout
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

        // ошибки оставляешь как у тебя было
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
                "onReceivedError (legacy): $errorCode $description, url=$failingUrl"
            )
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            Log.e(
                "KVWebView",
                "onReceivedError: code=${error?.errorCode} desc=${error?.description} url=${request?.url}"
            )
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

    // ==================== WebChromeClient (popup, файлы, пермишены) ====================

    webView.webChromeClient = object : WebChromeClient() {
        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: android.os.Message?
        ): Boolean {
            // ВАЖНО: здесь мы ТОЛЬКО создаём WebView, НО НЕ ДОБАВЛЯЕМ его в popupContainer.
            // Привязка произойдёт лениво в onPageFinished, если это реально http/https страница.
            val popupWebView = createConfiguredWebView(
                context = context,
                userAgent = userAgent,
                popupContainer = popupContainer,
                onPopupCreated = onPopupCreated,
                onPopupClosed = onPopupClosed,
                onFilePicker = onFilePicker,
                pushTokenState = pushTokenState
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
            val activity = context as? Activity
            WebPermissionManager.handlePermissionRequest(activity, request)
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
            result: JsPromptResult?
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



private fun handleExternalIntent(context: Context, intent: Intent): Boolean {
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

private const val WEB_PERMISSION_REQUEST_CODE = 1001

object WebPermissionManager {
    private var pendingRequest: PermissionRequest? = null
    private var pendingResources: Array<String> = emptyArray()

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

private fun Context.canHandleIntent(intent: Intent): Boolean =
    intent.resolveActivity(packageManager) != null


private fun createCaptureIntent(context: Context): Pair<Intent?, Uri?> {
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

private fun createFilePickerIntent(fileChooserParams: WebChromeClient.FileChooserParams?): Intent {
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

// ==================== Download listener ====================

private fun createDownloadListener(
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
                // fallback оставляем как есть
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

// ==================== Download filename utils ====================

private fun getFileNameFromContentDisposition(
    contentDisposition: String?
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



private class WebAppBridge(
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

private fun loadStoredPushToken(context: Context): String? =
    context.getSharedPreferences("web_bridge", Context.MODE_PRIVATE)
        .getString("push_token", null)
