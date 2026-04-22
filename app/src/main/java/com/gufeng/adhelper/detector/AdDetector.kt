package com.gufeng.adhelper.detector

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.graphics.Rect
import com.gufeng.adhelper.utils.PreferencesManager

/**
 * 广告检测器
 * 作者：古封
 * 功能：检测并识别汽水音乐中的广告元素
 */
class AdDetector(private val accessibilityService: AccessibilityService) {

    companion object {
        private const val TAG = "AdHelper-Detector"
    }

    private val preferencesManager = PreferencesManager(accessibilityService)
    
    // 屏幕尺寸
    val screenWidth = accessibilityService.resources.displayMetrics.widthPixels
    val screenHeight = accessibilityService.resources.displayMetrics.heightPixels
    
    // 当前检测到的秒数
    var countdownSeconds = 0
        private set
    
    /**
     * 广告优先级枚举
     * 倒计时 > 弹窗 > 领取成功关闭 > 领取按钮
     */
    enum class AdPriority {
        NONE,
        COUNTDOWN,
        POPUP,
        COLLECT_SUCCESS_CLOSE,
        COLLECT_BUTTON
    }
    
    /**
     * 广告信息数据类
     */
    data class AdInfo(
        val priority: AdPriority,
        val clickNode: AccessibilityNodeInfo?,
        val description: String = ""
    )

    // 广告关键词列表 - 汽水音乐专用
    private val popupKeywords = listOf(
        "广告", "观看激励视频", "解锁完整内容", "看广告免费听", "领取奖励", "继续观看"
    )
    
    private val collectSuccessKeywords = listOf(
        "领取成功", "已领取", "恭喜获得", "已获得", "领取成功X", "领取成功×", "可关闭"
    )
    
    private val collectButtonKeywords = listOf(
        "领取奖励", "继续观看", "再领一次", "可领取", "立即领取", "点击领取"
    )
    
    // 倒计时关键词 - 完整覆盖
    private val countdownKeywords = listOf(
        "秒后可领取奖励", "秒后可领取", "秒后领取奖励", "秒后可关闭",
        "秒后关闭", "秒后领取", "等待", "倒计时", "跳过广告"
    )
    
    private val closeButtonKeywords = listOf(
        "关闭", "skip", "SKIP", "跳过", "×", "✕", "✖", "X", "x", "close"
    )
    
    // 静音关键词 - 扩大范围
    private val muteKeywords = listOf(
        "广告", "声音", "喇叭", "音量", "静音", "mute", "volume", "speaker", "audio"
    )

    /**
     * 检测广告 - 主入口
     */
    fun detectAd(rootNode: AccessibilityNodeInfo): AdInfo {
        if (!preferencesManager.isServiceEnabled) {
            return AdInfo(AdPriority.NONE, null)
        }
        
        Log.d(TAG, "开始检测，屏幕尺寸: ${screenWidth}x${screenHeight}")
        
        // 调试：打印所有节点（限制深度防止日志爆炸）
        printAllNodes(rootNode, 0, 3)
        
        // 0. 优先检测倒计时（触发静音）
        val countdownNode = findCountdownNode(rootNode)
        if (countdownNode != null) {
            return AdInfo(AdPriority.COUNTDOWN, countdownNode, "检测到倒计时")
        }
        
        // 1. 检测弹窗
        val popupNode = findPopupNode(rootNode)
        if (popupNode != null) {
            Log.d(TAG, "检测到弹窗广告")
            return AdInfo(AdPriority.POPUP, popupNode, "检测到弹窗广告")
        }
        
        // 2. 检测领取成功关闭按钮
        val collectSuccessNode = findCollectSuccessNode(rootNode)
        if (collectSuccessNode != null) {
            Log.d(TAG, "检测到领取成功关闭")
            return AdInfo(AdPriority.COLLECT_SUCCESS_CLOSE, collectSuccessNode, "检测到领取成功关闭")
        }
        
        // 3. 检测领取按钮
        val collectButtonNode = findCollectButtonNode(rootNode)
        if (collectButtonNode != null) {
            Log.d(TAG, "检测到领取按钮")
            return AdInfo(AdPriority.COLLECT_BUTTON, collectButtonNode, "检测到领取按钮")
        }
        
        return AdInfo(AdPriority.NONE, null)
    }

    /**
     * 查找倒计时节点 - 专门用于触发静音
     */
    private fun findCountdownNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val clickableNodes = mutableListOf<AccessibilityNodeInfo>()
        findClickableNodes(rootNode, clickableNodes)
        
        for (node in clickableNodes) {
            val text = node.text?.toString() ?: ""
            val contentDesc = node.contentDescription?.toString() ?: ""
            val fullText = "$text $contentDesc"
            
            // 提取秒数
            val seconds = extractSeconds(fullText)
            if (seconds > 0) {
                countdownSeconds = seconds
                Log.d(TAG, "【倒计时检测】${seconds}秒 - 文字: $fullText")
                return node
            }
            
            // 检查关键词
            for (keyword in countdownKeywords) {
                if (fullText.contains(keyword)) {
                    countdownSeconds = extractSeconds(fullText)
                    Log.d(TAG, "【倒计时检测】${countdownSeconds}秒 - 匹配关键词: $keyword")
                    return node
                }
            }
        }
        
