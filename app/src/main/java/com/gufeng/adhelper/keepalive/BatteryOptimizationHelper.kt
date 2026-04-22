package com.gufeng.adhelper.keepalive

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * 电池优化引导工具
 * 作者：古封
 * 功能：引导用户关闭电池优化，确保后台服务稳定运行
 */
class BatteryOptimizationHelper(private val context: Context) {

    /**
     * 检查是否已关闭电池优化
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * 请求关闭电池优化
     */
    fun requestIgnoreBatteryOptimizations(): Intent? {
        if (isIgnoringBatteryOptimizations()) {
            return null
        }
        
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    /**
     * 获取电池优化设置Intent
     */
    fun getBatteryOptimizationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }

    /**
     * 获取引导说明文本
     */
    fun getGuideText(): String {
        return """
            为了确保广告助手在后台稳定运行，请按以下步骤操作：
            
            1. 点击「去设置」按钮
            2. 找到「汽水音乐广告助手」
            3. 选择「不限制」或「允许后台活动」
            4. 确认完成
            
            这样可以确保：
            • 应用在后台持续运行
            • 广告跳过功能正常工作
            • 不会因为省电策略被系统杀后台
        """.trimIndent()
    }

    /**
     * 获取简要提示
     */
    fun getBriefTip(): String {
        return "关闭电池限制，让广告助手稳定运行"
    }
}
