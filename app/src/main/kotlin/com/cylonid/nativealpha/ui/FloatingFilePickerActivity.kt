package com.cylonid.nativealpha.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback

/**
 * Transparent trampoline Activity used by FloatingWindowService to handle
 * WebView file-upload requests (onShowFileChooser).
 *
 * Because a Service cannot call startActivityForResult, we:
 *  1. Store the pending ValueCallback in the companion object.
 *  2. Start this Activity with the desired MIME types.
 *  3. This Activity launches the system file picker and forwards the result
 *     back to the waiting ValueCallback, then immediately finishes.
 */
class FloatingFilePickerActivity : Activity() {

    companion object {
        const val EXTRA_ACCEPT_TYPES = "accept_types"
        const val EXTRA_ALLOW_MULTIPLE = "allow_multiple"

        @Volatile
        var pendingCallback: ValueCallback<Array<Uri>>? = null

        private const val REQUEST_FILE = 9301
    }

    private var resultDelivered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (pendingCallback == null) {
            finish()
            return
        }

        val rawTypes = intent.getStringArrayExtra(EXTRA_ACCEPT_TYPES) ?: emptyArray()
        val allowMultiple = intent.getBooleanExtra(EXTRA_ALLOW_MULTIPLE, false)
        val validTypes = rawTypes.filter { it.isNotBlank() && it != "*" }

        val pickerIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = validTypes.firstOrNull() ?: "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            if (allowMultiple) putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            if (validTypes.size > 1) {
                putExtra(Intent.EXTRA_MIME_TYPES, validTypes.toTypedArray())
            }
        }

        try {
            startActivityForResult(
                Intent.createChooser(pickerIntent, "Select file to upload"),
                REQUEST_FILE
            )
        } catch (e: Exception) {
            deliverResult(null)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_FILE) {
            val uris: Array<Uri>? = if (resultCode == RESULT_OK && data != null) {
                when {
                    data.clipData != null -> {
                        val count = data.clipData!!.itemCount
                        Array(count) { i -> data.clipData!!.getItemAt(i).uri }
                    }
                    data.data != null -> arrayOf(data.data!!)
                    else -> null
                }
            } else null
            deliverResult(uris)
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!resultDelivered) {
            deliverResult(null)
        }
    }

    private fun deliverResult(uris: Array<Uri>?) {
        resultDelivered = true
        pendingCallback?.onReceiveValue(uris)
        pendingCallback = null
    }
}
