package br.com.mobilibus.tracker.domain.repository

import br.com.mobilibus.tracker.domain.model.FavoriteVehicle
import br.com.mobilibus.tracker.domain.model.Vehicle
import br.com.mobilibus.tracker.domain.model.VehicleFeed
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun register(email: String, password: String): Result<Unit>
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun logout()
}

interface VehicleRepository {
    suspend fun fetchVehicles(): VehicleFeed
}

interface FavoriteRepository {
    fun observe(userId: Long): Flow<List<FavoriteVehicle>>
    fun isFavorite(userId: Long, vehicleId: String): Flow<Boolean>
    suspend fun toggle(userId: Long, vehicle: Vehicle, currentlyFavorite: Boolean)
}
