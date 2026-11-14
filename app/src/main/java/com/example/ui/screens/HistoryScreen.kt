package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Market
import com.example.data.model.ProductItem
import com.example.ui.components.PhotoPreviewDialog
import com.example.ui.components.ProductThumbnail
import com.example.ui.viewmodel.HistoryReasonFilter
import com.example.util.DateUtils

@Composable
fun HistoryScreen(
    items: List<ProductItem>,
    markets: List<Market>,
    searchQuery: String,
    selectedMarketId: String?,
    reasonFilter: HistoryReasonFilter,
    onSearchChange: (String) -> Unit,
    onSelectMarket: (String?) -> Unit,
    onSelectReasonFilter: (HistoryReasonFilter) -> Unit,
    onRestoreProduct: (String) -> Unit,
    onDeletePermanently: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmDeleteTarget by remember { mutableStateOf<ProductItem?>(null) }
    var zoomedPhotoItem by remember { mutableStateOf<ProductItem?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Histórico e Arquivados",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Vencidos há mais de 3 dias são arquivados automaticamente e excluídos do banco após 15 dias do vencimento.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Buscar no histórico...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HistoryReasonFilter.entries.forEach { rf ->
                        FilterChip(
                            selected = reasonFilter == rf,
                            onClick = { onSelectReasonFilter(rf) },
                            label = { Text(rf.label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedMarketId == null,
                        onClick = { onSelectMarket(null) },
                        label = { Text("Todos os mercados") }
                    )
                    markets.forEach { market ->
                        FilterChip(
                            selected = selectedMarketId == market.id,
                            onClick = { onSelectMarket(market.id) },
                            label = { Text(market.name) }
                        )
                    }
                }
            }
        }

        if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Nenhum registro no histórico",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Produtos resolvidos, arquivados manualmente ou vencidos há mais de 3 dias aparecerão aqui.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    val (badgeText, badgeColor, badgeTextColor) = when (item.status) {
                        ProductItem.STATUS_ARCHIVED_AUTO -> Triple(
                            "Arquivamento automático",
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.onTertiary
                        )
                        ProductItem.STATUS_ARCHIVED_MANUAL -> Triple(
                            "Arquivamento manual",
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.onSecondary
                        )
                        ProductItem.STATUS_RESOLVED -> Triple(
                            "Resolvido",
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.onPrimary
                        )
                        else -> Triple(
                            "Arquivado",
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.onSecondary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_card_${item.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                ProductThumbnail(
                                    photoBase64 = item.photoBase64,
                                    contentDescription = item.name.ifBlank { "Foto do produto" },
                                    modifier = Modifier.size(68.dp),
                                    onClickPhoto = if (!item.photoBase64.isNullOrBlank()) {
                                        { zoomedPhotoItem = item }
                                    } else null
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = badgeColor
                                    ) {
                                        Text(
                                            text = badgeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = badgeTextColor,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.name.ifBlank { "Produto registrado por foto" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Storefront,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = item.marketName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Vencimento: ${DateUtils.isoToBrDisplay(item.expiryDate)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (!item.archivedDate.isNullOrBlank()) {
                                        Text(
                                            text = "Arquivado em: ${DateUtils.isoToBrDisplay(item.archivedDate)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onRestoreProduct(item.id) },
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(48.dp)
                                        .testTag("restore_product_${item.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Restore,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Restaurar")
                                }
                                OutlinedButton(
                                    onClick = { confirmDeleteTarget = item },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("delete_history_${item.id}")
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    zoomedPhotoItem?.let { item ->
        if (!item.photoBase64.isNullOrBlank()) {
            PhotoPreviewDialog(
                photoBase64 = item.photoBase64,
                title = item.name,
                onDismiss = { zoomedPhotoItem = null }
            )
        }
    }

    confirmDeleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDeleteTarget = null },
            title = { Text("Excluir definitivamente?") },
            text = {
                Text("Deseja apagar permanentemente \"${target.name.ifBlank { "este produto" }}\"? Esta ação não pode ser desfeita.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePermanently(target.id)
                        confirmDeleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteTarget = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
