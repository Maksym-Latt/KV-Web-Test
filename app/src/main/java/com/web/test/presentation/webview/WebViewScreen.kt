package com.web.test.presentation.webview

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun WebViewScreen(
    viewModel: WebViewViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val callbackState = remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val callback = callbackState.value
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val resultUri = data?.data
            if (resultUri != null) {
                callback?.onReceiveValue(arrayOf(resultUri))
            } else if (data?.clipData != null) {
                val uris = (0 until data.clipData!!.itemCount).mapNotNull { index ->
                    data.clipData?.getItemAt(index)?.uri
                }
                callback?.onReceiveValue(uris.toTypedArray())
            } else {
                callback?.onReceiveValue(null)
            }
        } else {
            callback?.onReceiveValue(null)
        }
        callbackState.value = null
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidWebView(
                url = viewModel.initialUrl,
                onFilePicker = { intent, fileCallback ->
                    callbackState.value = fileCallback
                    filePickerLauncher.launch(intent)
                }
            )
        }
    }
}

@Composable
private fun AndroidWebView(
    url: String,
    onFilePicker: (Intent, ValueCallback<Array<Uri>>) -> Unit
) {
    val context = LocalContext.current

    androidx.compose.ui.viewinterop.AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            WebView(context).apply {
                val webView = this

                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    setAcceptThirdPartyCookies(webView, true)
                }

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false
                settings.displayZoomControls = false

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val targetUri = request?.url ?: return false
                        val scheme = targetUri.scheme.orEmpty()
                        return if (scheme.startsWith("http")) {
                            false
                        } else {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, targetUri)
                                context.startActivity(intent)
                                true
                            } catch (_: Exception) {
                                false
                            }
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        val pickerIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                        }
                        val chooser = Intent(Intent.ACTION_CHOOSER).apply {
                            putExtra(Intent.EXTRA_INTENT, pickerIntent)
                            putExtra(Intent.EXTRA_TITLE, "Select file")
                        }
                        return if (filePathCallback != null) {
                            onFilePicker(chooser, filePathCallback)
                            true
                        } else {
                            false
                        }
                    }
                }

                loadUrl(url)
            }
        },
        update = { webView ->
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        }
    )
}
