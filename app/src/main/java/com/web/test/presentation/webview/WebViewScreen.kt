package com.web.test.presentation.webview

import android.app.Activity
import android.net.Uri
import android.webkit.ValueCallback
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

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
