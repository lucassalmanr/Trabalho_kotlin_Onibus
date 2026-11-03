package br.com.mobilibus.tracker.data.repository

import br.com.mobilibus.tracker.data.local.FavoriteDao
import br.com.mobilibus.tracker.data.local.FavoriteVehicleEntity
import br.com.mobilibus.tracker.domain.model.FavoriteVehicle
import br.com.mobilibus.tracker.domain.model.Vehicle
import br.com.mobilibus.tracker.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.map

class RoomFavoriteRepository(private val favorites: FavoriteDao) : FavoriteRepository {
    override fun observe(userId: Long) = favorites.observeForUser(userId).map { records ->
        records.map { it.asDomain() }
    }
    override fun isFavorite(userId: Long, vehicleId: String) = favorites.isFavorite(userId, vehicleId)

    override suspend fun toggle(userId: Long, vehicle: Vehicle, currentlyFavorite: Boolean) {
        if (currentlyFavorite) favorites.delete(userId, vehicle.id)
        else favorites.insert(
            FavoriteVehicleEntity(
                userId = userId,
                vehicleId = vehicle.id,
                label = vehicle.label,
                line = vehicle.line,
                destination = vehicle.destination,
                latitude = vehicle.latitude,
                longitude = vehicle.longitude
            )
        )
    }

    private fun FavoriteVehicleEntity.asDomain() = FavoriteVehicle(vehicleId, label, line, destination, latitude, longitude)
}
