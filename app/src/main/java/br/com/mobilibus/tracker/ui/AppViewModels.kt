package br.com.mobilibus.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.mobilibus.tracker.domain.model.Vehicle
import br.com.mobilibus.tracker.domain.repository.AuthRepository
import br.com.mobilibus.tracker.domain.repository.FavoriteRepository
import br.com.mobilibus.tracker.domain.repository.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface FeedUiState {
    data object Loading : FeedUiState
    data class Success(val vehicles: List<Vehicle>, val fetchedAt: Long) : FeedUiState
    data class Error(val message: String) : FeedUiState
}

class AuthViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _submitting = MutableStateFlow(false)
    val submitting = _submitting.asStateFlow()

    fun submit(email: String, password: String, register: Boolean) = viewModelScope.launch {
        _submitting.value = true
        _error.value = null
        val result = if (register) auth.register(email, password) else auth.login(email, password)
        _error.value = result.exceptionOrNull()?.message ?: if (result.isFailure) "Não foi possível continuar." else null
        _submitting.value = false
    }
}

class TrackerViewModel(
    private val userId: Long,
    private val favorites: FavoriteRepository,
    private val auth: AuthRepository,
    private val vehicles: VehicleRepository
) : ViewModel() {
    private val _feed = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val feed: StateFlow<FeedUiState> = _feed.asStateFlow()
    val favoriteVehicles = favorites.observe(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _feed.value = FeedUiState.Loading
        _feed.value = runCatching { vehicles.fetchVehicles() }
            .fold(
                onSuccess = { FeedUiState.Success(it.vehicles, it.fetchedAt) },
                onFailure = { FeedUiState.Error(it.message ?: "Falha ao carregar os veículos.") }
            )
    }

    fun toggleFavorite(vehicle: Vehicle, isFavorite: Boolean) = viewModelScope.launch {
        favorites.toggle(userId, vehicle, isFavorite)
    }

    fun logout() = viewModelScope.launch { auth.logout() }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : ViewModel> factory(crossinline create: () -> T) = object : ViewModelProvider.Factory {
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
}
