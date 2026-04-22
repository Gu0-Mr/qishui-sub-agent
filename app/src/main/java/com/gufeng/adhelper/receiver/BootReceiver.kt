package com.gufeng.adhelper.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gufeng.adhelper.service.KeepAliveService
import com.gufeng.adhelper.utils.PreferencesManager

/**
 * 开机广播接收器
 * 作者：古封
 * 功能：设备启动后自动启动后台保活服务
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        
        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == "android.intent.action.QUICKBOOT_POWERON") {
            
            val preferencesManager = PreferencesManager(context)
            
            // 如果启用了开机自启
            if (preferencesManager.isAutoStartEnabled) {
                startKeepAliveService(context)
            }
        }
    }

    /**
     * 启动保活服务
     */
    private fun startKeepAliveService(context: Context) {
        try {
            KeepAliveService.start(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
