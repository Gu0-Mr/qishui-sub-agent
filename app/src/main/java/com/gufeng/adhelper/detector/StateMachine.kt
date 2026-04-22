package com.gufeng.adhelper.detector

import androidx.lifecycle.MutableLiveData

class StateMachine {

    enum class State {
        IDLE,
        AD_SHOWN,
        COUNTDOWN,
        REWARD_READY,
        COLLECT_SUCCESS,
        COLLECT_BUTTON,
        POPUP_DETECTED,
        SKIPPING,
        WAITING
    }

    companion object {
        private const val MIN_SKIP_INTERVAL = 500L
        private const val WAIT_TIMEOUT = 30000L
    }

    @Volatile
    private var _currentState = State.IDLE
    val currentState: State get() = _currentState

    val countdownLiveData = MutableLiveData(0)
    val stateChangeLiveData = MutableLiveData<Pair<State, State>>()

    private var loopCount = 0
    private val MAX_LOOP_COUNT = 20
    private var adStartTime = 0L
    private val AD_TIMEOUT = 120000L

    @Volatile
    private var lastSkipTime = 0L

    @Volatile
    private var stateStartTime = 0L

    @Volatile
    private var consecutiveSkips = 0

    @Volatile
    private var isMuted = false

    private val stateListeners = mutableListOf<(State, State) -> Unit>()

    @Synchronized
    fun transitionTo(newState: State) {
        val oldState = _currentState
        if (!isValidTransition(oldState, newState)) return

        _currentState = newState
        stateStartTime = System.currentTimeMillis()

        when (newState) {
            State.SKIPPING -> {
                lastSkipTime = System.currentTimeMillis()
                consecutiveSkips++
            }
            State.IDLE -> {
                consecutiveSkips = 0
                resetMute()
                adStartTime = 0L
            }
            else -> {}
        }

        stateListeners.forEach { it(oldState, newState) }
        stateChangeLiveData.postValue(oldState to newState)
    }

    private fun isValidTransition(from: State, to: State): Boolean {
        return true
    }

    fun canSkip(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastSkipTime < MIN_SKIP_INTERVAL) return false
        if (consecutiveSkips > 10) {
            if (now - lastSkipTime < 500) return false
        }
        if (_currentState == State.WAITING && now - stateStartTime > WAIT_TIMEOUT) {
            transitionTo(State.IDLE)
            return false
        }
        return true
    }

    fun isActive(): Boolean = _currentState != State.IDLE

    fun getCurrentState(): State = _currentState

    fun getStateDuration(): Long = System.currentTimeMillis() - stateStartTime

    @Synchronized
    fun reset() {
        _currentState = State.IDLE
        stateStartTime = System.currentTimeMillis()
        consecutiveSkips = 0
        resetMute()
    }

    fun addStateListener(listener: (State, State) -> Unit) {
        stateListeners.add(listener)
    }

    fun removeStateListener(listener: (State, State) -> Unit) {
        stateListeners.remove(listener)
    }

    fun updateCountdown(seconds: Int) {
        if (seconds >= 0) {
            countdownLiveData.postValue(seconds)
            if (_currentState == State.IDLE || _currentState == State.AD_SHOWN) {
                _currentState = State.COUNTDOWN
            }
            if (seconds == 0) {
                transitionTo(State.REWARD_READY)
            }
        }
    }

    fun checkTimeout(): Boolean {
        if (adStartTime > 0 && System.currentTimeMillis() - adStartTime > AD_TIMEOUT) {
            transitionTo(State.IDLE)
            return true
        }
        return false
    }

    fun pause() {
        _currentState = State.WAITING
    }

    fun resume() {
        _currentState = State.IDLE
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun isMuted(): Boolean = isMuted

    private fun resetMute() {
        isMuted = false
    }

    private fun resetLoopCount() {
        loopCount = 0
    }

    fun getLoopCount(): Int = loopCount

    fun isRunning(): Boolean = _currentState != State.IDLE && _currentState != State.WAITING

    fun getStateDescription(): String {
        return when (_currentState) {
            State.IDLE -> "空闲"
            State.AD_SHOWN -> "广告显示中"
            State.COUNTDOWN -> "倒计时"
            State.REWARD_READY -> "可领取奖励"
            State.COLLECT_SUCCESS -> "领取成功"
            State.COLLECT_BUTTON -> "领取按钮"
            State.POPUP_DETECTED -> "弹窗检测中"
            State.SKIPPING -> "跳过中"
            State.WAITING -> "等待中"
        }
    }
}
