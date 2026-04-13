package com.foss.aihub.ui.webview

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebView
import android.widget.Toast
import com.foss.aihub.R
import com.foss.aihub.utils.readAssetsFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object DownloadHandler {
    fun handleDownload(
        webView: WebView,
        url: String,
        userAgent: String,
        contentDisposition: String?,
        mimeType: String?
    ) {
        when {
            url.startsWith("blob:") -> {
                val script =
                    webView.context.readAssetsFile("forceDownload.txt").replace("{{URL}}", url)
                        .trimIndent()

                webView.evaluateJavascript(script) { result ->
                    Log.d("WebView", "Blob download trigger result: $result")
                }

                Toast.makeText(
                    webView.context,
                    webView.context.getString(R.string.msg_processing_blob),
                    Toast.LENGTH_SHORT
                ).show()
            }

            url.startsWith("http") -> {
                downloadFileToDownloads(
                    webView.context, url, userAgent, contentDisposition, mimeType
                )
            }

            else -> {
                Toast.makeText(
                    webView.context,
                    webView.context.getString(R.string.msg_unsupported_scheme),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun downloadFileToDownloads(
        context: Context,
        url: String,
        userAgent: String,
        contentDisposition: String?,
        mimeType: String?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                val sanitized = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")

                Toast.makeText(
                    context,
                    context.getString(R.string.msg_downloading, sanitized),
                    Toast.LENGTH_SHORT
                ).show()

                downloadAndSaveStreaming(
                    context = context,
                    url = url,
                    userAgent = userAgent,
                    fileName = sanitized,
                    mimeType = mimeType ?: "application/octet-stream"
                )

                Toast.makeText(
                    context,
                    context.getString(R.string.msg_dowload_complete, sanitized),
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    context, context.getString(R.string.msg_download_failed), Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun downloadAndSaveStreaming(
        context: Context, url: String, userAgent: String, fileName: String, mimeType: String
    ) = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")

        var connection: HttpURLConnection? = null

        try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", userAgent)

            CookieManager.getInstance().getCookie(url)?.let { cookies ->
                if (cookies.isNotBlank()) {
                    connection.setRequestProperty("Cookie", cookies)
                }
            }

            connection.instanceFollowRedirects = true
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Server returned code ${connection.responseCode}")
            }

            val contentLength = connection.contentLengthLong
            val inputStream = connection.inputStream

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val itemUri = resolver.insert(collection, contentValues)
                    ?: throw Exception("Failed to create MediaStore entry")

                try {
                    resolver.openOutputStream(itemUri)?.use { outputStream ->
                        val buffer = ByteArray(16 * 1024) // 16KB chunks
                        var bytesRead: Int
                        var totalBytes = 0L

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead

                            if (contentLength > 0) {
                                val progress = (totalBytes * 100 / contentLength).toInt()
                                Log.d("Download", "Progress: $progress%")
                            }
                        }
                        outputStream.flush()
                    } ?: throw Exception("Failed to open output stream")

                    val updateValues = ContentValues().apply {
                        put(MediaStore.Downloads.IS_PENDING, 0)
                    }
                    resolver.update(itemUri, updateValues, null, null)

                    Log.d("Download", "File saved successfully (API 29+): $safeName")

                } catch (e: Exception) {
                    resolver.delete(itemUri, null, null)
                    throw e
                }

            } else {
                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                var finalFile = File(downloadsDir, safeName)

                var counter = 1
                while (finalFile.exists()) {
                    val nameWithoutExt = safeName.substringBeforeLast(".", safeName)
                    val ext = safeName.substringAfterLast(".", "")
                    val newName = if (ext.isNotEmpty()) "${nameWithoutExt}_$counter.$ext"
                    else "${nameWithoutExt}_$counter"

                    finalFile = File(downloadsDir, newName)
                    counter++
                }

                downloadsDir.mkdirs()

                finalFile.outputStream().use { outputStream ->
                    val buffer = ByteArray(16 * 1024)
                    var bytesRead: Int
                    var totalBytes = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead

                        if (contentLength > 0) {
                            val progress = (totalBytes * 100 / contentLength).toInt()
                            Log.d("Download", "Progress: $progress%")
                        }
                    }
                }

                MediaScannerConnection.scanFile(
                    context, arrayOf(finalFile.absolutePath), arrayOf(mimeType)
                ) { path, uri ->
                    Log.d("Download", "File scanned: $path → $uri")
                }

                Log.d("Download", "File saved successfully (Legacy): ${finalFile.absolutePath}")
            }

        } finally {
            connection?.disconnect()
        }
    }
}