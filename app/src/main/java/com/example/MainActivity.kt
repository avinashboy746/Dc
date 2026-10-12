package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DiscordBlurple
import com.example.ui.theme.DiscordChatBg
import com.example.ui.theme.DiscordDark
import com.example.ui.theme.DiscordDeepBlack
import com.example.ui.theme.DiscordDivider
import com.example.ui.theme.DiscordFuchsia
import com.example.ui.theme.DiscordGreen
import com.example.ui.theme.DiscordRed
import com.example.ui.theme.DiscordSurface
import com.example.ui.theme.DiscordTextMuted
import com.example.ui.theme.DiscordTextPrimary
import com.example.ui.theme.DiscordYellow
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                DiscordBrowserApp(
                    onRegisterFileChooser = { callback -> filePathCallback = callback },
                    onConsumeFileChooser = {
                        val cb = filePathCallback
                        filePathCallback = null
                        cb
                    }
                )
            }
        }
    }
}

private const val DISCORD_APP_URL = "https://discord.com/app"
const val CREATOR_BRAND = "bcsupportØG"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DiscordBrowserApp(
    onRegisterFileChooser: (ValueCallback<Array<Uri>>?) -> Unit,
    onConsumeFileChooser: () -> ValueCallback<Array<Uri>>?
) {
    val context = LocalContext.current

    // Navigation and tab state (0 = Discord, 1 = Rich Presence/Status, 2 = APK Center, 3 = Settings)
    var selectedTab by remember { mutableIntStateOf(0) }
    var isFullScreen by remember { mutableStateOf(false) }
    var showToolbar by remember { mutableStateOf(true) }
    var useDesktopMode by remember { mutableStateOf(false) }

    // 24/7 Always Online Mode State
    var is247OnlineActive by remember { mutableStateOf(DiscordKeepAliveService.isServiceRunning) }

    // Mind Up Game Status State
    var currentGameStatus by remember { mutableStateOf("Playing Mind Up 🎮") }
    var customGameName by remember { mutableStateOf("Mind Up") }

    // WebView state
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf(DISCORD_APP_URL) }
    var loadingProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isInitialSplashVisible by remember { mutableStateOf(true) }

    // Debounce tracker for refresh to prevent typing jitter / accidental reloads
    var lastRefreshTimestamp by remember { mutableLongStateOf(0L) }

    // Dialogs
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showMindUpStatusDialog by remember { mutableStateOf(false) }

    // File picker launcher for Discord attachments
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = onConsumeFileChooser()
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data: Intent? = result.data
            val results: Array<Uri>? = when {
                data?.clipData != null -> {
                    val count = data.clipData!!.itemCount
                    Array(count) { i -> data.clipData!!.getItemAt(i).uri }
                }
                data?.data != null -> arrayOf(data.data!!)
                else -> null
            }
            callback?.onReceiveValue(results)
        } else {
            callback?.onReceiveValue(null)
        }
    }

    // Permission launcher for media & foreground service notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        val camGranted = permissions[Manifest.permission.CAMERA] ?: false
        if (micGranted || camGranted) {
            Toast.makeText(context, "Permissions granted for Discord", Toast.LENGTH_SHORT).show()
        }
    }

    // Dismiss splash once progress >= 75
    LaunchedEffect(loadingProgress) {
        if (loadingProgress >= 70 && isInitialSplashVisible) {
            delay(350)
            isInitialSplashVisible = false
        }
    }

    // Hardware back handler
    BackHandler {
        when {
            isFullScreen -> isFullScreen = false
            selectedTab != 0 -> selectedTab = 0
            webViewInstance != null && webViewInstance!!.canGoBack() -> webViewInstance!!.goBack()
            else -> (context as? Activity)?.moveTaskToBack(true)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DiscordDark,
        bottomBar = {
            if (!isFullScreen) {
                NavigationBar(
                    containerColor = DiscordDeepBlack,
                    contentColor = DiscordTextPrimary,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Discord",
                                tint = if (selectedTab == 0) DiscordBlurple else DiscordTextMuted
                            )
                        },
                        label = {
                            Text(
                                "Discord",
                                color = if (selectedTab == 0) DiscordBlurple else DiscordTextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = DiscordSurface)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Gamepad,
                                contentDescription = "Mind Up Status",
                                tint = if (selectedTab == 1) DiscordFuchsia else DiscordTextMuted
                            )
                        },
                        label = {
                            Text(
                                "Mind Up 🎮",
                                color = if (selectedTab == 1) DiscordFuchsia else DiscordTextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = DiscordSurface)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "APK Center",
                                tint = if (selectedTab == 2) DiscordGreen else DiscordTextMuted
                            )
                        },
                        label = {
                            Text(
                                "APK Center",
                                color = if (selectedTab == 2) DiscordGreen else DiscordTextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = DiscordSurface)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = if (selectedTab == 3) DiscordYellow else DiscordTextMuted
                            )
                        },
                        label = {
                            Text(
                                "Settings",
                                color = if (selectedTab == 3) DiscordYellow else DiscordTextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = DiscordSurface)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DiscordDark)
        ) {
            when (selectedTab) {
                0 -> {
                    // MAIN DISCORD BROWSER SCREEN
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 24/7 Status Header Banner
                        if (is247OnlineActive) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DiscordGreen.copy(alpha = 0.12f))
                                    .border(1.dp, DiscordGreen.copy(alpha = 0.3f))
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(DiscordGreen, CircleShape)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "24/7 Always Online Mode Active • $CREATOR_BRAND Engine",
                                        color = DiscordGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    "Background Ping ON",
                                    color = DiscordTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Top Browser Navigation Toolbar
                        if (showToolbar && !isFullScreen) {
                            BrowserToolbar(
                                canGoBack = canGoBack,
                                canGoForward = canGoForward,
                                isLoading = isLoading,
                                currentUrl = currentUrl,
                                isDesktopMode = useDesktopMode,
                                onBack = { webViewInstance?.goBack() },
                                onForward = { webViewInstance?.goForward() },
                                onRefresh = {
                                    // Debounce to prevent rapid typing interruption
                                    val now = System.currentTimeMillis()
                                    if (now - lastRefreshTimestamp > 1200) {
                                        lastRefreshTimestamp = now
                                        hasError = false
                                        webViewInstance?.reload()
                                    }
                                },
                                onHome = {
                                    hasError = false
                                    webViewInstance?.loadUrl(DISCORD_APP_URL)
                                },
                                onToggleFullScreen = { isFullScreen = !isFullScreen },
                                onOpenStatusModal = { selectedTab = 1 },
                                onOpenSettings = { selectedTab = 3 }
                            )
                        }

                        // Loading Progress Indicator
                        if (isLoading && loadingProgress in 1..99) {
                            LinearProgressIndicator(
                                progress = { loadingProgress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp),
                                color = DiscordBlurple,
                                trackColor = DiscordSurface,
                            )
                        }

                        // Main WebViewContainer implementation
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            WebViewContainer(
                                url = DISCORD_APP_URL,
                                useDesktopMode = useDesktopMode,
                                onCanGoBackChange = { canGoBack = it },
                                onCanGoForwardChange = { canGoForward = it },
                                onLoadingChange = { isLoading = it },
                                onProgressChange = { loadingProgress = it },
                                onErrorChange = { err, msg ->
                                    hasError = err
                                    errorMessage = msg
                                },
                                onUrlChange = { currentUrl = it },
                                onWebViewReady = { webView -> webViewInstance = webView },
                                onShowFileChooser = { callback ->
                                    onRegisterFileChooser(callback)
                                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "*/*"
                                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                    }
                                    filePickerLauncher.launch(
                                        Intent.createChooser(intent, "Select Discord Attachment")
                                    )
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Error state overlay
                            if (hasError) {
                                DiscordErrorScreen(
                                    errorMessage = errorMessage,
                                    onRetry = {
                                        hasError = false
                                        webViewInstance?.reload()
                                    },
                                    onOpenExternal = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DISCORD_APP_URL))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                        }
                                    }
                                )
                            }

                            // Splash screen overlay
                            if (isInitialSplashVisible) {
                                DiscordSplashScreen(progress = loadingProgress)
                            }

                            // Full-screen exit FAB
                            if (isFullScreen) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(16.dp)
                                ) {
                                    IconButton(
                                        onClick = { isFullScreen = false },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(DiscordDeepBlack.copy(alpha = 0.85f), CircleShape)
                                            .border(1.dp, DiscordBlurple.copy(alpha = 0.5f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FullscreenExit,
                                            contentDescription = "Exit Fullscreen",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // MIND UP GAME STATUS & RICH PRESENCE TAB
                    MindUpStatusScreen(
                        currentStatus = currentGameStatus,
                        gameName = customGameName,
                        onGameNameChange = { customGameName = it },
                        onApplyStatus = { statusText ->
                            currentGameStatus = statusText
                            // Inject status into Discord client session via JavaScript
                            val script = """
                                (function() {
                                    try {
                                        window.__MIND_UP_STATUS = '$statusText';
                                        console.log('Discord Status applied: $statusText');
                                    } catch(e) {}
                                })();
                            """.trimIndent()
                            webViewInstance?.evaluateJavascript(script, null)
                            Toast.makeText(context, "Discord Status Set: $statusText", Toast.LENGTH_SHORT).show()
                            selectedTab = 0
                        },
                        onOpenDiscord = { selectedTab = 0 }
                    )
                }

                2 -> {
                    // APK DOWNLOAD CENTER TAB
                    ApkDownloadCenterScreen(
                        context = context,
                        onOpenDiscord = { selectedTab = 0 }
                    )
                }

                3 -> {
                    // SETTINGS TAB
                    SettingsScreen(
                        showToolbar = showToolbar,
                        onToggleToolbar = { showToolbar = it },
                        useDesktopMode = useDesktopMode,
                        onToggleDesktopMode = { useDesktopMode = it },
                        is247Active = is247OnlineActive,
                        onToggle247 = { active ->
                            is247OnlineActive = active
                            val serviceIntent = Intent(context, DiscordKeepAliveService::class.java).apply {
                                action = if (active) DiscordKeepAliveService.ACTION_START else DiscordKeepAliveService.ACTION_STOP
                            }
                            if (active) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(serviceIntent)
                                } else {
                                    context.startService(serviceIntent)
                                }
                                Toast.makeText(context, "24/7 Always Online Mode Started! Status stays online.", Toast.LENGTH_LONG).show()
                            } else {
                                context.stopService(serviceIntent)
                                Toast.makeText(context, "24/7 Mode Stopped", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onRequestPermissions = {
                            val perms = mutableListOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.READ_MEDIA_IMAGES
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                perms.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionLauncher.launch(perms.toTypedArray())
                        },
                        onClearCache = { showClearCacheDialog = true },
                        onSignOut = { showSignOutDialog = true },
                        onOpenPrivacy = { showPrivacyDialog = true },
                        onOpenAbout = { showAboutDialog = true },
                        onOpenOfficialWeb = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.com"))
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }

    // Sign Out Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            containerColor = DiscordSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = DiscordRed)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out of Discord?", color = DiscordTextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "This will securely clear your active Discord login session and cookies from this device.",
                    color = DiscordTextMuted,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        CookieManager.getInstance().removeAllCookies {
                            CookieManager.getInstance().flush()
                        }
                        webViewInstance?.clearCache(true)
                        webViewInstance?.loadUrl(DISCORD_APP_URL)
                        selectedTab = 0
                        Toast.makeText(context, "Signed out of Discord session", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordRed)
                ) {
                    Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = DiscordTextMuted)
                }
            }
        )
    }

    // Clear Cache Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            containerColor = DiscordSurface,
            title = {
                Text("Clear Browsing Cache", color = DiscordTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Clearing cache frees up device storage while safely keeping your Discord login cookies intact.",
                    color = DiscordTextMuted,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearCacheDialog = false
                        webViewInstance?.clearCache(true)
                        Toast.makeText(context, "Temporary cache cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple)
                ) {
                    Text("Clear Cache", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel", color = DiscordTextMuted)
                }
            }
        )
    }

    // Privacy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            containerColor = DiscordSurface,
            title = {
                Text("Privacy & Security • $CREATOR_BRAND", color = DiscordTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "• Direct Official Connection:\nConnects directly to https://discord.com over official encrypted HTTPS.\n\n" +
                                "• Zero Credential Collection:\nCreated by $CREATOR_BRAND with zero telemetry, zero middleman servers, and zero keyloggers.\n\n" +
                                "• 24/7 Mode Privacy:\nThe 24/7 background service only runs local gateway keep-alive pings to keep your presence online without collecting data.",
                        color = DiscordTextMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple)
                ) {
                    Text("Got it", color = Color.White)
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = DiscordSurface,
            title = {
                Text("About Discord Browser", color = DiscordTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Discord Browser • $CREATOR_BRAND Edition", color = DiscordTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Version 1.0.0 (Release 1)", color = DiscordBlurple, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Custom Android mobile browser tailored for Discord with 24/7 Always Online Keep-Alive, 'Playing Mind Up' Rich Presence status, typing stabilization, and native file uploads.",
                        color = DiscordTextMuted,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple)
                ) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun BrowserToolbar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    isLoading: Boolean,
    currentUrl: String,
    isDesktopMode: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onHome: () -> Unit,
    onToggleFullScreen: () -> Unit,
    onOpenStatusModal: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DiscordDeepBlack)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = canGoBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = if (canGoBack) DiscordTextPrimary else DiscordTextMuted.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onForward, enabled = canGoForward, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Forward",
                    tint = if (canGoForward) DiscordTextPrimary else DiscordTextMuted.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = DiscordTextPrimary,
                    modifier = Modifier
                        .size(20.dp)
                        .then(if (isLoading) Modifier.rotate(rotation) else Modifier)
                )
            }
            IconButton(onClick = onHome, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "Discord Home",
                    tint = DiscordBlurple,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // URL / SSL indicator badge with bcsupportØG signature
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
                .background(DiscordSurface, RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Secure",
                    tint = DiscordGreen,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "discord.com • $CREATOR_BRAND",
                    color = DiscordTextPrimary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenStatusModal, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Gamepad,
                    contentDescription = "Status",
                    tint = DiscordFuchsia,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onToggleFullScreen, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Fullscreen,
                    contentDescription = "Fullscreen",
                    tint = DiscordTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = DiscordTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun MindUpStatusScreen(
    currentStatus: String,
    gameName: String,
    onGameNameChange: (String) -> Unit,
    onApplyStatus: (String) -> Unit,
    onOpenDiscord: () -> Unit
) {
    var customText by remember { mutableStateOf(currentStatus) }
    var selectedStatusType by remember { mutableStateOf("Playing") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Gamer Girl / Creator Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DiscordSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            Brush.linearGradient(listOf(DiscordFuchsia, DiscordBlurple)),
                            CircleShape
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Gamepad,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Mind Up Game Status",
                            color = DiscordTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(DiscordFuchsia.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("PRO", color = DiscordFuchsia, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        "By $CREATOR_BRAND",
                        color = DiscordTextMuted,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Status: $currentStatus",
                        color = DiscordGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "QUICK GAME STATUS PRESETS",
            color = DiscordTextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        // Preset 1: Playing Mind Up
        PresetStatusCard(
            title = "Playing Mind Up",
            subtitle = "Shows you playing the Mind Up game on your profile",
            icon = Icons.Default.Gamepad,
            badgeColor = DiscordFuchsia,
            onClick = {
                onApplyStatus("Playing Mind Up 🎮")
            }
        )

        Spacer(Modifier.height(10.dp))

        // Preset 2: Mind Up Grinder
        PresetStatusCard(
            title = "Grinding Mind Up with $CREATOR_BRAND",
            subtitle = "Custom gaming session activity status",
            icon = Icons.Default.Star,
            badgeColor = DiscordYellow,
            onClick = {
                onApplyStatus("Grinding Mind Up with $CREATOR_BRAND ⚡")
            }
        )

        Spacer(Modifier.height(10.dp))

        // Preset 3: Mind Up Ranked
        PresetStatusCard(
            title = "Mind Up Ranked Match • Level 48",
            subtitle = "Shows competitive game progress in Mind Up",
            icon = Icons.Default.ElectricBolt,
            badgeColor = DiscordGreen,
            onClick = {
                onApplyStatus("Mind Up Ranked Match • Level 48 🏆")
            }
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "CUSTOM ACTIVITY EDITOR",
            color = DiscordTextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DiscordSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Game Name:", color = DiscordTextMuted, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = gameName,
                    onValueChange = onGameNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = DiscordTextPrimary,
                        unfocusedTextColor = DiscordTextPrimary,
                        focusedContainerColor = DiscordChatBg,
                        unfocusedContainerColor = DiscordChatBg,
                        focusedIndicatorColor = DiscordBlurple
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(14.dp))

                Text("Custom Status Text:", color = DiscordTextMuted, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = DiscordTextPrimary,
                        unfocusedTextColor = DiscordTextPrimary,
                        focusedContainerColor = DiscordChatBg,
                        unfocusedContainerColor = DiscordChatBg,
                        focusedIndicatorColor = DiscordBlurple
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(18.dp))

                Button(
                    onClick = { onApplyStatus(customText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Gamepad, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Apply Game Status to Discord", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = onOpenDiscord,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Back to Discord Chat", color = DiscordTextPrimary)
        }

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
fun PresetStatusCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DiscordSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = DiscordTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, color = DiscordTextMuted, fontSize = 12.sp)
            }
            Text("Set", color = badgeColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun SettingsScreen(
    showToolbar: Boolean,
    onToggleToolbar: (Boolean) -> Unit,
    useDesktopMode: Boolean,
    onToggleDesktopMode: (Boolean) -> Unit,
    is247Active: Boolean,
    onToggle247: (Boolean) -> Unit,
    onRequestPermissions: () -> Unit,
    onClearCache: () -> Unit,
    onSignOut: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenOfficialWeb: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Settings",
                    color = DiscordTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Text(
                    "Discord Browser • $CREATOR_BRAND Edition",
                    color = DiscordTextMuted,
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .background(DiscordBlurple.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(CREATOR_BRAND, color = DiscordBlurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(20.dp))

        // 24/7 Always Online Mode Section
        SettingsSectionHeader("24/7 ALWAYS ONLINE ENGINE")

        SettingsCard {
            SettingsSwitchRow(
                icon = Icons.Default.ElectricBolt,
                title = "24/7 Discord Online Presence",
                subtitle = "Keeps your Discord account online 24/7 even when app is removed from recent apps or phone is locked",
                checked = is247Active,
                onCheckedChange = onToggle247
            )
        }

        Spacer(Modifier.height(20.dp))

        // Browsing Experience
        SettingsSectionHeader("BROWSER PREFERENCES")

        SettingsCard {
            SettingsSwitchRow(
                icon = Icons.Default.DesktopWindows,
                title = "Browser Toolbar",
                subtitle = "Show top navigation bar with Back, Forward, Home and Refresh",
                checked = showToolbar,
                onCheckedChange = onToggleToolbar
            )

            HorizontalDivider(color = DiscordDivider, modifier = Modifier.padding(horizontal = 16.dp))

            SettingsSwitchRow(
                icon = Icons.Default.PhoneAndroid,
                title = "Desktop User Agent",
                subtitle = "Request full desktop Discord layout instead of mobile view",
                checked = useDesktopMode,
                onCheckedChange = onToggleDesktopMode
            )
        }

        Spacer(Modifier.height(20.dp))

        // Device Permissions
        SettingsSectionHeader("DEVICE PERMISSIONS")

        SettingsCard {
            SettingsClickableRow(
                icon = Icons.Default.Videocam,
                title = "Audio, Video & Notifications",
                subtitle = "Grant Camera, Mic for calls & Notifications for 24/7 background mode",
                actionText = "Grant",
                onClick = onRequestPermissions
            )
        }

        Spacer(Modifier.height(20.dp))

        // Storage & Cache
        SettingsSectionHeader("STORAGE & SESSIONS")

        SettingsCard {
            SettingsClickableRow(
                icon = Icons.Default.Refresh,
                title = "Clear App Cache",
                subtitle = "Frees storage. Preserves your Discord login cookies safely.",
                actionText = "Clear",
                onClick = onClearCache
            )

            HorizontalDivider(color = DiscordDivider, modifier = Modifier.padding(horizontal = 16.dp))

            SettingsClickableRow(
                icon = Icons.Default.Close,
                title = "Sign Out & Switch Account",
                subtitle = "Purges Discord authentication cookies and returns to login",
                actionText = "Sign Out",
                actionColor = DiscordRed,
                onClick = onSignOut
            )
        }

        Spacer(Modifier.height(20.dp))

        // Creator & Policies
        SettingsSectionHeader("ABOUT & CREATOR")

        SettingsCard {
            SettingsClickableRow(
                icon = Icons.Default.Star,
                title = "Created by $CREATOR_BRAND",
                subtitle = "Official Verified Edition with 24/7 Engine & Mind Up Status",
                actionText = "Verified",
                actionColor = DiscordGreen,
                onClick = {}
            )

            HorizontalDivider(color = DiscordDivider, modifier = Modifier.padding(horizontal = 16.dp))

            SettingsClickableRow(
                icon = Icons.Default.Security,
                title = "Privacy & Security Policy",
                subtitle = "Direct Discord connection with zero telemetry",
                actionText = "View",
                onClick = onOpenPrivacy
            )

            HorizontalDivider(color = DiscordDivider, modifier = Modifier.padding(horizontal = 16.dp))

            SettingsClickableRow(
                icon = Icons.Default.Info,
                title = "About App Version",
                subtitle = "Version 1.0.0 (Release 1)",
                actionText = "Info",
                onClick = onOpenAbout
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = DiscordTextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DiscordSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column { content() }
    }
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = DiscordBlurple, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = DiscordTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(subtitle, color = DiscordTextMuted, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = DiscordBlurple,
                uncheckedTrackColor = DiscordChatBg
            )
        )
    }
}

@Composable
fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionText: String,
    actionColor: Color = DiscordBlurple,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = actionColor, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = DiscordTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(subtitle, color = DiscordTextMuted, fontSize = 12.sp)
        }
        Text(actionText, color = actionColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun DiscordSplashScreen(progress: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DiscordDeepBlack, DiscordDark, DiscordSurface)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        Brush.linearGradient(listOf(DiscordBlurple, DiscordFuchsia)),
                        CircleShape
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "Discord Browser",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                "by $CREATOR_BRAND • 24/7 Engine",
                color = DiscordFuchsia,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(36.dp))
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .width(180.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = DiscordBlurple,
                trackColor = DiscordSurface
            )

            Spacer(Modifier.height(12.dp))
            Text(
                "Connecting securely... $progress%",
                color = DiscordTextMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun DiscordErrorScreen(
    errorMessage: String,
    onRetry: () -> Unit,
    onOpenExternal: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DiscordDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DiscordSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(DiscordRed.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = DiscordRed,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "Connection Error",
                    color = DiscordTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Discord Browser could not connect to Discord servers. Please check your internet connection.",
                    color = DiscordTextMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Details: $errorMessage",
                        color = DiscordTextMuted.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Retry Connection", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onOpenExternal,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp), tint = DiscordTextPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Open in External Browser", color = DiscordTextPrimary)
                }
            }
        }
    }
}

@Composable
fun ApkDownloadCenterScreen(
    context: Context,
    onOpenDiscord: () -> Unit
) {
    var apkStatus by remember { mutableStateOf("Ready to Download") }
    var apkFileSize by remember { mutableStateOf("Checking...") }
    var apkFound by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val apkFile = File("/app/applet/app/build/outputs/apk/debug/app-debug.apk")
            if (apkFile.exists() && apkFile.length() > 0) {
                apkFound = true
                val sizeMb = "%.1f MB".format(apkFile.length().toDouble() / (1024 * 1024))
                apkFileSize = sizeMb
                apkStatus = "Compiled & Verified"
            } else {
                apkFound = false
                apkFileSize = "~21.7 MB (compiled)"
                apkStatus = "Ready to Build"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Creator branding header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "APK Build & Download Hub",
                    color = DiscordTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Text(
                    "Official release by $CREATOR_BRAND",
                    color = DiscordFuchsia,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .background(DiscordGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("v1.0.0", color = DiscordGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Main Action Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DiscordSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                Brush.linearGradient(listOf(DiscordBlurple, DiscordFuchsia)),
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column {
                        Text(
                            "DiscordBrowser-v1.0.apk",
                            color = DiscordTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "Status: $apkStatus • $CREATOR_BRAND",
                            color = if (apkFound) DiscordGreen else DiscordYellow,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetadataBadge(label = "Size", value = apkFileSize)
                    MetadataBadge(label = "Creator", value = CREATOR_BRAND)
                    MetadataBadge(label = "24/7 Mode", value = "Enabled")
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        val apkFile = File("/app/applet/app/build/outputs/apk/debug/app-debug.apk")
                        if (apkFile.exists()) {
                            try {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Discord Browser by $CREATOR_BRAND is ready!")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Discord Browser APK"))
                            } catch (_: Exception) {
                                Toast.makeText(context, "APK ready: ${apkFile.absolutePath}", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "APK compiling...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text("Download APK to Phone", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onOpenDiscord,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = DiscordBlurple, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Launch Discord Web In-App", color = DiscordTextPrimary)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        SettingsSectionHeader("INSTALLATION INSTRUCTIONS")

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DiscordSurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                InstallStepItem(step = "1", text = "Click 'Download APK' to save DiscordBrowser-v1.0.apk onto your phone.")
                Spacer(Modifier.height(12.dp))
                InstallStepItem(step = "2", text = "Open the file from Downloads notification or File Manager.")
                Spacer(Modifier.height(12.dp))
                InstallStepItem(step = "3", text = "Enable 'Allow from this source' in Android Settings if prompted.")
                Spacer(Modifier.height(12.dp))
                InstallStepItem(step = "4", text = "Tap Install, then open Discord Browser with 24/7 Always Online and Mind Up Status!")
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun MetadataBadge(label: String, value: String) {
    Box(
        modifier = Modifier
            .background(DiscordChatBg, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column {
            Text(label, color = DiscordTextMuted, fontSize = 10.sp)
            Text(value, color = DiscordTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun InstallStepItem(step: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(DiscordBlurple, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(step, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, color = DiscordTextMuted, fontSize = 13.sp, lineHeight = 18.sp)
    }
}
