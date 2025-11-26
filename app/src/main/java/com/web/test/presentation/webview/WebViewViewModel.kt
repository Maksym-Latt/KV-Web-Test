package com.web.test.presentation.webview

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WebViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val initialUrl: String = savedStateHandle.get<String>("url")?.let { encoded ->
        Uri.decode(encoded)
    }.orEmpty()
}
