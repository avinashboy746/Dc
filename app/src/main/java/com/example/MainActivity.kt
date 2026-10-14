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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                KirbyApp(
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
const val APP_DISPLAY_NAME = "Kirby"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KirbyApp(
    onRegisterFileChooser: (ValueCallback<Array<Uri>>?) -> Unit,
    onConsumeFileChooser: () -> ValueCallback<Array<Uri>>?
) {
    val context = LocalContext.current

    // Clean 2-tab navigation: 0 = Discord, 1 = Settings
    var selectedTab by remember { mutableIntStateOf(0) }
    var useDesktopMode by remember { mutableStateOf(false) }

    // 24/7 Always Online Mode State
    var is247OnlineActive by remember { mutableStateOf(DiscordKeepAliveService.isServiceRunning) }

    // WebView state
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isInitialSplashVisible by remember { mutableStateOf(true) }

    // Dialogs
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

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

    // Hardware back navigation
    BackHandler {
        when {
            selectedTab != 0 -> selectedTab = 0
            webViewInstance != null && webViewInstance!!.canGoBack() -> webViewInstance!!.goBack()
            else -> (context as? Activity)?.moveTaskToBack(true)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DiscordDark,
        bottomBar = {
            // Clean 2-tab bottom bar: Discord and Settings only!
            NavigationBar(
                containerColor = DiscordDeepBlack,
                contentColor = DiscordTextPrimary,
                tonalElevation = 6.dp
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
                            fontSize = 12.sp,
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
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (selectedTab == 1) DiscordFuchsia else DiscordTextMuted
                        )
                    },
                    label = {
                        Text(
                            "Settings",
                            color = if (selectedTab == 1) DiscordFuchsia else DiscordTextMuted,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = DiscordSurface)
                )
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
                    // PURE DISCORD WEB EXPERIENCE — NO TOP LINK/URL BAR!
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 24/7 Active indicator (subtle header badge if active)
                        if (is247OnlineActive) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DiscordGreen.copy(alpha = 0.12f))
                                    .padding(horizontal = 14.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(DiscordGreen, CircleShape)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "24/7 Always Online Active",
                                        color = DiscordGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text("Kirby", color = DiscordTextMuted, fontSize = 10.sp)
                            }
                        }

                        // Subtle progress indicator while Discord loads
                        if (isLoading && loadingProgress in 1..99) {
                            LinearProgressIndicator(
                                progress = { loadingProgress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.5.dp),
                                color = DiscordBlurple,
                                trackColor = DiscordDark,
                            )
                        }

                        // Main WebViewContainer without any URL address bar
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
                                onWebViewReady = { webView -> webViewInstance = webView },
                                onShowFileChooser = { callback ->
                                    onRegisterFileChooser(callback)
                                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "*/*"
                                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                    }
                                    filePickerLauncher.launch(
                                        Intent.createChooser(intent, "Select Attachment")
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
                                        } catch (_: Exception) {}
                                    }
                                )
                            }

                            // Splash screen overlay
                            if (isInitialSplashVisible) {
                                KirbySplashScreen(progress = loadingProgress)
                            }
                        }
                    }
                }

                1 -> {
                    // SETTINGS TAB
                    SettingsScreen(
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
                        onReloadDiscord = {
                            webViewInstance?.reload()
                            selectedTab = 0
                            Toast.makeText(context, "Discord reloaded", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(context, "Signed out of Discord", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
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
                Text("Privacy & Security", color = DiscordTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "• Direct Official Connection:\nConnects directly to https://discord.com over official encrypted HTTPS.\n\n" +
                                "• Zero Credential Collection:\nZero telemetry, zero middleman servers, and zero keyloggers.\n\n" +
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
                Text("About $APP_DISPLAY_NAME", color = DiscordTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(APP_DISPLAY_NAME, color = DiscordTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Version 1.0.0 (Release 1)", color = DiscordFuchsia, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Custom clean Discord client with 24/7 Always Online Keep-Alive, typing stabilization, and full-screen view.",
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
fun SettingsScreen(
    useDesktopMode: Boolean,
    onToggleDesktopMode: (Boolean) -> Unit,
    is247Active: Boolean,
    onToggle247: (Boolean) -> Unit,
    onReloadDiscord: () -> Unit,
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
                    "$APP_DISPLAY_NAME Client Controls",
                    color = DiscordTextMuted,
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .background(DiscordFuchsia.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(APP_DISPLAY_NAME, color = DiscordFuchsia, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

        // Quick Actions
        SettingsSectionHeader("QUICK CONTROLS")

        SettingsCard {
            SettingsClickableRow(
                icon = Icons.Default.Refresh,
                title = "Reload Discord",
                subtitle = "Refresh the Discord web client session",
                actionText = "Reload",
                actionColor = DiscordBlurple,
                onClick = onReloadDiscord
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
                subtitle = "Grant Camera & Mic for Discord Voice & Video channels",
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

        // About & Links
        SettingsSectionHeader("ABOUT & POLICIES")

        SettingsCard {
            SettingsClickableRow(
                icon = Icons.Default.OpenInBrowser,
                title = "Official Discord Website",
                subtitle = "Visit discord.com in default device browser",
                actionText = "Open",
                onClick = onOpenOfficialWeb
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
                title = "About $APP_DISPLAY_NAME",
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
        Icon(icon, contentDescription = null, tint = DiscordFuchsia, modifier = Modifier.size(24.dp))
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
                checkedTrackColor = DiscordFuchsia,
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
fun KirbySplashScreen(progress: Int) {
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
                        Brush.linearGradient(listOf(DiscordFuchsia, DiscordBlurple)),
                        CircleShape
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🌸", fontSize = 48.sp)
            }

            Spacer(Modifier.height(24.dp))
            Text(
                APP_DISPLAY_NAME,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                "Discord Client",
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
                color = DiscordFuchsia,
                trackColor = DiscordSurface
            )

            Spacer(Modifier.height(12.dp))
            Text(
                "Loading Discord... $progress%",
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
                    "Could not connect to Discord servers. Please check your internet connection.",
                    color = DiscordTextMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

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
