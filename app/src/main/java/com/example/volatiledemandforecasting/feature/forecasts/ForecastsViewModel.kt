package com.example.volatiledemandforecasting.feature.forecasts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volatiledemandforecasting.core.datastore.StoreSelection
import com.example.volatiledemandforecasting.core.network.attempt
import com.example.volatiledemandforecasting.core.network.toUiText
import com.example.volatiledemandforecasting.core.ui.UiText
import com.example.volatiledemandforecasting.core.ui.components.UiState
import com.example.volatiledemandforecasting.core.ui.components.toUiState
import com.example.volatiledemandforecasting.data.forecast.BatchDto
import com.example.volatiledemandforecasting.data.forecast.BatchItemDto
import com.example.volatiledemandforecasting.data.forecast.BatchRepository
import com.example.volatiledemandforecasting.data.forecast.ForecastRepository
import com.example.volatiledemandforecasting.data.forecast.ForecastSummaryDto
import com.example.volatiledemandforecasting.data.forecast.ShownMethod
import com.example.volatiledemandforecasting.data.forecast.shownMethodOf
import com.example.volatiledemandforecasting.data.store.CapabilitiesDto
import com.example.volatiledemandforecasting.data.store.StoreRepository
import com.example.volatiledemandforecasting.data.workspace.ActiveStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The store-wide forecast: every product's expected sales for the next few days. */
data class StoreForecast(
    val store: StoreSelection,
    val capabilities: CapabilitiesDto,
    /** AI or simple, decided once for the store from its model's testing. */
    val shown: ShownMethod,
    /** Null until the store has been forecast once. */
    val batch: BatchDto?,
    /** False until the shop has uploaded sales; there is nothing to forecast before that. */
    val hasSales: Boolean = true,
    val items: List<BatchItemDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val loadingMore: Boolean = false,
    val starting: Boolean = false,
) {
    val hasMore: Boolean get() = items.size < total
}

/** Single-product forecasts, newest first: the forecast history the Forecasts tab shows next to the store forecast. */
data class ForecastHistory(
    val items: List<ForecastSummaryDto>,
    val total: Int,
    val page: Int = 1,
    val loadingMore: Boolean = false,
) {
    val hasMore: Boolean get() = items.size < total
}

/** The two views of the Forecasts tab. */
enum class ForecastsView { Store, Products }

private const val PAGE_SIZE = 50
private const val HISTORY_PAGE_SIZE = 20
private const val POLL_MS = 2500L

