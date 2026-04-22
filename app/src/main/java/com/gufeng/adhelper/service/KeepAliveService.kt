package com.gufeng.adhelper.service

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
import com.gufeng.adhelper.R
import com.gufeng.adhelper.ui.MainActivity
import com.gufeng.adhelper.utils.PreferencesManager

/**
 * 前台服务保活
 * 作者：古封
 * 功能：保持应用在后台运行，确保无障碍服务正常工作
 */
class KeepAliveService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "ad_helper_keepalive"
        private const val CHANNEL_NAME = "后台保活服务"
        
        @Volatile
        var isRunning = false
            private set
        
        fun start(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            context.stopService(Intent(context, KeepAliveService::class.java))
        }
    }

    private lateinit var notificationManager: NotificationManager
    private lateinit var preferencesManager: PreferencesManager
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        preferencesManager = PreferencesManager(this)
        
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        
        // 定期更新通知
        updateNotification()
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        releaseWakeLock()
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持广告助手在后台运行"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 创建前台通知
     */
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val skipCount = AdAccessibilityService.skipCount
        val serviceStatus = if (AdAccessibilityService.isServiceRunning) "运行中" else "未启动"
        
        val contentText = if (AdAccessibilityService.isServiceRunning) {
            "已跳过 $skipCount 次广告 • 服务$serviceStatus"
        } else {
            "请点击开启无障碍服务"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("汽水音乐广告助手")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    /**
     * 更新通知
     */
    private fun updateNotification() {
        if (preferencesManager.isKeepAliveEnabled) {
            notificationManager.notify(NOTIFICATION_ID, createNotification())
        }
    }

    /**
     * 获取WakeLock
     */
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AdHelper::KeepAliveWakeLock"
            ).apply {
                acquire(10 * 60 * 1000L) // 10分钟
            }
        }
    }

    /**
     * 释放WakeLock
     */
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }
}
