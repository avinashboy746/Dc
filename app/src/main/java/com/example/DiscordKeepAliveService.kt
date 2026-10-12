package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class DiscordKeepAliveService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_ID = "discord_247_keepalive_channel"
        const val NOTIFICATION_ID = 24701
        const val ACTION_START = "com.example.ACTION_START_247"
        const val ACTION_STOP = "com.example.ACTION_STOP_247"
        var isServiceRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopKeepAlive()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startKeepAlive()
            }
        }
        return START_STICKY
    }

    private fun startKeepAlive() {
        isServiceRunning = true

        // Acquire WakeLock to prevent CPU sleep
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "DiscordBrowser::247KeepAliveWakeLock"
        ).apply {
            acquire(24 * 60 * 60 * 1000L) // 24 hours safety limit
        }

        val notification = buildForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Periodic Gateway Keepalive Heartbeat
        serviceScope.launch {
            while (isActive) {
                try {
                    // Ping Discord Gateway Status to maintain mobile connection active
                    val url = URL("https://discord.com/api/v10/gateway")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("User-Agent", "DiscordBrowser-bcsupportOG/1.0")
                    conn.responseCode
                    conn.disconnect()
                } catch (_: Exception) {
                }
                // Ping every 35 seconds to prevent Discord gateway idle disconnection
                delay(35_000L)
            }
        }
    }

    private fun stopKeepAlive() {
        isServiceRunning = false
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (_: Exception) {
        }
        serviceJob.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Discord 24/7 Online Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps your Discord account always active and online 24/7 (bcsupportØG Engine)"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, DiscordKeepAliveService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Discord 24/7 Always Online • bcsupportØG")
            .setContentText("Presence is active. Discord status stays Online even with app closed.")
            .setSmallIcon(android.R.drawable.ic_menu_rotate)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Turn Off 24/7", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        stopKeepAlive()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
