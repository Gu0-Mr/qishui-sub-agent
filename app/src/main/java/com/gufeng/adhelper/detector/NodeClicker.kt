package com.gufeng.adhelper.detector

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.math.abs
import kotlin.math.sqrt

class NodeClicker(private val accessibilityService: AccessibilityService) {

    companion object {
        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_DELAY = 200L
        private const val DOUBLE_CLICK_INTERVAL = 100L
        private const val SWIPE_DISTANCE_THRESHOLD = 50f
    }

    private val handler = Handler(Looper.getMainLooper())
    private var totalClicks = 0
    private var successfulClicks = 0

    fun performClick(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        totalClicks++
        for (attempt in 1..MAX_RETRY_COUNT) {
            if (doClick(node)) {
                successfulClicks++
                return true
            }
            if (attempt < MAX_RETRY_COUNT) Thread.sleep(RETRY_DELAY)
        }
        return false
    }

    fun performDoubleClick(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (performClick(node)) {
            handler.postDelayed({ performClick(node) }, DOUBLE_CLICK_INTERVAL)
            return true
        }
        return false
    }

    fun performLongClick(node: AccessibilityNodeInfo?): Boolean {
        return node?.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK) ?: false
    }

    fun performSwipe(direction: SwipeDirection): Boolean {
        val metrics = accessibilityService.resources.displayMetrics
        val width = metrics.widthPixels.toFloat()
        val height = metrics.heightPixels.toFloat()
        
        val (startX, startY, endX, endY) = when (direction) {
            SwipeDirection.UP -> quadrupleOf(width / 2f, height * 0.8f, width / 2f, height * 0.2f)
            SwipeDirection.DOWN -> quadrupleOf(width / 2f, height * 0.2f, width / 2f, height * 0.8f)
            SwipeDirection.LEFT -> quadrupleOf(width * 0.8f, height / 2f, width * 0.2f, height / 2f)
            SwipeDirection.RIGHT -> quadrupleOf(width * 0.2f, height / 2f, width * 0.8f, height / 2f)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val gesture = GestureDescription.Builder()
                .addStroke(
                    GestureDescription.StrokeDescription(
                        Path().apply {
                            moveTo(startX, startY)
                            lineTo(endX, endY)
                        }, 0, 300
                    )
                )
                .build()
            return accessibilityService.dispatchGesture(gesture, null, null)
        }
        return false
    }

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

    fun findNode(
        rootNode: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (predicate(rootNode)) return rootNode
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i) ?: continue
            val result = findNode(child, predicate)
            if (result != null) return result
        }
        return null
    }

    fun findNearestClickableNode(
        rootNode: AccessibilityNodeInfo,
        targetX: Int,
        targetY: Int
    ): AccessibilityNodeInfo? {
        val candidates = mutableListOf<Pair<AccessibilityNodeInfo, Float>>()
        collectClickableNodes(rootNode, candidates, targetX, targetY)
        return candidates.minByOrNull { it.second }?.first
    }

    private fun collectClickableNodes(
        node: AccessibilityNodeInfo,
        result: MutableList<Pair<AccessibilityNodeInfo, Float>>,
        targetX: Int,
        targetY: Int
    ) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (node.isClickable && bounds.contains(targetX, targetY)) {
            val distance = sqrt(
                (bounds.centerX() - targetX).toDouble().pow(2) +
                (bounds.centerY() - targetY).toDouble().pow(2)
            ).toFloat()
            result.add(node to distance)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectClickableNodes(child, result, targetX, targetY)
            }
        }
    }

    private fun doClick(node: AccessibilityNodeInfo): Boolean {
        return if (node.isClickable) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
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

    fun getClickStats(): ClickStats {
        return ClickStats(
            total = totalClicks,
            successful = successfulClicks,
            successRate = if (totalClicks > 0) successfulClicks.toFloat() / totalClicks else 0f
        )
    }

    fun resetStats() {
        totalClicks = 0
        successfulClicks = 0
    }

    enum class SwipeDirection { UP, DOWN, LEFT, RIGHT }

    data class ClickStats(val total: Int, val successful: Int, val successRate: Float)

    data class Tuple4(val first: Float, val second: Float, val third: Float, val fourth: Float)

    private fun quadrupleOf(a: Float, b: Float, c: Float, d: Float): Tuple4 = Tuple4(a, b, c, d)
}

private fun Double.pow(n: Int): Double = Math.pow(this, n.toDouble())
