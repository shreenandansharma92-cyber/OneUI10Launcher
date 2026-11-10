package com.tabahi.oneui10launcher

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class AppModel(
    val label: String,
    val packageName: String,
    val icon: Bitmap
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge full screen futuristic experience
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Prevent accidental back button exit on Home Launcher
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Stay on Home Screen
            }
        })

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                OneUI10TabahiLauncher()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OneUI10TabahiLauncher() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // App list state
    var installedApps by remember { mutableStateOf<List<AppModel>>(emptyList()) }
    var bubbleApps by remember { mutableStateOf<List<AppModel>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    // UI states
    var isDrawerOpen by remember { mutableStateOf(false) }
    var isQuickPanelOpen by remember { mutableStateOf(false) }
    var isTabahiOverdrive by remember { mutableStateOf(true) }
    var isMediaPlaying by remember { mutableStateOf(true) }
    var expandQuickTiles by remember { mutableStateOf(true) }

    // Real System Settings States
    var hasWritePermission by remember { mutableStateOf(Settings.System.canWrite(context)) }
    var brightness by remember { mutableFloatStateOf(getSystemBrightness(context)) }
    var mediaVolume by remember { mutableFloatStateOf(getMediaVolume(context)) }
    var autoRotate by remember { mutableStateOf(getAutoRotate(context)) }
    var screenTimeoutSec by remember { mutableIntStateOf(getScreenTimeoutSeconds(context)) }

    // Live Telemetry & Clock
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    var batteryPercent by remember { mutableIntStateOf(100) }
    var ramUsedPercent by remember { mutableIntStateOf(45) }

    // Load installed apps asynchronously on launch
    LaunchedEffect(Unit) {
        val loaded = loadInstalledApps(context)
        installedApps = loaded
        bubbleApps = loaded.take(5)
    }

    // Live Clock + Real Hardware Telemetry Ticker
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("hh:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMM d • ONE UI 10 AI", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now).uppercase()

            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryPercent = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            ramUsedPercent = (((memInfo.totalMem - memInfo.availMem).toDouble() / memInfo.totalMem) * 100).toInt()

            hasWritePermission = Settings.System.canWrite(context)
            delay(2000)
        }
    }

    // Dynamic Holographic Background Animation
    val infiniteTransition = rememberInfiniteTransition(label = "bgGlow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isTabahiOverdrive) 1800 else 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val primaryAccent = if (isTabahiOverdrive) Color(0xFF00F5D4) else Color(0xFF4CC9F0)
    val secondaryAccent = if (isTabahiOverdrive) Color(0xFFF72585) else Color(0xFF7209B7)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF05050A),
                        Color(0xFF0F1026),
                        secondaryAccent.copy(alpha = glowPulse * 0.35f),
                        Color(0xFF040508)
                    )
                )
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -35 && !isQuickPanelOpen) {
                        isDrawerOpen = true
                    } else if (dragAmount > 35 && !isDrawerOpen) {
                        isQuickPanelOpen = true
                    }
                }
            }
    ) {
        // Ambient Cyberpunk / One UI 10 Spatial Orb
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = 60.dp, y = 80.dp)
                .blur(90.dp)
                .background(primaryAccent.copy(alpha = glowPulse * 0.3f), CircleShape)
        )

        // ================= MAIN HOME SCREEN =================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Status & Quick Panel Pull Trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, primaryAccent.copy(alpha = 0.4f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ANDROID 17 • ONE UI 10", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Button to open Quick Settings Control Center
                    IconButton(
                        onClick = { isQuickPanelOpen = !isQuickPanelOpen },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Quick Panel", tint = primaryAccent)
                    }
                }
            }

            // Spatial Hero Clock & Holographic Telemetry
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentTime.ifEmpty { "12:00" },
                    fontSize = 76.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = currentDate,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    color = primaryAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // One UI 10 "Now Brief" & "Now Nudge" Interactive AI Cards
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Now Brief Card (Telemetry + RAM Boost)
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        borderColor = primaryAccent.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("NOW BRIEF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryAccent)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Battery: $batteryPercent%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("AI RAM Load: $ramUsedPercent%", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    System.gc()
                                    Toast.makeText(context, "One UI 10 Neural RAM Purge Complete!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryAccent.copy(alpha = 0.2f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Purge RAM", fontSize = 11.sp, color = primaryAccent)
                            }
                        }
                    }

                    // Now Nudge Card (System Permission & Tabahi Mode)
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        borderColor = secondaryAccent.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = secondaryAccent, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("NOW NUDGE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = secondaryAccent)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (hasWritePermission) "System Control: ACTIVE" else "System Access Needed",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                if (hasWritePermission) "Direct Hardware Link Ready" else "Tap to grant WRITE_SETTINGS",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (!hasWritePermission) {
                                        requestWriteSettingsPermission(context)
                                    } else {
                                        isQuickPanelOpen = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = secondaryAccent.copy(alpha = 0.25f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    if (hasWritePermission) "Tweak System" else "Grant Access",
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Fluid Waveform Media Island (One UI 9/10 Signature)
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = Color.White.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.linearGradient(listOf(primaryAccent, secondaryAccent))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color.Black)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Spatial Audio Engine", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Dolby Atmos • 32-Bit Fluid DAC", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            }
                        }

                        // Animated Waveform Bars
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            repeat(5) { idx ->
                                val barHeight = if (isMediaPlaying) ((glowPulse * 24) + (idx * 3) % 14).dp else 6.dp
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(barHeight)
                                        .clip(CircleShape)
                                        .background(primaryAccent)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { isMediaPlaying = !isMediaPlaying }) {
                                Icon(
                                    if (isMediaPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Android 17 Floating Bubble Bar Dock + App Drawer Trigger
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "ANDROID 17 FLOATING BUBBLE BAR (LONG-PRESS APPS TO SPLIT-SCREEN)",
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(6.dp))
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = primaryAccent.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(bubbleApps) { app ->
                                Image(
                                    bitmap = app.icon.asImageBitmap(),
                                    contentDescription = app.label,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .combinedClickable(
                                            onClick = { launchApp(context, app.packageName, splitScreen = false) },
                                            onLongClick = { launchApp(context, app.packageName, splitScreen = true) }
                                        )
                                )
                            }
                        }

                        // App Drawer Launch Button
                        IconButton(
                            onClick = { isDrawerOpen = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(primaryAccent, secondaryAccent)))
                        ) {
                            Icon(Icons.Default.Apps, contentDescription = "All Apps", tint = Color.Black)
                        }
                    }
                }
            }
        }

        // ================= ONE UI 10 QUICK PANEL & REAL SYSTEM SETTINGS =================
        AnimatedVisibility(
            visible = isQuickPanelOpen,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE080914))
                    .statusBarsPadding()
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ONE UI 10 QUICK PANEL", color = primaryAccent, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Text("Direct Android 14 Hardware & Settings Bridge", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                        IconButton(onClick = { isQuickPanelOpen = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    // Permission Banner if WRITE_SETTINGS not yet enabled
                    if (!hasWritePermission) {
                        GlassCard(borderColor = Color(0xFFFFB703)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { requestWriteSettingsPermission(context) }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB703))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Tap here to allow 'Modify System Settings' so Brightness, Auto-Rotate, and Timeout sliders modify real OS values.",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Resizable Quick Panel Tiles (One UI 9/10 Feature)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("RESIZABLE CONTROL TILES", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { expandQuickTiles = !expandQuickTiles }) {
                            Text(if (expandQuickTiles) "Compact Grid" else "Expand Tiles", color = primaryAccent, fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickTile(
                            modifier = Modifier.weight(1f),
                            expanded = expandQuickTiles,
                            icon = Icons.Default.Wifi,
                            title = "Internet Panel",
                            subtitle = "Android 14 Native",
                            active = true,
                            accent = primaryAccent
                        ) {
                            openSystemPanel(context, Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                        }

                        QuickTile(
                            modifier = Modifier.weight(1f),
                            expanded = expandQuickTiles,
                            icon = Icons.Default.ScreenRotation,
                            title = "Auto Rotate",
                            subtitle = if (autoRotate) "ON (System)" else "Locked",
                            active = autoRotate,
                            accent = primaryAccent
                        ) {
                            if (ensureWriteSettings(context)) {
                                autoRotate = !autoRotate
                                setAutoRotate(context, autoRotate)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickTile(
                            modifier = Modifier.weight(1f),
                            expanded = expandQuickTiles,
                            icon = Icons.Default.Timer,
                            title = "Screen Timeout",
                            subtitle = "${screenTimeoutSec}s Real OS",
                            active = true,
                            accent = secondaryAccent
                        ) {
                            if (ensureWriteSettings(context)) {
                                val nextTimeout = when (screenTimeoutSec) {
                                    30 -> 60
                                    60 -> 300
                                    300 -> 600
                                    else -> 30
                                }
                                screenTimeoutSec = nextTimeout
                                setScreenTimeoutSeconds(context, nextTimeout)
                            }
                        }

                        QuickTile(
                            modifier = Modifier.weight(1f),
                            expanded = expandQuickTiles,
                            icon = Icons.Default.RocketLaunch,
                            title = "Tabahi Overdrive",
                            subtitle = if (isTabahiOverdrive) "MAX NEON 120Hz" else "Balanced",
                            active = isTabahiOverdrive,
                            accent = secondaryAccent
                        ) {
                            isTabahiOverdrive = !isTabahiOverdrive
                        }
                    }

                    // One UI 9/10 Thick Fluid Brightness Slider (Real System Control)
                    GlassCard(borderColor = primaryAccent.copy(alpha = 0.5f)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LightMode, contentDescription = null, tint = primaryAccent)
                                    Spacer(Modifier.width(8.dp))
                                    Text("System Brightness", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Text("${(brightness / 255f * 100).toInt()}%", color = primaryAccent, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = brightness,
                                onValueChange = { newVal ->
                                    brightness = newVal
                                    if (Settings.System.canWrite(context)) {
                                        setSystemBrightness(context, newVal.toInt())
                                    }
                                },
                                valueRange = 10f..255f,
                                colors = SliderDefaults.colors(
                                    thumbColor = primaryAccent,
                                    activeTrackColor = primaryAccent
                                )
                            )
                        }
                    }

                    // One UI 9/10 Thick Fluid Media Volume Slider (Real AudioManager Control)
                    GlassCard(borderColor = secondaryAccent.copy(alpha = 0.5f)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = secondaryAccent)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Hardware Media Volume", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Text("${(mediaVolume * 100).toInt()}%", color = secondaryAccent, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = mediaVolume,
                                onValueChange = { newVal ->
                                    mediaVolume = newVal
                                    setMediaVolume(context, newVal)
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = secondaryAccent,
                                    activeTrackColor = secondaryAccent
                                )
                            )
                        }
                    }

                    // Quick Shortcut Row to Native Android Settings & Default Home Chooser
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f))
                        ) {
                            Text("Set Default Home", color = Color.White, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f))
                        ) {
                            Text("System Settings", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // ================= SWIPE-UP APP DRAWER (ALL INSTALLED APPS) =================
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF2060711))
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search Bar + Close Drawer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search Apps with Galaxy AI...", color = Color.White.copy(alpha = 0.5f)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = primaryAccent) },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { isDrawerOpen = false }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap to Launch • Long-Press to Pin to Android 17 Bubble Dock & Split-Screen",
                        color = primaryAccent.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(12.dp))

                    val filteredApps = remember(installedApps, searchQuery) {
                        if (searchQuery.isBlank()) installedApps
                        else installedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .combinedClickable(
                                        onClick = {
                                            isDrawerOpen = false
                                            launchApp(context, app.packageName, splitScreen = false)
                                        },
                                        onLongClick = {
                                            if (bubbleApps.none { it.packageName == app.packageName }) {
                                                bubbleApps = (listOf(app) + bubbleApps).take(6)
                                            }
                                            Toast.makeText(context, "${app.label} pinned to Bubble Bar!", Toast.LENGTH_SHORT).show()
                                            isDrawerOpen = false
                                            launchApp(context, app.packageName, splitScreen = true)
                                        }
                                    )
                                    .padding(6.dp)
                            ) {
                                Image(
                                    bitmap = app.icon.asImageBitmap(),
                                    contentDescription = app.label,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = app.label,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================= REUSABLE GLASSMORPHISM COMPONENTS =================

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.04f)
                    )
                )
            )
            .border(1.dp, borderColor, RoundedCornerShape(26.dp))
    ) {
        content()
    }
}

