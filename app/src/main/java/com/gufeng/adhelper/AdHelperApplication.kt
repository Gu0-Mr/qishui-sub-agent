package com.gufeng.adhelper

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.gufeng.adhelper.utils.PreferencesManager

/**
 * Application类
 * 作者：古封
 * 功能：应用初始化、通知渠道创建
 */
class AdHelperApplication : Application() {

    companion object {
        const val CHANNEL_ID_MAIN = "ad_helper_main"
        const val CHANNEL_NAME_MAIN = "广告助手通知"
        
        const val CHANNEL_ID_KEEPALIVE = "ad_helper_keepalive"
        const val CHANNEL_NAME_KEEPALIVE = "后台保活服务"
        
        const val CHANNEL_ID_FLOAT = "ad_helper_float_window"
        const val CHANNEL_NAME_FLOAT = "悬浮窗服务"
        
        const val CHANNEL_ID_ALERT = "ad_helper_alert"
        const val CHANNEL_NAME_ALERT = "重要提醒"
    }

    lateinit var preferencesManager: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        
        // 初始化偏好设置管理器
        preferencesManager = PreferencesManager(this)
        
        // 创建通知渠道
        createNotificationChannels()
        
        // 检查日期变更
        checkDateChange()
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            
            // 主通知渠道
            val mainChannel = NotificationChannel(
                CHANNEL_ID_MAIN,
                CHANNEL_NAME_MAIN,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "广告助手的主要通知"
                enableLights(true)
                lightColor = 0xFF4CAF50.toInt()
            }
            
            // 保活服务渠道
            val keepAliveChannel = NotificationChannel(
                CHANNEL_ID_KEEPALIVE,
                CHANNEL_NAME_KEEPALIVE,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "后台保活服务通知"
                setShowBadge(false)
            }
            
            // 悬浮窗服务渠道
            val floatChannel = NotificationChannel(
                CHANNEL_ID_FLOAT,
                CHANNEL_NAME_FLOAT,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "悬浮窗显示服务"
                setShowBadge(false)
            }
            
            // 重要提醒渠道
            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERT,
                CHANNEL_NAME_ALERT,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "重要功能提醒"
                enableVibration(true)
            }
            
            notificationManager.createNotificationChannels(
                listOf(mainChannel, keepAliveChannel, floatChannel, alertChannel)
            )
        }
    }

    /**
     * 检查日期变更，重置每日统计
     */
    private fun checkDateChange() {
        preferencesManager.checkAndResetDaily()
    }
}
