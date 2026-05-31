package com.xpenseledger.app.notification.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpenseledger.app.notification.service.MonitoredApps
import com.xpenseledger.app.ui.theme.XpensePrimary
import com.xpenseledger.app.ui.theme.XpenseSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationListenerSettingsScreen(
    vm: NotificationListenerViewModel,
    pendingCount: Int,
    onBack: () -> Unit,
    onReviewPending: () -> Unit
) {
    val context          = LocalContext.current
    val isEnabled        by vm.isEnabled.collectAsState()
    val showConfirmation by vm.showConfirmation.collectAsState()
    val autoCategory     by vm.autoCategory.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Auto-Detect Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor        = Color(0xFF0F1923),
                    titleContentColor     = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Master toggle section ─────────────────────────────────────────
            item {
                SectionHeader("Auto-detect Transactions")
            }
            item {
                SettingToggleRow(
                    title     = "Enable notification monitoring",
                    subtitle  = "Listen to bank & UPI app notifications",
                    checked   = isEnabled,
                    onChecked = { vm.setEnabled(it) },
                    icon      = if (isEnabled) Icons.Default.NotificationsActive
                                else           Icons.Default.NotificationsOff,
                    iconTint  = if (isEnabled) XpensePrimary else MaterialTheme.colorScheme.outline
                )
            }
            item {
                SettingToggleRow(
                    title     = "Show confirmation notification",
                    subtitle  = "Display ✓ Add / ✏ Edit / ✗ Dismiss buttons",
                    checked   = showConfirmation,
                    onChecked = { vm.setShowConfirmation(it) }
                )
            }
            item {
                SettingToggleRow(
                    title     = "Auto-assign categories",
                    subtitle  = "Match merchant name to expense category",
                    checked   = autoCategory,
                    onChecked = { vm.setAutoCategory(it) }
                )
            }

            // ── Open system NLS settings ──────────────────────────────────────
            item {
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Text("Manage Notification Access in System Settings")
                }
            }

            // ── Pending review ────────────────────────────────────────────────
            if (pendingCount > 0) {
                item {
                    SectionHeader("Pending Review")
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(16.dp),
                        colors   = CardDefaults.cardColors(
                            containerColor = XpenseSecondary.copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                "$pendingCount transaction${if (pendingCount > 1) "s" else ""} waiting for review",
                                style    = MaterialTheme.typography.bodyMedium,
                                color    = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(onClick = onReviewPending) {
                                Text("Review Now", color = XpenseSecondary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ── Monitored apps ────────────────────────────────────────────────
            item { SectionHeader("Monitored Apps") }

            items(MonitoredApps.ALL) { app ->
                var enabled by remember { mutableStateOf(vm.isAppEnabled(app.packageName)) }
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(app.label, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text(app.type, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked         = enabled,
                        onCheckedChange = {
                            enabled = it
                            vm.setAppEnabled(app.packageName, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor  = XpensePrimary,
                            checkedTrackColor  = XpensePrimary.copy(alpha = 0.3f)
                        )
                    )
                }
                HorizontalDivider(thickness = 0.4.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style      = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color      = XpensePrimary,
        modifier   = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: androidx.compose.ui.graphics.Color = XpensePrimary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked         = checked,
                onCheckedChange = onChecked,
                colors          = SwitchDefaults.colors(
                    checkedThumbColor = XpensePrimary,
                    checkedTrackColor = XpensePrimary.copy(alpha = 0.3f)
                )
            )
        }
    }
}

