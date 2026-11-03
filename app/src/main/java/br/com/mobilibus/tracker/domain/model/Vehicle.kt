package br.com.mobilibus.tracker.domain.model

data class Vehicle(
    val id: String,
    val label: String,
    val line: String,
    val destination: String,
    val latitude: Double?,
    val longitude: Double?,
    val speedKmh: Int?,
    val timestamp: Long?
)

data class VehicleFeed(val vehicles: List<Vehicle>, val fetchedAt: Long)

data class FavoriteVehicle(
    val vehicleId: String,
    val label: String,
    val line: String,
    val destination: String,
    val latitude: Double?,
    val longitude: Double?
)
