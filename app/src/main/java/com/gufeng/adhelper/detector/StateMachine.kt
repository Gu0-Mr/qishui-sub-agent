package com.gufeng.adhelper.detector

/**
 * 状态机
 * 作者：古封
 * 功能：管理广告检测和跳过流程的状态转换
 */
class StateMachine {

    /**
     * 状态枚举
     */
    enum class State {
        IDLE,                  // 空闲状态
        AD_SHOWN,             // 广告已显示
        POPUP_DETECTED,       // 检测到弹窗
        COLLECT_SUCCESS,      // 领取成功
        COLLECT_BUTTON,       // 检测到领取按钮
        SKIPPING,             // 跳过中
        WAITING               // 等待中
    }

    companion object {
        // 最小跳过间隔（毫秒）
        private const val MIN_SKIP_INTERVAL = 300L
        
        // 等待超时时间（毫秒）
        private const val WAIT_TIMEOUT = 30000L
        
        // 最大连续跳过次数
        private const val MAX_CONSECUTIVE_SKIPS = 10
    }

    @Volatile
    private var currentState = State.IDLE
    
    @Volatile
    private var lastSkipTime = 0L
    
    @Volatile
    private var stateStartTime = 0L
    
    @Volatile
    private var consecutiveSkips = 0
    
    @Volatile
    private var isMuted = false
    
    // 状态变更监听器
    private val stateListeners = mutableListOf<(State, State) -> Unit>()

    /**
     * 状态转换
     */
    @Synchronized
    fun transitionTo(newState: State) {
        val oldState = currentState
        
        // 状态转换验证
        if (!isValidTransition(oldState, newState)) {
            return
        }
        
        currentState = newState
        stateStartTime = System.currentTimeMillis()
        
        // 如果是跳过操作，更新跳过时间
        if (newState == State.SKIPPING) {
            lastSkipTime = System.currentTimeMillis()
            consecutiveSkips++
        } else if (newState == State.IDLE) {
            consecutiveSkips = 0
        }
        
        // 通知监听器
        stateListeners.forEach { listener ->
            listener(oldState, newState)
        }
    }

    /**
     * 验证状态转换是否有效
     */
    private fun isValidTransition(from: State, to: State): Boolean {
        // 允许的状态转换映射
        val validTransitions = mapOf(
            State.IDLE to setOf(State.AD_SHOWN, State.POPUP_DETECTED),
            State.AD_SHOWN to setOf(
                State.POPUP_DETECTED, 
                State.COLLECT_SUCCESS, 
                State.COLLECT_BUTTON,
                State.SKIPPING,
                State.WAITING,
                State.IDLE
            ),
            State.POPUP_DETECTED to setOf(State.IDLE, State.WAITING),
            State.COLLECT_SUCCESS to setOf(State.IDLE),
            State.COLLECT_BUTTON to setOf(State.SKIPPING, State.WAITING, State.IDLE),
            State.SKIPPING to setOf(State.IDLE, State.WAITING),
            State.WAITING to setOf(State.SKIPPING, State.IDLE, State.AD_SHOWN)
        )
        
        // IDLE可以转到任何非WAITING的状态
        if (from == State.IDLE && to != State.WAITING) {
            return true
        }
        
        return validTransitions[from]?.contains(to) == true
    }

    /**
     * 检查是否可以执行跳过操作
     */
    fun canSkip(): Boolean {
        val now = System.currentTimeMillis()
        
        // 检查最小间隔
        if (now - lastSkipTime < MIN_SKIP_INTERVAL) {
            return false
        }
        
        // 检查连续跳过次数（放宽到10次）
        if (consecutiveSkips > MAX_CONSECUTIVE_SKIPS) {
            if (now - lastSkipTime < 500) {
                return false
            }
        }
        
        // 检查是否处于等待超时
        if (currentState == State.WAITING) {
            if (now - stateStartTime > WAIT_TIMEOUT) {
                transitionTo(State.IDLE)
                return false
            }
        }
        
        return true
    }
    
    /**
     * 设置静音状态
     */
    fun setMuted(muted: Boolean) {
        isMuted = muted
    }
    
    /**
     * 检查是否已静音
     */
    fun isMuted(): Boolean = isMuted

    /**
     * 检查是否在目标应用内
     */
    fun isActive(): Boolean {
        return currentState != State.IDLE
    }

    /**
     * 获取当前状态
     */
    fun getCurrentState(): State {
        return currentState
    }

    /**
     * 获取当前状态的持续时间
     */
    fun getStateDuration(): Long {
        return System.currentTimeMillis() - stateStartTime
    }

    /**
     * 重置状态机
     */
    @Synchronized
    fun reset() {
        currentState = State.IDLE
        stateStartTime = System.currentTimeMillis()
        consecutiveSkips = 0
        isMuted = false
    }

    /**
     * 添加状态变更监听器
     */
    fun addStateListener(listener: (State, State) -> Unit) {
        stateListeners.add(listener)
    }

    /**
     * 移除状态变更监听器
     */
    fun removeStateListener(listener: (State, State) -> Unit) {
        stateListeners.remove(listener)
    }

    /**
     * 获取状态描述
     */
    fun getStateDescription(): String {
        return when (currentState) {
            State.IDLE -> "空闲"
            State.AD_SHOWN -> "广告显示中"
            State.POPUP_DETECTED -> "弹窗检测中"
            State.COLLECT_SUCCESS -> "领取成功"
            State.COLLECT_BUTTON -> "领取按钮"
            State.SKIPPING -> "跳过中"
            State.WAITING -> "等待中"
        }
    }
}
