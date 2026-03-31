package com.xpenseledger.app.ui.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpenseledger.app.ui.security.rememberDebouncedClick
import com.xpenseledger.app.ui.theme.XpensePrimary
import com.xpenseledger.app.ui.theme.XpenseSecondary
import com.xpenseledger.app.ui.viewmodel.UserProfileViewModel
import kotlinx.coroutines.launch

private val GENDER_OPTIONS = listOf("Male", "Female", "Other", "Prefer not to say")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileVm:  UserProfileViewModel,
    onLogout:   () -> Unit
) {
    val profile        by profileVm.profile.collectAsState()
    val snackbarState  = remember { SnackbarHostState() }
    val scope          = rememberCoroutineScope()
    var showLogoutDlg  by remember { mutableStateOf(false) }

    // Editable local state — pre-filled from saved profile
    var name   by rememberSaveable(profile.name)   { mutableStateOf(profile.name) }
    var age    by rememberSaveable(profile.age)    { mutableStateOf(profile.age) }
    var gender by rememberSaveable(profile.gender) { mutableStateOf(profile.gender) }

    // Validation
    val nameError   = name.isNotBlank() && name.trim().length < 2
    val ageError    = age.isNotBlank() && (age.toIntOrNull() == null
                      || age.toInt() !in 1..120)
    val canSave     = name.isNotBlank() && !nameError && !ageError

    val saveProfile = rememberDebouncedClick {
        profileVm.saveProfile(name.trim(), age.trim(), gender)
        scope.launch { snackbarState.showSnackbar("✅ Profile saved") }
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarState) },
        containerColor = Color.Transparent,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D1117))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text       = "My Profile",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    modifier   = Modifier.align(Alignment.CenterStart)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Avatar card ───────────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(20.dp),
                colors    = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier              = Modifier.padding(20.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Gradient avatar circle
                    Box(
                        modifier         = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(XpensePrimary, XpenseSecondary),
                                    start = Offset(0f, 0f),
                                    end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (name.isNotBlank()) {
                            Text(
                                text       = name.trim().first().uppercaseChar().toString(),
                                style      = MaterialTheme.typography.headlineMedium,
                                color      = Color(0xFF0D1117),
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector        = Icons.Default.Person,
                                contentDescription = null,
                                tint               = Color(0xFF0D1117),
                                modifier           = Modifier.size(32.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text  = if (name.isNotBlank()) name.trim() else "Your Name",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (name.isNotBlank())
                                        MaterialTheme.colorScheme.onSurface
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (age.isNotBlank() || gender.isNotBlank()) {
                            Text(
                                text  = listOf(
                                    if (age.isNotBlank()) "$age yrs" else null,
                                    gender.ifBlank { null }
                                ).filterNotNull().joinToString("  •  "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Form ──────────────────────────────────────────────────────────
            Text(
                "Personal Details",
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color      = XpensePrimary
            )

            // Name
            OutlinedTextField(
                value         = name,
                onValueChange = { if (it.length <= 50) name = it },
                label         = { Text("Full Name") },
                placeholder   = { Text("e.g. Rahul Sharma") },
                singleLine    = true,
                isError       = nameError,
                supportingText = {
                    if (nameError) Text("Name must be at least 2 characters",
                        color = MaterialTheme.colorScheme.error)
                },
                modifier      = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Full name" },
                shape         = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization   = KeyboardCapitalization.Words,
                    autoCorrectEnabled = false
                ),
                colors = fieldColors(nameError)
            )

            // Age
            OutlinedTextField(
                value         = age,
                onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) age = it },
                label         = { Text("Age") },
                placeholder   = { Text("e.g. 28") },
                singleLine    = true,
                isError       = ageError,
                supportingText = {
                    if (ageError) Text("Enter a valid age (1–120)",
                        color = MaterialTheme.colorScheme.error)
                },
                modifier      = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Age" },
                shape         = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = fieldColors(ageError)
            )

            // Gender
            Text(
                "Gender",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GENDER_OPTIONS.forEach { option ->
                    FilterChip(
                        selected = gender == option,
                        onClick  = { gender = if (gender == option) "" else option },
                        label    = { Text(option, fontSize = 12.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor    = XpensePrimary.copy(alpha = 0.18f),
                            selectedLabelColor        = XpensePrimary,
                            selectedLeadingIconColor  = XpensePrimary
                        )
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Save button
            Button(
                onClick  = saveProfile,
                enabled  = canSave,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = XpensePrimary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null,
                    tint = Color(0xFF0D1117), modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Save Profile", color = Color(0xFF0D1117), fontWeight = FontWeight.SemiBold)
            }

            // ── Divider ───────────────────────────────────────────────────────
            HorizontalDivider(
                modifier  = Modifier.padding(vertical = 8.dp),
                thickness = 0.5.dp,
                color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            // ── Logout ────────────────────────────────────────────────────────
            Text(
                "Account",
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color      = XpensePrimary
            )

            Button(
                onClick  = { showLogoutDlg = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    "Logout",
                    color      = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // ── Logout confirmation dialog ────────────────────────────────────────────
    if (showLogoutDlg) {
        AlertDialog(
            onDismissRequest = { showLogoutDlg = false },
            title            = { Text("Logout") },
            text             = { Text("You will need your 6-digit PIN to access the app again.") },
            confirmButton    = {
                Button(
                    onClick = { showLogoutDlg = false; onLogout() },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Logout") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDlg = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun fieldColors(isError: Boolean) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = if (isError) MaterialTheme.colorScheme.error else XpensePrimary,
    unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error.copy(0.7f)
                           else MaterialTheme.colorScheme.outline,
    focusedLabelColor    = if (isError) MaterialTheme.colorScheme.error else XpensePrimary,
    cursorColor          = XpensePrimary
)

