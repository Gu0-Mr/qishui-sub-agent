package com.gufeng.adhelper.detector

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import android.graphics.Rect
import com.gufeng.adhelper.utils.PreferencesManager

/**
 * 广告检测器
 * 作者：古封
 * 功能：检测并识别汽水音乐中的广告元素
 */
class AdDetector(private val accessibilityService: AccessibilityService) {

    private val preferencesManager = PreferencesManager(accessibilityService)
    
    // 屏幕尺寸
    private val screenWidth = accessibilityService.resources.displayMetrics.widthPixels
    private val screenHeight = accessibilityService.resources.displayMetrics.heightPixels
    
    /**
     * 广告优先级枚举
     * 弹窗 > 领取成功关闭 > 领取按钮 > 倒计时
     */
    enum class AdPriority {
        NONE,
        COUNTDOWN,
        COLLECT_BUTTON,
        COLLECT_SUCCESS_CLOSE,
        POPUP
    }
    
    /**
     * 广告信息数据类
     */
    data class AdInfo(
        val priority: AdPriority,
        val clickNode: AccessibilityNodeInfo?,
        val description: String = ""
    )

    // 广告关键词列表 - 扩充版本
    private val popupKeywords = listOf(
        "广告", "观看激励视频", "解锁完整内容", "看广告免费听", "领取奖励", "继续观看"
    )
    
    private val collectSuccessKeywords = listOf(
        "领取成功", "已领取", "领取成功可关闭", "恭喜获得", "已获得", "领取成功X", "领取成功×"
    )
    
    private val collectButtonKeywords = listOf(
        "领取奖励", "继续观看", "再领一次", "领取", "可领取", "立即领取", "点击领取"
    )
    
    private val countdownKeywords = listOf(
        "秒后可领取", "秒后领取", "等待", "倒计时", "跳过广告", "关闭广告"
    )
    
    private val closeButtonKeywords = listOf(
        "关闭", "skip", "SKIP", "跳过", "×", "✕", "✖", "X", "x", "close", "关闭广告"
    )
    
    // 静音关键词
    private val muteKeywords = listOf(
        "广告", "声音", "喇叭", "音量", "mute", "volume", "speaker"
    )

    /**
     * 检测广告
     */
    fun detectAd(rootNode: AccessibilityNodeInfo): AdInfo {
        if (!preferencesManager.isServiceEnabled) {
            return AdInfo(AdPriority.NONE, null)
        }
        
        // 1. 优先检测弹窗
        val popupNode = findPopupNode(rootNode)
        if (popupNode != null) {
            return AdInfo(AdPriority.POPUP, popupNode, "检测到弹窗广告")
        }
        
        // 2. 检测领取成功关闭按钮
        val collectSuccessNode = findCollectSuccessNode(rootNode)
        if (collectSuccessNode != null) {
            return AdInfo(AdPriority.COLLECT_SUCCESS_CLOSE, collectSuccessNode, "检测到领取成功关闭")
        }
        
        // 3. 检测领取按钮
        val collectButtonNode = findCollectButtonNode(rootNode)
        if (collectButtonNode != null) {
            return AdInfo(AdPriority.COLLECT_BUTTON, collectButtonNode, "检测到领取按钮")
        }
        
        // 4. 检测倒计时跳过按钮
        val countdownNode = findCountdownSkipNode(rootNode)
        if (countdownNode != null) {
            return AdInfo(AdPriority.COUNTDOWN, countdownNode, "检测到倒计时跳过")
        }
        
        return AdInfo(AdPriority.NONE, null)
    }

    /**
     * 查找弹窗关闭按钮
     */
    private fun findPopupNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // 查找所有可点击的节点
        val clickableNodes = mutableListOf<AccessibilityNodeInfo>()
        findClickableNodes(rootNode, clickableNodes)
        
        for (node in clickableNodes) {
            val text = node.text?.toString()?.lowercase() ?: ""
            val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
            
            // 检查是否是弹窗广告的关闭按钮
            for (keyword in popupKeywords) {
                if (text.contains(keyword) || contentDesc.contains(keyword)) {
                    return node
                }
            }
            
            // 检查关闭图标（X符号等）
            for (keyword in closeButtonKeywords) {
                if (text.contains(keyword) || contentDesc.contains(keyword)) {
                    // 确保不是在主要内容区域
                    val parent = node.parent
                    if (parent != null) {
                        val parentClass = parent.className?.toString() ?: ""
                        // 如果父级是弹窗类型
                        if (parentClass.contains("Dialog") || 
                            parentClass.contains("Popup") ||
                            parentClass.contains("Window")) {
                            return node
                        }
                        parent.recycle()
                    }
                }
            }
        }
        
