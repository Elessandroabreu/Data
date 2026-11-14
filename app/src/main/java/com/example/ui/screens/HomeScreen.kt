package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.ActiveProductCard
import com.example.ui.theme.LocalExpiryPalette
import com.example.ui.viewmodel.DateFilter
import com.example.ui.viewmodel.HomeUiModel
import com.example.ui.viewmodel.StatusFilter
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    model: HomeUiModel,
    userName: String?,
    onSelectMarket: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectStatusFilter: (StatusFilter) -> Unit,
    onSelectDateFilter: (DateFilter) -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onAddProductClick: () -> Unit,
    onEditProduct: (String) -> Unit,
    onResolveProduct: (String) -> Unit,
    onArchiveManualProduct: (String) -> Unit,
    onDeleteProduct: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMarketSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val expiryColors = LocalExpiryPalette.current

    val firstName = remember(userName) {
        userName?.trim()?.substringBefore(" ")?.takeIf { it.isNotBlank() } ?: "Promotor"
    }
    val hasAnyFilter = model.selectedMarketId != null ||
        model.searchQuery.isNotBlank() ||
        model.statusFilter != StatusFilter.ALL ||
        model.dateFilter != DateFilter.All

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Cabeçalho limpo no estilo do layout: saudação + DATA + botão destacado "+ Cadastrar" + busca em pílula
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Olá, $firstName 👋",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "DATA",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 3.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Button(
                        onClick = onAddProductClick,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("add_product_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cadastrar",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                OutlinedTextField(
                    value = model.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = "Buscar por nome do produto",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (model.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_input")
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

        if (model.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Carregando produtos...")
                }
            }
            return@Column
        }

        if (model.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = model.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onRefresh) {
                        Text("Tentar novamente")
                    }
                }
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 3 Cartões KPI verticais com ícone circular no topo e contagem destacada
            item(key = "counters_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCounterTile(
                        label = "Vencidos\nrecentes",
                        count = model.expiredRecentCount,
                        icon = Icons.Default.Report,
                        tintBg = expiryColors.expiredRecentBg,
                        accentColor = expiryColors.expiredRecentBadge,
                        countColor = expiryColors.expiredRecentText,
                        labelColor = expiryColors.kpiLabelText,
                        selected = model.statusFilter == StatusFilter.EXPIRED_RECENT,
                        onClick = { onSelectStatusFilter(StatusFilter.EXPIRED_RECENT) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("counter_expired_recent")
                    )
                    KpiCounterTile(
                        label = "Vencem em\n3 dias",
                        count = model.dueIn3DaysCount,
                        icon = Icons.Default.Schedule,
                        tintBg = expiryColors.dueSoonBg,
                        accentColor = expiryColors.dueSoonBadge,
                        countColor = expiryColors.dueSoonText,
                        labelColor = expiryColors.kpiLabelText,
                        selected = model.statusFilter == StatusFilter.DUE_3_DAYS,
                        onClick = { onSelectStatusFilter(StatusFilter.DUE_3_DAYS) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("counter_due_3_days")
                    )
                    KpiCounterTile(
                        label = "Vencem em\n7 dias",
                        count = model.dueIn7DaysCount,
                        icon = Icons.Default.HourglassBottom,
                        tintBg = expiryColors.warningBg,
                        accentColor = expiryColors.warningBadge,
                        countColor = expiryColors.warningText,
                        labelColor = expiryColors.kpiLabelText,
                        selected = model.statusFilter == StatusFilter.DUE_7_DAYS,
                        onClick = { onSelectStatusFilter(StatusFilter.DUE_7_DAYS) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("counter_due_7_days")
                    )
                }
            }

            // Alerta interno na tela inicial quando houver itens críticos/próximos
            if (model.dueTodayCount > 0 || model.expiredRecentCount > 0 || model.dueIn3DaysCount > 0) {
                item(key = "in_app_alert_banner") {
                    InAppAlertBanner(
                        dueTodayCount = model.dueTodayCount,
                        expiredRecentCount = model.expiredRecentCount,
                        dueIn3DaysCount = model.dueIn3DaysCount,
                        onFilterUrgent = {
                            if (model.dueTodayCount > 0) {
                                onSelectStatusFilter(StatusFilter.DUE_TODAY)
                            } else {
                                onSelectStatusFilter(StatusFilter.DUE_3_DAYS)
                            }
                        }
                    )
                }
            }

            // Seção MERCADO com chips em pílula
            item(key = "market_filter_section") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MERCADO",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Ver todos (${model.markets.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showMarketSheet = true }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("market_dropdown_button")
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PillFilterChip(
                            label = "Todos",
                            selected = model.selectedMarketId == null,
                            onClick = { onSelectMarket(null) }
                        )
                        model.markets.forEach { market ->
                            PillFilterChip(
                                label = market.name,
                                selected = model.selectedMarketId == market.id,
                                onClick = {
                                    if (model.selectedMarketId == market.id) {
                                        onSelectMarket(null)
                                    } else {
                                        onSelectMarket(market.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Seção STATUS com chips em pílula + escolha de data
            item(key = "status_filter_section") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STATUS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val exactDateText = (model.dateFilter as? DateFilter.ExactDate)?.let {
                            "Data: ${DateUtils.isoToBrDisplay(it.isoDate)}"
                        } ?: "Filtrar por data"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showDatePicker = true }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = exactDateText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusFilter.entries.forEach { sf ->
                            PillFilterChip(
                                label = sf.label,
                                selected = model.statusFilter == sf,
                                onClick = { onSelectStatusFilter(sf) }
                            )
                        }
                    }
                }
            }

            // Cabeçalho PRODUTOS ATIVOS (N) + atualizar / limpar filtros
            item(key = "list_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PRODUTOS ATIVOS (${model.displayedProducts.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasAnyFilter) {
                            TextButton(onClick = onClearFilters) {
                                Text("Limpar")
                            }
                        }
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Atualizar lista e verificar vencidos",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (model.displayedProducts.isEmpty()) {
                item(key = "empty_state") {
                    EmptyProductsState(
                        hasAnyActive = model.totalActiveInSelection > 0,
                        selectedMarketName = model.selectedMarketName,
                        onClearFilters = onClearFilters,
                        onAddProduct = onAddProductClick
                    )
                }
            } else {
                items(
                    items = model.displayedProducts,
                    key = { it.id }
                ) { product ->
                    ActiveProductCard(
                        product = product,
                        todayIso = model.todayIso,
                        onEdit = { onEditProduct(product.id) },
                        onResolveConfirmed = { onResolveProduct(product.id) },
                        onArchiveManualConfirmed = { onArchiveManualProduct(product.id) },
                        onDeleteConfirmed = { onDeleteProduct(product.id) }
                    )
                }
            }
        }
    }

    if (showMarketSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMarketSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Filtrar por mercado",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                MarketSheetRow(
                    name = "Todos os mercados",
                    subtitle = "Mostrar todos os produtos ativos",
                    selected = model.selectedMarketId == null,
                    onClick = {
                        onSelectMarket(null)
                        showMarketSheet = false
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(model.markets, key = { it.id }) { market ->
                        MarketSheetRow(
                            name = market.name,
                            subtitle = if (market.active) "Mercado ativo" else "Desativado",
                            selected = model.selectedMarketId == market.id,
                            onClick = {
                                onSelectMarket(market.id)
                                showMarketSheet = false
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onSelectDateFilter(
                                DateFilter.ExactDate(DateUtils.utcMillisToIso(millis))
                            )
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Aplicar")
                }
            },
            dismissButton = {
                Row {
                    if (model.dateFilter != DateFilter.All) {
                        TextButton(
                            onClick = {
                                onSelectDateFilter(DateFilter.All)
                                showDatePicker = false
                            }
                        ) {
                            Text("Limpar data")
                        }
                    }
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancelar")
                    }
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun KpiCounterTile(
    label: String,
    count: Int,
    icon: ImageVector,
    tintBg: Color,
    accentColor: Color,
    countColor: Color,
    labelColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .heightIn(min = 138.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = tintBg,
        border = if (selected) BorderStroke(2.dp, accentColor) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = countColor
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = labelColor,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PillFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .height(42.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MarketSheetRow(
    name: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selecionado",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun InAppAlertBanner(
    dueTodayCount: Int,
    expiredRecentCount: Int,
    dueIn3DaysCount: Int,
    onFilterUrgent: () -> Unit
) {
    val expiryColors = LocalExpiryPalette.current
    val isCritical = dueTodayCount > 0 || expiredRecentCount > 0
    val accentColor = if (isCritical) expiryColors.dueTodayAccent else expiryColors.dueSoonBadge
    val bgColor = if (isCritical) expiryColors.dueTodayPillBg else expiryColors.dueSoonBg
    val bannerTextColor = if (isCritical) expiryColors.dueTodayText else expiryColors.dueSoonText
    val message = buildString {
        if (dueTodayCount > 0) append("$dueTodayCount produto(s) vence(m) HOJE! ")
        if (expiredRecentCount > 0) append("$expiredRecentCount vencido(s) recente(s). ")
        if (dueTodayCount == 0 && dueIn3DaysCount > 0) {
            append("$dueIn3DaysCount produto(s) vence(m) em até 3 dias.")
        }
    }.trim()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onFilterUrgent),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Alerta ativo",
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Atenção na gôndola",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = bannerTextColor
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = bannerTextColor
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = accentColor
            ) {
                Text(
                    text = "Filtrar",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyProductsState(
    hasAnyActive: Boolean,
    selectedMarketName: String,
    onClearFilters: () -> Unit,
    onAddProduct: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_state),
                contentDescription = "Prateleira vazia",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (hasAnyActive) {
                    "Nenhum produto encontrado com esses filtros"
                } else {
                    "Nenhum produto ativo em $selectedMarketName"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (hasAnyActive) {
                    "Tente limpar a busca ou escolher outro filtro."
                } else {
                    "Toque no botão abaixo para cadastrar um produto rapidamente com foto ou nome."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (hasAnyActive) {
                Button(
                    onClick = onClearFilters,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Limpar filtros")
                }
            } else {
                Button(
                    onClick = onAddProduct,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cadastrar produto")
                }
            }
        }
    }
}
