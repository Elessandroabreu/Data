package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ExpiryUrgency
import com.example.data.model.Market
import com.example.data.model.ProductItem
import com.example.data.model.UserSettings
import com.example.data.repository.DataRepository
import com.example.notifications.ExpiryNotificationHelper
import com.example.util.DateUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class StatusFilter(val label: String) {
    ALL("Todos"),
    EXPIRED_RECENT("Vencidos recentes"),
    DUE_TODAY("Vence hoje"),
    DUE_3_DAYS("Até 3 dias"),
    DUE_4_TO_7_DAYS("4 a 7 dias"),
    DUE_7_DAYS("Até 7 dias"),
    SAFE("+7 dias")
}

sealed interface DateFilter {
    data object All : DateFilter
    data object Today : DateFilter
    data object Next3Days : DateFilter
    data object Next7Days : DateFilter
    data class ExactDate(val isoDate: String) : DateFilter
}

enum class HistoryReasonFilter(val label: String) {
    ALL("Todos"),
    AUTO("Automático"),
    MANUAL("Manual"),
    RESOLVED("Resolvido")
}

data class HomeUiModel(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val todayIso: String = DateUtils.todayIso(),
    val markets: List<Market> = emptyList(),
    val activeMarkets: List<Market> = emptyList(),
    val selectedMarketId: String? = null,
    val selectedMarketName: String = "Todos os mercados",
    val searchQuery: String = "",
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val dateFilter: DateFilter = DateFilter.All,
    val expiredRecentCount: Int = 0,
    val dueTodayCount: Int = 0,
    val dueIn3DaysCount: Int = 0,
    val dueIn7DaysCount: Int = 0,
    val totalActiveInSelection: Int = 0,
    val allActiveProducts: List<ProductItem> = emptyList(),
    val displayedProducts: List<ProductItem> = emptyList(),
    val historyProducts: List<ProductItem> = emptyList(),
    val notificationsEnabled: Boolean = true
)

