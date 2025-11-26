package com.web.test.presentation.webview

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun AndroidWebView(
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
            popupWebViewState.value != null -> {
                val popup = popupWebViewState.value
                val parent = popup?.parent as? FrameLayout
                parent?.removeView(popup)
                popup?.destroy()
                popupWebViewState.value = null
            }

            mainWebViewState.value?.canGoBack() == true -> {
                mainWebViewState.value?.goBack()
            }

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
