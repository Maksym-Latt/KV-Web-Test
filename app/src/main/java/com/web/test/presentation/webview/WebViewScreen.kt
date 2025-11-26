package com.web.test.presentation.webview

import android.app.Activity
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
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
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.io.File
import org.json.JSONObject

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
        // если хочешь полностью свой UA:
        "Chrome/18.0.1025.133 Mobile Safari/535.19 KVWebTest/1.0"

        // или, более мягко:
        // WebSettings.getDefaultUserAgent(context)
        //     .replace("wv", "") + " KVWebTest/1.0"
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val stableUserAgent = remember { WebSettings.getDefaultUserAgent(context) + " KVWebTest/1.0" }
    val mainWebViewState = remember { mutableStateOf<WebView?>(null) }
    val popupWebViewState = remember { mutableStateOf<WebView?>(null) }
    val pushTokenState = remember { mutableStateOf(loadStoredPushToken(context)) }

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
                activity?.moveTaskToBack(true)
            }
        }
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

            mainWebView.loadUrl(url)
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

private fun createConfiguredWebView(
    context: Context,
    userAgent: String,
    popupContainer: FrameLayout,
    onPopupCreated: (WebView) -> Unit,
    onPopupClosed: (WebView) -> Unit,
    onFilePicker: (Intent, ValueCallback<Array<Uri>>, Uri?) -> Unit,
    pushTokenState: MutableState<String?>
): WebView {
    val webView = WebView(context)

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

        // ==================== Navigation ====================

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val targetUri = request?.url ?: return false
            return handleUrlOverride(context, targetUri)
        }

        @Suppress("OverridingDeprecatedMember")
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            url: String?
        ): Boolean {
            val targetUri = url?.let { Uri.parse(it) } ?: return false
            return handleUrlOverride(context, targetUri)
        }

        private fun handleUrlOverride(context: Context, targetUri: Uri): Boolean {
            val scheme = targetUri.scheme?.lowercase().orEmpty()
            val host = targetUri.host?.lowercase().orEmpty()
            val url = targetUri.toString()

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

            val whatsappHosts = setOf("wa.me", "api.whatsapp.com")
            val telegramHosts = setOf("t.me", "telegram.me")
            val viberHosts = setOf("viber.com", "www.viber.com", "invite.viber.com")
            val facebookHosts = setOf("m.me", "facebook.com", "www.facebook.com")
            val instagramHosts = setOf("instagram.com", "www.instagram.com")
            val twitterHosts = setOf("twitter.com", "www.twitter.com", "x.com", "www.x.com")

            // ----- intent:// -----
            if (scheme == "intent") {
                return try {
                    val parsedIntent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    if (handleExternalIntent(context, parsedIntent)) {
                        true
                    } else {
                        parsedIntent.getStringExtra("browser_fallback_url")?.let { fallbackUrl ->
                            val fallbackIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(fallbackUrl)
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            handleExternalIntent(context, fallbackIntent)
                        } ?: false
                    }
                } catch (_: Exception) {
                    false
                }
            }

            // ----- директные схемы: whatsapp://, tg://, fb:// и т.д. -----
            if (scheme in externalSchemes) {
                val intent = Intent(Intent.ACTION_VIEW, targetUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return handleExternalIntent(context, intent)
            }

            // ----- http/https, но с особыми хостами (wa.me, t.me и т.д.) -----
            if (scheme == "http" || scheme == "https") {
                val openExternally = when {
                    host in whatsappHosts -> true
                    host in telegramHosts -> true
                    host in viberHosts -> true
                    host in facebookHosts -> true
                    host in instagramHosts -> true
                    host in twitterHosts -> true
                    else -> false
                }

                if (openExternally) {
                    val intent = Intent(Intent.ACTION_VIEW, targetUri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    return handleExternalIntent(context, intent)
                }

                // обычные сайты → в WebView
                return false
            }

            // всё остальное по умолчанию игнорим (WebView попробует сам)
            return false
        }

        // ==================== Page lifecycle ====================

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            Log.d("KVWebView", "onPageFinished: $url")

            pushTokenState.value?.let { token ->
                val escapedToken = JSONObject.quote(token)
                view?.evaluateJavascript(
                    "window.dispatchEvent(new CustomEvent('NativePushToken', { detail: $escapedToken }));",
                    null
                )
            }
        }

        // ==================== Errors ====================

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

    webView.webChromeClient = object : WebChromeClient() {
        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: android.os.Message?
        ): Boolean {
            val popupWebView = createConfiguredWebView(
                context = context,
                userAgent = userAgent,
                popupContainer = popupContainer,
                onPopupCreated = onPopupCreated,
                onPopupClosed = onPopupClosed,
                onFilePicker = onFilePicker,
                pushTokenState = pushTokenState
            )

            popupContainer.addView(
                popupWebView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

            onPopupCreated(popupWebView)

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
            request?.grant(request.resources)
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

private fun Context.canHandleIntent(intent: Intent): Boolean =
    intent.resolveActivity(packageManager) != null


private fun createCaptureIntent(context: Context): Pair<Intent?, Uri?> {
    return try {
        val imageFile = File.createTempFile("capture_", ".jpg", context.cacheDir)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
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

private fun createDownloadListener(
    context: Context,
    userAgent: String
): DownloadListener {
    return DownloadListener { url, ua, contentDisposition, mimeType, _ ->
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            addRequestHeader("User-Agent", ua ?: userAgent)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setMimeType(mimeType)
            setAllowedOverRoaming(true)
            setAllowedOverMetered(true)
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
        }
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }
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
