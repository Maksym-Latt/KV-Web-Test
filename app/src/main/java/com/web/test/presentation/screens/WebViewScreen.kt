package com.web.test.presentation.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WebViewScreen(url: String) {
    val context = LocalContext.current
    val fileCallback: MutableState<ValueCallback<Array<Uri>>?> = remember { mutableStateOf(null) }
    val capturedImageUri = remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val callback = fileCallback.value
        if (result.resultCode == Activity.RESULT_OK) {
            val dataUri = result.data?.data
            val uriResult = when {
                dataUri != null -> arrayOf(dataUri)
                capturedImageUri.value != null -> arrayOf(capturedImageUri.value!!)
                else -> null
            }
            callback?.onReceiveValue(uriResult)
        } else {
            callback?.onReceiveValue(null)
        }
        fileCallback.value = null
        capturedImageUri.value = null
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                settings.cacheMode = WebSettings.LOAD_DEFAULT

                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean {
                        val targetUrl = request?.url ?: return false
                        return when (targetUrl.scheme) {
                            "http", "https" -> false
                            "mailto", "tel", "geo", "intent" -> {
                                val intent = Intent(Intent.ACTION_VIEW, targetUrl)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                ctx.startActivity(intent)
                                true
                            }

                            else -> false
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?,
                    ): Boolean {
                        fileCallback.value?.onReceiveValue(null)
                        fileCallback.value = filePathCallback

                        val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "image/*"
                        }

                        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        val outputUri = createImageFile(ctx)
                        outputUri?.let { uri ->
                            capturedImageUri.value = uri
                            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                            cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        val intentArray = if (cameraIntent.resolveActivity(ctx.packageManager) != null && outputUri != null) {
                            arrayOf(cameraIntent)
                        } else {
                            arrayOf()
                        }

                        val chooserIntent = Intent(Intent.ACTION_CHOOSER).apply {
                            putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                            putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
                        }
                        launcher.launch(chooserIntent)
                        return true
                    }
                }

                if (url.isNotBlank()) {
                    loadUrl(url)
                }
            }
        },
        update = { webView ->
            if (url.isNotBlank() && webView.url != url) {
                webView.loadUrl(url)
            }
        },
    )
}

private fun createImageFile(context: android.content.Context): Uri? {
    return try {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir,
        )
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (ex: IOException) {
        null
    }
}
