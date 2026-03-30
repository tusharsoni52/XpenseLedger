package com.xpenseledger.app.ui.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpenseledger.app.security.biometrics.BiometricAuthManager
import com.xpenseledger.app.ui.screens.auth.components.NumericKeypad
import com.xpenseledger.app.ui.screens.auth.components.PinDotRow
import com.xpenseledger.app.ui.screens.auth.components.PinDotState
import com.xpenseledger.app.ui.theme.DarkError
import com.xpenseledger.app.ui.theme.XpensePrimary
import com.xpenseledger.app.ui.theme.XpenseSecondary
import com.xpenseledger.app.ui.viewmodel.AuthEvent
import com.xpenseledger.app.ui.viewmodel.AuthMode
import com.xpenseledger.app.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
//  Design tokens — all private to this file
// ─────────────────────────────────────────────────────────────────────────────

private val CyanAccent      = Color(0xFF22D3EE)   // XpensePrimary — cyan
private val TealStart       = Color(0xFF0D4F5C)   // deep teal gradient start
private val IndigoEnd       = Color(0xFF1E1250)   // deep indigo gradient end
private val MidCyan         = Color(0xFF0C3A52)   // centre blend
private val GlassFill       = Color(0x1AFFFFFF)   // 10% white — card body
private val GlassBorderTop  = Color(0x40FFFFFF)   // 25% white — bright top edge
private val GlassBorderBot  = Color(0x0DFFFFFF)   // 5%  white — dim bottom edge
private val GlassHighlight  = Color(0x18FFFFFF)   // inner top shimmer
private val SubtextColor    = Color(0x99FFFFFF)   // 60% white — secondary text
private val LockRingBg      = Color(0x26000000)   // dark circle behind lock icon

