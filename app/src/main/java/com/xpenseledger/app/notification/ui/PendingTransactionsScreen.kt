package com.xpenseledger.app.notification.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpenseledger.app.notification.model.PendingTransaction
import com.xpenseledger.app.ui.theme.XpensePrimary
import com.xpenseledger.app.ui.theme.XpenseSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingTransactionsScreen(
    vm: NotificationListenerViewModel,
    onBack: () -> Unit,
    onEditTransaction: (PendingTransaction) -> Unit
) {
    val pending by vm.pendingTransactions.collectAsState()
    val fmt = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Pending Review",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor            = Color(0xFF0F1923),
                    titleContentColor         = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        if (pending.isEmpty()) {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "All caught up!",
                        style     = MaterialTheme.typography.titleMedium,
                        color     = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "No pending transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "${pending.size} transaction${if (pending.size > 1) "s" else ""} detected",
                        style  = MaterialTheme.typography.bodySmall,
                        color  = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(pending, key = { it.id }) { tx ->
                    PendingTransactionCard(
                        tx     = tx,
                        time   = fmt.format(Date(tx.detectedAt)),
                        onAdd  = { vm.addTransaction(tx) },
                        onEdit = { onEditTransaction(tx) },
                        onDismiss = { vm.dismissTransaction(tx) }
                    )
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun PendingTransactionCard(
    tx: PendingTransaction,
    time: String,
    onAdd: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── Header row ───────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "₹%.2f".format(tx.amount),
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = if (tx.transactionType == "INCOME") Color(0xFF34D399)
                                     else MaterialTheme.colorScheme.onSurface
                    )
                    if (tx.merchant.isNotBlank()) {
                        Text(
                            tx.merchant,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Badge(
                        containerColor = if (tx.transactionType == "INCOME")
                            Color(0xFF34D399).copy(alpha = 0.2f)
                        else XpensePrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            if (tx.transactionType == "INCOME") "Income" else "Expense",
                            color    = if (tx.transactionType == "INCOME") Color(0xFF34D399) else XpensePrimary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tx.sourceLabel,
                        style  = MaterialTheme.typography.bodySmall,
                        color  = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // ── Category ─────────────────────────────────────────────────────
            Text(
                "${tx.category}  ›  ${tx.subCategory}",
                style  = MaterialTheme.typography.bodySmall,
                color  = XpenseSecondary,
                fontSize = 12.sp
            )
            Text(
                time,
                style  = MaterialTheme.typography.bodySmall,
                color  = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.4.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(Modifier.height(8.dp))

            // ── Action buttons ────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Add
                Button(
                    onClick  = onAdd,
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = XpensePrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null,
                        modifier = Modifier.size(16.dp), tint = Color(0xFF0F1923))
                    Spacer(Modifier.width(4.dp))
                    Text("Add", color = Color(0xFF0F1923), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                // Edit
                OutlinedButton(
                    onClick  = onEdit,
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape    = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null,
                        modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit", fontSize = 13.sp)
                }
                // Dismiss
                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape    = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null,
                        modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Skip", fontSize = 13.sp)
                }
            }
        }
    }
}