@Composable
fun QuickTile(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    icon: ImageVector,
    title: String,
    subtitle: String,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val bg = if (active) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.07f)
    val border = if (active) accent else Color.White.copy(alpha = 0.15f)

    Row(
        modifier = modifier
            .height(if (expanded) 74.dp else 54.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = if (active) accent else Color.White)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
            if (expanded) {
                Text(subtitle, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

// ================= ANDROID 14 PACKAGE MANAGER & SYSTEM SETTINGS HELPERS =================

fun loadInstalledApps(context: Context): List<AppModel> {
    val pm = context.packageManager
    val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val resolveInfos: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(mainIntent, 0)
    }

    return resolveInfos
        .mapNotNull { info ->
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName) return@mapNotNull null
            val label = info.loadLabel(pm).toString()
            val drawable = info.loadIcon(pm)
            AppModel(
                label = label,
                packageName = pkg,
                icon = drawableToBitmap(drawable)
            )
        }
        .sortedBy { it.label.lowercase(Locale.getDefault()) }
}

fun drawableToBitmap(drawable: Drawable): Bitmap {
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 128
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 128
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

fun launchApp(context: Context, packageName: String, splitScreen: Boolean) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (launchIntent != null) {
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (splitScreen) {
            // Android Multi-Window / Bubble Adjacent Launch
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
        context.startActivity(launchIntent)
    }
}

fun ensureWriteSettings(context: Context): Boolean {
    return if (Settings.System.canWrite(context)) {
        true
    } else {
        requestWriteSettingsPermission(context)
        false
    }
}

fun requestWriteSettingsPermission(context: Context) {
    Toast.makeText(context, "Enable 'Allow modify system settings' for One UI 10 Tabahi", Toast.LENGTH_LONG).show()
    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

fun getSystemBrightness(context: Context): Float {
    return try {
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS).toFloat()
    } catch (e: Exception) {
        150f
    }
}

fun setSystemBrightness(context: Context, value: Int) {
    try {
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            value.coerceIn(10, 255)
        )
    } catch (_: Exception) {}
}

fun getAutoRotate(context: Context): Boolean {
    return try {
        Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION) == 1
    } catch (e: Exception) {
        false
    }
}

fun setAutoRotate(context: Context, enabled: Boolean) {
    try {
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
            if (enabled) 1 else 0
        )
    } catch (_: Exception) {}
}

fun getScreenTimeoutSeconds(context: Context): Int {
    return try {
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT) / 1000
    } catch (e: Exception) {
        60
    }
}

fun setScreenTimeoutSeconds(context: Context, seconds: Int) {
    try {
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_OFF_TIMEOUT,
            seconds * 1000
        )
        Toast.makeText(context, "System Screen Timeout set to ${seconds}s", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {}
}

fun getMediaVolume(context: Context): Float {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
    return cur.toFloat() / max.toFloat()
}

fun setMediaVolume(context: Context, fraction: Float) {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val target = (fraction * max).toInt().coerceIn(0, max)
    am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
}

fun openSystemPanel(context: Context, panelAction: String) {
    try {
        val intent = Intent(panelAction).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
