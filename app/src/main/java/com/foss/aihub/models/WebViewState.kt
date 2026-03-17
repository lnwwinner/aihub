package com.foss.aihub.models

enum class WebViewState {
    IDLE,       
    LOADING,    
    SUCCESS,    
    ERROR       
}

data class ServiceUiState(
    val webViewState: WebViewState = WebViewState.LOADING,
    val isLoading: Boolean = true,
    val progress: Int = 0,
    val error: Pair<Int, String>? = null,
    val isVisible: Boolean = false
)