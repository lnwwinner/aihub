package com.foss.aihub.models

import android.webkit.JsResult

sealed class JsDialog {
    data class Alert(val message: String, val result: JsResult) : JsDialog()
    data class Confirm(val message: String, val result: JsResult) : JsDialog()
    data class Prompt(val message: String, val result: JsResult) : JsDialog()
    data class BeforeUnload(val message: String, val result: JsResult) : JsDialog()
}