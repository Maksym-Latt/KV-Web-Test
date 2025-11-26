package com.web.test.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WebViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    companion object {
        private const val TAG = "WebViewViewModel"
    }

    val targetUrl: String = savedStateHandle.get<String>("url")?.let(Uri::decode).orEmpty().also { decoded ->
        if (decoded.isBlank()) {
            Log.e(TAG, "Received blank target url from navigation arguments")
        } else {
            Log.i(TAG, "Loaded target url from navigation arguments: $decoded")
        }
    }
}