class DataViewModel(
    private val repository: DataRepository,
    private val currentUserId: String,
    private val appContext: Context
) : ViewModel() {

    private val _selectedMarketId = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _statusFilter = MutableStateFlow(StatusFilter.ALL)
    private val _dateFilter = MutableStateFlow<DateFilter>(DateFilter.All)
    private val _todayIso = MutableStateFlow(DateUtils.todayIso())

    private val _historySearch = MutableStateFlow("")
    val historySearch: StateFlow<String> = _historySearch.asStateFlow()

    private val _historyMarketId = MutableStateFlow<String?>(null)
    val historyMarketId: StateFlow<String?> = _historyMarketId.asStateFlow()

    private val _historyReasonFilter = MutableStateFlow(HistoryReasonFilter.ALL)
    val historyReasonFilter: StateFlow<HistoryReasonFilter> = _historyReasonFilter.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var lastAutoArchiveRunDate: String? = null
    private var lastNotifiedSignature: String? = null

    private val settingsFlow: StateFlow<UserSettings?> = repository.observeSettings(currentUserId)
        .catch { error ->
            if (error is CancellationException &&
                generateSequence<Throwable>(error) { it.cause }.none { it is FirebaseFirestoreException }
            ) throw error
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val marketsState: StateFlow<UiState<List<Market>>> = repository.observeMarkets(currentUserId)
        .map<List<Market>, UiState<List<Market>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            emit(UiState.Error(root.localizedMessage ?: "Erro ao carregar mercados."))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val productsState: StateFlow<UiState<List<ProductItem>>> = repository.observeProducts(currentUserId)
        .map<List<ProductItem>, UiState<List<ProductItem>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            emit(UiState.Error(root.localizedMessage ?: "Erro ao carregar produtos."))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val filterInputsFlow = combine(
        _selectedMarketId,
        _searchQuery,
        _statusFilter,
        _dateFilter,
        _todayIso
    ) { marketId, query, status, dateFilter, today ->
        FilterInputs(marketId, query, status, dateFilter, today)
    }

    val homeUiModel: StateFlow<HomeUiModel> = combine(
        marketsState,
        productsState,
        settingsFlow,
        filterInputsFlow
    ) { mState, pState, settings, filters ->
        val today = filters.today
        val notificationsEnabled = settings?.notificationsEnabled ?: true

        if (mState is UiState.Error) {
            return@combine HomeUiModel(
                isLoading = false,
                errorMessage = mState.message,
                todayIso = today,
                notificationsEnabled = notificationsEnabled
            )
        }
        if (pState is UiState.Error) {
            return@combine HomeUiModel(
                isLoading = false,
                errorMessage = pState.message,
                todayIso = today,
                notificationsEnabled = notificationsEnabled
            )
        }
        if (mState is UiState.Loading || pState is UiState.Loading) {
            return@combine HomeUiModel(
                isLoading = true,
                todayIso = today,
                notificationsEnabled = notificationsEnabled
            )
        }

        val markets = (mState as UiState.Success).data
        val activeMarkets = markets.filter { it.active }
        val allProducts = (pState as UiState.Success).data

        // Produtos ativos (excluindo os já marcados para arquivamento automático > 3 dias)
        val activeProducts = allProducts
            .filter {
                it.status == ProductItem.STATUS_ACTIVE &&
                    DateUtils.daysUntilExpiry(it.expiryDate, today) >= -3L
            }
            .sortedWith(compareBy<ProductItem> { it.expiryDate }.thenBy { it.name.lowercase() })

        val selectedMarketName = filters.marketId?.let { id ->
            markets.firstOrNull { it.id == id }?.name
        } ?: "Todos os mercados"

        val byMarket = if (filters.marketId == null) {
            activeProducts
        } else {
            activeProducts.filter { it.marketId == filters.marketId }
        }

        var expiredRecent = 0
        var dueToday = 0
        var dueIn3Days = 0
        var dueIn7Days = 0

        byMarket.forEach { product ->
            when (val diff = DateUtils.daysUntilExpiry(product.expiryDate, today)) {
                in -3L..-1L -> expiredRecent++
                0L -> {
                    dueToday++
                    dueIn3Days++
                    dueIn7Days++
                }
                in 1L..3L -> {
                    dueIn3Days++
                    dueIn7Days++
                }
                in 4L..7L -> dueIn7Days++
            }
        }

        val queryLower = filters.query.trim().lowercase()
        val filtered = byMarket.filter { product ->
            val matchesQuery = queryLower.isEmpty() ||
                product.name.lowercase().contains(queryLower) ||
                product.notes.lowercase().contains(queryLower) ||
                product.marketName.lowercase().contains(queryLower)

            val diff = DateUtils.daysUntilExpiry(product.expiryDate, today)
            val urgency = DateUtils.classifyUrgency(product.expiryDate, today)

            val matchesStatus = when (filters.status) {
                StatusFilter.ALL -> true
                StatusFilter.EXPIRED_RECENT -> urgency == ExpiryUrgency.EXPIRED_RECENT
                StatusFilter.DUE_TODAY -> urgency == ExpiryUrgency.DUE_TODAY
                StatusFilter.DUE_3_DAYS -> diff in 0L..3L
                StatusFilter.DUE_4_TO_7_DAYS -> diff in 4L..7L
                StatusFilter.DUE_7_DAYS -> diff in 0L..7L
                StatusFilter.SAFE -> diff > 7L
            }

            val matchesDate = when (val df = filters.dateFilter) {
                DateFilter.All -> true
                DateFilter.Today -> diff == 0L
                DateFilter.Next3Days -> diff in 0L..3L
                DateFilter.Next7Days -> diff in 0L..7L
                is DateFilter.ExactDate -> product.expiryDate == df.isoDate
            }

            matchesQuery && matchesStatus && matchesDate
        }

        val history = allProducts
            .filter { it.status != ProductItem.STATUS_ACTIVE }
            .sortedByDescending { it.archivedDate ?: it.expiryDate }

        HomeUiModel(
            isLoading = false,
            errorMessage = null,
            todayIso = today,
            markets = markets,
            activeMarkets = activeMarkets,
            selectedMarketId = filters.marketId,
            selectedMarketName = selectedMarketName,
            searchQuery = filters.query,
            statusFilter = filters.status,
            dateFilter = filters.dateFilter,
            expiredRecentCount = expiredRecent,
            dueTodayCount = dueToday,
            dueIn3DaysCount = dueIn3Days,
            dueIn7DaysCount = dueIn7Days,
            totalActiveInSelection = byMarket.size,
            allActiveProducts = activeProducts,
            displayedProducts = filtered,
            historyProducts = history,
            notificationsEnabled = notificationsEnabled
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        HomeUiModel()
    )

    val filteredHistory: StateFlow<List<ProductItem>> = combine(
        homeUiModel,
        _historySearch,
        _historyMarketId,
        _historyReasonFilter
    ) { model, search, marketId, reasonFilter ->
        val q = search.trim().lowercase()
        model.historyProducts.filter { item ->
            val matchesSearch = q.isEmpty() ||
                item.name.lowercase().contains(q) ||
                item.marketName.lowercase().contains(q) ||
                item.notes.lowercase().contains(q)
            val matchesMarket = marketId == null || item.marketId == marketId
            val matchesReason = when (reasonFilter) {
                HistoryReasonFilter.ALL -> true
                HistoryReasonFilter.AUTO -> item.status == ProductItem.STATUS_ARCHIVED_AUTO
                HistoryReasonFilter.MANUAL -> item.status == ProductItem.STATUS_ARCHIVED_MANUAL
                HistoryReasonFilter.RESOLVED -> item.status == ProductItem.STATUS_RESOLVED
            }
            matchesSearch && matchesMarket && matchesReason
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureDefaultMarketsSeeded()
        }
        // Observa produtos para arquivar automaticamente vencidos > 3 dias e manter cache de notificações
        viewModelScope.launch {
            combine(productsState, settingsFlow) { pState, settings ->
                pState to (settings?.notificationsEnabled ?: true)
            }.collect { (pState, notifEnabled) ->
                if (pState is UiState.Success) {
                    val today = DateUtils.todayIso()
                    _todayIso.value = today
                    val products = pState.data
                    val hasExpiredOver15Days = products.any {
                        DateUtils.daysUntilExpiry(it.expiryDate, today) < -15L
                    }
                    val purged = if (hasExpiredOver15Days) {
                        repository.purgeExpiredOver15Days(products, today).getOrDefault(0)
                    } else {
                        0
                    }
                    val hasExpiredOver3Days = products.any {
                        val diff = DateUtils.daysUntilExpiry(it.expiryDate, today)
                        it.status == ProductItem.STATUS_ACTIVE && diff in -15L..-4L
                    }
                    val archived = if (hasExpiredOver3Days) {
                        repository.autoArchiveExpiredProducts(products, today).getOrDefault(0)
                    } else {
                        0
                    }
                    if ((archived > 0 || purged > 0) && lastAutoArchiveRunDate != today) {
                        lastAutoArchiveRunDate = today
                        val parts = buildList {
                            if (archived > 0) add("$archived arquivado(s) automaticamente")
                            if (purged > 0) add("$purged excluído(s) após 15 dias do vencimento")
                        }
                        _messages.tryEmit(parts.joinToString(" e ") + ".")
                    }

                    val activeOnly = products.filter {
                        it.status == ProductItem.STATUS_ACTIVE &&
                            DateUtils.daysUntilExpiry(it.expiryDate, today) >= -3L
                    }
                    ExpiryNotificationHelper.cacheProductsAndSettings(
                        appContext,
                        notifEnabled,
                        activeOnly
                    )
                    if (notifEnabled) {
                        ExpiryNotificationHelper.scheduleDailyReminder(appContext)
                        val urgentIds = activeOnly
                            .filter { DateUtils.daysUntilExpiry(it.expiryDate, today) in 0L..3L }
                            .joinToString(",") { "${it.id}:${it.expiryDate}" }
                        val sig = "$today|$urgentIds"
                        if (urgentIds.isNotEmpty() && lastNotifiedSignature != sig) {
                            lastNotifiedSignature = sig
                            ExpiryNotificationHelper.notifyUrgentFromCache(appContext)
                        }
                    } else {
                        ExpiryNotificationHelper.cancelDailyReminder(appContext)
                    }
                }
            }
        }
    }

    fun selectMarket(marketId: String?) {
        _selectedMarketId.value = marketId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(filter: StatusFilter) {
        _statusFilter.value = if (_statusFilter.value == filter && filter != StatusFilter.ALL) {
            StatusFilter.ALL
        } else {
            filter
        }
    }

    fun setDateFilter(filter: DateFilter) {
        _dateFilter.value = filter
    }

    fun clearAllHomeFilters() {
        _selectedMarketId.value = null
        _searchQuery.value = ""
        _statusFilter.value = StatusFilter.ALL
        _dateFilter.value = DateFilter.All
    }

    fun setHistorySearch(query: String) {
        _historySearch.value = query
    }

    fun setHistoryMarketId(marketId: String?) {
        _historyMarketId.value = marketId
    }

    fun setHistoryReasonFilter(filter: HistoryReasonFilter) {
        _historyReasonFilter.value = filter
    }

    fun refreshAndRunAutoArchive() {
        viewModelScope.launch {
            val today = DateUtils.todayIso()
            _todayIso.value = today
            val current = (productsState.value as? UiState.Success)?.data ?: return@launch
            val purgeResult = repository.purgeExpiredOver15Days(current, today)
            val archiveResult = repository.autoArchiveExpiredProducts(current, today)
            if (purgeResult.isFailure || archiveResult.isFailure) {
                _messages.tryEmit("Não foi possível atualizar. Verifique sua conexão.")
                return@launch
            }
            val purged = purgeResult.getOrDefault(0)
            val archived = archiveResult.getOrDefault(0)
            val msg = when {
                archived > 0 && purged > 0 ->
                    "$archived arquivado(s) e $purged excluído(s) após 15 dias."
                archived > 0 ->
                    "$archived produto(s) arquivado(s) automaticamente."
                purged > 0 ->
                    "$purged produto(s) vencido(s) há mais de 15 dias excluído(s)."
                else ->
                    "Lista atualizada."
            }
            _messages.tryEmit(msg)
        }
    }

    fun saveProduct(
        editingProductId: String?,
        name: String,
        market: Market,
        expiryDateIso: String,
        notes: String,
        photoBase64: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = if (editingProductId == null) {
                repository.addProduct(
                    name = name,
                    marketId = market.id,
                    marketName = market.name,
                    expiryDateIso = expiryDateIso,
                    notes = notes,
                    photoBase64 = photoBase64
                ).map { Unit }
            } else {
                repository.updateProduct(
                    productId = editingProductId,
                    name = name,
                    marketId = market.id,
                    marketName = market.name,
                    expiryDateIso = expiryDateIso,
                    notes = notes,
                    photoBase64 = photoBase64
                )
            }
            _isSaving.value = false
            result.onSuccess {
                _messages.tryEmit(
                    if (editingProductId == null) "Produto cadastrado com sucesso."
                    else "Produto atualizado."
                )
                onSuccess()
            }.onFailure { err ->
                _messages.tryEmit(
                    err.localizedMessage ?: "Erro ao salvar produto. Verifique a conexão."
                )
            }
        }
    }

    fun markProductResolved(productId: String) {
        viewModelScope.launch {
            repository.archiveProduct(
                productId = productId,
                targetStatus = ProductItem.STATUS_RESOLVED,
                reason = ProductItem.REASON_RESOLVED
            ).onSuccess {
                _messages.tryEmit("Produto marcado como resolvido e movido para o histórico.")
            }.onFailure {
                _messages.tryEmit("Erro ao resolver produto. Tente novamente.")
            }
        }
    }

    fun archiveProductManually(productId: String) {
        viewModelScope.launch {
            repository.archiveProduct(
                productId = productId,
                targetStatus = ProductItem.STATUS_ARCHIVED_MANUAL,
                reason = ProductItem.REASON_MANUAL
            ).onSuccess {
                _messages.tryEmit("Produto arquivado manualmente no histórico.")
            }.onFailure {
                _messages.tryEmit("Erro ao arquivar produto. Tente novamente.")
            }
        }
    }

    fun restoreProduct(productId: String) {
        viewModelScope.launch {
            repository.restoreProduct(productId)
                .onSuccess {
                    _messages.tryEmit("Produto restaurado para a lista ativa.")
                }
                .onFailure {
                    _messages.tryEmit("Erro ao restaurar produto.")
                }
        }
    }

    fun deleteProductPermanently(productId: String) {
        viewModelScope.launch {
            repository.deleteProductPermanently(productId)
                .onSuccess {
                    _messages.tryEmit("Registro excluído definitivamente.")
                }
                .onFailure {
                    _messages.tryEmit("Erro ao excluir registro.")
                }
        }
    }

    fun addMarket(name: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.addMarket(name)
                .onSuccess {
                    _messages.tryEmit("Mercado \"$name\" cadastrado.")
                    onDone()
                }
                .onFailure { err ->
                    _messages.tryEmit(err.localizedMessage ?: "Erro ao cadastrar mercado.")
                }
        }
    }

    fun updateMarket(marketId: String, newName: String, active: Boolean, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateMarket(marketId, newName, active)
                .onSuccess {
                    _messages.tryEmit("Mercado atualizado.")
                    onDone()
                }
                .onFailure { err ->
                    _messages.tryEmit(err.localizedMessage ?: "Erro ao atualizar mercado.")
                }
        }
    }

    fun toggleMarketActive(market: Market) {
        viewModelScope.launch {
            val newState = !market.active
            repository.setMarketActive(market.id, newState)
                .onSuccess {
                    _messages.tryEmit(
                        if (newState) "Mercado \"${market.name}\" ativado."
                        else "Mercado \"${market.name}\" desativado."
                    )
                }
                .onFailure {
                    _messages.tryEmit("Erro ao alterar status do mercado.")
                }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setNotificationsEnabled(enabled)
                .onSuccess {
                    if (enabled) {
                        ExpiryNotificationHelper.scheduleDailyReminder(appContext)
                    } else {
                        ExpiryNotificationHelper.cancelDailyReminder(appContext)
                    }
                    _messages.tryEmit(
                        if (enabled) "Notificações ativadas."
                        else "Notificações desativadas."
                    )
                }
                .onFailure {
                    _messages.tryEmit("Erro ao salvar preferência de notificações.")
                }
        }
    }

    fun triggerTestNotification() {
        val sent = ExpiryNotificationHelper.notifyUrgentFromCache(appContext)
        if (sent) {
            _messages.tryEmit("Alerta de vencimento enviado.")
        } else {
            _messages.tryEmit("Sem produtos vencendo em até 3 dias ou permissão pendente.")
        }
    }

    fun findProductById(productId: String?): ProductItem? {
        if (productId == null) return null
        val list = (productsState.value as? UiState.Success)?.data ?: return null
        return list.firstOrNull { it.id == productId }
    }

    private data class FilterInputs(
        val marketId: String?,
        val query: String,
        val status: StatusFilter,
        val dateFilter: DateFilter,
        val today: String
    )
}
