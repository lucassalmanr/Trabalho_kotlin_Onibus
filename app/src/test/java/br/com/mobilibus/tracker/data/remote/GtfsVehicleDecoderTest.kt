package br.com.mobilibus.tracker.data.remote

import com.google.protobuf.InvalidProtocolBufferException
import com.google.transit.realtime.GtfsRealtime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GtfsVehicleDecoderTest {
    @Test
    fun `converts a valid vehicle including speed and timestamp`() {
        val result = GtfsVehicleDecoder.decode(feedOf(vehicleEntity(speed = 10f, timestamp = 1_700_000_000)))

        assertEquals(1, result.vehicles.size)
        result.vehicles.single().also { vehicle ->
            assertEquals("bus-10", vehicle.id)
            assertEquals("Ônibus 10", vehicle.label)
            assertEquals("9101", vehicle.line)
            assertEquals(-19.868765, vehicle.latitude!!, 0.00001)
            assertEquals(-43.9386, vehicle.longitude!!, 0.00001)
            assertEquals(36, vehicle.speedKmh)
            assertEquals(1_700_000_000L, vehicle.timestamp)
        }
    }

    @Test
    fun `accepts optional fields missing`() {
        val result = GtfsVehicleDecoder.decode(feedOf(vehicleEntity(includeOptionalFields = false)))

        result.vehicles.single().also { vehicle ->
            assertEquals("entity-1", vehicle.id)
            assertEquals("entity-1", vehicle.label)
            assertEquals("Linha indisponível", vehicle.line)
            assertEquals("Destino não informado", vehicle.destination)
            assertNull(vehicle.speedKmh)
            assertNull(vehicle.timestamp)
        }
    }

    @Test
    fun `skips deleted entities and invalid coordinates`() {
        val deleted = GtfsRealtime.FeedEntity.newBuilder().setId("deleted").setIsDeleted(true).build()
        val invalid = vehicleEntity(latitude = 91f)

        val result = GtfsVehicleDecoder.decode(feedOf(deleted, invalid))

        assertTrue(result.vehicles.isEmpty())
        assertEquals(1, result.deleted)
        assertEquals(1, result.invalidCoordinates)
        assertEquals(1, result.vehicleEntities)
    }

    @Test
    fun `rejects malformed protobuf bytes`() {
        assertThrows(InvalidProtocolBufferException::class.java) {
            GtfsVehicleDecoder.decode(byteArrayOf(0x0A, 0x7F))
        }
    }

    private fun feedOf(vararg entities: GtfsRealtime.FeedEntity): GtfsRealtime.FeedMessage =
        GtfsRealtime.FeedMessage.newBuilder()
            .setHeader(GtfsRealtime.FeedHeader.newBuilder().setGtfsRealtimeVersion("1.0"))
            .addAllEntity(entities.asList())
            .build()

    private fun vehicleEntity(
        latitude: Float = -19.868765f,
        speed: Float? = null,
        timestamp: Long? = null,
        includeOptionalFields: Boolean = true
    ): GtfsRealtime.FeedEntity {
        val position = GtfsRealtime.Position.newBuilder()
            .setLatitude(latitude)
            .setLongitude(-43.9386f)
            .apply { speed?.let(::setSpeed) }
            .build()
        val vehicle = GtfsRealtime.VehiclePosition.newBuilder().setPosition(position)
        if (includeOptionalFields) {
            vehicle.setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder().setId("bus-10").setLabel("Ônibus 10"))
            vehicle.setTrip(GtfsRealtime.TripDescriptor.newBuilder().setRouteId("9101"))
            timestamp?.let(vehicle::setTimestamp)
        }
        return GtfsRealtime.FeedEntity.newBuilder().setId("entity-1").setVehicle(vehicle).build()
    }
}