class ForecastsViewModel(
    private val batches: BatchRepository,
    private val stores: StoreRepository,
    private val forecasts: ForecastRepository,
    private val activeStore: ActiveStore,
) : ViewModel() {

    private val _view = MutableStateFlow(ForecastsView.Store)
    val view: StateFlow<ForecastsView> = _view.asStateFlow()

    private val _history = MutableStateFlow<UiState<ForecastHistory>>(UiState.Loading)
    val history: StateFlow<UiState<ForecastHistory>> = _history.asStateFlow()

    /** A [JobStatus] to filter the history by, or null for all. */
    private val _historyStatus = MutableStateFlow<String?>(null)
    val historyStatus: StateFlow<String?> = _historyStatus.asStateFlow()

    private var historyJob: Job? = null

    private val _state = MutableStateFlow<UiState<StoreForecast>>(UiState.Loading)
    val state: StateFlow<UiState<StoreForecast>> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** The length for the next "Forecast all products". Null until capabilities are known. */
    private val _horizon = MutableStateFlow<Int?>(null)
    val horizon: StateFlow<Int?> = _horizon.asStateFlow()

    private val _messages = Channel<UiText>(Channel.BUFFERED)
    val messages: Flow<UiText> = _messages.receiveAsFlow()

    private var store: StoreSelection? = null
    private var pollJob: Job? = null
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            // An import changes the history, so the latest batch may now be outdated.
            combine(activeStore.selected, activeStore.dataRevision) { s, _ -> s }.collect {
                store = it
                load()
                loadHistory()
            }
        }
    }

    fun load() {
        val s = store ?: return
        pollJob?.cancel()
        _state.value = UiState.Loading
        viewModelScope.launch {
            attempt {
                coroutineScope {
                    val caps = async { stores.capabilities(s.storeId) }
                    val latest = async { batches.latest(s.storeId) }
                    val history = async { runCatching { stores.historySummary(s.storeId).historyEnd != null }.getOrDefault(true) }
                    val capabilities = caps.await()
                    // The model's own testing decides AI or simple. No model, or no card: simple.
                    val card = if (capabilities.activeModel != null) attempt { stores.activeModel(s.storeId) }.getOrNull()?.card else null
                    StoreForecast(s, capabilities, shownMethodOf(card?.backtest), latest.await(), hasSales = history.await())
                }
            }
                .onSuccess { data ->
                    if (_horizon.value == null || _horizon.value !in data.capabilities.horizons) {
                        _horizon.value = data.batch?.horizonDays?.takeIf { it in data.capabilities.horizons }
                            ?: data.capabilities.horizons.firstOrNull { it == 7 }
                            ?: data.capabilities.horizons.firstOrNull()
                    }
                    _state.value = UiState.Content(data)
                    afterBatch(data)
                }
                .onFailure { _state.value = it.toUiState() }
        }
    }

    fun setHorizon(days: Int) = _horizon.update { days }

    fun setView(view: ForecastsView) {
        _view.value = view
        // Coming back to the list shows forecasts started from a product since it was loaded.
        if (view == ForecastsView.Products) loadHistory()
    }

    fun setHistoryStatus(status: String?) {
        if (status == _historyStatus.value) return
        _historyStatus.value = status
        loadHistory()
    }

    fun loadHistory() {
        val s = store ?: return
        historyJob?.cancel()
        if (_history.value !is UiState.Content) _history.value = UiState.Loading
        historyJob = viewModelScope.launch {
            // batch = false: store-wide runs are shown by the store forecast, not repeated here.
            attempt { forecasts.list(s.storeId, replay = false, status = _historyStatus.value, page = 1, pageSize = HISTORY_PAGE_SIZE, batch = false) }
                .onSuccess { page ->
                    _history.value = if (page.items.isEmpty()) UiState.Empty else UiState.Content(ForecastHistory(page.items, page.total))
                }
                .onFailure { _history.value = it.toUiState() }
        }
    }

    fun loadMoreHistory() {
        val s = store ?: return
        val current = (_history.value as? UiState.Content)?.data ?: return
        if (current.loadingMore || !current.hasMore) return
        _history.value = UiState.Content(current.copy(loadingMore = true))
        viewModelScope.launch {
            val next = current.page + 1
            attempt { forecasts.list(s.storeId, replay = false, status = _historyStatus.value, page = next, pageSize = HISTORY_PAGE_SIZE, batch = false) }
                .onSuccess { page -> _history.value = UiState.Content(ForecastHistory(current.items + page.items, page.total, next)) }
                .onFailure {
                    _history.value = UiState.Content(current.copy(loadingMore = false))
                    _messages.send(it.toUiText())
                }
        }
    }

    fun setQuery(value: String) {
        _query.value = value
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            val data = content() ?: return@launch
            loadItems(data, page = 1)
        }
    }

    /** "Forecast all products": starts a batch, or picks up the one already running. */
    fun forecastAll() {
        val s = store ?: return
        val data = content() ?: return
        val days = _horizon.value ?: return
        if (data.starting || data.batch?.isRunning == true) return
        _state.value = UiState.Content(data.copy(starting = true))
        viewModelScope.launch {
            attempt { batches.create(s.storeId, days) }
                .onSuccess { batch ->
                    val next = data.copy(batch = batch, items = emptyList(), total = 0, starting = false)
                    _state.value = UiState.Content(next)
                    afterBatch(next)
                }
                .onFailure {
                    _state.value = UiState.Content(data.copy(starting = false))
                    _messages.send(it.toUiText())
                }
        }
    }

    /** Queues again the products that failed for a temporary reason. */
    fun retryFailed() {
        val batch = content()?.batch ?: return
        viewModelScope.launch {
            attempt { batches.retry(batch.batchId) }
                .onSuccess { updated ->
                    val current = content() ?: return@onSuccess
                    val next = current.copy(batch = updated)
                    _state.value = UiState.Content(next)
                    afterBatch(next)
                }
                .onFailure { _messages.send(it.toUiText()) }
        }
    }

    fun loadMore() {
        val data = content() ?: return
        if (data.loadingMore || !data.hasMore) return
        _state.value = UiState.Content(data.copy(loadingMore = true))
        viewModelScope.launch { loadItems(data, page = data.page + 1) }
    }

    private fun content(): StoreForecast? = (_state.value as? UiState.Content)?.data

    /** A running batch is polled until it finishes; a finished one shows its rows. */
    private fun afterBatch(data: StoreForecast) {
        val batch = data.batch ?: return
        if (batch.isRunning) {
            pollJob?.cancel()
            pollJob = viewModelScope.launch { poll(batch.batchId) }
        } else {
            viewModelScope.launch { loadItems(data, page = 1) }
        }
    }

    private suspend fun poll(batchId: String) {
        while (true) {
            delay(POLL_MS)
            val batch = attempt { batches.batch(batchId) }.getOrNull() ?: continue
            val data = content() ?: return
            if (data.batch?.batchId != batchId) return
            val next = data.copy(batch = batch)
            _state.value = UiState.Content(next)
            if (!batch.isRunning) {
                loadItems(next, page = 1)
                return
            }
        }
    }

    private suspend fun loadItems(data: StoreForecast, page: Int) {
        val batch = data.batch ?: return
        attempt { batches.items(batch, _query.value, page, PAGE_SIZE) }
            .onSuccess { result ->
                val current = content() ?: data
                val items = if (page == 1) result.items else current.items + result.items
                _state.value = UiState.Content(current.copy(items = items, total = result.total, page = page, loadingMore = false))
            }
            .onFailure {
                content()?.let { current -> _state.value = UiState.Content(current.copy(loadingMore = false)) }
                _messages.send(it.toUiText())
            }
    }
}
