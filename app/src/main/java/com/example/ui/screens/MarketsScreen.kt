package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.data.model.Market
import com.example.data.model.ProductItem

@Composable
fun MarketsScreen(
    markets: List<Market>,
    activeProducts: List<ProductItem>,
    onAddMarket: (String) -> Unit,
    onEditMarket: (marketId: String, newName: String, active: Boolean) -> Unit,
    onToggleActive: (Market) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingMarket by remember { mutableStateOf<Market?>(null) }
    var confirmDeactivateMarket by remember { mutableStateOf<Market?>(null) }

    val sortedMarkets = remember(markets) {
        markets.sortedWith(compareByDescending<Market> { it.active }.thenBy { it.name.lowercase() })
    }

    val countByMarketId = remember(activeProducts) {
        activeProducts
            .filter { it.status == ProductItem.STATUS_ACTIVE }
            .groupingBy { it.marketId }
            .eachCount()
    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mercados",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${markets.count { it.active }} ativos • ${markets.size} no total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("add_market_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Novo mercado")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(sortedMarkets, key = { it.id }) { market ->
                val productCount = countByMarketId[market.id] ?: 0
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (market.active) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = if (market.active) 0.7f else 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("market_item_${market.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (market.active) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = if (market.active) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = market.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (market.active) {
                                    "$productCount produto(s) ativo(s)"
                                } else {
                                    "Desativado • $productCount ativo(s)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FilledTonalButton(
                            onClick = { editingMarket = market },
                            modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Editar ${market.name}",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Editar")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Switch(
                            checked = market.active,
                            onCheckedChange = { wantActive ->
                                if (!wantActive) {
                                    confirmDeactivateMarket = market
                                } else {
                                    onToggleActive(market)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        MarketNameDialog(
            title = "Cadastrar mercado",
            initialName = "",
            confirmLabel = "Cadastrar",
            onDismiss = { showAddDialog = false },
            onConfirm = { newName ->
                onAddMarket(newName)
                showAddDialog = false
            }
        )
    }

    editingMarket?.let { market ->
        MarketNameDialog(
            title = "Editar mercado",
            initialName = market.name,
            confirmLabel = "Salvar",
            onDismiss = { editingMarket = null },
            onConfirm = { newName ->
                onEditMarket(market.id, newName, market.active)
                editingMarket = null
            }
        )
    }

    confirmDeactivateMarket?.let { market ->
        AlertDialog(
            onDismissRequest = { confirmDeactivateMarket = null },
            title = { Text("Desativar ${market.name}?") },
            text = {
                Text("Mercados desativados não aparecem para novos cadastros, mas os produtos já cadastrados continuam preservados.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onToggleActive(market)
                        confirmDeactivateMarket = null
                    }
                ) {
                    Text("Desativar")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeactivateMarket = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun MarketNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    label = { Text("Nome do supermercado") },
                    placeholder = { Text("Ex: Angeloni Centro") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("market_name_dialog_input")
                )
                if (error != null) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        error = "Digite o nome do mercado."
                    } else {
                        onConfirm(trimmed)
                    }
                }
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
