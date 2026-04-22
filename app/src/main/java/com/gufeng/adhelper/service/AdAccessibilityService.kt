package com.gufeng.adhelper.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
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
        preferencesManager = PreferencesManager(this)
        adDetector = AdDetector(this)
        stateMachine = StateMachine()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or 
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                        AccessibilityEvent.TYPE_TOAST_CLOSED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                   AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        val packageName = event.packageName?.toString() ?: return
        
        // 只处理汽水音乐
        if (packageName != TARGET_PACKAGE) {
            isInTargetApp = false
            stateMachine.reset()
            return
        }
        
        isInTargetApp = true
        lastEventTime = System.currentTimeMillis()
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleContentChanged(event)
            }
            AccessibilityEvent.TYPE_TOAST_CLOSED -> {
                handleToastClosed(event)
            }
        }
    }

    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val className = event.className?.toString() ?: return
        
        // 检测是否进入广告页面
        if (adDetector.isAdActivity(className)) {
            stateMachine.transitionTo(StateMachine.State.AD_SHOWN)
            findAndClickAdNodes(rootInActiveWindow)
        }
    }

    private fun handleContentChanged(event: AccessibilityEvent) {
        if (!isServiceRunning || !preferencesManager.isServiceEnabled) return
        
        val rootNode = rootInActiveWindow ?: return
        
        try {
            // 按照优先级检测广告元素
            val adInfo = adDetector.detectAd(rootNode)
            
            when (adInfo.priority) {
                AdDetector.AdPriority.POPUP -> {
                    // 最高优先级：弹窗
                    stateMachine.transitionTo(StateMachine.State.POPUP_DETECTED)
                    adInfo.clickNode?.let { node ->
                        adDetector.performClick(node)
                        incrementSkipCount()
                        preferencesManager.incrementTodaySkipCount()
                        stateMachine.transitionTo(StateMachine.State.IDLE)
                    }
                }
                AdDetector.AdPriority.COLLECT_SUCCESS_CLOSE -> {
                    // 领取成功关闭
                    stateMachine.transitionTo(StateMachine.State.COLLECT_SUCCESS)
                    adInfo.clickNode?.let { node ->
                        adDetector.performClick(node)
                        incrementSkipCount()
                        preferencesManager.incrementTodaySkipCount()
                        stateMachine.transitionTo(StateMachine.State.IDLE)
                    }
                }
                AdDetector.AdPriority.COLLECT_BUTTON -> {
                    // 领取按钮
                    stateMachine.transitionTo(StateMachine.State.COLLECT_BUTTON)
                    adInfo.clickNode?.let { node ->
                        adDetector.performClick(node)
                    }
                }
                AdDetector.AdPriority.COUNTDOWN -> {
                    // 倒计时跳过
                    if (stateMachine.canSkip()) {
                        stateMachine.transitionTo(StateMachine.State.SKIPPING)
                        adInfo.clickNode?.let { node ->
                            adDetector.performClick(node)
                            incrementSkipCount()
                            preferencesManager.incrementTodaySkipCount()
                            stateMachine.transitionTo(StateMachine.State.IDLE)
                        }
                    }
                }
                AdDetector.AdPriority.NONE -> {
                    // 无广告
                    stateMachine.transitionTo(StateMachine.State.IDLE)
                }
            }
        } finally {
            rootNode.recycle()
        }
    }

    private fun handleToastClosed(event: AccessibilityEvent) {
        // 处理toast关闭事件
    }

    private fun findAndClickAdNodes(rootNode: AccessibilityNodeInfo?) {
        if (rootNode == null || !isServiceRunning) return
        
        try {
            val adInfo = adDetector.detectAd(rootNode)
            adInfo.clickNode?.let { node ->
                adDetector.performClick(node)
                incrementSkipCount()
                preferencesManager.incrementTodaySkipCount()
            }
        } finally {
            rootNode.recycle()
        }
    }

    override fun onInterrupt() {
        // 服务中断处理
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        stateMachine.reset()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isServiceRunning = false
        return super.onUnbind(intent)
    }
}
