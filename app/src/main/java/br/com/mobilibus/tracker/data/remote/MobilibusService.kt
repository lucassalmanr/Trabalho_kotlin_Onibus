package br.com.mobilibus.tracker.data.remote

import android.util.Log
import br.com.mobilibus.tracker.BuildConfig
import br.com.mobilibus.tracker.domain.model.Vehicle
import br.com.mobilibus.tracker.domain.model.VehicleFeed
import br.com.mobilibus.tracker.domain.repository.VehicleRepository
import com.google.transit.realtime.GtfsRealtime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import kotlin.math.roundToInt

interface MobilibusService {
    @GET("web/4ch6j/vehicle-positions")
    suspend fun vehiclePositions(@Query("accesskey") accessKey: String): ResponseBody
}

class MobilibusVehicleRepository(private val service: MobilibusService) : VehicleRepository {
    override suspend fun fetchVehicles(): VehicleFeed = withContext(Dispatchers.IO) {
        try {
            check(BuildConfig.MOBILIBUS_ACCESS_KEY.isNotBlank()) {
                "A chave da API Mobilibus não foi configurada."
            }
            val bytes = service.vehiclePositions(BuildConfig.MOBILIBUS_ACCESS_KEY).use { response ->
                Log.d(TAG, "Resposta recebida: tipo=${response.contentType()}, tamanho declarado=${response.contentLength()} bytes")
                check(response.contentLength() != 0L) { "A API retornou uma resposta vazia." }
                response.bytes().also { Log.d(TAG, "Corpo da resposta lido: ${it.size} bytes") }
            }
            val decoded = GtfsVehicleDecoder.decode(bytes)
            check(decoded.vehicleEntities == 0 || decoded.vehicles.isNotEmpty()) {
                "O feed contém ${decoded.vehicleEntities} veículos, mas nenhuma posição válida foi convertida. Consulte o Logcat."
            }
            Log.i(TAG, decoded.summary())
            VehicleFeed(decoded.vehicles, System.currentTimeMillis())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Falha ao buscar ou decodificar o feed GTFS-Realtime.", error)
            throw IllegalStateException("Não foi possível carregar as posições dos veículos. Consulte o Logcat.", error)
        }
    }

    private companion object {
        const val TAG = "MobilibusFeed"
    }
}

object RemoteGraph {
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://realtime4.mobilibus.com/")
        .build()

    val vehicleRepository: VehicleRepository = MobilibusVehicleRepository(retrofit.create(MobilibusService::class.java))
}

/** Converts the official GTFS-Realtime schema to the app's domain model. */
internal object GtfsVehicleDecoder {
    data class DecodeResult(
        val gtfsVersion: String,
        val entities: Int,
        val vehicleEntities: Int,
        val vehicles: List<Vehicle>,
        val deleted: Int,
        val withoutVehicle: Int,
        val withoutPosition: Int,
        val invalidCoordinates: Int,
        val missingId: Int
    ) {
        fun summary(): String =
            "GTFS-RT versão=$gtfsVersion, entidades=$entities, veículos=$vehicleEntities, " +
                "convertidos=${vehicles.size}, descartados: excluídos=$deleted, semVeículo=$withoutVehicle, " +
                "semPosição=$withoutPosition, coordenadasInválidas=$invalidCoordinates, semId=$missingId"
    }

    fun decode(bytes: ByteArray): DecodeResult = decode(GtfsRealtime.FeedMessage.parseFrom(bytes))

    fun decode(feed: GtfsRealtime.FeedMessage): DecodeResult {
        val vehicles = mutableListOf<Vehicle>()
        var vehicleEntities = 0
        var deleted = 0
        var withoutVehicle = 0
        var withoutPosition = 0
        var invalidCoordinates = 0
        var missingId = 0

        for (entity in feed.entityList) {
            if (entity.isDeleted) {
                deleted++
                continue
            }
            if (!entity.hasVehicle()) {
                withoutVehicle++
                continue
            }

            vehicleEntities++
            val vehiclePosition = entity.vehicle
            if (!vehiclePosition.hasPosition()) {
                withoutPosition++
                continue
            }

            val position = vehiclePosition.position
            val latitude = position.latitude.toDouble()
            val longitude = position.longitude.toDouble()
            if (!latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
                invalidCoordinates++
                continue
            }

            val descriptor = vehiclePosition.vehicle
            val id = descriptor.id.ifBlank { entity.id }.trim()
            if (id.isBlank()) {
                missingId++
                continue
            }

            vehicles += Vehicle(
                id = id,
                label = descriptor.label.ifBlank { id },
                line = vehiclePosition.trip.routeId.ifBlank { "Linha indisponível" },
                destination = "Destino não informado",
                latitude = latitude,
                longitude = longitude,
                speedKmh = position.takeIf { it.hasSpeed() }
                    ?.speed
                    ?.takeIf { it.isFinite() && it >= 0f }
                    ?.times(3.6f)
                    ?.roundToInt(),
                timestamp = vehiclePosition.takeIf { it.hasTimestamp() }?.timestamp
            )
        }

        return DecodeResult(
            gtfsVersion = feed.header.gtfsRealtimeVersion,
            entities = feed.entityCount,
            vehicleEntities = vehicleEntities,
            vehicles = vehicles.distinctBy { it.id },
            deleted = deleted,
            withoutVehicle = withoutVehicle,
            withoutPosition = withoutPosition,
            invalidCoordinates = invalidCoordinates,
            missingId = missingId
        )
    }
}
