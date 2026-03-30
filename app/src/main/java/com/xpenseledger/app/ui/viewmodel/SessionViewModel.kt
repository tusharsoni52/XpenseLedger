package com.xpenseledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpenseledger.app.security.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    val isLocked = sessionManager.isLocked

    private var timerJob: Job? = null

    fun startInactivityTimer(timeoutMs: Long = 2 * 60 * 1000L) {
        if (timerJob != null) return
        timerJob = viewModelScope.launch {
            while (true) {
                delay(5_000L)
                sessionManager.checkTimeout(timeoutMs)
            }
        }
    }

    fun onUserInteraction() {
        sessionManager.onUserInteraction()
    }

    fun unlock() {
        sessionManager.unlock()
    }

    fun lock() {
        sessionManager.lock()
    }
}
