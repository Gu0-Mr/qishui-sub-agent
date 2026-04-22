package com.gufeng.adhelper.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.gufeng.adhelper.detector.AdDetector
import com.gufeng.adhelper.detector.StateMachine
import com.gufeng.adhelper.utils.PreferencesManager

/**
 * 汽水音乐广告助手 - 核心无障碍服务
 * 作者：古封
 * 功能：自动检测并跳过汽水音乐广告
 */
class AdAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AdHelper"
        private const val TARGET_PACKAGE = "com.netease.cloudmusic"
        
        @Volatile
        var isServiceRunning = false
            private set
        
        @Volatile
        var skipCount = 0
            private set
        
        fun incrementSkipCount() {
            skipCount++
        }
        
        fun resetSkipCount() {
            skipCount = 0
        }
    }

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var adDetector: AdDetector
    private lateinit var stateMachine: StateMachine
    
    private var lastEventTime = 0L
    private var isInTargetApp = false

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "=== onCreate: 服务创建 ===")
        preferencesManager = PreferencesManager(this)
        adDetector = AdDetector(this)
        stateMachine = StateMachine()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.e(TAG, "=== onServiceConnected: 无障碍服务已连接 ===")
        isServiceRunning = true
        
        // 强制设置 serviceInfo，监听所有事件
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                   AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                   AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                   AccessibilityServiceInfo.DEFAULT
            notificationTimeout = 100
        }
        serviceInfo = info
        Log.e(TAG, "=== serviceInfo 设置完成，eventTypes = TYPES_ALL_MASK ===")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        Log.e(TAG, "=== 收到事件: ${event?.eventType} ===")
        
        if (event == null) {
            Log.e(TAG, "事件为 null")
            return
        }
        
        val packageName = event.packageName?.toString() ?: ""
        Log.e(TAG, "当前包名: [$packageName]")
        
        // 检查是否是汽水音乐
        if (packageName != TARGET_PACKAGE) {
            Log.e(TAG, "非汽水音乐，跳过。包名: $packageName")
            return
        }
        
        Log.e(TAG, "=== 汽水音乐事件，开始处理 ===")
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                Log.e(TAG, "窗口状态变化")
                handleWindowStateChanged(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                Log.e(TAG, "窗口内容变化")
                handleContentChanged(event)
            }
            else -> {
                Log.e(TAG, "其他事件类型: ${event.eventType}")
                handleContentChanged(event)
            }
        }
    }

    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val className = event.className?.toString() ?: return
        Log.e(TAG, "窗口状态变化: $className")
        
        // 检测是否进入广告页面
        if (adDetector.isAdActivity(className)) {
            Log.e(TAG, "检测到广告Activity")
            stateMachine.transitionTo(StateMachine.State.AD_SHOWN)
            findAndClickAdNodes(rootInActiveWindow)
        }
    }

    private fun handleContentChanged(event: AccessibilityEvent) {
        Log.e(TAG, "=== handleContentChanged 开始 ===")
        
        if (!isServiceRunning) {
            Log.e(TAG, "服务未运行")
            return
        }
        
        if (!preferencesManager.isServiceEnabled) {
            Log.e(TAG, "服务未启用")
            return
        }
        
        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            Log.e(TAG, "rootNode 为 null")
            return
        }
        
        Log.e(TAG, "rootNode 获取成功，开始检测广告")
        
        try {
            // 按照优先级检测广告元素
            val adInfo = adDetector.detectAd(rootNode)
            Log.e(TAG, "检测结果: ${adInfo.priority}, ${adInfo.description}")
            
            when (adInfo.priority) {
                AdDetector.AdPriority.COUNTDOWN -> {
                    // 检测到倒计时，立即尝试静音
                    Log.e(TAG, "【倒计时检测】${adInfo.description}")
                    if (!stateMachine.isMuted()) {
                        val muted = adDetector.detectAndClickMute(rootNode)
                        if (muted) {
                            stateMachine.setMuted(true)
                            Log.e(TAG, "【静音成功】广告声音已关闭")
                        } else {
                            Log.e(TAG, "【静音失败】未找到喇叭图标")
                        }
                    }
                    // 更新状态机
                    stateMachine.transitionTo(StateMachine.State.COUNTDOWN)
                }
                AdDetector.AdPriority.POPUP -> {
                    // 弹窗
                    Log.e(TAG, "【弹窗检测】${adInfo.description}")
                    stateMachine.transitionTo(StateMachine.State.POPUP_DETECTED)
                    adInfo.clickNode?.let { node ->
                        if (adDetector.performClick(node)) {
                            Log.e(TAG, "【点击成功】弹窗关闭")
                            incrementSkipCount()
                            preferencesManager.incrementTodaySkipCount()
                        } else {
                            Log.e(TAG, "【点击失败】弹窗未关闭")
                        }
                        stateMachine.transitionTo(StateMachine.State.IDLE)
                    }
                }
                AdDetector.AdPriority.COLLECT_SUCCESS_CLOSE -> {
                    // 领取成功关闭
                    Log.e(TAG, "【领取成功检测】${adInfo.description}")
                    stateMachine.transitionTo(StateMachine.State.COLLECT_SUCCESS)
                    adInfo.clickNode?.let { node ->
                        if (adDetector.performClick(node)) {
                            Log.e(TAG, "【点击成功】领取成功关闭")
                            incrementSkipCount()
                            preferencesManager.incrementTodaySkipCount()
                        } else {
                            Log.e(TAG, "【点击失败】未关闭")
                        }
                        stateMachine.transitionTo(StateMachine.State.IDLE)
                    }
                }
                AdDetector.AdPriority.COLLECT_BUTTON -> {
                    // 领取按钮
                    Log.e(TAG, "【领取按钮检测】${adInfo.description}")
                    stateMachine.transitionTo(StateMachine.State.COLLECT_BUTTON)
                    adInfo.clickNode?.let { node ->
                        if (adDetector.performClick(node)) {
                            Log.e(TAG, "【点击成功】领取")
                        } else {
                            Log.e(TAG, "【点击失败】未领取")
                        }
                    }
                }
                AdDetector.AdPriority.NONE -> {
                    // 无广告
                    // Log.e(TAG, "【无广告】")
                    stateMachine.transitionTo(StateMachine.State.IDLE)
                }
            }
        } finally {
            rootNode.recycle()
        }
    }

    private fun findAndClickAdNodes(rootNode: AccessibilityNodeInfo?) {
        if (rootNode == null || !isServiceRunning) return
        
        Log.e(TAG, "findAndClickAdNodes 开始")
        
        try {
            val adInfo = adDetector.detectAd(rootNode)
            adInfo.clickNode?.let { node ->
                if (adDetector.performClick(node)) {
                    Log.e(TAG, "【点击成功】广告跳过")
                    incrementSkipCount()
                    preferencesManager.incrementTodaySkipCount()
                }
            }
        } finally {
            rootNode.recycle()
        }
    }

    override fun onInterrupt() {
        Log.e(TAG, "=== onInterrupt: 服务中断 ===")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        stateMachine.reset()
        Log.e(TAG, "=== onDestroy: 服务销毁 ===")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isServiceRunning = false
        Log.e(TAG, "=== onUnbind: 服务解绑 ===")
        return super.onUnbind(intent)
    }
}
