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

    /**
     * Pending-lock job: started in [lockWithGracePeriod], cancelled in [onUserInteraction]
     * or [unlock] if the user returns before the grace window expires.
     */
    private var graceLockJob: Job? = null

    /** Grace period before locking (ms). Gives the user 3 s to return from a
     *  brief task-switch without losing their in-progress form. */
    private val GRACE_PERIOD_MS = 3_000L

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
        // Cancel a pending grace-period lock if the user came back quickly
        graceLockJob?.cancel()
        graceLockJob = null
    }

    fun unlock() {
        graceLockJob?.cancel()
        graceLockJob = null
        sessionManager.unlock()
    }

    /** Immediate lock — used for explicit logout. */
    fun lock() {
        graceLockJob?.cancel()
        graceLockJob = null
        sessionManager.lock()
    }

    /**
     * Delayed lock triggered by [MainActivity.onStop].
     *
     * Waits [GRACE_PERIOD_MS] before locking so brief interruptions
     * (notification shade, biometric prompt, quick task-switch) do not
     * destroy the user's in-progress form.
     *
     * If [onUserInteraction] or [unlock] is called before the timer fires
     * the pending lock is cancelled entirely.
     */
    fun lockWithGracePeriod() {
        if (graceLockJob?.isActive == true) return   // already pending
        graceLockJob = viewModelScope.launch {
            delay(GRACE_PERIOD_MS)
            sessionManager.lock()
            graceLockJob = null
        }
    }
}
