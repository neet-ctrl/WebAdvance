package com.cylonid.nativealpha.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.webview.WebViewClientWithDownload

/**
 * Creates a floating overlay window INSIDE the WebViewActivity's own process.
 *
 * WHY THIS APPROACH:
 * The old FloatingWindowService ran in the MAIN process (com.cylonid.nativealpha).
 * WebView apps run in separate :webapp_N processes. Android does not allow two
 * different processes to use the same WebView data directory simultaneously —
 * this caused the "Using WebView from more than one process" crash.
 *
 * By creating the overlay directly from WebViewActivity (which already runs in
 * :webapp_N), the new floating WebView is in the SAME process. This means:
 *   - No crash (single data directory lock, single process)
 *   - Same cookies and login session automatically
 *   - Same page loaded via last-visited URL
 *   - Only one WebView visible at a time (activity moved to background)
 */
class WebViewFloatingOverlay(
    private val activity: ComponentActivity,
    private val webAppId: Long,
    private val webAppName: String,
    private val webAppHomeUrl: String,
    private val currentUrl: String
) {
    private val windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var floatingWebView: WebView? = null
    private var isMinimized = false
    private var originalHeight = 0

    fun show() {
        val dm = activity.resources.displayMetrics
        val defaultWidth = (dm.widthPixels * 0.9f).toInt()
        val defaultHeight = (dm.heightPixels * 0.75f).toInt()
        originalHeight = defaultHeight

        val params = buildLayoutParams(defaultWidth, defaultHeight, dm.widthPixels, dm.heightPixels)
        val cornerRadiusPx = dm.density * 22f

        val view = LayoutInflater.from(activity).inflate(R.layout.floating_window, null).apply {
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(v: View?, outline: android.graphics.Outline?) {
                    outline?.setRoundRect(0, 0, v?.width ?: 0, v?.height ?: 0, cornerRadiusPx)
                }
            }
        }

        setupWebView(view)
        setupTitleBar(view, params)
        setupButtons(view, params)
        setupResizeHandle(view, params)

        windowManager.addView(view, params)
        overlayView = view
        overlayParams = params
    }

    private fun buildLayoutParams(width: Int, height: Int, screenW: Int, screenH: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams().apply {
            this.width = width
            this.height = height
            x = (screenW - width) / 2
            y = (screenH - height) / 4
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(view: View) {
        val wv = view.findViewById<WebView>(R.id.floatingWebView)
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        val urlChipText = view.findViewById<TextView>(R.id.urlChipText)
        val titleText = view.findViewById<TextView>(R.id.titleText)

        wv.webViewClient = WebViewClientWithDownload(
            context = activity,
            onPageStarted = { url ->
                view.post { urlChipText?.text = formatUrlChip(url) }
            },
            onPageFinished = { url ->
                view.post {
                    urlChipText?.text = formatUrlChip(url)
                    val docTitle = wv.title
                    if (!docTitle.isNullOrBlank()) titleText?.text = docTitle
                    WebViewActivity.saveLastVisitedUrl(activity, webAppId, url)
                }
            }
        ).apply { adblockEnabled = false }

        wv.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                wv2: WebView?,
                filePathCallback: android.webkit.ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                FloatingFilePickerActivity.pendingCallback?.onReceiveValue(null)
                FloatingFilePickerActivity.pendingCallback = filePathCallback
                val acceptTypes = fileChooserParams?.acceptTypes ?: emptyArray()
                val allowMultiple = fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                return try {
                    activity.startActivity(Intent(activity, FloatingFilePickerActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtra(FloatingFilePickerActivity.EXTRA_ACCEPT_TYPES, acceptTypes)
                        putExtra(FloatingFilePickerActivity.EXTRA_ALLOW_MULTIPLE, allowMultiple)
                    })
                    true
                } catch (_: Exception) {
                    FloatingFilePickerActivity.pendingCallback?.onReceiveValue(null)
                    FloatingFilePickerActivity.pendingCallback = null
                    false
                }
            }
        }

        wv.loadUrl(currentUrl)
        floatingWebView = wv
    }

    private fun setupTitleBar(view: View, params: WindowManager.LayoutParams) {
        val titleText = view.findViewById<TextView>(R.id.titleText)
        val urlChipText = view.findViewById<TextView>(R.id.urlChipText)
        val appIconText = view.findViewById<TextView>(R.id.appIconText)

        titleText?.text = webAppName
        appIconText?.text = initialFor(webAppName)
        urlChipText?.text = formatUrlChip(currentUrl)

        view.findViewById<View>(R.id.urlChipEditIcon)?.visibility = View.GONE

        val titleBar = view.findViewById<LinearLayout>(R.id.titleBar)
        var dragStartX = 0f
        var dragStartY = 0f
        var windowStartX = 0
        var windowStartY = 0

        titleBar?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragStartX = event.rawX
                    dragStartY = event.rawY
                    windowStartX = params.x
                    windowStartY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = windowStartX + (event.rawX - dragStartX).toInt()
                    params.y = windowStartY + (event.rawY - dragStartY).toInt()
                    overlayView?.let { windowManager.updateViewLayout(it, params) }
                    true
                }
                else -> false
            }
        }
    }

    private fun setupButtons(view: View, params: WindowManager.LayoutParams) {
        val closeButton = view.findViewById<ImageButton>(R.id.closeButton)
        val minimizeButton = view.findViewById<ImageButton>(R.id.minimizeButton)
        val maximizeButton = view.findViewById<ImageButton>(R.id.maximizeButton)
        val returnButton = view.findViewById<ImageButton>(R.id.returnToAppButton)

        closeButton?.setOnClickListener {
            dismiss()
            returnToApp()
        }

        minimizeButton?.setOnClickListener {
            val container = view.findViewById<FrameLayout>(R.id.webViewContainer)
            if (!isMinimized) {
                isMinimized = true
                container?.visibility = View.GONE
                params.height = WindowManager.LayoutParams.WRAP_CONTENT
                overlayView?.let { windowManager.updateViewLayout(it, params) }
            } else {
                isMinimized = false
                container?.visibility = View.VISIBLE
                params.height = originalHeight
                overlayView?.let { windowManager.updateViewLayout(it, params) }
            }
        }

        maximizeButton?.setOnClickListener {
            val dm = activity.resources.displayMetrics
            params.width = dm.widthPixels
            params.height = dm.heightPixels
            params.x = 0
            params.y = 0
            overlayView?.let { windowManager.updateViewLayout(it, params) }
        }

        returnButton?.setOnClickListener {
            dismiss()
            returnToApp()
        }

        view.findViewById<ImageButton>(R.id.toolbarOverflowButton)?.setOnClickListener {
            val wv = floatingWebView ?: return@setOnClickListener
            val currentPageUrl = wv.url ?: webAppHomeUrl
            val items = arrayOf<CharSequence>("Back", "Forward", "Home", "Refresh", "Copy URL")
            val builder = android.app.AlertDialog.Builder(activity)
            builder.setItems(items) { _, which ->
                when (which) {
                    0 -> if (wv.canGoBack()) wv.goBack()
                    1 -> if (wv.canGoForward()) wv.goForward()
                    2 -> wv.loadUrl(webAppHomeUrl)
                    3 -> wv.reload()
                    4 -> {
                        val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("URL", currentPageUrl))
                        Toast.makeText(activity, "URL copied", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            val dialog = builder.create()
            dialog.window?.setType(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE
            )
            dialog.show()
        }
    }

    private fun setupResizeHandle(view: View, params: WindowManager.LayoutParams) {
        val resizeHandle = view.findViewById<View>(R.id.resizeHandle) ?: return
        var initialWidth = 0
        var initialHeight = 0
        var initialX = 0f
        var initialY = 0f

        resizeHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialWidth = params.width
                    initialHeight = params.height
                    initialX = event.rawX
                    initialY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialX).toInt()
                    val dy = (event.rawY - initialY).toInt()
                    params.width = (initialWidth + dx).coerceAtLeast(300)
                    params.height = (initialHeight + dy).coerceAtLeast(200)
                    originalHeight = params.height
                    overlayView?.let { windowManager.updateViewLayout(it, params) }
                    true
                }
                else -> false
            }
        }
    }

    private fun returnToApp() {
        try {
            val intent = Intent(activity, activity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            activity.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun dismiss() {
        floatingWebView?.let { wv ->
            val url = wv.url
            if (!url.isNullOrBlank() && !url.startsWith("about:")) {
                WebViewActivity.saveLastVisitedUrl(activity, webAppId, url)
            }
            try {
                wv.stopLoading()
                wv.onPause()
            } catch (_: Exception) {}
        }
        overlayView?.let { v ->
            try { windowManager.removeView(v) } catch (_: Exception) {}
        }
        overlayView = null
        overlayParams = null
        floatingWebView = null
        if (activeOverlay === this) activeOverlay = null
    }

    private fun formatUrlChip(url: String?): String {
        if (url.isNullOrBlank()) return ""
        return try {
            val uri = Uri.parse(url)
            val host = uri.host ?: return url
            val clean = host.removePrefix("www.")
            val path = uri.path?.takeIf { it.isNotBlank() && it != "/" } ?: ""
            if (path.isNotEmpty()) "$clean$path" else clean
        } catch (_: Exception) { url }
    }

    private fun initialFor(name: String): String {
        val ch = name.trim().firstOrNull { it.isLetterOrDigit() } ?: 'W'
        return ch.uppercaseChar().toString()
    }

    companion object {
        var activeOverlay: WebViewFloatingOverlay? = null
    }
}