// ─────────────────────────────────────────────────────────────────────────────
//  Root screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LoginScreen(
    biometricAuthManager: BiometricAuthManager?,
    canUseBiometrics:     Boolean,
    onAuthenticated:      () -> Unit,
    vm:                   AuthViewModel = hiltViewModel()
) {
    val mode           by vm.mode.collectAsState()
    val pinLength      by vm.pinLength.collectAsState()
    val isLockedOut    by vm.isLockedOut.collectAsState()
    val lockoutSec     by vm.lockoutSecondsRemaining.collectAsState()
    val failedAttempts by vm.failedAttempts.collectAsState()
    val legacyPinLen   by vm.legacyPinLength.collectAsState()

    val dotCount = when {
        mode != AuthMode.MIGRATE_PIN -> AuthViewModel.MAX_PIN_LENGTH
        legacyPinLen > 0             -> legacyPinLen
        else                         -> AuthViewModel.LEGACY_MAX_PIN_LENGTH
    }

    var dotState       by remember { mutableStateOf(PinDotState.NORMAL) }
    val snackbarState   = remember { SnackbarHostState() }

    // ── Slow lock-ring pulse (non-distracting) ────────────────────────────────
    val inf = rememberInfiniteTransition(label = "lockPulse")
    val pulseAlpha by inf.animateFloat(
        initialValue  = 0.25f, targetValue  = 0.65f,
        animationSpec = infiniteRepeatable(tween(2800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pAlpha"
    )
    val ringScale by inf.animateFloat(
        initialValue  = 1.00f, targetValue  = 1.09f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "rScale"
    )

    // ── Events ────────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is AuthEvent.Success    -> { dotState = PinDotState.SUCCESS; onAuthenticated() }
                is AuthEvent.PinResetDone -> {
                    dotState = PinDotState.SUCCESS
                    snackbarState.showSnackbar("✅ PIN changed successfully")
                    delay(200L); dotState = PinDotState.NORMAL
                }
                is AuthEvent.Failure    -> { dotState = PinDotState.FAILURE; delay(700L); dotState = PinDotState.NORMAL }
                is AuthEvent.Lockout    -> { dotState = PinDotState.FAILURE; delay(700L); dotState = PinDotState.NORMAL }
            }
        }
    }

    LaunchedEffect(mode) {
        if (mode == AuthMode.UNLOCK && canUseBiometrics && biometricAuthManager != null)
            biometricAuthManager.authenticate { success -> if (success) vm.onBiometricSuccess() }
    }

    Box(Modifier.fillMaxSize()) {

        // ── LAYERED BACKGROUND ────────────────────────────────────────────────
        GlassBackground {

            Column(
                modifier            = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                // ── App title ─────────────────────────────────────────────────
                Text(
                    text          = "XpenseLedger",
                    style         = MaterialTheme.typography.headlineSmall,
                    fontWeight    = FontWeight.Bold,
                    color         = Color.White,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "Your data stays on your device",
                    style = MaterialTheme.typography.labelMedium,
                    color = SubtextColor
                )

                Spacer(Modifier.height(28.dp))

                // ── GLASS CARD ────────────────────────────────────────────────
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {

                        // ── Lock icon ─────────────────────────────────────────
                        GlassLockIcon(
                            pulseAlpha = pulseAlpha,
                            ringScale  = ringScale
                        )

                        Spacer(Modifier.height(20.dp))

                        // ── Mode subtitle ─────────────────────────────────────
                        AnimatedContent(
                            targetState   = modeSubtitle(mode, isLockedOut),
                            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(110)) },
                            label         = "subtitle"
                        ) { subtitle ->
                            Text(
                                text      = subtitle,
                                style     = MaterialTheme.typography.bodyMedium,
                                color     = if (isLockedOut) MaterialTheme.colorScheme.error
                                            else SubtextColor,
                                textAlign = TextAlign.Center,
                                modifier  = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        // ── PIN dots ──────────────────────────────────────────
                        PinDotRow(
                            filledCount = pinLength,
                            maxCount    = dotCount,
                            state       = dotState
                        )

                        // ── Status message ────────────────────────────────────
                        Box(
                            Modifier.fillMaxWidth().height(22.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState   = statusMessage(mode, isLockedOut, lockoutSec, failedAttempts),
                                transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(110)) },
                                label         = "status"
                            ) { msg ->
                                if (msg.isNotEmpty()) {
                                    Text(
                                        text  = msg,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isLockedOut) DarkError else Color(0xFFFFB74D)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                // ── KEYPAD — outside card for full breathing room ─────────────
                NumericKeypad(
                    onDigit        = vm::onDigit,
                    onBackspace    = vm::onBackspace,
                    onConfirm      = vm::onConfirm,
                    confirmEnabled = if (mode == AuthMode.MIGRATE_PIN) pinLength >= 4
                                     else pinLength >= AuthViewModel.MIN_PIN_LENGTH,
                    enabled        = !isLockedOut,
                    modifier       = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                // ── Biometric ─────────────────────────────────────────────────
                AnimatedVisibility(
                    visible = mode == AuthMode.UNLOCK && canUseBiometrics
                              && biometricAuthManager != null && !isLockedOut,
                    enter = fadeIn(tween(220)) + expandVertically(),
                    exit  = fadeOut(tween(180)) + shrinkVertically()
                ) {
                    TextButton(onClick = {
                        biometricAuthManager?.authenticate { if (it) vm.onBiometricSuccess() }
                    }) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(XpensePrimary, XpenseSecondary),
                                        start = Offset(0f, 0f),
                                        end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = null,
                                tint     = Color(0xFF050818),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text  = "Use Biometrics",
                            color = Color.White.copy(alpha = 0.70f),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // ── Forgot PIN ────────────────────────────────────────────────
                AnimatedVisibility(
                    visible = mode in setOf(AuthMode.UNLOCK, AuthMode.MIGRATE_PIN) && !isLockedOut,
                    enter   = fadeIn(tween(220)) + expandVertically(),
                    exit    = fadeOut(tween(180)) + shrinkVertically()
                ) {
                    TextButton(onClick = { vm.initiateReset() }) {
                        Text(
                            text  = "Forgot PIN?",
                            color = Color.White.copy(0.35f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                // ── Cancel reset ──────────────────────────────────────────────
                AnimatedVisibility(
                    visible = mode in setOf(
                        AuthMode.VERIFY_OLD_PIN, AuthMode.RESET_PIN, AuthMode.CONFIRM_RESET
                    ),
                    enter = fadeIn(tween(220)) + expandVertically(),
                    exit  = fadeOut(tween(180)) + shrinkVertically()
                ) {
                    TextButton(onClick = { vm.cancelReset() }) {
                        Text(
                            text  = "Cancel",
                            color = Color.White.copy(0.35f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarState,
            modifier  = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  GlassBackground  — teal → cyan → indigo layered gradient + radial glows
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen layered background for the Login screen.
 *
 * Layers (bottom → top):
 * 1. Dark teal → cyan-mid → deep indigo diagonal gradient
 * 2. Radial cyan glow at top-right
 * 3. Radial indigo glow at bottom-left
 * 4. Faint diagonal light streak (static — no animation)
 * 5. Content
 */
@Composable
private fun GlassBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            // ── Base gradient ─────────────────────────────────────────────────
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to TealStart,
                        0.40f to MidCyan,
                        0.70f to Color(0xFF0F1A3D),
                        1.00f to IndigoEnd
                    ),
                    start = Offset(0f, 0f),
                    end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
    ) {
        // ── Radial glow orbs ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // Top-right cyan orb
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(CyanAccent.copy(alpha = 0.16f), Color.Transparent),
                            center = Offset(size.width * 0.88f, size.height * 0.10f),
                            radius = size.width * 0.65f
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.10f),
                        radius = size.width * 0.65f
                    )
                    // Bottom-left indigo orb
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(XpenseSecondary.copy(alpha = 0.20f), Color.Transparent),
                            center = Offset(size.width * 0.12f, size.height * 0.88f),
                            radius = size.width * 0.60f
                        ),
                        center = Offset(size.width * 0.12f, size.height * 0.88f),
                        radius = size.width * 0.60f
                    )
                }
        )

        // ── Diagonal light streak (single pass, no animation) ─────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.36f to Color(0x08FFFFFF),
                            0.40f to Color(0x12FFFFFF),
                            0.44f to Color(0x08FFFFFF),
                            1.00f to Color.Transparent
                        ),
                        start = Offset(0f, Float.POSITIVE_INFINITY),
                        end   = Offset(Float.POSITIVE_INFINITY, 0f)
                    )
                )
        )

        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  GlassCard  — reusable glassmorphism card
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Glassmorphism card.
 *
 * Layers (back → front):
 * 1. Semi-transparent fill (10% white)
 * 2. Gradient border: bright top-left (25% white) → dim bottom-right (5% white)
 * 3. Inner top shimmer via [drawWithContent] — simulates light hitting the glass
 *
 * No blur (RenderEffect requires API 31 and causes heavy recomposition).
 * The combination of translucency + gradient border + inner shimmer achieves
 * the glass illusion without any API constraints.
 *
 * @param cornerRadius  Rounded corner size. Default 24dp.
 * @param elevation     Not used directly (glass cards sit above background via
 *                      translucency), kept as a parameter for future shadow support.
 */
@Composable
fun GlassCard(
    modifier:      Modifier = Modifier,
    cornerRadius:  Dp       = 24.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            // 1. Semi-transparent fill
            .background(GlassFill)
            // 2. Gradient border
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to GlassBorderTop,
                        0.40f to Color.White.copy(alpha = 0.10f),
                        0.70f to Color.White.copy(alpha = 0.04f),
                        1.00f to GlassBorderBot
                    ),
                    start = Offset(0f, 0f),
                    end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            // 3. Inner top shimmer drawn OVER content
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(GlassHighlight, Color.Transparent),
                        endY = size.height * 0.20f
                    )
                )
            }
    ) {
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  GlassLockIcon  — focal point lock icon with animated concentric glow rings
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GlassLockIcon(
    pulseAlpha: Float,
    ringScale:  Float
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier         = Modifier.size(112.dp)
    ) {
        // Layer 1 — diffuse outer glow (largest, pulses)
        Box(
            modifier = Modifier
                .size(112.dp)
                .scale(ringScale)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                CyanAccent.copy(alpha = pulseAlpha * 0.20f),
                                Color.Transparent
                            )
                        )
                    )
                }
        )

        // Layer 2 — mid sweep-gradient ring stroke (pulses with scale)
        Box(
            modifier = Modifier
                .size(90.dp)
                .scale(ringScale)
                .border(
                    width = 1.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            CyanAccent.copy(alpha = pulseAlpha),
                            XpenseSecondary.copy(alpha = pulseAlpha * 0.45f),
                            CyanAccent.copy(alpha = pulseAlpha * 0.15f),
                            CyanAccent.copy(alpha = pulseAlpha)
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Layer 3 — icon circle: glass fill + cyan gradient border + lock icon
        Box(
            modifier = Modifier
                .size(68.dp)
                // Glass fill inside the icon circle
                .background(LockRingBg, CircleShape)
                // Cyan → indigo gradient ring
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            CyanAccent.copy(alpha = 0.85f),
                            XpenseSecondary.copy(alpha = 0.50f)
                        )
                    ),
                    shape = CircleShape
                )
                // Subtle inner radial glow
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(CyanAccent.copy(alpha = 0.10f), Color.Transparent)
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Lock,
                contentDescription = "Secure",
                tint               = CyanAccent,
                modifier           = Modifier.size(30.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Pure helpers — no Compose state, never cause recomposition
// ─────────────────────────────────────────────────────────────────────────────

private fun modeSubtitle(mode: AuthMode, isLockedOut: Boolean): String = when (mode) {
    AuthMode.SET_PIN        -> "Create a 6-digit PIN"
    AuthMode.CONFIRM_PIN    -> "Re-enter to confirm"
    AuthMode.MIGRATE_PIN    -> "Enter current PIN to upgrade"
    AuthMode.VERIFY_OLD_PIN -> "Enter your current PIN"
    AuthMode.RESET_PIN      -> "Choose a new 6-digit PIN"
    AuthMode.CONFIRM_RESET  -> "Re-enter new PIN to confirm"
    AuthMode.UNLOCK         -> if (isLockedOut) "Account locked" else "Enter your PIN"
}

private fun statusMessage(
    mode:           AuthMode,
    isLockedOut:    Boolean,
    lockoutSec:     Int,
    failedAttempts: Int
): String {
    val max = AuthViewModel.MAX_ATTEMPTS
    return when {
        isLockedOut -> "Try again in ${lockoutSec}s"
        (mode == AuthMode.UNLOCK || mode == AuthMode.MIGRATE_PIN)
            && failedAttempts in 1 until max ->
            "${max - failedAttempts} attempt${if (max - failedAttempts == 1) "" else "s"} remaining"
        mode == AuthMode.VERIFY_OLD_PIN && failedAttempts in 1 until max ->
            "Wrong PIN — ${max - failedAttempts} left"
        else -> ""
    }
}
