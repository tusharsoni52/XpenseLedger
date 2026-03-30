package com.xpenseledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpenseledger.app.security.pin.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Public types ──────────────────────────────────────────────────────────────

/**
 * All possible authentication flow stages.
 *
 *  SET_PIN        → first-launch: choose a new 6-digit PIN
 *  CONFIRM_PIN    → re-enter the chosen PIN to confirm it matches
 *  UNLOCK         → normal unlock: enter existing PIN
 *  MIGRATE_PIN    → legacy PIN (<6 digits): verify old PIN, then force setting a new 6-digit PIN
 *  VERIFY_OLD_PIN → reset flow step 1: verify the current PIN before changing
 *  RESET_PIN      → reset flow step 2: choose the new 6-digit PIN
 *  CONFIRM_RESET  → reset flow step 3: confirm the new PIN
 */
enum class AuthMode {
    SET_PIN, CONFIRM_PIN,
    UNLOCK,
    MIGRATE_PIN,
    VERIFY_OLD_PIN, RESET_PIN, CONFIRM_RESET
}

sealed class AuthEvent {
    object Success        : AuthEvent()
    object Failure        : AuthEvent()
    object PinResetDone   : AuthEvent()          // new PIN saved after reset
    data class Lockout(val seconds: Int) : AuthEvent()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * Owns all authentication state and logic.  The UI gets only:
 *   • [pinLength]  — number of digits entered (never the digits themselves)
 *   • [mode]       — current flow stage
 *   • [isLockedOut] / [lockoutSecondsRemaining]
 *   • [failedAttempts]
 *   • [events]     — one-shot success / failure / lockout events
 *
 * PIN validation is delegated to [PinManager] and the result is NEVER
 * propagated to the UI as a boolean — only via the sealed [AuthEvent] type.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val pinManager: PinManager
) : ViewModel() {

    companion object {
        const val MAX_ATTEMPTS = 5
        /** Exactly 6 digits — mandatory for all new PINs. */
        const val PIN_LENGTH     = 6
        const val MAX_PIN_LENGTH = PIN_LENGTH
        const val MIN_PIN_LENGTH = PIN_LENGTH
        /** The maximum legacy PIN length we still accept during migration. */
        const val LEGACY_MAX_PIN_LENGTH = 5

        private val LOCKOUT_DURATIONS = intArrayOf(30, 60, 120, 300)
    }

    // ── Exposed state ─────────────────────────────────────────────────────────

    private val _mode = MutableStateFlow(
        when {
            !pinManager.hasPin()                        -> AuthMode.SET_PIN
            pinManager.storedPinLength() < PIN_LENGTH   -> AuthMode.MIGRATE_PIN
            else                                        -> AuthMode.UNLOCK
        }
    )
    val mode: StateFlow<AuthMode> = _mode.asStateFlow()

    /**
     * The actual length of the legacy PIN (4 or 5).
     * Only non-zero when mode == MIGRATE_PIN.
     * Exposed so the UI can show the right number of dots.
     */
    private val _legacyPinLength = MutableStateFlow(
        if (pinManager.hasPin() && pinManager.storedPinLength() < PIN_LENGTH)
            pinManager.storedPinLength() else 0
    )
    val legacyPinLength: StateFlow<Int> = _legacyPinLength.asStateFlow()

    /** How many digits have been typed — never the actual characters. */
    private val _pinLength = MutableStateFlow(0)
    val pinLength: StateFlow<Int> = _pinLength.asStateFlow()

    private val _failedAttempts = MutableStateFlow(0)
    val failedAttempts: StateFlow<Int> = _failedAttempts.asStateFlow()

    private val _isLockedOut = MutableStateFlow(false)
    val isLockedOut: StateFlow<Boolean> = _isLockedOut.asStateFlow()

    private val _lockoutSecondsRemaining = MutableStateFlow(0)
    val lockoutSecondsRemaining: StateFlow<Int> = _lockoutSecondsRemaining.asStateFlow()

    /** One-shot events consumed by the UI. Buffer = 1 prevents dropped events. */
    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    // ── Private sensitive state — NEVER exposed ───────────────────────────────

    private val pinBuffer  = StringBuilder()
    private var firstPin   = ""   // stores first entry during set / reset confirm steps
    private var lockoutCount = 0
    private var lockoutJob: Job? = null

    // ── Input handlers ────────────────────────────────────────────────────────

    fun onDigit(digit: Int) {
        if (_isLockedOut.value) return
        val maxLen = effectiveMaxLen()
        if (pinBuffer.length >= maxLen) return
        pinBuffer.append(digit)
        _pinLength.value = pinBuffer.length
        // In MIGRATE_PIN with unknown stored length we don't auto-submit —
        // the user taps ✓ so they can type exactly however many digits their old PIN was.
        val skipAutoSubmit = _mode.value == AuthMode.MIGRATE_PIN
                             && _legacyPinLength.value == 0
        if (!skipAutoSubmit && pinBuffer.length == maxLen) {
            // Launch in a coroutine so _pinLength.value = maxLen is committed to the
            // StateFlow and Compose recomposes (all dots filled) BEFORE clearBuffer()
            // resets it back to 0. Without this, the 6th dot never appears filled.
            viewModelScope.launch {
                kotlinx.coroutines.yield()   // suspend → let collectors read the new value
                processPin()
            }
        }
    }

    fun onBackspace() {
        if (_isLockedOut.value) return
        if (pinBuffer.isEmpty()) return
        pinBuffer.deleteCharAt(pinBuffer.length - 1)
        _pinLength.value = pinBuffer.length
    }

    /** Explicit confirm tap — normally triggered automatically at max length. */
    fun onConfirm() {
        if (_isLockedOut.value) return
        // In MIGRATE_PIN accept any length from 4 upward (old PIN could be 4 or 5 digits)
        val minLen = if (_mode.value == AuthMode.MIGRATE_PIN) 4 else MIN_PIN_LENGTH
        if (pinBuffer.length < minLen) return
        viewModelScope.launch {
            kotlinx.coroutines.yield()
            processPin()
        }
    }

    /** Returns the effective max digit count for the current mode. */
    private fun effectiveMaxLen(): Int {
        if (_mode.value != AuthMode.MIGRATE_PIN) return MAX_PIN_LENGTH
        val stored = _legacyPinLength.value
        // If we stored the exact legacy length, respect it; otherwise allow up to LEGACY_MAX
        return if (stored > 0) stored else LEGACY_MAX_PIN_LENGTH
    }

    fun onBiometricSuccess() {
        _failedAttempts.value = 0
        lockoutCount = 0
        clearBuffer()
        _events.tryEmit(AuthEvent.Success)
    }

    /**
     * Starts the reset flow.
     * Valid from UNLOCK (user remembers old PIN but wants to change it)
     * and from MIGRATE_PIN (user wants to change directly without the old PIN flow).
     */
    fun initiateReset() {
        if (_isLockedOut.value) return
        clearBuffer()
        _mode.value = AuthMode.VERIFY_OLD_PIN
    }

    /** Cancels an in-progress reset/migration and returns to the appropriate mode. */
    fun cancelReset() {
        clearBuffer()
        firstPin = ""
        _mode.value = if (pinManager.hasPin()) {
            if (pinManager.storedPinLength() < PIN_LENGTH) AuthMode.MIGRATE_PIN
            else AuthMode.UNLOCK
        } else {
            AuthMode.SET_PIN
        }
    }

    // ── Core logic ────────────────────────────────────────────────────────────

    private fun processPin() {
        val pin = pinBuffer.toString()
        when (_mode.value) {

            AuthMode.SET_PIN -> {
                firstPin = pin
                clearBuffer()
                _mode.value = AuthMode.CONFIRM_PIN
            }

            AuthMode.CONFIRM_PIN -> {
                val matches = pin == firstPin
                firstPin = ""
                clearBuffer()
                if (matches) {
                    pinManager.savePin(pin)
                    _events.tryEmit(AuthEvent.Success)
                } else {
                    _mode.value = AuthMode.SET_PIN
                    _events.tryEmit(AuthEvent.Failure)
                }
            }

            AuthMode.UNLOCK -> {
                val valid = pinManager.validate(pin)
                clearBuffer()
                if (valid) {
                    _failedAttempts.value = 0
                    lockoutCount = 0
                    _events.tryEmit(AuthEvent.Success)
                } else {
                    val total = _failedAttempts.value + 1
                    _failedAttempts.value = total
                    _events.tryEmit(AuthEvent.Failure)
                    if (total >= MAX_ATTEMPTS) {
                        _failedAttempts.value = 0
                        startLockout()
                    }
                }
            }

            AuthMode.MIGRATE_PIN -> {
                // Verify the legacy short PIN using the stored hash
                val valid = pinManager.validate(pin)
                clearBuffer()
                if (valid) {
                    // Wipe the old PIN — user must now set a 6-digit one
                    pinManager.clearPin()
                    _legacyPinLength.value = 0
                    _failedAttempts.value  = 0
                    lockoutCount           = 0
                    _mode.value = AuthMode.SET_PIN
                } else {
                    val total = _failedAttempts.value + 1
                    _failedAttempts.value = total
                    _events.tryEmit(AuthEvent.Failure)
                    if (total >= MAX_ATTEMPTS) {
                        _failedAttempts.value = 0
                        startLockout()
                    }
                }
            }

            // ── Reset flow ────────────────────────────────────────────────────

            AuthMode.VERIFY_OLD_PIN -> {
                val valid = pinManager.validate(pin)
                clearBuffer()
                if (valid) {
                    // Old PIN verified — now let the user pick a new one
                    _mode.value = AuthMode.RESET_PIN
                } else {
                    val total = _failedAttempts.value + 1
                    _failedAttempts.value = total
                    _events.tryEmit(AuthEvent.Failure)
                    if (total >= MAX_ATTEMPTS) {
                        _failedAttempts.value = 0
                        startLockout()
                    }
                }
            }

            AuthMode.RESET_PIN -> {
                firstPin = pin
                clearBuffer()
                _mode.value = AuthMode.CONFIRM_RESET
            }

            AuthMode.CONFIRM_RESET -> {
                val matches = pin == firstPin
                firstPin = ""
                clearBuffer()
                if (matches) {
                    pinManager.savePin(pin)
                    _failedAttempts.value = 0
                    _mode.value = AuthMode.UNLOCK
                    _events.tryEmit(AuthEvent.PinResetDone)
                } else {
                    // Mismatch → go back to choosing the new PIN
                    _mode.value = AuthMode.RESET_PIN
                    _events.tryEmit(AuthEvent.Failure)
                }
            }
        }
    }

    private fun startLockout() {
        val durationSec = LOCKOUT_DURATIONS.getOrElse(lockoutCount) { LOCKOUT_DURATIONS.last() }
        lockoutCount++
        _isLockedOut.value = true
        _lockoutSecondsRemaining.value = durationSec
        _events.tryEmit(AuthEvent.Lockout(durationSec))
        lockoutJob?.cancel()
        lockoutJob = viewModelScope.launch {
            for (remaining in (durationSec - 1) downTo 0) {
                delay(1_000L)
                _lockoutSecondsRemaining.value = remaining
            }
            _isLockedOut.value = false
        }
    }

    private fun clearBuffer() {
        // Overwrite with zeros before clearing to reduce sensitive-data lifetime
        for (i in pinBuffer.indices) pinBuffer[i] = '0'
        pinBuffer.clear()
        _pinLength.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        clearBuffer()
        firstPin = ""
    }
}