        // 检查所有文本节点（包括不可点击的）
        val allNodes = mutableListOf<AccessibilityNodeInfo>()
        findAllNodes(rootNode, allNodes)
        
        for (node in allNodes) {
            val text = node.text?.toString() ?: ""
            val contentDesc = node.contentDescription?.toString() ?: ""
            val fullText = "$text $contentDesc"
            
            for (keyword in countdownKeywords) {
                if (fullText.contains(keyword)) {
                    val seconds = extractSeconds(fullText)
                    countdownSeconds = if (seconds > 0) seconds else 30
                    Log.d(TAG, "【倒计时文本检测】${countdownSeconds}秒 - $fullText")
                    return node
                }
            }
        }
        
        return null
    }

    /**
     * 提取秒数
     */
    private fun extractSeconds(text: String): Int {
        val pattern = Regex("(\\d+)\\s*秒")
        val match = pattern.find(text)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    }

    /**
     * 查找弹窗关闭按钮
     */
    private fun findPopupNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val clickableNodes = mutableListOf<AccessibilityNodeInfo>()
        findClickableNodes(rootNode, clickableNodes)
        
        for (node in clickableNodes) {
            val text = node.text?.toString()?.lowercase() ?: ""
            val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
            
            // 检查弹窗关键词
            for (keyword in popupKeywords) {
                if (text.contains(keyword) || contentDesc.contains(keyword)) {
                    return node
                }
            }
            
            // 检查关闭图标
            for (keyword in closeButtonKeywords) {
                if (text.contains(keyword) || contentDesc.contains(keyword)) {
                    val bounds = Rect()
                    node.getBoundsInScreen(bounds)
                    // 只检测右上角区域
                    if (bounds.left >= screenWidth * 0.7f && bounds.top <= screenHeight * 0.3f) {
                        return node
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
            
            for (keyword in collectSuccessKeywords) {
                if (text.contains(keyword) || contentDesc.contains(keyword)) {
                    Log.d(TAG, "【领取成功】找到: $text")
                    return node
                }
            }
            
            // 检查"知道了"等确认按钮
            val confirmKeywords = listOf("知道了", "好的", "确定")
            for (keyword in confirmKeywords) {
                if (text.contains(keyword)) {
                    return node
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
        
        for (node in clickableNodes) {
            val text = node.text?.toString() ?: ""
            val contentDesc = node.contentDescription?.toString() ?: ""
            
            for (keyword in collectButtonKeywords) {
                if (text.contains(keyword) || contentDesc.contains(keyword)) {
                    Log.d(TAG, "【领取按钮】找到: $text")
                    return node
                }
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
     * 递归获取所有节点
     */
    private fun findAllNodes(
        node: AccessibilityNodeInfo,
        result: MutableList<AccessibilityNodeInfo>
    ) {
        result.add(node)
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findAllNodes(child, result)
            }
        }
    }

    /**
     * 检测并点击静音喇叭（左上角）
     */
    fun detectAndClickMute(rootNode: AccessibilityNodeInfo): Boolean {
        val allNodes = mutableListOf<AccessibilityNodeInfo>()
        findAllNodes(rootNode, allNodes)
        
        // 左上角区域：左侧30%，上方30%
        val leftEnd = (screenWidth * 0.3f).toInt()
        val topEnd = (screenHeight * 0.3f).toInt()
        
        for (node in allNodes) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            
            // 检查是否在左上角区域
            if (bounds.left <= leftEnd && bounds.top <= topEnd) {
                val text = node.text?.toString()?.lowercase() ?: ""
                val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
                val className = node.className?.toString()?.lowercase() ?: ""
                
                // 检查是否包含静音相关关键词
                if (muteKeywords.any { text.contains(it) || contentDesc.contains(it) || className.contains(it) }) {
                    Log.d(TAG, "【静音检测】找到喇叭: text=$text, desc=$contentDesc, bounds=$bounds")
                    if (performClick(node)) {
                        Log.d(TAG, "【静音成功】广告声音已关闭")
                        return true
                    }
                }
            }
        }
        
        Log.d(TAG, "【静音检测】未找到喇叭图标")
        return false
    }

    /**
     * 执行点击
     */
    fun performClick(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        
        if (node.isClickable) {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.d(TAG, "【点击结果】${if (result) "成功" else "失败"}")
            return result
        }
        
        // 尝试父节点
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable) {
                val result = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                parent.recycle()
                Log.d(TAG, "【点击父节点结果】${if (result) "成功" else "失败"}")
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
            "AdActivity", "RewardActivity", "VideoActivity", "Interstitial", "ad"
        )
        return adActivityKeywords.any { className.contains(it, ignoreCase = true) }
    }
    
    /**
     * 打印所有节点（用于调试）
     */
    private fun printAllNodes(node: AccessibilityNodeInfo, depth: Int, maxDepth: Int) {
        if (depth > maxDepth) return
        
        val indent = "  ".repeat(depth)
        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        val className = node.className?.toString() ?: ""
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        
        // 只打印有内容的节点
        if (text.isNotEmpty() || contentDesc.isNotEmpty()) {
            Log.d(TAG, "${indent}[$className] text='$text' desc='$contentDesc' bounds=$bounds clickable=${node.isClickable}")
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                printAllNodes(child, depth + 1, maxDepth)
                child.recycle()
            }
        }
    }
}
