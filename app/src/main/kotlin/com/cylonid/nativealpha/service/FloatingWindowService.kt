package com.cylonid.nativealpha.service

import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.view.*
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.widget.EditText
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.links.LinkHistoryTracker
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.model.WindowEntity
import com.cylonid.nativealpha.model.WindowPresetEntity
import com.cylonid.nativealpha.repository.WebAppRepository
import com.cylonid.nativealpha.ui.WebViewActivity
import com.cylonid.nativealpha.waos.util.WaosConstants
import com.cylonid.nativealpha.webview.SessionManager
import com.cylonid.nativealpha.webview.WebViewClientWithDownload
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class FloatingWindowService : Service() {

    @Inject
    lateinit var windowManager: WindowManager

    @Inject
    lateinit var webAppRepository: WebAppRepository

    @Inject
    lateinit var waosClipboard: com.cylonid.nativealpha.manager.ClipboardManager

    @Inject
    lateinit var credentialManager: com.cylonid.nativealpha.manager.CredentialManager

    @Inject
    lateinit var downloadManager: com.cylonid.nativealpha.manager.DownloadManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val floatingWindows = mutableMapOf<Long, FloatingWindowView>()
    private var currentFrontWindow: Long? = null
    private val _openWindows = MutableStateFlow<List<WindowEntity>>(emptyList())
    val openWindows: StateFlow<List<WindowEntity>> = _openWindows.asStateFlow()

    private var actionPanelView: View? = null
    private var urlEditorView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val webAppId = intent?.getLongExtra("webAppId", 0L) ?: 0L
        val webAppUrl = intent?.getStringExtra("webAppUrl") ?: "https://example.com"
        val webAppName = intent?.getStringExtra("webAppName") ?: "Web App"
        val windowId = intent?.getLongExtra("windowId", 0L) ?: 0L

        when (action) {
            ACTION_ADD_WINDOW -> addFloatingWindow(webAppId, webAppUrl, webAppName)
            ACTION_REMOVE_WINDOW -> removeFloatingWindow(windowId)
            ACTION_CLOSE_ALL -> closeAllWindows()
            ACTION_MINIMIZE_WINDOW -> minimizeWindow(windowId)
            ACTION_MAXIMIZE_WINDOW -> maximizeWindow(windowId)
        }

        return START_STICKY
    }

    private fun addFloatingWindow(webAppId: Long, webAppUrl: String, webAppName: String) {
        if (floatingWindows.containsKey(webAppId)) return

        // The FloatingWindowService runs in the MAIN process. Each webapp slot process
        // (":webapp_0"…":webapp_7") already owns a locked data-directory suffix of the
        // form "app_profile_<id>". Trying to use the same suffix in a second process causes
        // the crash: "Using WebView from more than one process at once with the same data
        // directory is not supported." (crbug/558377).
        // Fix: give the floating window its own dedicated suffix so it never conflicts.
        try {
            WebView.setDataDirectorySuffix("floating")
        } catch (_: Exception) {
            // Already set for this process — safe to ignore.
        }

        val layoutParams = WindowManager.LayoutParams().apply {
            width = 800
            height = 600
            x = 100
            y = 100
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
        }

        val cornerRadiusPx = resources.displayMetrics.density * 22f
        val view = LayoutInflater.from(this).inflate(R.layout.floating_window, null).apply {
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: android.view.View?, outline: android.graphics.Outline?) {
                    outline?.setRoundRect(0, 0, view?.width ?: 0, view?.height ?: 0, cornerRadiusPx)
                }
            }
        }

        val webView = view.findViewById<WebView>(R.id.floatingWebView)
        val titleText = view.findViewById<TextView>(R.id.titleText)
        val urlChipText = view.findViewById<TextView>(R.id.urlChipText)
        val appIconText = view.findViewById<TextView>(R.id.appIconText)
        val overflowButton = view.findViewById<ImageButton>(R.id.toolbarOverflowButton)
        titleText.text = webAppName
        appIconText.text = initialFor(webAppName)
        urlChipText.text = formatUrlChip(webAppUrl)

        val webViewClient = WebViewClientWithDownload(
            context = this,
            onPageStarted = { url ->
                view.post { urlChipText.text = formatUrlChip(url) }
            },
            onPageFinished = { url ->
                view.post {
                    urlChipText.text = formatUrlChip(url)
                    val docTitle = webView.title
                    if (!docTitle.isNullOrBlank()) titleText.text = docTitle
                    // Keep last-URL in sync so main WebViewActivity and floating window share history
                    WebViewActivity.saveLastVisitedUrl(this@FloatingWindowService, webAppId, url)
                }
            }
        ).apply {
            adblockEnabled = false
        }

        val windowView = FloatingWindowView(
            view = view,
            layoutParams = layoutParams,
            webView = webView,
            webViewClient = webViewClient,
            appId = webAppId,
            appName = webAppName,
            isMinimized = false,
            isMaximized = false,
            originalWidth = 800,
            originalHeight = 600,
            originalX = 100,
            originalY = 100
        )

        // Drag support via title bar
        val titleBar = view.findViewById<LinearLayout>(R.id.titleBar)
        var dragStartX = 0f
        var dragStartY = 0f
        var windowStartX = 0
        var windowStartY = 0
        titleBar?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (currentFrontWindow != webAppId) {
                        bringWindowToFront(webAppId)
                    }
                    dragStartX = event.rawX
                    dragStartY = event.rawY
                    windowStartX = layoutParams.x
                    windowStartY = layoutParams.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = windowStartX + (event.rawX - dragStartX).toInt()
                    layoutParams.y = windowStartY + (event.rawY - dragStartY).toInt()
                    windowManager.updateViewLayout(view, layoutParams)
                    true
                }
                else -> false
            }
        }

        view.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (currentFrontWindow != webAppId) {
                    bringWindowToFront(webAppId)
                }
            }
            false
        }

        webView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (currentFrontWindow != webAppId) {
                    bringWindowToFront(webAppId)
                }
            }
            false
        }

        // Configure WebView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            // Strip Android WebView markers ("; wv)" and "Version/4.0 ") from the
            // default UA so Cloudflare and other bot-detection services see a normal
            // Chrome-for-Android UA rather than an in-app WebView UA.
            val rawUA = userAgentString ?: ""
            userAgentString = rawUA
                .replace("; wv)", ")")
                .replace("; wv;", ";")
                .replace("Version/4.0 ", "")
        }
        webView.webViewClient = webViewClient
        // Wire up a WebChromeClient so web pages can open the system file picker
        // (e.g. ChatGPT upload, Gmail attach, any <input type="file">).
        // Services cannot call startActivityForResult, so we use a trampoline
        // Activity (FloatingFilePickerActivity) and a static callback holder.
        webView.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onShowFileChooser(
                wv: android.webkit.WebView?,
                filePathCallback: android.webkit.ValueCallback<Array<android.net.Uri>>?,
                fileChooserParams: android.webkit.WebChromeClient.FileChooserParams?
            ): Boolean {
                com.cylonid.nativealpha.ui.FloatingFilePickerActivity.pendingCallback
                    ?.onReceiveValue(null)
                com.cylonid.nativealpha.ui.FloatingFilePickerActivity.pendingCallback =
                    filePathCallback
                val acceptTypes = fileChooserParams?.acceptTypes ?: emptyArray()
                val allowMultiple = fileChooserParams?.mode ==
                        android.webkit.WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE
                val launchIntent = Intent(
                    this@FloatingWindowService,
                    com.cylonid.nativealpha.ui.FloatingFilePickerActivity::class.java
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(
                        com.cylonid.nativealpha.ui.FloatingFilePickerActivity.EXTRA_ACCEPT_TYPES,
                        acceptTypes
                    )
                    putExtra(
                        com.cylonid.nativealpha.ui.FloatingFilePickerActivity.EXTRA_ALLOW_MULTIPLE,
                        allowMultiple
                    )
                }
                return try {
                    startActivity(launchIntent)
                    true
                } catch (e: Exception) {
                    com.cylonid.nativealpha.ui.FloatingFilePickerActivity.pendingCallback
                        ?.onReceiveValue(null)
                    com.cylonid.nativealpha.ui.FloatingFilePickerActivity.pendingCallback = null
                    false
                }
            }

            override fun onProgressChanged(view: android.webkit.WebView?, newProgress: Int) {
                // no-op — progress not shown in floating window title bar
            }
        }

        // Inject anti-bot-detection script that runs before ANY page script, so
        // Cloudflare TurnstileBot and similar services see a real Chrome browser:
        //   • navigator.webdriver → undefined  (biggest Cloudflare signal)
        //   • window.chrome → minimal runtime object  (absent in bare WebView)
        //   • navigator.plugins → non-empty  (empty list = headless giveaway)
        //   • navigator.languages → ['en-US','en']
        val antiBotJs = """
            (function() {
                try { Object.defineProperty(navigator,'webdriver',{get:()=>undefined,configurable:true}); } catch(e){}
                try { if(!window.chrome){window.chrome={runtime:{},loadTimes:function(){},csi:function(){},app:{}};} } catch(e){}
                try { Object.defineProperty(navigator,'plugins',{get:()=>[{name:'Chrome PDF Plugin',filename:'internal-pdf-viewer',description:'Portable Document Format'}],configurable:true}); } catch(e){}
                try { Object.defineProperty(navigator,'languages',{get:()=>['en-US','en'],configurable:true}); } catch(e){}
            })();
        """.trimIndent()
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(webView, antiBotJs, setOf("*"))
        }

        // Open at the last page the user visited in the main WebView (or the home URL if none)
        val startUrl = WebViewActivity.readLastVisitedUrl(this, webAppId)
            ?.takeIf { it.isNotBlank() } ?: webAppUrl
        webView.loadUrl(startUrl)

        val closeButton = view.findViewById<ImageButton>(R.id.closeButton)
        val minimizeButton = view.findViewById<ImageButton>(R.id.minimizeButton)
        val maximizeButton = view.findViewById<ImageButton>(R.id.maximizeButton)
        val returnToAppButton = view.findViewById<ImageButton>(R.id.returnToAppButton)

        closeButton?.setOnClickListener {
            removeFloatingWindow(webAppId)
        }

        minimizeButton?.setOnClickListener {
            minimizeWindow(webAppId)
        }

        maximizeButton?.setOnClickListener {
            maximizeWindow(webAppId)
        }

        // "Return to app" — save the current page, close the floating window, and
        // bring the main full-screen WebViewActivity for this app to the foreground.
        // Uses WebAppRouter so the correct sandboxed process/activity slot is resumed.
        returnToAppButton?.setOnClickListener {
            val currentUrl = webView.url?.takeIf { it.isNotBlank() && !it.startsWith("about:") }
            if (currentUrl != null) {
                WebViewActivity.saveLastVisitedUrl(this@FloatingWindowService, webAppId, currentUrl)
            }
            removeFloatingWindow(webAppId)
            com.cylonid.nativealpha.util.WebAppRouter.launch(this@FloatingWindowService, webAppId)
        }

        overflowButton?.setOnClickListener {
            showActionPanel(it, windowView, webAppUrl)
        }

        // URL chip → open the address editor
        val urlChipContainer = view.findViewById<View>(R.id.urlChipContainer)
        val urlChipEditIcon = view.findViewById<View>(R.id.urlChipEditIcon)
        val openEditor = View.OnClickListener { showUrlEditor(windowView) }
        urlChipContainer?.setOnClickListener(openEditor)
        urlChipEditIcon?.setOnClickListener(openEditor)

        val resizeHandle = view.findViewById<View>(R.id.resizeHandle)
        var initialWidth = 0
        var initialHeight = 0
        var initialResizeTouchX = 0f
        var initialResizeTouchY = 0f

        resizeHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialWidth = layoutParams.width
                    initialHeight = layoutParams.height
                    initialResizeTouchX = event.rawX
                    initialResizeTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialResizeTouchX).toInt()
                    val deltaY = (event.rawY - initialResizeTouchY).toInt()
                    layoutParams.width = (initialWidth + deltaX).coerceAtLeast(300)
                    layoutParams.height = (initialHeight + deltaY).coerceAtLeast(200)
                    windowManager.updateViewLayout(view, layoutParams)
                    true
                }
                else -> false
            }
        }

        windowManager.addView(view, layoutParams)
        floatingWindows[webAppId] = windowView
        updateOpenWindows()

        serviceScope.launch {
            loadWebAppConfig(webAppId, webView, titleText, windowView)
        }
    }

    private fun showActionPanel(anchor: View, windowView: FloatingWindowView, webAppUrl: String) {
        dismissActionPanel()
        val panel = LayoutInflater.from(this).inflate(R.layout.floating_action_panel, null, false)

        val currentUrl = windowView.webView.url ?: webAppUrl

        // Panel header: app identity
        panel.findViewById<TextView>(R.id.panelAppTitle).text = windowView.appName
        panel.findViewById<TextView>(R.id.panelAppUrl).text = formatUrlChip(currentUrl)
        panel.findViewById<TextView>(R.id.panelAppIcon).text = initialFor(windowView.appName)

        // Navigation row
        panel.findViewById<View>(R.id.navBack).setOnClickListener {
            if (windowView.webView.canGoBack()) windowView.webView.goBack()
            dismissActionPanel()
        }
        panel.findViewById<View>(R.id.navForward).setOnClickListener {
            if (windowView.webView.canGoForward()) windowView.webView.goForward()
            dismissActionPanel()
        }
        panel.findViewById<View>(R.id.navHome).setOnClickListener {
            windowView.webView.loadUrl(webAppUrl)
            dismissActionPanel()
        }
        panel.findViewById<View>(R.id.navRefresh).setOnClickListener {
            windowView.webView.reload()
            dismissActionPanel()
        }

        // Modes (toggle tiles — keep the panel open so users see the new state)
        bindToggleTile(
            panel, R.id.tileDesktop, R.drawable.ic_fw_desktop_24,
            "Desktop site", windowView.desktopModeEnabled
        ) { pill ->
            windowView.desktopModeEnabled = !windowView.desktopModeEnabled
            applyDesktopMode(windowView)
            applyPillState(pill, windowView.desktopModeEnabled)
        }
        bindToggleTile(
            panel, R.id.tileAdblock, R.drawable.ic_fw_shield_24,
            "Ad block", windowView.adblockEnabled
        ) { pill ->
            windowView.adblockEnabled = !windowView.adblockEnabled
            applyAdblock(windowView)
            applyPillState(pill, windowView.adblockEnabled)
        }
        bindToggleTile(
            panel, R.id.tileAutoScroll, R.drawable.ic_fw_autoscroll_24,
            "Auto scroll", windowView.autoScrollEnabled
        ) { pill ->
            windowView.autoScrollEnabled = !windowView.autoScrollEnabled
            applyAutoScroll(windowView)
            applyPillState(pill, windowView.autoScrollEnabled)
        }
        bindToggleTile(
            panel, R.id.tileAutoClick, R.drawable.ic_fw_autoclick_24,
            "Auto click", windowView.autoClickEnabled
        ) { pill ->
            windowView.autoClickEnabled = !windowView.autoClickEnabled
            applyAutoClick(windowView)
            applyPillState(pill, windowView.autoClickEnabled)
        }

        // Page actions
        bindActionTile(panel, R.id.tileOpenBrowser, R.drawable.ic_baseline_open_in_browser_24, "Open in browser") {
            handleToolbarAction(ACTION_TOOLBAR_OPEN_BROWSER, windowView, webAppUrl)
            dismissActionPanel()
        }
        bindActionTile(panel, R.id.tileShare, R.drawable.ic_baseline_share_24, "Share URL") {
            handleToolbarAction(ACTION_TOOLBAR_SHARE, windowView, webAppUrl)
            dismissActionPanel()
        }
        bindActionTile(panel, R.id.tileCopyUrl, R.drawable.ic_baseline_content_copy_24, "Copy URL") {
            handleToolbarAction(ACTION_TOOLBAR_COPY_URL, windowView, webAppUrl)
            dismissActionPanel()
        }

        // Tools
        bindActionTile(panel, R.id.tileCredentials, R.drawable.ic_fw_key_24, "Credentials") {
            handleToolbarAction(ACTION_TOOLBAR_CREDENTIALS, windowView, webAppUrl)
            dismissActionPanel()
        }
        bindActionTile(panel, R.id.tileClipboard, R.drawable.ic_fw_clipboard_24, "Clipboard manager") {
            handleToolbarAction(ACTION_TOOLBAR_CLIPBOARD, windowView, webAppUrl)
            dismissActionPanel()
        }
        bindActionTile(panel, R.id.tileDownloads, R.drawable.ic_baseline_cloud_download_24, "Downloads") {
            handleToolbarAction(ACTION_TOOLBAR_DOWNLOADS, windowView, webAppUrl)
            dismissActionPanel()
        }

        // Position the panel just below and aligned to the right edge of the anchor button
        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        val density = resources.displayMetrics.density
        val panelWidthPx = (320 * density + 12 * density).toInt()
        val xOffset = (location[0] + anchor.width - panelWidthPx).coerceAtLeast((8 * density).toInt())
        val yOffset = location[1] + anchor.height + (4 * density).toInt()

        val params = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
            x = xOffset
            y = yOffset
            windowAnimations = android.R.style.Animation_Dialog
        }

        panel.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                dismissActionPanel()
                true
            } else {
                false
            }
        }

        // Cap the action panel height so it never grows past ~half the screen,
        // and let the inner ScrollView handle overflow.
        val scroll = panel.findViewById<View>(R.id.actionPanelScroll)
        if (scroll != null) {
            val maxPanelHeight = (resources.displayMetrics.heightPixels * 0.5f).toInt()
            val lp = scroll.layoutParams
            lp.height = maxPanelHeight
            scroll.layoutParams = lp
        }

        try {
            windowManager.addView(panel, params)
            actionPanelView = panel
        } catch (_: Exception) {
            actionPanelView = null
        }
    }

    private fun dismissActionPanel() {
        actionPanelView?.let { panel ->
            try {
                windowManager.removeView(panel)
            } catch (_: Exception) {
            }
        }
        actionPanelView = null
    }

    private fun showUrlEditor(windowView: FloatingWindowView) {
        dismissActionPanel()
        dismissUrlEditor()

        val editor = LayoutInflater.from(this).inflate(R.layout.floating_url_editor, null, false)
        val input = editor.findViewById<EditText>(R.id.urlInput)
        val goBtn = editor.findViewById<TextView>(R.id.urlGo)
        val cancelBtn = editor.findViewById<TextView>(R.id.urlCancel)
        val clearBtn = editor.findViewById<ImageButton>(R.id.urlClear)

        val currentUrl = windowView.webView.url ?: ""
        input.setText(currentUrl)
        input.setSelection(input.text.length)

        clearBtn.setOnClickListener {
            input.text.clear()
            input.requestFocus()
        }
        cancelBtn.setOnClickListener { dismissUrlEditor() }

        val submit = submit@{
            val raw = input.text.toString().trim()
            if (raw.isEmpty()) return@submit
            val finalUrl = normalizeUrl(raw)
            windowView.webView.loadUrl(finalUrl)
            recordManualNavigation(windowView, finalUrl)
            dismissUrlEditor()
        }

        goBtn.setOnClickListener { submit() }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                actionId == EditorInfo.IME_ACTION_SEND
            ) {
                submit(); true
            } else false
        }

        editor.isFocusableInTouchMode = true
        editor.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                dismissUrlEditor(); true
            } else false
        }

        val params = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND
            dimAmount = 0.4f
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.CENTER
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            windowAnimations = android.R.style.Animation_Dialog
        }

        try {
            windowManager.addView(editor, params)
            urlEditorView = editor
            input.requestFocus()
        } catch (_: Exception) {
            urlEditorView = null
        }
    }

    private fun dismissUrlEditor() {
        urlEditorView?.let { editor ->
            try {
                windowManager.removeView(editor)
            } catch (_: Exception) {
            }
        }
        urlEditorView = null
    }

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return trimmed
        // Already has a scheme
        if (trimmed.matches(Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"))) return trimmed
        // Looks like a search query (has whitespace or no dot)
        val looksLikeUrl = !trimmed.contains(' ') && (trimmed.contains('.') || trimmed.startsWith("localhost"))
        return if (looksLikeUrl) "https://$trimmed"
        else "https://www.google.com/search?q=${Uri.encode(trimmed)}"
    }

    private fun recordManualNavigation(windowView: FloatingWindowView, url: String) {
        serviceScope.launch {
            try {
                LinkHistoryTracker(applicationContext, windowView.appId).recordAction(
                    url = url,
                    pageTitle = windowView.appName,
                    action = "manual_navigate",
                    format = "PLAIN_URL",
                    referrer = windowView.webView.url
                )
            } catch (_: Exception) {
            }
        }
    }

    private fun bindToggleTile(
        panel: View,
        tileId: Int,
        iconRes: Int,
        title: String,
        initialState: Boolean,
        onToggle: (TextView) -> Unit
    ) {
        val tile = panel.findViewById<View>(tileId)
        val icon = tile.findViewById<ImageView>(R.id.tileIcon)
        val titleView = tile.findViewById<TextView>(R.id.tileTitle)
        val pill = tile.findViewById<TextView>(R.id.tileStatePill)
        icon.setImageResource(iconRes)
        icon.setColorFilter(ContextCompat.getColor(this, R.color.fw_text_primary))
        titleView.text = title
        pill.visibility = View.VISIBLE
        applyPillState(pill, initialState)
        tile.setOnClickListener { onToggle(pill) }
    }

    private fun bindActionTile(
        panel: View,
        tileId: Int,
        iconRes: Int,
        title: String,
        onClick: () -> Unit
    ) {
        val tile = panel.findViewById<View>(tileId)
        val icon = tile.findViewById<ImageView>(R.id.tileIcon)
        val titleView = tile.findViewById<TextView>(R.id.tileTitle)
        val pill = tile.findViewById<TextView>(R.id.tileStatePill)
        icon.setImageResource(iconRes)
        icon.setColorFilter(ContextCompat.getColor(this, R.color.fw_text_primary))
        titleView.text = title
        pill.visibility = View.GONE
        tile.setOnClickListener { onClick() }
    }

    private fun applyPillState(pill: TextView, on: Boolean) {
        if (on) {
            pill.text = "ON"
            pill.setBackgroundResource(R.drawable.fw_state_pill_on)
            pill.setTextColor(ContextCompat.getColor(this, R.color.fw_state_on_text))
        } else {
            pill.text = "OFF"
            pill.setBackgroundResource(R.drawable.fw_state_pill_off)
            pill.setTextColor(ContextCompat.getColor(this, R.color.fw_state_off_text))
        }
    }

    private fun initialFor(name: String): String {
        val ch = name.trim().firstOrNull { it.isLetterOrDigit() } ?: 'W'
        return ch.uppercaseChar().toString()
    }

    private fun formatUrlChip(url: String?): String {
        if (url.isNullOrBlank()) return ""
        return try {
            val uri = Uri.parse(url)
            val host = uri.host ?: return url
            val cleanHost = host.removePrefix("www.")
            val path = uri.path?.takeIf { it.isNotBlank() && it != "/" } ?: ""
            if (path.isNotEmpty()) "$cleanHost$path" else cleanHost
        } catch (_: Exception) {
            url
        }
    }

    private fun handleToolbarAction(actionId: Int, windowView: FloatingWindowView, webAppUrl: String) {
        val webView = windowView.webView
        when (actionId) {
            ACTION_TOOLBAR_BACK -> if (webView.canGoBack()) webView.goBack()
            ACTION_TOOLBAR_FORWARD -> if (webView.canGoForward()) webView.goForward()
            ACTION_TOOLBAR_HOME -> webView.loadUrl(webAppUrl)
            ACTION_TOOLBAR_REFRESH -> webView.reload()
            ACTION_TOOLBAR_DESKTOP -> {
                windowView.desktopModeEnabled = !windowView.desktopModeEnabled
                applyDesktopMode(windowView)
            }
            ACTION_TOOLBAR_ADBLOCK -> {
                windowView.adblockEnabled = !windowView.adblockEnabled
                applyAdblock(windowView)
            }
            ACTION_TOOLBAR_AUTOSCROLL -> {
                windowView.autoScrollEnabled = !windowView.autoScrollEnabled
                applyAutoScroll(windowView)
            }
            ACTION_TOOLBAR_AUTOCLICK -> {
                windowView.autoClickEnabled = !windowView.autoClickEnabled
                applyAutoClick(windowView)
            }
            ACTION_TOOLBAR_OPEN_BROWSER -> {
                val currentUrl = webView.url ?: webAppUrl
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
            }
            ACTION_TOOLBAR_SHARE -> {
                val currentUrl = webView.url ?: webAppUrl
                startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, currentUrl)
                            putExtra(Intent.EXTRA_SUBJECT, windowView.view.contentDescription ?: "Web page")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        },
                        "Share URL"
                    ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                )
            }
            ACTION_TOOLBAR_COPY_URL -> {
                val currentUrl = webView.url ?: webAppUrl
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("URL", currentUrl))
                if (currentUrl.isNotBlank()) {
                    waosClipboard.copyToAppClipboard(
                        windowView.appId,
                        currentUrl,
                        com.cylonid.nativealpha.manager.ClipboardItem.Type.URL
                    )
                    android.widget.Toast.makeText(
                        this,
                        "URL copied — saved to Clipboard Manager",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            ACTION_TOOLBAR_CREDENTIALS -> showToolInWindow(windowView, ToolType.VAULT)
            ACTION_TOOLBAR_CLIPBOARD -> showToolInWindow(windowView, ToolType.CLIPBOARD)
            ACTION_TOOLBAR_DOWNLOADS -> showToolInWindow(windowView, ToolType.DOWNLOADS)
        }
    }

    // -------------------------------------------------------------------------
    // In-window tool panel: Clipboard / Credentials / Downloads render INSIDE
    // the floating window itself. A "back" button in the sub-header restores
    // the WebView. Items are loaded one-shot from the same DAOs the full-screen
    // tool activities use.
    // -------------------------------------------------------------------------

    private enum class ToolType { CLIPBOARD, VAULT, DOWNLOADS }

    private fun showToolInWindow(windowView: FloatingWindowView, type: ToolType) {
        val container = windowView.view.findViewById<android.widget.FrameLayout>(R.id.webViewContainer)
            ?: return

        // If a tool panel is already showing, swap content rather than stacking.
        windowView.toolPanelView?.let {
            try { container.removeView(it) } catch (_: Exception) {}
            windowView.toolPanelView = null
        }

        val panel = LayoutInflater.from(this).inflate(R.layout.floating_tool_panel, container, false)
        val title = panel.findViewById<TextView>(R.id.toolTitle)
        val subtitle = panel.findViewById<TextView>(R.id.toolSubtitle)
        val list = panel.findViewById<LinearLayout>(R.id.toolListContainer)
        val empty = panel.findViewById<TextView>(R.id.toolEmptyState)
        val backBtn = panel.findViewById<ImageButton>(R.id.toolBackButton)

        title.text = when (type) {
            ToolType.CLIPBOARD -> "Clipboard"
            ToolType.VAULT -> "Vault"
            ToolType.DOWNLOADS -> "Downloads"
        }
        empty.text = when (type) {
            ToolType.CLIPBOARD -> "No clipboard items yet."
            ToolType.VAULT -> "No saved logins yet."
            ToolType.DOWNLOADS -> "No downloads yet."
        }

        backBtn.setOnClickListener { dismissToolPanel(windowView) }

        // Hide the WebView + resize handle while the tool panel is up.
        windowView.webView.visibility = View.GONE
        windowView.view.findViewById<View>(R.id.resizeHandle)?.visibility = View.GONE

        container.addView(
            panel,
            android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        windowView.toolPanelView = panel

        // Populate from the data layer.
        serviceScope.launch {
            try {
                when (type) {
                    ToolType.CLIPBOARD -> populateClipboard(windowView, list, subtitle, empty)
                    ToolType.VAULT -> populateVault(windowView, list, subtitle, empty)
                    ToolType.DOWNLOADS -> populateDownloads(windowView, list, subtitle, empty)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    empty.visibility = View.VISIBLE
                    empty.text = "Couldn't load (${e.message ?: "unknown error"})"
                }
            }
        }
    }

    private fun dismissToolPanel(windowView: FloatingWindowView) {
        val container = windowView.view.findViewById<android.widget.FrameLayout>(R.id.webViewContainer)
        windowView.toolPanelView?.let { panel ->
            try { container?.removeView(panel) } catch (_: Exception) {}
        }
        windowView.toolPanelView = null
        windowView.webView.visibility = View.VISIBLE
        windowView.view.findViewById<View>(R.id.resizeHandle)?.visibility = View.VISIBLE
    }

    private suspend fun populateClipboard(
        windowView: FloatingWindowView,
        list: LinearLayout,
        subtitle: TextView,
        empty: TextView
    ) {
        val items = waosClipboard
            .getAppClipboardItems(windowView.appId, limit = 200)
            .firstOrNull().orEmpty()
        withContext(Dispatchers.Main) {
            subtitle.text = if (items.isEmpty()) "" else "${items.size}"
            empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            list.removeAllViews()
            items.forEach { item ->
                val row = LayoutInflater.from(this@FloatingWindowService)
                    .inflate(R.layout.floating_tool_row, list, false)
                row.findViewById<ImageView>(R.id.rowIcon).setImageResource(R.drawable.ic_fw_clipboard_24)
                row.findViewById<TextView>(R.id.rowPrimary).text = item.content
                row.findViewById<TextView>(R.id.rowSecondary).text =
                    "${item.type.name.lowercase().replaceFirstChar { it.uppercase() }} · ${formatRelativeTime(item.timestamp.time)}"
                val btn = row.findViewById<ImageButton>(R.id.rowAction)
                btn.setImageResource(R.drawable.ic_baseline_content_copy_24)
                btn.contentDescription = "Copy"
                btn.setOnClickListener {
                    waosClipboard.copyToSystemClipboard(item.content)
                    android.widget.Toast.makeText(this@FloatingWindowService, "Copied", android.widget.Toast.LENGTH_SHORT).show()
                }
                row.setOnClickListener {
                    waosClipboard.copyToSystemClipboard(item.content)
                    android.widget.Toast.makeText(this@FloatingWindowService, "Copied", android.widget.Toast.LENGTH_SHORT).show()
                }
                list.addView(row)
            }
        }
    }

    private suspend fun populateVault(
        windowView: FloatingWindowView,
        list: LinearLayout,
        subtitle: TextView,
        empty: TextView
    ) {
        val creds = credentialManager
            .getDecryptedCredentialsForApp(windowView.appId)
            .firstOrNull().orEmpty()
        withContext(Dispatchers.Main) {
            subtitle.text = if (creds.isEmpty()) "" else "${creds.size}"
            empty.visibility = if (creds.isEmpty()) View.VISIBLE else View.GONE
            list.removeAllViews()
            creds.forEach { cred ->
                val row = LayoutInflater.from(this@FloatingWindowService)
                    .inflate(R.layout.floating_tool_row, list, false)
                row.findViewById<ImageView>(R.id.rowIcon).setImageResource(R.drawable.ic_fw_key_24)
                row.findViewById<TextView>(R.id.rowPrimary).text =
                    cred.title.ifBlank { cred.username.ifBlank { "Login" } }
                row.findViewById<TextView>(R.id.rowSecondary).text = cred.username.ifBlank { "—" }
                val btn = row.findViewById<ImageButton>(R.id.rowAction)
                btn.setImageResource(R.drawable.ic_baseline_content_copy_24)
                btn.contentDescription = "Copy password"
                btn.setOnClickListener {
                    val clip = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clip?.setPrimaryClip(ClipData.newPlainText("Password", cred.password))
                    android.widget.Toast.makeText(this@FloatingWindowService, "Password copied", android.widget.Toast.LENGTH_SHORT).show()
                }
                row.setOnClickListener {
                    val clip = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clip?.setPrimaryClip(ClipData.newPlainText("Username", cred.username))
                    android.widget.Toast.makeText(this@FloatingWindowService, "Username copied", android.widget.Toast.LENGTH_SHORT).show()
                }
                list.addView(row)
            }
        }
    }

    private suspend fun populateDownloads(
        windowView: FloatingWindowView,
        list: LinearLayout,
        subtitle: TextView,
        empty: TextView
    ) {
        val items = downloadManager
            .getDownloadsForApp(windowView.appId)
            .firstOrNull().orEmpty()
            .sortedByDescending { it.timestamp }
        withContext(Dispatchers.Main) {
            subtitle.text = if (items.isEmpty()) "" else "${items.size}"
            empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            list.removeAllViews()
            items.forEach { dl ->
                val row = LayoutInflater.from(this@FloatingWindowService)
                    .inflate(R.layout.floating_tool_row, list, false)
                row.findViewById<ImageView>(R.id.rowIcon)
                    .setImageResource(R.drawable.ic_baseline_cloud_download_24)
                row.findViewById<TextView>(R.id.rowPrimary).text = dl.fileName
                val sizeStr = if (dl.fileSize > 0) dl.formattedFileSize else "—"
                row.findViewById<TextView>(R.id.rowSecondary).text =
                    "${dl.status.name.lowercase().replaceFirstChar { it.uppercase() }} · $sizeStr"
                val btn = row.findViewById<ImageButton>(R.id.rowAction)
                btn.setImageResource(R.drawable.ic_baseline_open_in_browser_24)
                btn.contentDescription = "Open"
                val openClick = openClick@{
                    val path = dl.filePath ?: return@openClick
                    val file = java.io.File(path)
                    if (!file.exists()) {
                        android.widget.Toast.makeText(this@FloatingWindowService, "File not found", android.widget.Toast.LENGTH_SHORT).show()
                        return@openClick
                    }
                    try {
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            this@FloatingWindowService,
                            "${packageName}.fileprovider",
                            file
                        )
                        startActivity(Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, dl.mimeType ?: "*/*")
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(this@FloatingWindowService, "Can't open: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                btn.setOnClickListener { openClick() }
                row.setOnClickListener { openClick() }
                list.addView(row)
            }
        }
    }

    private fun formatRelativeTime(timestampMs: Long): String {
        val diff = System.currentTimeMillis() - timestampMs
        return when {
            diff < 60_000 -> "just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            diff < 604_800_000 -> "${diff / 86_400_000}d ago"
            else -> java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
                .format(java.util.Date(timestampMs))
        }
    }

    private fun applyDesktopMode(windowView: FloatingWindowView) {
        val webView = windowView.webView
        if (windowView.originalUserAgent == null) {
            windowView.originalUserAgent = webView.settings.userAgentString
        }
        webView.settings.userAgentString = if (windowView.desktopModeEnabled) {
            DESKTOP_USER_AGENT
        } else {
            windowView.originalUserAgent ?: webView.settings.userAgentString
        }
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        if (windowView.desktopModeEnabled) {
            webView.evaluateJavascript(
                "javascript:(function(){var meta=document.querySelector('meta[name=viewport]'); if(!meta){meta=document.createElement('meta'); meta.name='viewport'; document.head.appendChild(meta);} meta.content='width=device-width, initial-scale=0.5, maximum-scale=3.0, user-scalable=yes'; var style=document.createElement('style'); style.textContent='body{min-width:100vw!important;overflow-x:auto;} @media (max-width:768px){body{font-size:14px;}}'; document.head.appendChild(style);})()",
                null
            )
        }
        webView.reload()
    }

    private fun applyAdblock(windowView: FloatingWindowView) {
        windowView.webViewClient.adblockEnabled = windowView.adblockEnabled
        windowView.webView.reload()
    }

    private fun applyAutoScroll(windowView: FloatingWindowView) {
        val webView = windowView.webView
        if (windowView.autoScrollEnabled) {
            webView.evaluateJavascript(
                "javascript:(function(){window.waosAutoScroll={interval:null,start:function(){this.stop();this.interval=setInterval(function(){window.scrollBy(0,2);},50);},stop:function(){if(this.interval){clearInterval(this.interval);this.interval=null;}}};window.waosAutoScroll.start();})()",
                null
            )
        } else {
            webView.evaluateJavascript(
                "javascript:(function(){if(window.waosAutoScroll){window.waosAutoScroll.stop();}})()",
                null
            )
        }
    }

    private fun applyAutoClick(windowView: FloatingWindowView) {
        val webView = windowView.webView
        if (windowView.autoClickEnabled) {
            webView.evaluateJavascript(
                "javascript:(function(){window.waosAutoClick={interval:null,start:function(){this.stop();this.interval=setInterval(function(){var btn=document.querySelector('button'); if(btn) btn.click();},1500);},stop:function(){if(this.interval){clearInterval(this.interval);this.interval=null;}}};window.waosAutoClick.start();})()",
                null
            )
        } else {
            webView.evaluateJavascript(
                "javascript:(function(){if(window.waosAutoClick){window.waosAutoClick.stop();}})()",
                null
            )
        }
    }

    private suspend fun loadWebAppConfig(webAppId: Long, webView: WebView, titleText: TextView, windowView: FloatingWindowView) {
        val app = webAppRepository.getWebAppById(webAppId).firstOrNull()
        app?.let { webApp ->
            withContext(Dispatchers.Main) {
                titleText.text = webApp.name
                windowView.appName = webApp.name
                windowView.view.findViewById<TextView>(R.id.appIconText)?.text = initialFor(webApp.name)
                windowView.view.findViewById<TextView>(R.id.urlChipText)?.text =
                    formatUrlChip(webView.url ?: webApp.baseUrl)
                webView.settings.javaScriptEnabled = webApp.isJavaScriptEnabled
                webView.settings.builtInZoomControls = webApp.isEnableZooming
                webView.settings.setSupportZoom(true)
                webView.settings.displayZoomControls = false
                webView.settings.useWideViewPort = true
                webView.settings.loadWithOverviewMode = true

                val requestedUserAgent = webApp.userAgent?.takeIf { it.isNotBlank() }
                val isSavedDesktopMode = requestedUserAgent?.contains("Mozilla/5.0 (X11; Linux x86_64)") == true || webApp.isRequestDesktop
                var shouldReload = false
                if (requestedUserAgent != null) {
                    webView.settings.userAgentString = requestedUserAgent
                    shouldReload = true
                } else if (isSavedDesktopMode) {
                    webView.settings.userAgentString = DESKTOP_USER_AGENT
                    shouldReload = true
                }

                windowView.originalUserAgent = webView.settings.userAgentString
                windowView.desktopModeEnabled = isSavedDesktopMode
                windowView.adblockEnabled = webApp.isUseAdblock || webApp.isAdblockEnabled
                windowView.webViewClient.adblockEnabled = windowView.adblockEnabled

                if (webApp.floatingWindowWidth > 0) {
                    windowView.layoutParams.width = webApp.floatingWindowWidth
                    windowView.originalWidth = webApp.floatingWindowWidth
                }
                if (webApp.floatingWindowHeight > 0) {
                    windowView.layoutParams.height = webApp.floatingWindowHeight
                    windowView.originalHeight = webApp.floatingWindowHeight
                }
                windowManager.updateViewLayout(windowView.view, windowView.layoutParams)

                if (windowView.desktopModeEnabled) {
                    applyDesktopMode(windowView)
                }
                if (windowView.adblockEnabled) {
                    applyAdblock(windowView)
                }
                if (shouldReload && !windowView.desktopModeEnabled && !windowView.adblockEnabled) {
                    webView.reload()
                }

                // Show PIN overlay when the app is locked (mirrors main WebViewActivity behaviour)
                if (webApp.isLocked && !webApp.pin.isNullOrBlank()) {
                    showPinOverlay(windowView, webApp.pin!!)
                }
            }
        }
    }

    /**
     * Overlay a PIN-entry view directly inside the floating window's webViewContainer.
     * The WebView is hidden until the correct PIN is entered.
     * This mirrors the AlertDialog shown in WebViewActivity for locked apps.
     */
    private fun showPinOverlay(windowView: FloatingWindowView, correctPin: String) {
        val container = windowView.view.findViewById<android.widget.FrameLayout>(R.id.webViewContainer)
            ?: return
        windowView.webView.visibility = View.GONE
        windowView.view.findViewById<View>(R.id.resizeHandle)?.visibility = View.GONE

        val dp = resources.displayMetrics.density

        val overlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0F0F23.toInt())
            gravity = Gravity.CENTER
            setPadding((24 * dp).toInt(), (32 * dp).toInt(), (24 * dp).toInt(), (32 * dp).toInt())
        }

        val lockEmoji = TextView(this).apply {
            text = "🔒"
            textSize = 36f
            gravity = Gravity.CENTER
        }
        overlay.addView(lockEmoji, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).also { it.bottomMargin = (8 * dp).toInt() })

        val titleView = TextView(this).apply {
            text = "App Locked"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 17f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        overlay.addView(titleView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).also { it.bottomMargin = (4 * dp).toInt() })

        val subtitleView = TextView(this).apply {
            text = "Enter PIN to unlock"
            setTextColor(0xFFAAAAAA.toInt())
            textSize = 13f
            gravity = Gravity.CENTER
        }
        overlay.addView(subtitleView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).also { it.bottomMargin = (20 * dp).toInt() })

        val pinInput = EditText(this).apply {
            hint = "PIN"
            setHintTextColor(0xFF666666.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 18f
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF1E1E3A.toInt())
            setPadding((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), (12 * dp).toInt())
        }
        overlay.addView(pinInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).also { it.bottomMargin = (8 * dp).toInt() })

        val errorView = TextView(this).apply {
            setTextColor(0xFFFF5555.toInt())
            textSize = 12f
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        overlay.addView(errorView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).also { it.bottomMargin = (12 * dp).toInt() })

        val unlockBtn = android.widget.Button(this).apply {
            text = "Unlock"
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                if (pinInput.text.toString() == correctPin) {
                    container.removeView(overlay)
                    windowView.webView.visibility = View.VISIBLE
                    windowView.view.findViewById<View>(R.id.resizeHandle)?.visibility = View.VISIBLE
                } else {
                    errorView.text = "Incorrect PIN"
                    errorView.visibility = View.VISIBLE
                    pinInput.text.clear()
                }
            }
        }
        overlay.addView(unlockBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        container.addView(overlay, android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        ))
    }

    private fun removeFloatingWindow(windowId: Long) {
        floatingWindows[windowId]?.let { windowView ->
            dismissActionPanel()
            dismissUrlEditor()
            dismissToolPanel(windowView)
            windowManager.removeView(windowView.view)
            floatingWindows.remove(windowId)
            if (currentFrontWindow == windowId) {
                currentFrontWindow = null
            }
            updateOpenWindows()
        }
    }

    private fun minimizeWindow(windowId: Long) {
        floatingWindows[windowId]?.let { windowView ->
            windowView.isMinimized = !windowView.isMinimized
            if (windowView.isMinimized) {
                // Save original size
                windowView.originalWidth = windowView.layoutParams.width
                windowView.originalHeight = windowView.layoutParams.height

                // Minimize to title bar only
                windowView.layoutParams.width = 300
                windowView.layoutParams.height = 50
                windowView.webView.visibility = View.GONE
            } else {
                // Restore original size
                windowView.layoutParams.width = windowView.originalWidth
                windowView.layoutParams.height = windowView.originalHeight
                // Only unhide the WebView if no tool panel is currently shown.
                if (windowView.toolPanelView == null) {
                    windowView.webView.visibility = View.VISIBLE
                }
            }
            windowManager.updateViewLayout(windowView.view, windowView.layoutParams)
        }
    }

    private fun maximizeWindow(windowId: Long) {
        floatingWindows[windowId]?.let { windowView ->
            windowView.isMaximized = !windowView.isMaximized
            if (windowView.isMaximized) {
                // Save current position and size
                windowView.originalX = windowView.layoutParams.x
                windowView.originalY = windowView.layoutParams.y
                windowView.originalWidth = windowView.layoutParams.width
                windowView.originalHeight = windowView.layoutParams.height

                // Maximize to screen size
                val displayMetrics = resources.displayMetrics
                windowView.layoutParams.x = 0
                windowView.layoutParams.y = 0
                windowView.layoutParams.width = displayMetrics.widthPixels
                windowView.layoutParams.height = displayMetrics.heightPixels
            } else {
                // Restore previous size and position
                windowView.layoutParams.x = windowView.originalX
                windowView.layoutParams.y = windowView.originalY
                windowView.layoutParams.width = windowView.originalWidth
                windowView.layoutParams.height = windowView.originalHeight
            }
            windowManager.updateViewLayout(windowView.view, windowView.layoutParams)
        }
    }

    private fun closeAllWindows() {
        dismissActionPanel()
        dismissUrlEditor()
        floatingWindows.values.forEach { windowView ->
            windowManager.removeView(windowView.view)
        }
        floatingWindows.clear()
        currentFrontWindow = null
        updateOpenWindows()
        stopSelf()
    }

    private fun bringWindowToFront(windowId: Long) {
        floatingWindows[windowId]?.let { windowView ->
            try {
                windowManager.removeView(windowView.view)
                windowManager.addView(windowView.view, windowView.layoutParams)
                currentFrontWindow = windowId
            } catch (ignored: Exception) {
            }
        }
    }

    fun getCurrentWindows(): List<WindowEntity> {
        return floatingWindows.map { (id, windowView) ->
            WindowEntity(
                id = id,
                appName = windowView.appName,
                x = windowView.layoutParams.x,
                y = windowView.layoutParams.y,
                width = windowView.layoutParams.width,
                height = windowView.layoutParams.height,
                isMinimized = windowView.isMinimized
            )
        }
    }

    fun loadPreset(preset: WindowPresetEntity) {
        closeAllWindows()
        // TODO: Implement loading preset windows
    }

    override fun onDestroy() {
        dismissActionPanel()
        dismissUrlEditor()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun updateOpenWindows() {
        _openWindows.value = getCurrentWindows()
    }

    companion object {
        const val ACTION_ADD_WINDOW = "ADD_WINDOW"
        const val ACTION_REMOVE_WINDOW = "REMOVE_WINDOW"
        const val ACTION_CLOSE_ALL = "CLOSE_ALL"
        const val ACTION_MINIMIZE_WINDOW = "MINIMIZE_WINDOW"
        const val ACTION_MAXIMIZE_WINDOW = "MAXIMIZE_WINDOW"

        const val ACTION_TOOLBAR_BACK = 101
        const val ACTION_TOOLBAR_FORWARD = 102
        const val ACTION_TOOLBAR_HOME = 103
        const val ACTION_TOOLBAR_REFRESH = 104
        const val ACTION_TOOLBAR_DESKTOP = 105
        const val ACTION_TOOLBAR_ADBLOCK = 106
        const val ACTION_TOOLBAR_AUTOSCROLL = 107
        const val ACTION_TOOLBAR_AUTOCLICK = 108
        const val ACTION_TOOLBAR_OPEN_BROWSER = 109
        const val ACTION_TOOLBAR_SHARE = 110
        const val ACTION_TOOLBAR_COPY_URL = 111
        const val ACTION_TOOLBAR_CREDENTIALS = 112
        const val ACTION_TOOLBAR_CLIPBOARD = 113
        const val ACTION_TOOLBAR_DOWNLOADS = 114

        const val DESKTOP_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    }
}

data class FloatingWindowView(
    val view: View,
    val layoutParams: WindowManager.LayoutParams,
    val webView: WebView,
    val webViewClient: WebViewClientWithDownload,
    val appId: Long,
    var appName: String,
    var isMinimized: Boolean = false,
    var isMaximized: Boolean = false,
    var originalWidth: Int = 800,
    var originalHeight: Int = 600,
    var originalX: Int = 100,
    var originalY: Int = 100,
    var desktopModeEnabled: Boolean = false,
    var adblockEnabled: Boolean = false,
    var autoScrollEnabled: Boolean = false,
    var autoClickEnabled: Boolean = false,
    var originalUserAgent: String? = null,
    var toolPanelView: View? = null
)