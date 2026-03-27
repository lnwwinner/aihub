package com.foss.aihub.ui.screens.dialogs

import android.content.Context
import android.webkit.JsPromptResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.foss.aihub.R
import com.foss.aihub.models.JsDialog

@Composable
fun JsDialogHandler(
    dialog: JsDialog?, context: Context, onDismiss: () -> Unit
) {
    when (dialog) {
        is JsDialog.Alert -> {
            AlertDialog(
                onDismissRequest = {
                    dialog.result.cancel()
                    onDismiss()
                },
                title = { Text(context.getString(R.string.label_alert)) },
                text = { Text(dialog.message) },
                confirmButton = {
                    TextButton(onClick = {
                        dialog.result.confirm()
                        onDismiss()
                    }) {
                        Text(context.getString(R.string.action_ok))
                    }
                },
                shape = MaterialTheme.shapes.medium,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            )
        }

        is JsDialog.Confirm -> {
            AlertDialog(
                onDismissRequest = {
                    dialog.result.cancel()
                    onDismiss()
                },
                title = { Text(context.getString(R.string.label_confirm)) },
                text = { Text(dialog.message) },
                confirmButton = {
                    TextButton(onClick = {
                        dialog.result.confirm()
                        onDismiss()
                    }) {
                        Text(context.getString(R.string.action_ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        dialog.result.cancel()
                        onDismiss()
                    }) {
                        Text(context.getString(R.string.action_cancel))
                    }
                },
                shape = MaterialTheme.shapes.medium,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            )
        }

        is JsDialog.Prompt -> {
            var inputText by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = {
                    dialog.result.cancel()
                    onDismiss()
                },
                title = { Text(context.getString(R.string.label_prompt)) },
                text = {
                    Column {
                        Text(dialog.message, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = inputText, onValueChange = {
                                inputText = it
                            }, singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        (dialog.result as? JsPromptResult)?.confirm(inputText)
                            ?: dialog.result.confirm()
                        onDismiss()
                    }) {
                        Text(context.getString(R.string.action_ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        dialog.result.cancel()
                        onDismiss()
                    }) {
                        Text(context.getString(R.string.action_cancel))
                    }
                },
                shape = MaterialTheme.shapes.medium,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            )
        }

        is JsDialog.BeforeUnload -> {
            AlertDialog(
                onDismissRequest = {
                    dialog.result.cancel()
                    onDismiss()
                },
                title = { Text(context.getString(R.string.label_before_leave)) },
                text = { Text(dialog.message) },
                confirmButton = {
                    TextButton(onClick = {
                        dialog.result.confirm()
                        onDismiss()
                    }) {
                        Text(context.getString(R.string.action_leave))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        dialog.result.cancel()
                        onDismiss()
                    }) {
                        Text(context.getString(R.string.action_stay))
                    }
                },
                shape = MaterialTheme.shapes.medium,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            )
        }

        null -> Unit
    }
}