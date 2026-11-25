package com.example.volatiledemandforecasting.feature.workspace.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.datastore.StoreSelection
import com.example.volatiledemandforecasting.core.network.ApiException
import com.example.volatiledemandforecasting.core.network.attempt
import com.example.volatiledemandforecasting.core.network.rejectedFields
import com.example.volatiledemandforecasting.core.network.toUiText
import com.example.volatiledemandforecasting.core.ui.UiText
import com.example.volatiledemandforecasting.data.workspace.ActiveStore
import com.example.volatiledemandforecasting.data.workspace.CreateStoreRequest
import com.example.volatiledemandforecasting.data.workspace.WorkspaceDto
import com.example.volatiledemandforecasting.data.workspace.WorkspaceRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId

enum class StoreField { WorkspaceName, Code, StoreName, Country, Timezone, Currency }

/** Field checks that need no server. The backend still validates everything. */
object StoreFormValidation {
    fun required(value: String): UiText? = if (value.isBlank()) UiText.Res(R.string.create_required) else null

    fun country(value: String): UiText? =
        if (value.trim().length != 2 || !value.trim().all { it.isLetter() }) UiText.Res(R.string.create_country_invalid) else null

    fun currency(value: String): UiText? =
        if (value.trim().length != 3 || !value.trim().all { it.isLetter() }) UiText.Res(R.string.create_currency_invalid) else null

    fun timezone(value: String): UiText? =
        if (value.isBlank() || value.trim() !in ZoneId.getAvailableZoneIds()) UiText.Res(R.string.create_timezone_invalid) else null
}

data class CreateWorkspaceState(
    val workspaceName: String = "",
    val code: String = "",
    val storeName: String = "",
    // Offered as defaults for Kenyan deployments and can be changed (spec §8.3).
    val country: String = "KE",
    val subdivision: String = "",
    val timezone: String = "Africa/Nairobi",
    val currency: String = "KES",
    val errors: Map<StoreField, UiText> = emptyMap(),
    val submitting: Boolean = false,
    /** Set once the workspace exists, so a failed store step doesn't create a second workspace on retry. */
    val createdWorkspace: WorkspaceDto? = null,
    /** True when [createdWorkspace] existed before this form opened: only its shop is being added. */
    val existingWorkspace: Boolean = false,
    val error: UiText? = null,
)

class CreateWorkspaceViewModel(
    private val repository: WorkspaceRepository,
    private val activeStore: ActiveStore,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateWorkspaceState())
    val state: StateFlow<CreateWorkspaceState> = _state.asStateFlow()

    private val _done = Channel<Unit>(Channel.BUFFERED)
    val done: Flow<Unit> = _done.receiveAsFlow()

    fun edit(transform: CreateWorkspaceState.() -> CreateWorkspaceState) =
        _state.update { it.transform().copy(error = null) }

    /** Adds the shop to a business that has none yet, instead of creating a second business. */
    fun useWorkspace(workspace: WorkspaceDto) = _state.update {
        if (it.createdWorkspace != null) it else it.copy(createdWorkspace = workspace, existingWorkspace = true)
    }

    fun clearError(field: StoreField) = _state.update { it.copy(errors = it.errors - field, error = null) }

    private fun validate(s: CreateWorkspaceState): Map<StoreField, UiText> = buildMap {
        if (s.createdWorkspace == null) StoreFormValidation.required(s.workspaceName)?.let { put(StoreField.WorkspaceName, it) }
        StoreFormValidation.required(s.code)?.let { put(StoreField.Code, it) }
        StoreFormValidation.required(s.storeName)?.let { put(StoreField.StoreName, it) }
        StoreFormValidation.country(s.country)?.let { put(StoreField.Country, it) }
        StoreFormValidation.timezone(s.timezone)?.let { put(StoreField.Timezone, it) }
        StoreFormValidation.currency(s.currency)?.let { put(StoreField.Currency, it) }
    }

    fun submit() {
        val s = _state.value
        if (s.submitting) return
        val errors = validate(s)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        _state.update { it.copy(submitting = true, error = null, errors = emptyMap()) }
        viewModelScope.launch {
            val workspace = s.createdWorkspace
                ?: attempt { repository.createWorkspace(s.workspaceName.trim()) }
                    .onSuccess { w -> _state.update { it.copy(createdWorkspace = w) } }
                    .getOrElse { e ->
                        fail(e)
                        return@launch
                    }
            attempt {
                repository.createStore(
                    workspace.id,
                    CreateStoreRequest(
                        code = s.code.trim(),
                        name = s.storeName.trim(),
                        country = s.country.trim().uppercase(),
                        subdivision = s.subdivision.trim().ifEmpty { null },
                        timezone = s.timezone.trim(),
                        currency = s.currency.trim().uppercase(),
                    ),
                )
            }
                .onSuccess { store ->
                    activeStore.select(
                        StoreSelection(workspace.id, workspace.name, workspace.role, workspace.isReference, store.id, store.name)
                    )
                    _state.update { it.copy(submitting = false) }
                    _done.send(Unit)
                }
                .onFailure { fail(it) }
        }
    }

    private fun fail(e: Throwable) {
        val rejected = (e as? ApiException)?.rejectedFields().orEmpty()
        val fieldErrors = buildMap<StoreField, UiText> {
            val msg = UiText.Res(R.string.create_rejected)
            if ("code" in rejected) put(StoreField.Code, msg)
            if ("name" in rejected) put(if (_state.value.createdWorkspace == null) StoreField.WorkspaceName else StoreField.StoreName, msg)
            if ("country" in rejected) put(StoreField.Country, msg)
            if ("timezone" in rejected) put(StoreField.Timezone, msg)
            if ("currency" in rejected) put(StoreField.Currency, msg)
        }
        _state.update { it.copy(submitting = false, errors = fieldErrors, error = if (fieldErrors.isEmpty()) e.toUiText() else null) }
    }
}