        return null
    }

    /**
     * 查找领取成功关闭节点
     */
    private fun findCollectSuccessNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val clickableNodes = mutableListOf<AccessibilityNodeInfo>()
        findClickableNodes(rootNode, clickableNodes)
        
        for (node in clickableNodes) {
            val text = node.text?.toString() ?: ""
            val contentDesc = node.contentDescription?.toString() ?: ""
            
            // 检查领取成功相关关键词
            for (keyword in collectSuccessKeywords) {
                if (text.contains(keyword) || contentDesc.contains(keyword)) {
                    return node
                }
            }
            
            // 检查"知道了"、"好的"等确认按钮
            val confirmKeywords = listOf("知道了", "好的", "确定", "我知道了")
            for (keyword in confirmKeywords) {
                if (text.contains(keyword)) {
                    // 检查周围是否有成功相关的元素
                    val parent = node.parent
                    if (parent != null) {
                        val parentText = parent.text?.toString() ?: ""
                        val parentDesc = parent.contentDescription?.toString() ?: ""
                        for (successKeyword in collectSuccessKeywords) {
                            if (parentText.contains(successKeyword) || 
                                parentDesc.contains(successKeyword)) {
                                parent.recycle()
                                return node
                            }
                        }
                        parent.recycle()
                    }
                }
            }
        }
        
        return null
    }

    /**
     * 查找领取按钮
     */
    private fun findCollectButtonNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val clickableNodes = mutableListOf<AccessibilityNodeInfo>()
        findClickableNodes(rootNode, clickableNodes)
        
        // 按优先级排序
        val prioritizedNodes = mutableListOf<Pair<AccessibilityNodeInfo, Int>>()
        
        for (node in clickableNodes) {
            val text = node.text?.toString() ?: ""
            val contentDesc = node.contentDescription?.toString() ?: ""
            
            var priority = 0
            
            // 高优先级：可领取奖励
            for (keyword in collectButtonKeywords) {
                if (text.contains(keyword) || contentDesc.contains(keyword)) {
                    priority = when {
                        keyword in listOf("可领取", "立即领取") -> 3
                        keyword == "领取奖励" -> 2
                        else -> 1
                    }
                    break
                }
            }
            
            // 检查秒后可领取（需要等待）
            for (keyword in countdownKeywords) {
                if (text.contains(keyword)) {
                    priority = 0
                    break
                }
            }
            
            if (priority > 0) {
                prioritizedNodes.add(node to priority)
            }
        }
        
        // 返回优先级最高的节点
        return prioritizedNodes.maxByOrNull { it.second }?.first
    }

    /**
     * 查找倒计时跳过按钮
     */
    private fun findCountdownSkipNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val clickableNodes = mutableListOf<AccessibilityNodeInfo>()
        findClickableNodes(rootNode, clickableNodes)
        
        for (node in clickableNodes) {
            val text = node.text?.toString() ?: ""
            val contentDesc = node.contentDescription?.toString() ?: ""
            
            // 检查倒计时跳过相关
            for (keyword in countdownKeywords) {
                if (text.contains(keyword) || contentDesc.contains(keyword)) {
                    // 检查是否可点击
                    if (node.isClickable) {
                        return node
                    }
                }
            }
            
            // 检查数字+秒的格式（如"5秒"）
            val numberPattern = Regex("^\\d+\\s*秒")
            if (numberPattern.containsMatchIn(text)) {
                return node
            }
        }
        
        return null
    }

    /**
     * 递归查找所有可点击的节点
     */
    private fun findClickableNodes(
        node: AccessibilityNodeInfo,
        result: MutableList<AccessibilityNodeInfo>
    ) {
        if (node.isClickable && node.isEnabled) {
            result.add(node)
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findClickableNodes(child, result)
            }
        }
    }

    /**
     * 执行点击
     */
    fun performClick(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        
        // 如果当前节点不可点击，尝试父节点
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable) {
                val result = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                parent.recycle()
                return result
            }
            val temp = parent
            parent = parent.parent
            temp.recycle()
        }
        
        return false
    }

    /**
     * 判断是否是广告Activity
     */
    fun isAdActivity(className: String): Boolean {
        val adActivityKeywords = listOf(
            "AdActivity", "RewardActivity", "VideoActivity", "Interstitial"
        )
        return adActivityKeywords.any { className.contains(it, ignoreCase = true) }
    }
    
    /**
     * 检测右上角区域（扩大范围）
     */
    private fun isInTopRightArea(bounds: Rect): Boolean {
        val rightStart = (screenWidth * 0.6f).toInt()
        val topEnd = (screenHeight * 0.4f).toInt()
        return bounds.left >= rightStart && bounds.top <= topEnd
    }
    
    /**
     * 检测左上角区域（用于静音按钮）
     */
    private fun isInTopLeftArea(bounds: Rect): Boolean {
        val leftEnd = (screenWidth * 0.3f).toInt()
        val topEnd = (screenHeight * 0.3f).toInt()
        return bounds.left <= leftEnd && bounds.top <= topEnd
    }
    
    /**
     * 检测静音按钮（左上角喇叭图标）
     */
    fun detectMuteButton(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val clickableNodes = mutableListOf<AccessibilityNodeInfo>()
        findClickableNodes(rootNode, clickableNodes)
        
        for (node in clickableNodes) {
            val text = node.text?.toString()?.lowercase() ?: ""
            val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            
            // 只检测左上角区域
            if (isInTopLeftArea(bounds)) {
                if (muteKeywords.any { text.contains(it) || contentDesc.contains(it) }) {
                    return node
                }
            }
        }
        return null
    }
}
