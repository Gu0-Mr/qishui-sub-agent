package com.gufeng.adhelper.utils

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 偏好设置管理器
 * 作者：古封
 * 功能：管理应用的所有持久化设置
 */
class PreferencesManager(context: Context) {

    companion object {
        private const val PREF_NAME = "ad_helper_prefs"
        
        // Key定义
        private const val KEY_SERVICE_ENABLED = "service_enabled"
        private const val KEY_KEEP_ALIVE_ENABLED = "keep_alive_enabled"
        private const val KEY_FLOAT_WINDOW_ENABLED = "float_window_enabled"
        private const val KEY_AUTO_START_ENABLED = "auto_start_enabled"
        
        private const val KEY_TODAY_SKIP_COUNT = "today_skip_count"
        private const val KEY_TOTAL_SKIP_COUNT = "total_skip_count"
        private const val KEY_LAST_SKIP_DATE = "last_skip_date"
        
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_FIRST_LAUNCH_TIME = "first_launch_time"
        
        private const val KEY_DETECTION_DELAY = "detection_delay"
        private const val KEY_SKIP_INTERVAL = "skip_interval"
        
        private const val KEY_POPUP_ENABLED = "popup_enabled"
        private const val KEY_COLLECT_BUTTON_ENABLED = "collect_button_enabled"
        private const val KEY_COUNTDOWN_ENABLED = "countdown_enabled"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // ========== 服务开关 ==========

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SERVICE_ENABLED, value).apply()

    var isKeepAliveEnabled: Boolean
        get() = prefs.getBoolean(KEY_KEEP_ALIVE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_ALIVE_ENABLED, value).apply()

    var isFloatWindowEnabled: Boolean
        get() = prefs.getBoolean(KEY_FLOAT_WINDOW_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_FLOAT_WINDOW_ENABLED, value).apply()

    var isAutoStartEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_START_ENABLED, value).apply()

    // ========== 统计计数 ==========

    var todaySkipCount: Int
        get() = prefs.getInt(KEY_TODAY_SKIP_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_TODAY_SKIP_COUNT, value).apply()

    var totalSkipCount: Int
        get() = prefs.getInt(KEY_TOTAL_SKIP_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_SKIP_COUNT, value).apply()

    /**
     * 递增今日跳过计数
     */
    fun incrementTodaySkipCount() {
        todaySkipCount++
        totalSkipCount++
        prefs.edit().putString(KEY_LAST_SKIP_DATE, getCurrentDate()).apply()
    }

    /**
     * 重置今日跳过计数
     */
    fun resetTodaySkipCount() {
        todaySkipCount = 0
    }

    /**
     * 检查并重置每日统计
     */
    fun checkAndResetDaily() {
        val lastDate = prefs.getString(KEY_LAST_SKIP_DATE, "") ?: ""
        val currentDate = getCurrentDate()
        
        if (lastDate != currentDate) {
            todaySkipCount = 0
            prefs.edit().putString(KEY_LAST_SKIP_DATE, currentDate).apply()
        }
    }

    // ========== 首次启动 ==========

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()

    var firstLaunchTime: Long
        get() = prefs.getLong(KEY_FIRST_LAUNCH_TIME, 0)
        set(value) = prefs.edit().putLong(KEY_FIRST_LAUNCH_TIME, value).apply()

    /**
     * 标记首次启动完成
     */
    fun markFirstLaunchComplete() {
        isFirstLaunch = false
        if (firstLaunchTime == 0L) {
            firstLaunchTime = System.currentTimeMillis()
        }
    }

    // ========== 检测参数 ==========

    var detectionDelay: Long
        get() = prefs.getLong(KEY_DETECTION_DELAY, 300L)
        set(value) = prefs.edit().putLong(KEY_DETECTION_DELAY, value).apply()

    var skipInterval: Long
        get() = prefs.getLong(KEY_SKIP_INTERVAL, 500L)
        set(value) = prefs.edit().putLong(KEY_SKIP_INTERVAL, value).apply()

    // ========== 功能开关 ==========

    var isPopupEnabled: Boolean
        get() = prefs.getBoolean(KEY_POPUP_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_POPUP_ENABLED, value).apply()

    var isCollectButtonEnabled: Boolean
        get() = prefs.getBoolean(KEY_COLLECT_BUTTON_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_COLLECT_BUTTON_ENABLED, value).apply()

    var isCountdownEnabled: Boolean
        get() = prefs.getBoolean(KEY_COUNTDOWN_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_COUNTDOWN_ENABLED, value).apply()

    // ========== 工具方法 ==========

    /**
     * 获取当前日期字符串
     */
    private fun getCurrentDate(): String {
        return dateFormat.format(Date())
    }

    /**
     * 获取应用使用天数
     */
    fun getUsageDays(): Int {
        return if (firstLaunchTime > 0) {
            ((System.currentTimeMillis() - firstLaunchTime) / (24 * 60 * 60 * 1000)).toInt() + 1
        } else {
            0
        }
    }

    /**
     * 获取平均每日跳过数
     */
    fun getAverageDailySkipCount(): Int {
        val days = getUsageDays()
        return if (days > 0) totalSkipCount / days else 0
    }

    /**
     * 清除所有数据
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    /**
     * 注册偏好设置监听器
     */
    fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    /**
     * 注销偏好设置监听器
     */
    fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
