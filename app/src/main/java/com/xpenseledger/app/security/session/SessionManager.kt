package com.xpenseledger.app.security.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SessionManager {

    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked

    private var lastInteractionTime: Long = System.currentTimeMillis()

    fun onUserInteraction() {
        lastInteractionTime = System.currentTimeMillis()
    }

    fun lock() {
        _isLocked.value = true
    }

    fun unlock() {
        _isLocked.value = false
        lastInteractionTime = System.currentTimeMillis()
    }

    fun checkTimeout(timeoutMs: Long) {
        if (!_isLocked.value && System.currentTimeMillis() - lastInteractionTime > timeoutMs) {
            _isLocked.value = true
        }
    }
}
