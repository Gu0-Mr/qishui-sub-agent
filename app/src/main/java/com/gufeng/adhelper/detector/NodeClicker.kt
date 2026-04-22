package com.gufeng.adhelper.detector

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo
import android.graphics.Rect
import kotlin.math.abs

/**
 * 节点点击执行器
 * 作者：古封
 * 功能：精确执行节点点击操作，处理各种边界情况
 */
class NodeClicker(private val accessibilityService: AccessibilityService) {

    companion object {
        // 点击重试次数
        private const val MAX_RETRY_COUNT = 3
        
        // 重试间隔（毫秒）
        private const val RETRY_DELAY = 200L
        
        // 双击间隔（毫秒）
        private const val DOUBLE_CLICK_INTERVAL = 100L
        
        // 滑动距离阈值
        private const val SWIPE_DISTANCE_THRESHOLD = 50f
    }

    private val handler = Handler(Looper.getMainLooper())
    
    // 点击统计
    private var totalClicks = 0
    private var successfulClicks = 0

    /**
     * 执行点击
     */
    fun performClick(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        
        totalClicks++
        
        for (attempt in 1..MAX_RETRY_COUNT) {
            if (doClick(node)) {
                successfulClicks++
                return true
            }
            
            if (attempt < MAX_RETRY_COUNT) {
                Thread.sleep(RETRY_DELAY)
            }
        }
        
        return false
    }

    /**
     * 执行双击
     */
    fun performDoubleClick(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        
        if (performClick(node)) {
            handler.postDelayed({
                performClick(node)
            }, DOUBLE_CLICK_INTERVAL)
            return true
        }
        
        return false
    }

    /**
     * 执行长按
     */
    fun performLongClick(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        
        return node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
    }

    /**
     * 执行滑动
     */
    fun performSwipe(node: AccessibilityNodeInfo?, direction: SwipeDirection): Boolean {
        if (node == null) return false
        
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        
        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()
        
        val (startX, startY, endX, endY) = when (direction) {
            SwipeDirection.UP -> quadrupleOf(
                centerX, centerY + SWIPE_DISTANCE_THRESHOLD,
                centerX, centerY - SWIPE_DISTANCE_THRESHOLD
            )
            SwipeDirection.DOWN -> quadrupleOf(
                centerX, centerY - SWIPE_DISTANCE_THRESHOLD,
                centerX, centerY + SWIPE_DISTANCE_THRESHOLD
            )
            SwipeDirection.LEFT -> quadrupleOf(
                centerX + SWIPE_DISTANCE_THRESHOLD, centerY,
                centerX - SWIPE_DISTANCE_THRESHOLD, centerY
            )
            SwipeDirection.RIGHT -> quadrupleOf(
                centerX - SWIPE_DISTANCE_THRESHOLD, centerY,
                centerX + SWIPE_DISTANCE_THRESHOLD, centerY
            )
        }
        
        return accessibilityService.performGlobalAction(
            AccessibilityService.GLOBAL_ACTION_SCROLL_BACKWARD
        )
    }

    /**
     * 查找并点击最佳节点
     */
    fun findAndClickBestNode(
        rootNode: AccessibilityNodeInfo,
        predicates: List<(AccessibilityNodeInfo) -> Boolean>
    ): Boolean {
        for (predicate in predicates) {
            val node = findNode(rootNode, predicate)
            if (node != null) {
                val result = performClick(node)
                node.recycle()
                if (result) return true
            }
        }
        return false
    }

    /**
     * 查找符合条件的节点
     */
    fun findNode(
        rootNode: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (predicate(rootNode)) {
            return rootNode
        }
        
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i) ?: continue
            val result = findNode(child, predicate)
            if (result != null) {
                return result
            }
        }
        
        return null
    }

    /**
     * 查找最近的可点击节点
     */
    fun findNearestClickableNode(
        rootNode: AccessibilityNodeInfo,
        targetX: Int,
        targetY: Int
    ): AccessibilityNodeInfo? {
        val candidates = mutableListOf<Pair<AccessibilityNodeInfo, Float>>()
        
        collectClickableNodes(rootNode, candidates, targetX, targetY)
        
        return candidates.minByOrNull { it.second }?.first
    }

    /**
     * 收集所有可点击节点并计算距离
     */
    private fun collectClickableNodes(
        node: AccessibilityNodeInfo,
        result: MutableList<Pair<AccessibilityNodeInfo, Float>>,
        targetX: Int,
        targetY: Int
    ) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        
        if (node.isClickable && bounds.contains(targetX, targetY)) {
            val distance = calculateDistance(
                bounds.centerX().toFloat(), bounds.centerY().toFloat(),
                targetX.toFloat(), targetY.toFloat()
            )
            result.add(node to distance)
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectClickableNodes(child, result, targetX, targetY)
            }
        }
    }

    /**
     * 计算两点之间的距离
     */
    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return kotlin.math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
    }

    /**
     * 执行点击操作
     */
    private fun doClick(node: AccessibilityNodeInfo): Boolean {
        return if (node.isClickable) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            // 尝试父节点
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
            false
        }
    }

    /**
     * 获取点击统计
     */
    fun getClickStats(): ClickStats {
        return ClickStats(
            total = totalClicks,
            successful = successfulClicks,
            successRate = if (totalClicks > 0) successfulClicks.toFloat() / totalClicks else 0f
        )
    }

    /**
     * 重置统计
     */
    fun resetStats() {
        totalClicks = 0
        successfulClicks = 0
    }

    /**
     * 滑动方向枚举
     */
    enum class SwipeDirection {
        UP, DOWN, LEFT, RIGHT
    }

    /**
     * 点击统计数据类
     */
    data class ClickStats(
        val total: Int,
        val successful: Int,
        val successRate: Float
    )

    /**
     * 辅助函数：创建四元组
     */
    private fun quadrupleOf(a: Float, b: Float, c: Float, d: Float): Tuple4 {
        return Tuple4(a, b, c, d)
    }

    /**
     * 四元组
     */
    data class Tuple4(
        val first: Float,
        val second: Float,
        val third: Float,
        val fourth: Float
    )
}
