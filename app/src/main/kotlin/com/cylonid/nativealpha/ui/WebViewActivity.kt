package com.cylonid.nativealpha.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CodeOff
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DoNotTouch
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cylonid.nativealpha.links.LinkHistoryTracker
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.ui.theme.BgDeep
import com.cylonid.nativealpha.ui.theme.BgDark
import com.cylonid.nativealpha.ui.theme.CardBorder
import com.cylonid.nativealpha.ui.theme.CardSurface
import com.cylonid.nativealpha.ui.theme.CyanPrimary
import com.cylonid.nativealpha.ui.theme.ErrorRed
import com.cylonid.nativealpha.ui.theme.GradCyanEnd
import com.cylonid.nativealpha.ui.theme.GradCyanStart
import com.cylonid.nativealpha.ui.theme.GradVioletEnd
import com.cylonid.nativealpha.ui.theme.GradVioletStart
import com.cylonid.nativealpha.ui.theme.StatusActive
import com.cylonid.nativealpha.ui.theme.TextMuted
import com.cylonid.nativealpha.ui.theme.TextPrimary
import com.cylonid.nativealpha.ui.theme.TextSecondary
import com.cylonid.nativealpha.ui.theme.VioletSecondary
import com.cylonid.nativealpha.util.ScreenshotUtil
import com.cylonid.nativealpha.viewmodel.ConsoleMessageData
import com.cylonid.nativealpha.viewmodel.WebViewViewModel
import com.cylonid.nativealpha.ui.DownloadHistoryActivity
import com.cylonid.nativealpha.waos.util.WaosConstants
import com.cylonid.nativealpha.webview.SessionManager
import com.cylonid.nativealpha.webview.WebViewClientWithDownload
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
open class WebViewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val webAppId = intent.getLongExtra("webAppId", 0L)

        // WAOS Session Isolation: apply separate WebView data directory BEFORE
        // setContent() so it takes effect before any WebView is instantiated.
        // This ensures cookies, localStorage, IndexedDB, cache are fully isolated
        // per app — no sharing across apps.
        SessionManager.applyIsolation(webAppId)

        // Apply or clear FLAG_SECURE based on the user's blockScreenshots setting.
        // By default (false) we explicitly clear the flag so the system can always
        // take screenshots of this activity. FLAG_SECURE is only applied when the
        // user has explicitly turned on the "Block Screenshots" security option.
        val securityPrefs = getSharedPreferences("security_prefs", MODE_PRIVATE)
        val blockScreenshots = securityPrefs.getBoolean("block_screenshots", false)
        if (blockScreenshots) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // Persistent cookies (incl. third-party, needed for OAuth logins).
        android.webkit.CookieManager.getInstance().setAcceptCookie(true)

        setContent {
            com.cylonid.nativealpha.ui.theme.WAOSTheme(
                themeMode = com.cylonid.nativealpha.ui.theme.ThemeState.mode
            ) {
                WebViewScreen(
                    webAppId = webAppId,
                    onBackPressed = { finish() }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Flush WebView cookies to disk so they survive a process kill (e.g.
        // user removing the app from Recents). Without this, in-memory cookies
        // are lost and the user appears logged out next time.
        try { android.webkit.CookieManager.getInstance().flush() } catch (_: Exception) {}
    }

    override fun onStop() {
        super.onStop()
        try { android.webkit.CookieManager.getInstance().flush() } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        WebViewFloatingOverlay.activeOverlay?.dismiss()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        WebViewFloatingOverlay.activeOverlay?.dismiss()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CREDENTIALS && resultCode == RESULT_OK && data != null) {
            val username = data.getStringExtra("CREDENTIAL_USERNAME")
            val password = data.getStringExtra("CREDENTIAL_PASSWORD")
            if (username != null && password != null) {
                pendingAutoFill = Pair(username, password)
            }
        }
    }

    companion object {
        const val REQUEST_CODE_CREDENTIALS = 4231
        var pendingAutoFill: Pair<String, String>? = null

        /** SharedPreferences-backed last-visited-URL store (per app, per process). */
        const val PREF_LAST_URL = "waos_last_url"
        fun lastUrlKey(appId: Long) = "app_${appId}_last_url"

        @JvmStatic
        fun saveLastVisitedUrl(context: Context, appId: Long, url: String) {
            if (appId <= 0L || url.isBlank()) return
            // Skip about:blank, data: URLs, and the like.
            if (url.startsWith("about:") || url.startsWith("data:")) return
            try {
                context.getSharedPreferences(PREF_LAST_URL, Context.MODE_PRIVATE)
                    .edit().putString(lastUrlKey(appId), url).apply()
            } catch (_: Exception) {}
        }

        @JvmStatic
        fun readLastVisitedUrl(context: Context, appId: Long): String? {
            return try {
                context.getSharedPreferences(PREF_LAST_URL, Context.MODE_PRIVATE)
                    .getString(lastUrlKey(appId), null)
            } catch (_: Exception) { null }
        }

        // ---------------------------------------------------------------
        // PERSISTENT WEBVIEW BACK/FORWARD HISTORY (per app, survives kill)
        // ---------------------------------------------------------------
        // Serialize the entire WebView back-forward stack (and form data, scroll
        // position, etc) to a per-app file via Parcel marshalling of the Bundle
        // returned by WebView.saveState. Restored before the first loadUrl so
        // pressing Back continues to work across process restarts.

        private fun historyFile(context: Context, appId: Long): java.io.File {
            val dir = java.io.File(context.filesDir, "waos_history")
            if (!dir.exists()) dir.mkdirs()
            return java.io.File(dir, "app_${appId}.bundle")
        }

        @JvmStatic
        fun saveWebViewHistory(context: Context, appId: Long, webView: WebView) {
            if (appId <= 0L) return
            val parcel = Parcel.obtain()
            try {
                val bundle = Bundle()
                webView.saveState(bundle)
                if (bundle.isEmpty) return
                bundle.writeToParcel(parcel, 0)
                val bytes = parcel.marshall()
                historyFile(context, appId).writeBytes(bytes)
            } catch (e: Exception) {
                Log.w("WebViewActivity", "saveWebViewHistory failed: ${e.message}")
            } finally {
                parcel.recycle()
            }
        }

        /** @return true if the WebView's back-forward list was restored. */
        @JvmStatic
        fun restoreWebViewHistory(context: Context, appId: Long, webView: WebView): Boolean {
            if (appId <= 0L) return false
            val file = historyFile(context, appId)
            if (!file.exists() || file.length() == 0L) return false
            val parcel = Parcel.obtain()
            return try {
                val bytes = file.readBytes()
                parcel.unmarshall(bytes, 0, bytes.size)
                parcel.setDataPosition(0)
                val bundle = Bundle.CREATOR.createFromParcel(parcel)
                bundle.classLoader = WebView::class.java.classLoader
                val restored = webView.restoreState(bundle)
                restored != null && restored.size > 0
            } catch (e: Exception) {
                Log.w("WebViewActivity", "restoreWebViewHistory failed: ${e.message}")
                try { file.delete() } catch (_: Exception) {}
                false
            } finally {
                parcel.recycle()
            }
        }

        @JvmStatic
        fun clearWebViewHistory(context: Context, appId: Long) {
            try { historyFile(context, appId).delete() } catch (_: Exception) {}
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    webAppId: Long,
    onBackPressed: () -> Unit,
    viewModel: WebViewViewModel = viewModel()
) {
    val context = LocalContext.current
    val webViewState by viewModel.webViewState.collectAsState()
    val consoleMessages by viewModel.consoleMessages.collectAsState()
    val showConsole by viewModel.showConsole.collectAsState()
    val isDesktopMode by viewModel.isDesktopMode.collectAsState()
    val isAdblockEnabled by viewModel.isAdblockEnabled.collectAsState()
    val isAutoScrollEnabled by viewModel.isAutoScrollEnabled.collectAsState()
    val isAutoClickEnabled by viewModel.isAutoClickEnabled.collectAsState()
    val webApp by viewModel.webApp.collectAsState()

    var showFindBar by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var findResultCount by remember { mutableStateOf(0) }
    val webViewRef = remember { mutableStateOf<android.webkit.WebView?>(null) }
    val webAppRef = remember { mutableStateOf<WebApp?>(null) }
    var initialUrlLoaded by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showPageHistory by remember { mutableStateOf(false) }
    var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    var showSessionPasteDialog by remember { mutableStateOf(false) }
    var sessionPasteText by remember { mutableStateOf("") }
    // Holds the WebChromeClient file-chooser callback while the system file picker is open
    val fileChooserCallbackState = remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    // SAF picker for Import Session (.waos files)
    val importSessionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val app = webApp
        if (app == null) {
            android.widget.Toast.makeText(context, "App not loaded yet", android.widget.Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        try {
            // Copy the picked file to a temp file so SessionManager can read by path.
            val cacheDir = java.io.File(context.cacheDir, "session_imports").apply { mkdirs() }
            val tempFile = java.io.File(cacheDir, "import_${System.currentTimeMillis()}.waos")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { input.copyTo(it) }
            }
            viewModel.requestSessionImport(tempFile.absolutePath, context)
            android.widget.Toast.makeText(context, "Importing session…", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Import failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    // File picker launched by WebChromeClient.onShowFileChooser (e.g. ChatGPT upload, Gmail attach)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        fileChooserCallbackState.value?.onReceiveValue(uris)
        fileChooserCallbackState.value = null
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val request = pendingPermissionRequest
        if (request != null) {
            val grantedResources = request.resources.orEmpty().filter { resource ->
                val permissionName = when (resource) {
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> Manifest.permission.CAMERA
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> Manifest.permission.RECORD_AUDIO
                    else -> null
                }
                permissionName != null && results[permissionName] == true
            }
            if (grantedResources.isNotEmpty()) {
                request.grant(grantedResources.toTypedArray())
                android.widget.Toast.makeText(context, "Permission granted", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                request.deny()
                android.widget.Toast.makeText(context, "Permission denied", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        pendingPermissionRequest = null
    }

    var pinUnlocked by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    val settingsPrefs = remember { context.getSharedPreferences("waos_settings", Context.MODE_PRIVATE) }
    val developerModeEnabled = remember { settingsPrefs.getBoolean("developer_mode", false) }
    val floatingWindowsEnabled = remember { settingsPrefs.getBoolean("floating_windows", true) }
    val autoScrollSpeed = remember { settingsPrefs.getInt("auto_scroll_speed", 3) }

    LaunchedEffect(webAppId) {
        viewModel.loadWebApp(webAppId)
    }

    // Resolve the URL to load: prefer the per-app last-visited URL stored in
    // SharedPreferences (process-safe, survives back-press / process kill), then
    // fall back to the value persisted in the database, then to the home URL.
    fun resolveLaunchUrl(): String? {
        val prefsUrl = WebViewActivity.readLastVisitedUrl(context, webAppId)
        return prefsUrl?.takeIf { it.isNotBlank() }
            ?: webApp?.lastVisitedUrl?.takeIf { it.isNotBlank() }
            ?: webApp?.url
    }

    // Re-trigger when WebApp loads OR when the WebView is finally created so we
    // never silently drop the very first loadUrl call. Attempts to restore the
    // entire back/forward stack from disk first; only loads the URL if no
    // history was restored.
    LaunchedEffect(webApp?.url, webViewRef.value) {
        val wv = webViewRef.value
        if (!initialUrlLoaded && wv != null &&
            (webApp?.isLocked == false || pinUnlocked)) {
            val restored = WebViewActivity.restoreWebViewHistory(context, webAppId, wv)
            if (!restored) {
                val urlToLoad = resolveLaunchUrl()
                if (!urlToLoad.isNullOrBlank()) {
                    wv.loadUrl(urlToLoad)
                }
            }
            initialUrlLoaded = true
        }
    }

    LaunchedEffect(pinUnlocked) {
        if (pinUnlocked) {
            val wv = webViewRef.value
            if (wv != null && !initialUrlLoaded) {
                val restored = WebViewActivity.restoreWebViewHistory(context, webAppId, wv)
                if (!restored) {
                    val urlToLoad = resolveLaunchUrl()
                    if (!urlToLoad.isNullOrBlank()) {
                        wv.loadUrl(urlToLoad)
                    }
                }
                initialUrlLoaded = true
            }
        }
    }

    // Pause/resume the WebView with the host activity, and flush cookies +
    // last-visited URL on dispose so logins and the current page survive a
    // process kill.
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            try {
                webViewRef.value?.let { wv ->
                    val current = wv.url
                    if (!current.isNullOrBlank()) {
                        WebViewActivity.saveLastVisitedUrl(context, webAppId, current)
                    }
                    // Persist the FULL back/forward stack so Back works after
                    // process kill / Recents swipe.
                    WebViewActivity.saveWebViewHistory(context, webAppId, wv)
                    wv.onPause()
                }
                android.webkit.CookieManager.getInstance().flush()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(webApp) {
        webApp?.let { app ->
            if (app.isLocked && !pinUnlocked) {
                showPinDialog = true
            }
            val window = (context as? ComponentActivity)?.window
            if (app.isKeepAwake) {
                window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    LaunchedEffect(Unit) {
        val autoFill = WebViewActivity.pendingAutoFill
        if (autoFill != null) {
            viewModel.autoFillCredentials(autoFill.first, autoFill.second)
            WebViewActivity.pendingAutoFill = null
        }
    }

    // Auto-copy the plain-JSON session payload to the system clipboard the
    // moment an export finishes. This is the primary transport — the user
    // can paste it into another WAOS app's Import Session dialog to clone
    // a logged-in session across apps without writing a file.
    LaunchedEffect(webViewState.lastSessionExportJson) {
        val json = webViewState.lastSessionExportJson
        if (!json.isNullOrBlank()) {
            try {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE)
                    as android.content.ClipboardManager
                val label = "WAOS Session - ${webApp?.name ?: "App"}"
                cm.setPrimaryClip(android.content.ClipData.newPlainText(label, json))
                android.widget.Toast.makeText(
                    context,
                    "Session JSON copied to clipboard. Paste it in another app's Import Session.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Log.w("SessionExport", "Clipboard copy failed: ${e.message}")
            }
            viewModel.clearSessionExportJson()
        }
    }

    // Surface a one-shot error toast when an import attempt fails (bad JSON,
    // unreadable file, etc.) so the user gets immediate feedback.
    LaunchedEffect(webViewState.sessionImportError) {
        val err = webViewState.sessionImportError
        if (!err.isNullOrBlank()) {
            android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearSessionImportError()
        }
    }

    // Show toast when session export finishes; also copy the encrypted backup
    // into the in-app downloader so the user can find it under Downloads.
    LaunchedEffect(webViewState.lastSessionExportPath) {
        val path = webViewState.lastSessionExportPath
        if (path != null) {
            val filename = path.substringAfterLast('/')
            val appName = webApp?.name ?: "App"
            // Copy export to the app-private WAOS/<App>/Sessions folder and
            // register in the in-app download history. The file lives in
            // app-private storage (invisible to Gallery / file manager) but
            // the app has full read/write access without permissions.
            val privatePath = try {
                val safeApp = appName.replace(Regex("[^a-zA-Z0-9]"), "_")
                val baseDir = java.io.File(context.getExternalFilesDir(null), "WAOS")
                val sessionsDir = java.io.File(java.io.File(baseDir, safeApp), "Sessions").apply { mkdirs() }
                val dest = java.io.File(sessionsDir, filename)
                java.io.File(path).copyTo(dest, overwrite = true)
                viewModel.registerSessionExportDownload(dest.absolutePath, filename)
                dest.absolutePath
            } catch (e: Exception) {
                Log.w("SessionExport", "Failed to copy session export: ${e.message}")
                null
            }
            android.widget.Toast.makeText(
                context,
                if (privatePath != null) "✓ Session saved → $filename"
                else "✓ Session backup created: $filename",
                android.widget.Toast.LENGTH_LONG
            ).show()
            Log.i("SessionExport", "Session exported for $appName to $path (private=$privatePath)")
            viewModel.clearSessionExportPath()
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false; onBackPressed() },
            containerColor = CardSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Brush.radialGradient(listOf(VioletSecondary.copy(0.3f), Color.Transparent)),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = VioletSecondary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("App Locked", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("Enter PIN to unlock ${webApp?.name ?: "this app"}", color = TextSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it; pinError = false },
                        label = { Text("PIN", color = TextMuted) },
                        isError = pinError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VioletSecondary,
                            unfocusedBorderColor = CardBorder,
                            cursorColor = VioletSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    if (pinError) {
                        Spacer(Modifier.height(6.dp))
                        Text("Incorrect PIN", color = ErrorRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput == webApp?.pin) {
                            pinUnlocked = true
                            showPinDialog = false
                            pinInput = ""
                        } else {
                            pinError = true
                        }
                    },
                    modifier = Modifier.background(
                        Brush.horizontalGradient(listOf(GradVioletStart, GradVioletEnd)),
                        RoundedCornerShape(8.dp)
                    )
                ) { Text("Unlock") }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false; onBackPressed() }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    webViewState.shouldShowImageLongPressDialog?.let { imageUrl ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissImageLongPressDialog() },
            containerColor = CardSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Brush.radialGradient(listOf(VioletSecondary.copy(0.3f), Color.Transparent)),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Photo, contentDescription = null, tint = VioletSecondary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Image Options", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("What would you like to do with this image?", color = TextSecondary, fontSize = 14.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        val wv = webViewRef.value
                        if (imageUrl.startsWith("data:")) {
                            // data: URL → decode locally.
                            viewModel.handleDownload(
                                url = imageUrl,
                                webApp = webApp,
                                userAgent = wv?.settings?.userAgentString,
                                referer = wv?.url
                            )
                        } else {
                            // Try fetching the image through the WebView first
                            // (gives us cookies + the exact origin context).
                            // If XHR fails (e.g. CORS), fall back to the system
                            // download manager — which also has cookies wired.
                            wv?.evaluateJavascript("""
                                (function() {
                                    function fallback() {
                                        if (window.waosDownloadBlob && window.waosDownloadBlob.linkLongPress) {
                                            window.waosDownloadBlob.__waosImageFallback && window.waosDownloadBlob.__waosImageFallback('$imageUrl');
                                        }
                                    }
                                    try {
                                        var xhr = new XMLHttpRequest();
                                        xhr.open('GET', '$imageUrl', true);
                                        xhr.responseType = 'blob';
                                        xhr.withCredentials = true;
                                        xhr.onload = function() {
                                            if ((xhr.status === 200 || xhr.status === 0) && xhr.response) {
                                                var b = xhr.response;
                                                var fr = new FileReader();
                                                fr.onloadend = function() {
                                                    var b64 = (fr.result || '').toString().split(',')[1] || '';
                                                    var name = ('$imageUrl'.split('/').pop() || '').split('?')[0];
                                                    if (window.waosDownloadBlob && window.waosDownloadBlob.downloadBlobWithMime) {
                                                        window.waosDownloadBlob.downloadBlobWithMime(name, b64, b.type || '');
                                                    }
                                                };
                                                fr.readAsDataURL(b);
                                            } else { fallback(); }
                                        };
                                        xhr.onerror = function() { fallback(); };
                                        xhr.send();
                                    } catch(e) { fallback(); }
                                })();
                            """.trimIndent(), null)
                        }
                        viewModel.dismissImageLongPressDialog()
                    },
                    modifier = Modifier.background(
                        Brush.horizontalGradient(listOf(GradVioletStart, GradVioletEnd)),
                        RoundedCornerShape(8.dp)
                    )
                ) { Text("Download") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        // Share image URL
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, imageUrl)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Image URL"))
                        viewModel.dismissImageLongPressDialog()
                    }) {
                        Text("Share URL", color = TextMuted)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { viewModel.dismissImageLongPressDialog() }) {
                        Text("Cancel", color = TextMuted)
                    }
                }
            }
        )
    }

    webViewState.shouldShowLinkLongPressDialog?.let { linkUrl ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissLinkLongPressDialog() },
            containerColor = CardSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Brush.radialGradient(listOf(CyanPrimary.copy(0.3f), Color.Transparent)),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Link Options", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("Link:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(linkUrl, color = CyanPrimary, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Link", linkUrl))
                        viewModel.captureClipboardItem(linkUrl, "link-longpress")
                        android.widget.Toast.makeText(context, "Link copied — saved to Clipboard Manager", android.widget.Toast.LENGTH_SHORT).show()
                        viewModel.dismissLinkLongPressDialog()
                    },
                    modifier = Modifier.background(
                        Brush.horizontalGradient(listOf(GradCyanStart, GradCyanEnd)),
                        RoundedCornerShape(8.dp)
                    )
                ) { Text("Copy Link") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl))
                        context.startActivity(intent)
                        viewModel.dismissLinkLongPressDialog()
                    }) { Text("Open in Browser", color = TextMuted) }
                    TextButton(onClick = { viewModel.dismissLinkLongPressDialog() }) {
                        Text("Cancel", color = TextMuted)
                    }
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    webViewState.selectedText?.let { selectedText ->
        androidx.compose.animation.AnimatedVisibility(
            visible = true,
            enter = androidx.compose.animation.slideInVertically { it } + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutVertically { it } + androidx.compose.animation.fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardSurface)
                        .border(1.dp, CyanPrimary.copy(0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ContentCopy, null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        selectedText.take(60) + if (selectedText.length > 60) "…" else "",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 2
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Text", selectedText))
                            viewModel.captureClipboardItem(selectedText, "selection")
                            android.widget.Toast.makeText(context, "Copied — saved to Clipboard Manager", android.widget.Toast.LENGTH_SHORT).show()
                            viewModel.clearSelectedText()
                        },
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = CyanPrimary)
                    ) { Text("Copy", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    IconButton(onClick = { viewModel.clearSelectedText() }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }

    LaunchedEffect(webViewState.shouldOpenCredentialKeeper) {
        if (webViewState.shouldOpenCredentialKeeper) {
            val intent = Intent(context, CredentialVaultActivity::class.java).apply {
                putExtra(WaosConstants.EXTRA_WAOS_APP_ID, webAppId)
            }
            (context as? ComponentActivity)?.startActivityForResult(
                intent,
                WebViewActivity.REQUEST_CODE_CREDENTIALS
            )
            viewModel.clearActionFlags()
        }
    }

    LaunchedEffect(webViewState.shouldOpenClipboardManager) {
        if (webViewState.shouldOpenClipboardManager) {
            val intent = Intent(context, ClipboardManagerActivity::class.java).apply {
                putExtra(WaosConstants.EXTRA_CLIPBOARD_APP_ID, webAppId)
            }
            context.startActivity(intent)
            viewModel.clearActionFlags()
        }
    }

    LaunchedEffect(webViewState.shouldOpenDownloadHistory) {
        if (webViewState.shouldOpenDownloadHistory) {
            val intent = Intent(context, DownloadHistoryActivity::class.java).apply {
                putExtra(WaosConstants.EXTRA_DOWNLOAD_APP_ID, webAppId)
                putExtra("APP_DISPLAY_NAME", webApp?.name ?: "App")
            }
            context.startActivity(intent)
            viewModel.clearActionFlags()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .statusBarsPadding()
    ) {
        val urlEditScope = rememberCoroutineScope()
        WaosTopBar(
            title = webApp?.name ?: "Web App",
            url = webViewState.currentUrl,
            isLoading = webViewState.isLoading,
            progress = webViewState.progress,
            onBack = onBackPressed,
            onEditUrl = { newUrl ->
                val wv = webViewRef.value ?: return@WaosTopBar
                val previous = wv.url
                wv.loadUrl(newUrl)
                urlEditScope.launch {
                    try {
                        LinkHistoryTracker(context.applicationContext, webAppId).recordAction(
                            url = newUrl,
                            pageTitle = webApp?.name ?: "",
                            action = "manual_navigate",
                            format = "PLAIN_URL",
                            referrer = previous
                        )
                    } catch (_: Exception) {
                    }
                }
            }
        )

        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = { ctx ->
                    val webView = WebView(ctx)
                    webViewRef.value = webView
                    webView.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    webView.settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        builtInZoomControls = true
                        displayZoomControls = false
                        setSupportZoom(true)
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        mediaPlaybackRequiresUserGesture = false
                        // Enable persistent cache so service workers / asset caches
                        // are reused after process restart.
                        cacheMode = WebSettings.LOAD_DEFAULT
                        // Strip Android WebView markers ("; wv)" and "Version/4.0 ")
                        // from the default UA so Cloudflare and other bot-detection
                        // services treat this as a normal Chrome browser, not a WebView.
                        val rawUA = userAgentString ?: ""
                        userAgentString = rawUA
                            .replace("; wv)", ")")
                            .replace("; wv;", ";")
                            .replace("Version/4.0 ", "")
                    }
                    // Cookies (incl. 3rd-party for OAuth flows like Google sign-in).
                    android.webkit.CookieManager.getInstance().setAcceptCookie(true)
                    android.webkit.CookieManager.getInstance()
                        .setAcceptThirdPartyCookies(webView, true)
                    webView.webViewClient = WebViewClientWithDownload(
                        context = ctx,
                        onPageStarted = { url -> viewModel.onPageStarted(url) },
                        onPageFinished = { url ->
                            viewModel.onPageFinished(url)
                            // Persist the current page in SharedPreferences so it
                            // survives back-press and process kill, and flush
                            // cookies right away so logins survive a Recents swipe.
                            WebViewActivity.saveLastVisitedUrl(ctx, webAppId, url)
                            // Save back/forward stack on every page load so we
                            // never lose history if the process is killed.
                            try {
                                WebViewActivity.saveWebViewHistory(ctx, webAppId, webView)
                            } catch (_: Exception) {}
                            // Persist a website favicon on first successful load
                            // so the dashboard card shows the site icon.
                            viewModel.updateFaviconIfNeeded()
                            try { android.webkit.CookieManager.getInstance().flush() } catch (_: Exception) {}
                            webAppRef.value?.let { app ->
                                if (app.isDarkModeEnabled) injectDarkMode(webView)
                            }
                            // Inject the clipboard capture listener. This
                            // mirrors Chrome by hooking every copy/cut path:
                            // the native copy/cut events, the modern
                            // navigator.clipboard.writeText/.write APIs, and
                            // the legacy document.execCommand('copy') call.
                            // Anything copied from inside the page lands in
                            // the in-app clipboard manager.
                            webViewRef.value?.evaluateJavascript("""
                                javascript:(function() {
                                    if (window.__waosClipInstalled) return;
                                    window.__waosClipInstalled = true;
                                    function send(text, hint) {
                                        try {
                                            if (!text) return;
                                            var s = (''+text);
                                            if (s.length > 50000) s = s.substring(0, 50000);
                                            if (window.waosClipboard && window.waosClipboard.captured) {
                                                window.waosClipboard.captured(s, hint || '');
                                            }
                                        } catch(e) {}
                                    }
                                    document.addEventListener('copy', function(e) {
                                        try {
                                            var t = '';
                                            if (e.clipboardData) t = e.clipboardData.getData('text/plain') || '';
                                            if (!t && window.getSelection) t = window.getSelection().toString();
                                            send(t, '');
                                        } catch(e) {}
                                    }, true);
                                    document.addEventListener('cut', function(e) {
                                        try {
                                            var t = '';
                                            if (e.clipboardData) t = e.clipboardData.getData('text/plain') || '';
                                            if (!t && window.getSelection) t = window.getSelection().toString();
                                            send(t, '');
                                        } catch(e) {}
                                    }, true);
                                    try {
                                        if (navigator.clipboard && navigator.clipboard.writeText) {
                                            var origWriteText = navigator.clipboard.writeText.bind(navigator.clipboard);
                                            navigator.clipboard.writeText = function(text) {
                                                send(text, '');
                                                return origWriteText(text);
                                            };
                                        }
                                        if (navigator.clipboard && navigator.clipboard.write) {
                                            var origWrite = navigator.clipboard.write.bind(navigator.clipboard);
                                            navigator.clipboard.write = function(items) {
                                                try {
                                                    if (items && items.length) {
                                                        items.forEach(function(it) {
                                                            if (it && it.types) {
                                                                if (it.types.indexOf('text/plain') >= 0 && it.getType) {
                                                                    it.getType('text/plain').then(function(b) {
                                                                        b.text().then(function(t) { send(t, ''); });
                                                                    });
                                                                } else if (it.types.indexOf('image/png') >= 0 || it.types.indexOf('image/jpeg') >= 0) {
                                                                    send('Image copied', 'image');
                                                                }
                                                            }
                                                        });
                                                    }
                                                } catch(e) {}
                                                return origWrite(items);
                                            };
                                        }
                                    } catch(e) {}
                                    try {
                                        var origExec = document.execCommand;
                                        document.execCommand = function(cmd) {
                                            var r = origExec.apply(document, arguments);
                                            try {
                                                if (cmd && (cmd.toLowerCase() === 'copy' || cmd.toLowerCase() === 'cut')) {
                                                    var t = window.getSelection ? window.getSelection().toString() : '';
                                                    if (!t) {
                                                        var el = document.activeElement;
                                                        if (el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') && el.value) {
                                                            t = el.value.substring(el.selectionStart || 0, el.selectionEnd || el.value.length);
                                                        }
                                                    }
                                                    send(t, '');
                                                }
                                            } catch(e) {}
                                            return r;
                                        };
                                    } catch(e) {}
                                })()
                            """.trimIndent(), null)
                            // Inject auto scroll functionality
                            webViewRef.value?.evaluateJavascript("""
                                javascript:(function() {
                                    window.waosAutoScroll = {
                                        interval: null,
                                        start: function(speed) {
                                            this.stop();
                                            this.interval = setInterval(function() {
                                                    window.scrollBy(0, speed);
                                                }, 50);
                                            },
                                            stop: function() {
                                                if (this.interval) {
                                                    clearInterval(this.interval);
                                                    this.interval = null;
                                                }
                                            }
                                        };
                                    })()
                                """.trimIndent(), null)
                                // Inject image long press functionality
                                webViewRef.value?.evaluateJavascript("""
                                    javascript:(function() {
                                        var longPressTimer;
                                        var longPressElement;
                                        
                                        function handleLongPress(element) {
                                            if (element.tagName === 'IMG') {
                                                var src = element.src;
                                                if (src) {
                                                    window.waosDownloadBlob && window.waosDownloadBlob.imageLongPress(src);
                                                }
                                            }
                                        }
                                        
                                        document.addEventListener('touchstart', function(e) {
                                            longPressTimer = setTimeout(function() {
                                                handleLongPress(e.target);
                                            }, 500);
                                        });
                                        
                                        document.addEventListener('touchend', function(e) {
                                            clearTimeout(longPressTimer);
                                        });
                                        
                                        document.addEventListener('touchmove', function(e) {
                                            clearTimeout(longPressTimer);
                                        });
                                    })()
                                """.trimIndent(), null)
                                // Inject link long press functionality
                                webViewRef.value?.evaluateJavascript("""
                                    javascript:(function() {
                                        if (window._waosLinkLongPressAttached) return;
                                        window._waosLinkLongPressAttached = true;
                                        var linkTimer;
                                        var touchMoved = false;
                                        document.addEventListener('touchstart', function(e) {
                                            touchMoved = false;
                                            var el = e.target;
                                            while (el && el.tagName !== 'A') el = el.parentElement;
                                            if (el && el.href) {
                                                var href = el.href;
                                                linkTimer = setTimeout(function() {
                                                    if (!touchMoved && window.waosDownloadBlob) {
                                                        window.waosDownloadBlob.linkLongPress(href);
                                                    }
                                                }, 600);
                                            }
                                        }, true);
                                        document.addEventListener('touchend', function() { clearTimeout(linkTimer); }, true);
                                        document.addEventListener('touchmove', function() { touchMoved = true; clearTimeout(linkTimer); }, true);
                                    })()
                                """.trimIndent(), null)
                                // Inject text selection callback
                                webViewRef.value?.evaluateJavascript("""
                                    javascript:(function() {
                                        if (window._waosTextSelAttached) return;
                                        window._waosTextSelAttached = true;
                                        var selTimer;
                                        document.addEventListener('selectionchange', function() {
                                            clearTimeout(selTimer);
                                            selTimer = setTimeout(function() {
                                                var sel = window.getSelection ? window.getSelection().toString().trim() : '';
                                                if (sel.length > 0 && window.waosDownloadBlob) {
                                                    window.waosDownloadBlob.textSelected(sel);
                                                }
                                            }, 300);
                                        });
                                    })()
                                """.trimIndent(), null)
                            },
                            onDownloadStart = { filename, downloadUrl ->
                                if (downloadUrl.startsWith("blob:")) {
                                    // Handle blob URL download
                                    webViewRef.value?.evaluateJavascript("""
                                        (function() {
                                            fetch('$downloadUrl')
                                                .then(response => response.blob())
                                                .then(blob => {
                                                    const reader = new FileReader();
                                                    reader.onload = function() {
                                                        const base64 = reader.result.split(',')[1];
                                                        if (window.waosDownloadBlob && window.waosDownloadBlob.downloadBlob) {
                                                            window.waosDownloadBlob.downloadBlob('$filename', base64);
                                                        }
                                                    };
                                                    reader.readAsDataURL(blob);
                                                })
                                                .catch(error => console.error('Blob download failed:', error));
                                        })();
                                    """.trimIndent(), null)
                                } else {
                                    viewModel.handleDownload(downloadUrl, webAppRef.value)
                                }
                            }
                        )
                    webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                        when {
                            url.startsWith("blob:") -> {
                                // Pull the blob into JS, base64-encode it, and
                                // pipe back through the JS bridge.
                                webViewRef.value?.evaluateJavascript("""
                                    (function() {
                                        try {
                                            var xhr = new XMLHttpRequest();
                                            xhr.open('GET', '$url', true);
                                            xhr.responseType = 'blob';
                                            xhr.onload = function() {
                                                if (xhr.status === 200 || xhr.status === 0) {
                                                    var blob = xhr.response;
                                                    var reader = new FileReader();
                                                    reader.onloadend = function() {
                                                        var b64 = (reader.result || '').toString().split(',')[1] || '';
                                                        var name = (blob && blob.name) ? blob.name : '';
                                                        var mime = (blob && blob.type) ? blob.type : '${mimeType ?: ""}';
                                                        if (window.waosDownloadBlob && window.waosDownloadBlob.downloadBlobWithMime) {
                                                            window.waosDownloadBlob.downloadBlobWithMime(name, b64, mime);
                                                        } else if (window.waosDownloadBlob && window.waosDownloadBlob.downloadBlob) {
                                                            window.waosDownloadBlob.downloadBlob(name, b64);
                                                        }
                                                    };
                                                    reader.readAsDataURL(blob);
                                                }
                                            };
                                            xhr.send();
                                        } catch(e) { console.error('blob fetch failed', e); }
                                    })();
                                """.trimIndent(), null)
                            }
                            else -> {
                                viewModel.handleDownload(
                                    url = url,
                                    webApp = webAppRef.value,
                                    userAgent = userAgent,
                                    contentDisposition = contentDisposition,
                                    mimeType = mimeType,
                                    referer = webViewRef.value?.url
                                )
                            }
                        }
                    }
                    webView.webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(msg: ConsoleMessage?): Boolean {
                                msg?.let {
                                    viewModel.addConsoleMessage(
                                        ConsoleMessageData(
                                            message = it.message(),
                                            level = it.messageLevel().ordinal,
                                            sourceId = it.sourceId() ?: "",
                                            lineNumber = it.lineNumber()
                                        )
                                    )
                                }
                                return super.onConsoleMessage(msg)
                            }
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                viewModel.onProgressChanged(newProgress)
                            }
                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: WebChromeClient.FileChooserParams?
                            ): Boolean {
                                // Cancel any stale pending callback first
                                fileChooserCallbackState.value?.onReceiveValue(null)
                                fileChooserCallbackState.value = filePathCallback
                                return try {
                                    val intent = fileChooserParams?.createIntent()
                                        ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                            type = "*/*"
                                            addCategory(Intent.CATEGORY_OPENABLE)
                                        }
                                    filePickerLauncher.launch(intent)
                                    true
                                } catch (e: Exception) {
                                    fileChooserCallbackState.value = null
                                    filePathCallback?.onReceiveValue(null)
                                    false
                                }
                            }
                            override fun onPermissionRequest(request: PermissionRequest?) {
                                val app = webAppRef.value
                                val requestedResources = request?.resources?.filter { resource ->
                                    when (resource) {
                                        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> app?.isCameraPermission == true
                                        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> app?.isMicrophonePermission == true
                                        else -> false
                                    }
                                } ?: emptyList()

                                if (requestedResources.isEmpty()) {
                                    request?.deny()
                                    return
                                }

                                val androidPermissions = requestedResources.mapNotNull { resource ->
                                    when (resource) {
                                        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> Manifest.permission.CAMERA
                                        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> Manifest.permission.RECORD_AUDIO
                                        else -> null
                                    }
                                }

                                val alreadyGranted = androidPermissions.all { permission ->
                                    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                                }

                                if (alreadyGranted) {
                                    request?.grant(requestedResources.toTypedArray())
                                    return
                                }

                                pendingPermissionRequest = request
                                permissionLauncher.launch(androidPermissions.toTypedArray())
                            }
                        }
                    webView.addJavascriptInterface(object {
                        @JavascriptInterface
                        fun downloadBlob(filename: String, base64Data: String) {
                            viewModel.handleBlobDownload(filename, base64Data, null, webAppRef.value)
                        }
                        @JavascriptInterface
                        fun downloadBlobWithMime(filename: String, base64Data: String, mime: String?) {
                            viewModel.handleBlobDownload(filename, base64Data, mime, webAppRef.value)
                        }
                        @JavascriptInterface
                        fun imageLongPress(imageUrl: String) {
                            viewModel.handleImageLongPress(imageUrl, webAppRef.value)
                        }
                        @JavascriptInterface
                        fun linkLongPress(url: String) {
                            if (url.isNotBlank()) {
                                viewModel.handleLinkLongPress(url)
                            }
                        }
                        @JavascriptInterface
                        fun textSelected(text: String) {
                            if (text.isNotBlank()) {
                                viewModel.handleTextSelected(text)
                            }
                        }
                        @JavascriptInterface
                        fun __waosImageFallback(url: String) {
                            // Called from JS when the in-page XHR for an image
                            // fails (CORS, etc). Drops back to the system
                            // download path which already forwards cookies/UA.
                            if (url.isBlank()) return
                            val wv = webViewRef.value
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                viewModel.handleDownload(
                                    url = url,
                                    webApp = webAppRef.value,
                                    userAgent = wv?.settings?.userAgentString,
                                    referer = wv?.url
                                )
                            }
                        }
                    }, "waosDownloadBlob")

                    // Dedicated bridge for the page-side clipboard listener.
                    // Receives every copy/cut intercepted by the injected JS
                    // (native events + navigator.clipboard + execCommand) and
                    // hands it to the viewmodel for de-dup + persistence.
                    webView.addJavascriptInterface(object {
                        @JavascriptInterface
                        fun captured(text: String, hint: String?) {
                            if (text.isBlank()) return
                            viewModel.captureClipboardItem(text, hint)
                        }
                    }, "waosClipboard")

                    // Inject anti-bot-detection script before ANY page script runs.
                    // Cloudflare TurnstileBot silently fails (reverts to un-ticked state)
                    // when it detects these WebView-specific signals:
                    //   1. navigator.webdriver = true  → most important signal
                    //   2. window.chrome absent        → Cloudflare gates on this
                    //   3. navigator.plugins empty     → headless/WebView giveaway
                    //   4. navigator.languages wrong   → fingerprinting signal
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

                    webView
                },
                update = { webView ->
                    webAppRef.value = webApp
                    (webView.webViewClient as? WebViewClientWithDownload)?.adblockEnabled = isAdblockEnabled
                    webApp?.let { app ->
                        webView.settings.javaScriptEnabled = app.isJavaScriptEnabled
                        webView.settings.builtInZoomControls = app.isEnableZooming
                        webView.settings.setSupportZoom(true)
                        webView.settings.displayZoomControls = false
                        if (!app.userAgent.isNullOrBlank() && !isDesktopMode) {
                            webView.settings.userAgentString = app.userAgent
                        }
                    }
                    if (isDesktopMode) {
                        webView.settings.userAgentString =
                            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                        webView.settings.useWideViewPort = true
                        webView.settings.loadWithOverviewMode = true
                        // Inject desktop mode enhancements
                        webView.evaluateJavascript("""
                            javascript:(function() {
                                var meta = document.querySelector('meta[name=viewport]');
                                if (!meta) {
                                    meta = document.createElement('meta');
                                    meta.name = 'viewport';
                                    document.head.appendChild(meta);
                                }
                                meta.content = 'width=device-width, initial-scale=0.5, maximum-scale=3.0, user-scalable=yes';
                                // Force desktop layout without breaking the page
                                var style = document.createElement('style');
                                style.textContent = 'body { min-width: 100vw !important; overflow-x: auto; } @media (max-width: 768px) { body { font-size: 14px; } }';
                                document.head.appendChild(style);
                            })()
                        """.trimIndent(), null)
                    } else {
                        webView.settings.useWideViewPort = true
                        webView.settings.loadWithOverviewMode = true
                    }
                    if (webViewState.shouldGoBack) { webView.goBack(); viewModel.clearActionFlags() }
                    if (webViewState.shouldGoForward) { webView.goForward(); viewModel.clearActionFlags() }
                    webViewState.shouldLoadUrl?.let { url -> webView.loadUrl(url); viewModel.clearActionFlags() }
                    if (webViewState.shouldReload) { webView.reload(); viewModel.clearActionFlags() }
                    if (webViewState.shouldRefresh) { webView.reload(); viewModel.clearRefreshFlag() }
                    webViewState.javaScriptToExecute?.let { js ->
                        webView.evaluateJavascript(js, null)
                        viewModel.clearJavaScriptCommand()
                    }
                    viewModel.updateNavigationState(webView.canGoBack(), webView.canGoForward())
                    webViewState.shouldShareUrl?.let { urlToShare ->
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, urlToShare)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share URL"))
                        viewModel.clearActionFlags()
                    }
                    webViewState.shouldCopyUrl?.let { urlToCopy ->
                        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("URL", urlToCopy))
                        viewModel.captureClipboardItem(urlToCopy, "address-bar")
                        android.widget.Toast.makeText(context, "URL copied — saved to Clipboard Manager", android.widget.Toast.LENGTH_SHORT).show()
                        viewModel.clearActionFlags()
                    }
                    if (webViewState.shouldPrint) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            val pm = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                            pm.print("WAOS Document", webView.createPrintDocumentAdapter("WAOS Print"), null)
                        }
                        viewModel.clearActionFlags()
                    }
                    if (webViewState.shouldZoomIn) { webView.zoomIn(); viewModel.clearActionFlags() }
                    if (webViewState.shouldZoomOut) { webView.zoomOut(); viewModel.clearActionFlags() }
                    if (webViewState.shouldTakeScreenshot) {
                        val bitmap = Bitmap.createBitmap(webView.width, webView.height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        webView.draw(canvas)
                        val appName = webApp?.name?.replace(Regex("[^a-zA-Z0-9]"), "_") ?: "App"
                        val filePath = ScreenshotUtil.saveScreenshot(context, appName, bitmap)
                        if (filePath != null) {
                            android.widget.Toast.makeText(context, "Screenshot saved to Downloads/WAOS/$appName/Screenshots/", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, "Failed to save screenshot", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        viewModel.clearActionFlags()
                    }
                    if (webViewState.shouldShowPageSource) {
                        webView.loadUrl("view-source:" + webView.url)
                        viewModel.clearActionFlags()
                    }
                    if (webViewState.shouldSavePage) {
                        val filename = "page_${System.currentTimeMillis()}.mht"
                        webView.saveWebArchive(filename)
                        viewModel.clearActionFlags()
                    }
                    // Reader mode, translate, adblock would need additional implementation
                    if (webViewState.shouldToggleReaderMode) {
                        viewModel.clearActionFlags()
                    }
                    if (webViewState.shouldTranslate) {
                        viewModel.clearActionFlags()
                    }
                    if (webViewState.shouldToggleAdblock) {
                        viewModel.clearActionFlags()
                    }
                    // WAOS Session Export: extract localStorage + sessionStorage via JS then save encrypted snapshot
                    if (webViewState.shouldExportSession) {
                        val currentUrl = webViewState.currentUrl.ifBlank { webApp?.url ?: "" }
                        val ua = webView.settings.userAgentString ?: ""
                        val app = webApp
                        if (app != null && currentUrl.isNotBlank()) {
                            webView.evaluateJavascript(SessionManager.buildLocalStorageExtractJs()) { lsJson ->
                                webView.evaluateJavascript(SessionManager.buildSessionStorageExtractJs()) { ssJson ->
                                    val sessionMgr = SessionManager(context, app.id, app.name)
                                    // Pass the raw evaluateJavascript results — the SessionManager
                                    // safely unwraps WebView's JSON-encoded string layer (the old
                                    // manual trim+replace mangled values that contained their own
                                    // quoted JSON, e.g. marketing_attribution).
                                    val snapshot = sessionMgr.buildSessionSnapshot(
                                        currentUrl,
                                        ua,
                                        lsJson ?: "{}",
                                        ssJson ?: "{}"
                                    )
                                    sessionMgr.saveLastSessionSnapshot(snapshot)
                                    val path = sessionMgr.exportSession(snapshot)
                                    val jsonString = sessionMgr.exportSessionAsJsonString(snapshot)
                                    viewModel.onSessionExportComplete(path, jsonString)
                                }
                            }
                        } else {
                            viewModel.onSessionExportComplete(null, null)
                        }
                    }
                    // WAOS Session Import: inject localStorage + sessionStorage restore JS after page load
                    webViewState.shouldImportSession?.let { restoreData ->
                        if (!webViewState.isLoading) {
                            webView.evaluateJavascript(restoreData.localStorageRestoreJs, null)
                            webView.evaluateJavascript(restoreData.sessionStorageRestoreJs, null)
                            viewModel.onSessionImportApplied()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (webViewState.isLoading && webViewState.progress < 100) {
                LinearProgressIndicator(
                    progress = { webViewState.progress / 100f },
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopStart),
                    color = CyanPrimary,
                    trackColor = CyanPrimary.copy(alpha = 0.15f)
                )
            }

            Column(modifier = Modifier.align(Alignment.TopCenter)) {
                AnimatedVisibility(
                    visible = showFindBar,
                    enter = slideInVertically { height -> -height } + fadeIn(),
                    exit = slideOutVertically { height -> -height } + fadeOut()
                ) {
                    WaosFindBar(
                        query = findQuery,
                        onQueryChange = { q ->
                            findQuery = q
                            webViewRef.value?.findAllAsync(q)
                        },
                        onFindNext = { webViewRef.value?.findNext(true) },
                        onFindPrev = { webViewRef.value?.findNext(false) },
                        onClose = {
                            showFindBar = false
                            findQuery = ""
                            webViewRef.value?.clearMatches()
                        }
                    )
                }
            }

            webViewState.error?.let { error ->
                WaosErrorPanel(error = error, onRetry = { viewModel.refresh() })
            }
        }

        if (showConsole) {
            WaosConsolePanel(
                messages = consoleMessages,
                onExecuteCommand = { viewModel.executeJavaScript(it) },
                onClearLogs = { viewModel.clearConsoleMessages() },
                modifier = Modifier.height(280.dp)
            )
        }

        WaosBottomBar(
            canGoBack = webViewState.canGoBack,
            canGoForward = webViewState.canGoForward,
            isDesktopMode = isDesktopMode,
            isAdblockEnabled = isAdblockEnabled,
            isAutoScrollEnabled = isAutoScrollEnabled,
            isAutoClickEnabled = isAutoClickEnabled,
            developerModeEnabled = developerModeEnabled,
            showConsole = showConsole,
            floatingWindowsEnabled = floatingWindowsEnabled,
            showMoreMenu = showMoreMenu,
            onMoreMenuToggle = { showMoreMenu = !showMoreMenu },
            onMoreMenuDismiss = { showMoreMenu = false },
            onGoBack = { viewModel.goBack() },
            onGoForward = { viewModel.goForward() },
            onHome = { viewModel.goHome() },
            onRefresh = { viewModel.refresh() },
            onFind = { showFindBar = !showFindBar },
            onDesktop = { viewModel.toggleDesktopMode() },
            onAdblock = { viewModel.toggleAdblock() },
            onAutoScroll = { viewModel.toggleAutoScroll(autoScrollSpeed) },
            onAutoClick = { viewModel.toggleAutoClick() },
            onConsole = { viewModel.toggleConsole() },
            onShare = { viewModel.sharePage() },
            onCopyUrl = { viewModel.copyUrl() },
            onOpenBrowser = {
                val currentUrl = webViewState.currentUrl.ifBlank { webApp?.url ?: return@WaosBottomBar }
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)))
            },
            onFloat = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(context)) {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                    context.startActivity(intent)
                } else {
                    webApp?.let { app ->
                        val currentUrl = webViewRef.value?.url
                            ?.takeIf { it.isNotBlank() && !it.startsWith("about:") }
                            ?: WebViewActivity.readLastVisitedUrl(context, webAppId)
                            ?: app.url
                        WebViewActivity.saveLastVisitedUrl(context, webAppId, currentUrl)
                        val intent = Intent(context, com.cylonid.nativealpha.service.FloatingWindowService::class.java).apply {
                            action = com.cylonid.nativealpha.service.FloatingWindowService.ACTION_ADD_WINDOW
                            putExtra("webAppId", app.id)
                            putExtra("webAppUrl", currentUrl)
                            putExtra("webAppName", app.name)
                        }
                        context.startService(intent)
                    }
                }
            },
            onCredentials = { viewModel.openCredentialKeeper() },
            onClipboard = { viewModel.openClipboardManager() },
            onDownloads = { viewModel.openDownloadHistory() },
            onScreenshot = { viewModel.takeScreenshot() },
            onZoomIn = { viewModel.zoomIn() },
            onZoomOut = { viewModel.zoomOut() },
            onPrint = { viewModel.printPage() },
            onHistory = { showPageHistory = true },
            onPageSource = { viewModel.showPageSource() },
            onSavePage = { viewModel.savePage() },
            onReaderMode = { viewModel.toggleReaderMode() },
            onTranslate = { viewModel.translate() },
            onAdBlockSettings = { viewModel.toggleAdblockSettings() },
            onExportSession = { viewModel.requestSessionExport() },
            onImportSession = {
                // Pre-fill the paste dialog with whatever is on the clipboard
                // so a typical "copy from app A → import in app B" flow is
                // a single tap.
                sessionPasteText = try {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE)
                        as android.content.ClipboardManager
                    cm.primaryClip?.takeIf { it.itemCount > 0 }
                        ?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                } catch (_: Exception) { "" }
                showSessionPasteDialog = true
            }
        )
    }

    if (showSessionPasteDialog) {
        AlertDialog(
            onDismissRequest = {
                showSessionPasteDialog = false
                sessionPasteText = ""
            },
            containerColor = CardSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Import Session") },
            text = {
                Column {
                    Text(
                        "Paste the session JSON copied from another WAOS app, " +
                                "or pick a saved .waos file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = sessionPasteText,
                        onValueChange = { sessionPasteText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 280.dp),
                        placeholder = { Text("{\"appId\":..., \"cookies\":[...], ...}") },
                        singleLine = false,
                        maxLines = 12
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        try {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                as android.content.ClipboardManager
                            sessionPasteText = cm.primaryClip
                                ?.takeIf { it.itemCount > 0 }
                                ?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                        } catch (_: Exception) {}
                    }) { Text("Paste from clipboard") }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val text = sessionPasteText
                        showSessionPasteDialog = false
                        sessionPasteText = ""
                        if (text.isBlank()) {
                            android.widget.Toast.makeText(
                                context,
                                "Nothing to import — paste the session JSON first.",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            viewModel.requestSessionImportFromText(text, context)
                        }
                    }
                ) { Text("Import") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showSessionPasteDialog = false
                        sessionPasteText = ""
                        try {
                            importSessionLauncher.launch(arrayOf("*/*"))
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(
                                context,
                                "Could not open file picker: ${e.message}",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }) { Text("Pick file…") }
                    TextButton(onClick = {
                        showSessionPasteDialog = false
                        sessionPasteText = ""
                    }) { Text("Cancel") }
                }
            }
        )
    }

    if (showPageHistory) {
        WaosPageHistoryDialog(
            webView = webViewRef.value,
            onDismiss = { showPageHistory = false },
            onNavigate = { steps ->
                webViewRef.value?.let { wv ->
                    if (steps != 0 && wv.canGoBackOrForward(steps)) {
                        wv.goBackOrForward(steps)
                    }
                }
                showPageHistory = false
            },
            onClearHistory = {
                webViewRef.value?.clearHistory()
                WebViewActivity.clearWebViewHistory(context, webAppId)
                showPageHistory = false
                android.widget.Toast.makeText(context, "History cleared", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaosPageHistoryDialog(
    webView: android.webkit.WebView?,
    onDismiss: () -> Unit,
    onNavigate: (Int) -> Unit,
    onClearHistory: () -> Unit
) {
    val list = remember(webView) { webView?.copyBackForwardList() }
    val current = list?.currentIndex ?: -1
    val total = list?.size ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, null, tint = CyanPrimary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Page History", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            if (total == 0) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No browsing history yet", color = TextMuted, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(total) { idx ->
                        // Show newest entries first.
                        val realIdx = total - 1 - idx
                        val item = list!!.getItemAtIndex(realIdx)
                        val isCurrent = realIdx == current
                        val steps = realIdx - current
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCurrent) CyanPrimary.copy(0.12f) else CardSurface)
                                .border(
                                    1.dp,
                                    if (isCurrent) CyanPrimary.copy(0.5f) else CardBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        if (isCurrent) CyanPrimary.copy(0.25f) else CardBorder.copy(0.3f),
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${realIdx + 1}",
                                    color = if (isCurrent) CyanPrimary else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.title?.takeIf { it.isNotBlank() } ?: "(untitled page)",
                                    color = if (isCurrent) CyanPrimary else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    item.url ?: "",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (!isCurrent) {
                                TextButton(onClick = { onNavigate(steps) }) {
                                    Text(
                                        if (steps < 0) "← Back" else "Forward →",
                                        color = CyanPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                Text(
                                    "current",
                                    color = StatusActive,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        },
        dismissButton = {
            if (total > 0) {
                TextButton(onClick = onClearHistory) {
                    Text("Clear History", color = ErrorRed)
                }
            }
        }
    )
}

@Composable
private fun WaosTopBar(
    title: String,
    url: String,
    isLoading: Boolean,
    progress: Int,
    onBack: () -> Unit,
    onEditUrl: (String) -> Unit = {}
) {
    var showEditor by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgDark)
            .border(width = 1.dp, color = CardBorder, shape = RoundedCornerShape(0.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardSurface)
                        .clickable { showEditor = true }
                        .padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (url.isBlank()) "Tap to enter URL"
                        else url.replace("https://", "").replace("http://", ""),
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(BgDark)
                            .clickable { showEditor = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit URL",
                            tint = CyanPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
            if (isLoading) {
                Text(
                    text = "$progress%",
                    color = CyanPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(StatusActive, androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(Modifier.width(8.dp))
            }
        }
    }

    if (showEditor) {
        UrlEditorDialog(
            initialUrl = url,
            onDismiss = { showEditor = false },
            onSubmit = { entered ->
                showEditor = false
                val normalized = normalizeUserUrl(entered)
                if (normalized.isNotBlank()) onEditUrl(normalized)
            }
        )
    }
}

@Composable
private fun UrlEditorDialog(
    initialUrl: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialUrl) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Language, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Edit address", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("https://example.com or search…", color = TextMuted, fontSize = 13.sp) },
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton(onClick = { text = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = CardBorder,
                    cursorColor = CyanPrimary
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(text) },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) { Text("Go", color = BgDeep, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

private fun normalizeUserUrl(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    if (trimmed.matches(Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"))) return trimmed
    val looksLikeUrl = !trimmed.contains(' ') &&
            (trimmed.contains('.') || trimmed.startsWith("localhost"))
    return if (looksLikeUrl) "https://$trimmed"
    else "https://www.google.com/search?q=${Uri.encode(trimmed)}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaosBottomBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    isDesktopMode: Boolean,
    isAdblockEnabled: Boolean,
    isAutoScrollEnabled: Boolean,
    isAutoClickEnabled: Boolean,
    developerModeEnabled: Boolean,
    showConsole: Boolean,
    floatingWindowsEnabled: Boolean,
    showMoreMenu: Boolean,
    onMoreMenuToggle: () -> Unit,
    onMoreMenuDismiss: () -> Unit,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onHome: () -> Unit,
    onRefresh: () -> Unit,
    onFind: () -> Unit,
    onDesktop: () -> Unit,
    onAdblock: () -> Unit,
    onAutoScroll: () -> Unit,
    onAutoClick: () -> Unit,
    onConsole: () -> Unit,
    onShare: () -> Unit,
    onCopyUrl: () -> Unit,
    onOpenBrowser: () -> Unit,
    onFloat: () -> Unit,
    onCredentials: () -> Unit,
    onClipboard: () -> Unit,
    onDownloads: () -> Unit,
    onScreenshot: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onPrint: () -> Unit,
    onHistory: () -> Unit,
    onPageSource: () -> Unit,
    onSavePage: () -> Unit,
    onReaderMode: () -> Unit,
    onTranslate: () -> Unit,
    onAdBlockSettings: () -> Unit,
    onExportSession: () -> Unit,
    onImportSession: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgDark)
            .border(width = 1.dp, color = CardBorder, RoundedCornerShape(0.dp))
            .navigationBarsPadding()
    ) {
        Divider(color = CardBorder, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WaosToolbarBtn(onClick = onGoBack, enabled = canGoBack) {
                Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(18.dp))
            }
            WaosToolbarBtn(onClick = onGoForward, enabled = canGoForward) {
                Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
            }
            WaosToolbarBtn(onClick = onHome) {
                Icon(Icons.Default.Home, null, modifier = Modifier.size(18.dp))
            }
            WaosToolbarBtn(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
            }
            WaosToolbarBtn(onClick = onFind) {
                Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
            }
            WaosToolbarDivider()
            WaosToolbarBtn(onClick = onDesktop, active = isDesktopMode, activeColor = CyanPrimary) {
                Icon(if (isDesktopMode) Icons.Default.Computer else Icons.Default.Smartphone, null, modifier = Modifier.size(18.dp))
            }
            WaosToolbarBtn(onClick = onAdblock, active = isAdblockEnabled, activeColor = StatusActive) {
                Icon(Icons.Default.Block, null, modifier = Modifier.size(18.dp))
            }
            WaosToolbarBtn(onClick = onAutoScroll, active = isAutoScrollEnabled, activeColor = CyanPrimary) {
                Icon(if (isAutoScrollEnabled) Icons.Default.PlayArrow else Icons.Default.Pause, null, modifier = Modifier.size(18.dp))
            }
            WaosToolbarBtn(onClick = onAutoClick, active = isAutoClickEnabled, activeColor = VioletSecondary) {
                Icon(if (isAutoClickEnabled) Icons.Default.TouchApp else Icons.Default.DoNotTouch, null, modifier = Modifier.size(18.dp))
            }
            WaosToolbarDivider()
            WaosToolbarBtn(onClick = onZoomIn) {
                Icon(Icons.Default.ZoomIn, null, modifier = Modifier.size(18.dp))
            }
            WaosToolbarBtn(onClick = onZoomOut) {
                Icon(Icons.Default.ZoomOut, null, modifier = Modifier.size(18.dp))
            }
            WaosToolbarDivider()
            WaosToolbarBtn(onClick = onShare) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
            }
            WaosToolbarBtn(onClick = onCopyUrl) {
                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
            }
            WaosToolbarBtn(onClick = onOpenBrowser) {
                Icon(Icons.Default.Launch, null, modifier = Modifier.size(18.dp))
            }
            WaosToolbarDivider()
            WaosToolbarBtn(onClick = onCredentials) {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp))
            }
            WaosToolbarBtn(onClick = onClipboard) {
                Icon(Icons.Default.ContentPaste, null, modifier = Modifier.size(18.dp))
            }
            WaosToolbarBtn(onClick = onDownloads) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
            }
            WaosToolbarBtn(onClick = onScreenshot) {
                Icon(Icons.Default.Camera, null, modifier = Modifier.size(18.dp))
            }
            if (developerModeEnabled) {
                WaosToolbarBtn(onClick = onConsole, active = showConsole, activeColor = CyanPrimary) {
                    Icon(if (showConsole) Icons.Default.CodeOff else Icons.Default.Code, null, modifier = Modifier.size(18.dp))
                }
            }
            if (floatingWindowsEnabled) {
                WaosToolbarBtn(onClick = onFloat) {
                    Icon(Icons.Default.Launch, null, modifier = Modifier.size(18.dp))
                }
            }
            WaosToolbarBtn(onClick = onMoreMenuToggle) {
                Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(18.dp))
            }
        }

        DropdownMenu(
            expanded = showMoreMenu,
            onDismissRequest = onMoreMenuDismiss,
            modifier = Modifier.background(CardSurface)
        ) {
            DropdownMenuItem(
                text = { Text("Print Page", color = TextPrimary) },
                onClick = { onPrint(); onMoreMenuDismiss() },
                leadingIcon = { Icon(Icons.Default.Print, null, tint = TextSecondary) }
            )
            DropdownMenuItem(
                text = { Text("Page History", color = TextPrimary) },
                onClick = { onHistory(); onMoreMenuDismiss() },
                leadingIcon = { Icon(Icons.Default.History, null, tint = TextSecondary) }
            )
            DropdownMenuItem(
                text = { Text("Page Source", color = TextPrimary) },
                onClick = { onPageSource(); onMoreMenuDismiss() },
                leadingIcon = { Icon(Icons.Default.Code, null, tint = TextSecondary) }
            )
            DropdownMenuItem(
                text = { Text("Save Page", color = TextPrimary) },
                onClick = { onSavePage(); onMoreMenuDismiss() },
                leadingIcon = { Icon(Icons.Default.Save, null, tint = TextSecondary) }
            )
            DropdownMenuItem(
                text = { Text("Reader Mode", color = TextPrimary) },
                onClick = { onReaderMode(); onMoreMenuDismiss() },
                leadingIcon = { Icon(Icons.Default.Description, null, tint = TextSecondary) }
            )
            DropdownMenuItem(
                text = { Text("Translate", color = TextPrimary) },
                onClick = { onTranslate(); onMoreMenuDismiss() },
                leadingIcon = { Icon(Icons.Default.Language, null, tint = TextSecondary) }
            )
            DropdownMenuItem(
                text = { Text("AdBlock Settings", color = TextPrimary) },
                onClick = { onAdBlockSettings(); onMoreMenuDismiss() },
                leadingIcon = { Icon(Icons.Default.Block, null, tint = TextSecondary) }
            )
            Divider(color = CardBorder, thickness = 1.dp)
            DropdownMenuItem(
                text = { Text("Export Session", color = TextPrimary) },
                onClick = { onExportSession(); onMoreMenuDismiss() },
                leadingIcon = { Icon(Icons.Default.Save, null, tint = CyanPrimary) }
            )
            DropdownMenuItem(
                text = { Text("Import Session", color = TextPrimary) },
                onClick = { onImportSession(); onMoreMenuDismiss() },
                leadingIcon = { Icon(Icons.Default.ContentPaste, null, tint = VioletSecondary) }
            )
        }
    }
}

@Composable
private fun WaosToolbarBtn(
    onClick: () -> Unit,
    enabled: Boolean = true,
    active: Boolean = false,
    activeColor: Color = CyanPrimary,
    content: @Composable () -> Unit
) {
    val tint = when {
        !enabled -> TextMuted.copy(alpha = 0.3f)
        active -> activeColor
        else -> TextSecondary
    }
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(40.dp)
            .then(
                if (active) Modifier.background(activeColor.copy(0.12f), RoundedCornerShape(8.dp))
                else Modifier
            )
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides tint
        ) {
            content()
        }
    }
}

@Composable
private fun WaosToolbarDivider() {
    Box(
        modifier = Modifier
            .height(20.dp)
            .width(1.dp)
            .background(CardBorder)
            .padding(horizontal = 4.dp)
    )
}

@Composable
private fun WaosFindBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onFindNext: () -> Unit,
    onFindPrev: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(0.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Find in page…", color = TextMuted, fontSize = 13.sp) },
            modifier = Modifier.weight(1f).height(48.dp),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 14.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanPrimary,
                unfocusedBorderColor = CardBorder,
                cursorColor = CyanPrimary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
        IconButton(onClick = onFindPrev, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.KeyboardArrowUp, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onFindNext, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.KeyboardArrowDown, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun WaosConsolePanel(
    messages: List<ConsoleMessageData>,
    onExecuteCommand: (String) -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var command by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(
        modifier = modifier
            .background(Color(0xFF060A12))
            .border(1.dp, CardBorder, RoundedCornerShape(0.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D1421))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(CyanPrimary, androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Column {
                    Text("JS Console", color = CyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Advanced Debugger with ${messages.size} messages", color = TextMuted, fontSize = 9.sp)
                }
                Spacer(Modifier.width(12.dp))
                if (messages.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (messages.any { it.level == ConsoleMessage.MessageLevel.ERROR.ordinal }) 
                                    ErrorRed.copy(0.2f) 
                                else CyanPrimary.copy(0.15f),
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                1.dp,
                                if (messages.any { it.level == ConsoleMessage.MessageLevel.ERROR.ordinal }) 
                                    ErrorRed.copy(0.4f) 
                                else CyanPrimary.copy(0.3f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val errorCount = messages.count { it.level == ConsoleMessage.MessageLevel.ERROR.ordinal }
                            val warnCount = messages.count { it.level == ConsoleMessage.MessageLevel.WARNING.ordinal }
                            if (errorCount > 0) {
                                Text("$errorCount error", color = ErrorRed, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                            }
                            if (warnCount > 0) {
                                Text("$warnCount warning", color = Color(0xFFFFB800), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Copy All Button
                Button(
                    onClick = {
                        if (messages.isNotEmpty()) {
                            val allLogs = messages.joinToString("\n") { msg ->
                                val lvl = when (msg.level) {
                                    ConsoleMessage.MessageLevel.ERROR.ordinal -> "ERROR"
                                    ConsoleMessage.MessageLevel.WARNING.ordinal -> "WARN"
                                    else -> "LOG"
                                }
                                "[$lvl] ${msg.sourceId}:${msg.lineNumber} - ${msg.message}"
                            }
                            val cm = context.getSystemService(android.content.ClipboardManager::class.java)
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("Console Logs", allLogs))
                            android.widget.Toast.makeText(context, "All logs copied (${messages.size} messages)", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = messages.isNotEmpty(),
                    modifier = Modifier.height(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary.copy(0.2f),
                        disabledContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, null, tint = if (messages.isNotEmpty()) CyanPrimary else TextMuted, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy All", color = if (messages.isNotEmpty()) CyanPrimary else TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                // Clear Button
                Button(
                    onClick = {
                        if (messages.isNotEmpty()) {
                            android.widget.Toast.makeText(context, "Console cleared", android.widget.Toast.LENGTH_SHORT).show()
                            onClearLogs()
                        }
                    },
                    enabled = messages.isNotEmpty(),
                    modifier = Modifier.height(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed.copy(0.2f),
                        disabledContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Delete, null, tint = if (messages.isNotEmpty()) ErrorRed else TextMuted, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear", color = if (messages.isNotEmpty()) ErrorRed else TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(6.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(messages) { message ->
                val levelName = when (message.level) {
                    ConsoleMessage.MessageLevel.ERROR.ordinal -> "ERR"
                    ConsoleMessage.MessageLevel.WARNING.ordinal -> "WRN"
                    else -> "LOG"
                }
                val bgColor = when (message.level) {
                    ConsoleMessage.MessageLevel.ERROR.ordinal -> ErrorRed.copy(0.07f)
                    ConsoleMessage.MessageLevel.WARNING.ordinal -> Color(0xFFFFB800).copy(0.07f)
                    else -> Color.Transparent
                }
                val textColor = when (message.level) {
                    ConsoleMessage.MessageLevel.ERROR.ordinal -> ErrorRed
                    ConsoleMessage.MessageLevel.WARNING.ordinal -> Color(0xFFFFB800)
                    else -> Color(0xFFAABBCC)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor, RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "[$levelName]",
                        color = textColor.copy(0.6f),
                        fontSize = 9.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(32.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        message.message,
                        color = textColor,
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        androidx.compose.material3.HorizontalDivider(color = CardBorder.copy(0.5f), thickness = 1.dp)

        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                placeholder = { Text("// Enter JS snippet…", color = TextMuted, fontSize = 11.sp) },
                modifier = Modifier.weight(1f),
                minLines = 1,
                maxLines = 4,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = CardBorder.copy(0.5f),
                    focusedContainerColor = Color(0xFF0A1020),
                    unfocusedContainerColor = Color(0xFF0A1020),
                    cursorColor = CyanPrimary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = {
                    if (command.isNotBlank()) { onExecuteCommand(command); command = "" }
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Brush.linearGradient(listOf(GradCyanStart, GradCyanEnd)),
                        RoundedCornerShape(8.dp)
                    )
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun WaosErrorPanel(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .border(1.dp, ErrorRed.copy(0.3f), RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(ErrorRed.copy(0.15f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Error, null, tint = ErrorRed, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Failed to Load", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(error, color = TextSecondary, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onRetry,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Icon(Icons.Default.Refresh, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Retry", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun injectDarkMode(webView: WebView) {
    webView.evaluateJavascript(
        """
        (function() {
            const style = document.createElement('style');
            style.textContent = `
                html { filter: invert(1) hue-rotate(180deg); }
                img, video, canvas, svg { filter: invert(1) hue-rotate(180deg); }
            `;
            document.head.appendChild(style);
        })();
        """.trimIndent(),
        null
    )
}
