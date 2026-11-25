package com.example.volatiledemandforecasting.feature.products.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volatiledemandforecasting.core.datastore.StoreSelection
import com.example.volatiledemandforecasting.core.network.attempt
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.components.UiState
import com.example.volatiledemandforecasting.core.ui.components.toUiState
import com.example.volatiledemandforecasting.data.catalog.ProductDto
import com.example.volatiledemandforecasting.data.catalog.ProductRepository
import com.example.volatiledemandforecasting.data.forecast.ForecastRepository
import com.example.volatiledemandforecasting.data.forecast.ForecastSummaryDto
import com.example.volatiledemandforecasting.data.store.StoreRepository
import com.example.volatiledemandforecasting.data.workspace.ActiveStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

const val HISTORY_DAYS = 90

/** The sales periods the chart can show. The API allows up to two years per request. */
val HISTORY_RANGES = listOf(30, HISTORY_DAYS, 365)

data class ProductDetail(
    val store: StoreSelection,
    val product: ProductDto,
    /** One value per day from [firstDate]. Null is a day with no recorded sales, drawn as a gap, never as 0. */
    val units: List<Double?>,
    val firstDate: LocalDate?,
    /** How many days [units] covers. */
    val days: Int = HISTORY_DAYS,
    /** This product's most recent forecasts, newest first (single-product and store-wide). */
    val recentForecasts: List<ForecastSummaryDto> = emptyList(),
) {
    val hasAnySales: Boolean get() = units.any { it != null }

    /** Units sold over the period: a plain sum of recorded days. Unrecorded days add nothing. */
    val totalUnits: Double get() = units.sumOf { it ?: 0.0 }

    /** Days in the period with a recorded value (including recorded zeros). */
    val recordedDays: Int get() = units.count { it != null }

    /** The last day in the period with recorded sales, and what it sold. */
    val lastRecorded: Pair<LocalDate, Double>?
        get() {
            val first = firstDate ?: return null
            val index = units.indexOfLast { it != null }
            return if (index < 0) null else first.plusDays(index.toLong()) to units[index]!!
        }
}

class ProductDetailViewModel(
    private val productId: String,
    private val products: ProductRepository,
    private val stores: StoreRepository,
    private val forecasts: ForecastRepository,
    private val activeStore: ActiveStore,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ProductDetail>>(UiState.Loading)
    val state: StateFlow<UiState<ProductDetail>> = _state.asStateFlow()

    private val _days = MutableStateFlow(HISTORY_DAYS)
    val days: StateFlow<Int> = _days.asStateFlow()

    private var store: StoreSelection? = null
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            activeStore.selected.collect {
                store = it
                load()
            }
        }
    }

    fun setDays(days: Int) {
        if (days == _days.value) return
        _days.value = days
        load()
    }

    fun load() {
        val s = store ?: return
        val days = _days.value
        loadJob?.cancel()
        _state.value = UiState.Loading
        loadJob = viewModelScope.launch {
            attempt {
                coroutineScope {
                    val product = async { products.product(s.storeId, productId) }
                    val recent = async {
                        runCatching {
                            forecasts.list(s.storeId, replay = false, status = null, page = 1, pageSize = 3, productId = productId).items
                        }.getOrDefault(emptyList())
                    }
                    val end = Formatting.parseDate(stores.historySummary(s.storeId).historyEnd)
                    val units = if (end == null) null else {
                        val start = end.minusDays(days - 1L)
                        val history = products.history(s.storeId, productId, start.toString(), end.toString())
                        val byDate = history.points.associate { it.date.take(10) to it.units }
                        start to (0 until days).map { byDate[start.plusDays(it.toLong()).toString()] }
                    }
                    ProductDetail(s, product.await(), units?.second.orEmpty(), units?.first, days, recent.await())
                }
            }
                .onSuccess { _state.value = UiState.Content(it) }
                .onFailure { _state.value = it.toUiState() }
        }
    }

    internal companion object {
        fun daysBetween(a: LocalDate, b: LocalDate) = ChronoUnit.DAYS.between(a, b).toInt()
    }
}
