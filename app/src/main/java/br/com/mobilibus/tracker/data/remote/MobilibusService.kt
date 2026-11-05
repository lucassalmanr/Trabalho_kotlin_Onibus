package br.com.mobilibus.tracker.data.remote

import br.com.mobilibus.tracker.domain.model.Vehicle
import br.com.mobilibus.tracker.domain.model.VehicleFeed
import br.com.mobilibus.tracker.domain.repository.VehicleRepository
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

interface MobilibusService {
    @GET("web/4ch6j/vehicle-positions")
    suspend fun vehiclePositions(@Query("accesskey") accessKey: String = ACCESS_KEY): ResponseBody

    private companion object {
        const val ACCESS_KEY = "982a57efd77a9462bf1665696fb25984"
    }
}

class MobilibusVehicleRepository(private val service: MobilibusService) : VehicleRepository {
    override suspend fun fetchVehicles(): VehicleFeed {
        val bytes = service.vehiclePositions().use { response ->
            check(response.contentLength() != 0L) { "A API retornou uma resposta vazia." }
            response.bytes()
        }
        return VehicleFeed(ProtoVehicleDecoder.decode(bytes), System.currentTimeMillis())
    }
}

object RemoteGraph {
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://realtime4.mobilibus.com/")
        .build()

    val vehicleRepository: VehicleRepository = MobilibusVehicleRepository(retrofit.create(MobilibusService::class.java))
}

/**
 * The endpoint replies with application/x-protobuf, not JSON. This small, defensive
 * reader supports the protobuf field types used by the public position feed.
 */
object ProtoVehicleDecoder {
    fun decode(bytes: ByteArray): List<Vehicle> = ProtoReader(bytes).fields
        // Field 1 is feed metadata; each actual vehicle is a repeated field 2 message.
        .filter { it.number == 2 && it.bytes != null }
        .mapNotNull { runCatching { decodeEntry(it.bytes!!) }.getOrNull() }
        .distinctBy { it.id }

    private fun decodeEntry(bytes: ByteArray): Vehicle? {
        // CORREÇÃO: entry agora mantém a instância de ProtoReader para usar as funções customizadas
        val entry = ProtoReader(bytes)
        val id = entry.string(1).orEmpty()
        val payload = entry.message(2) ?: return null
        val label = payload.string(1).orEmpty().ifBlank { id }
        if (label.isBlank()) return null

        val line = payload.string(5).orEmpty().ifBlank {
            label.trim().substringBefore(' ').ifBlank { "Linha indisponível" }
        }
        val destinationMessage = payload.message(8)
        val destination = destinationMessage?.printableStrings()?.lastOrNull()
            ?: "Destino não informado"
        val position = payload.messages(2)
            .firstOrNull { it.fields.any { field -> field.fixed32 != null } }
        val floats = position?.fields?.mapNotNull { it.floatValue } ?: emptyList()

        return Vehicle(
            id = id.ifBlank { "$line-$label" },
            label = label,
            line = line,
            destination = destination,
            latitude = floats.getOrNull(0)?.toDouble()?.takeIf { it in -90.0..90.0 },
            longitude = floats.getOrNull(1)?.toDouble()?.takeIf { it in -180.0..180.0 },
            speedKmh = floats.getOrNull(2)?.toInt()?.takeIf { it >= 0 },
            timestamp = position?.fields?.firstOrNull { it.number == 5 }?.varint
        )
    }

    private class ProtoReader(private val source: ByteArray) {
        val fields: List<Field> = buildList {
            var offset = 0
            while (offset < source.size) {
                val (tag, afterTag) = readVarint(source, offset) ?: break
                offset = afterTag
                val number = (tag shr 3).toInt()
                when ((tag and 7).toInt()) {
                    0 -> {
                        val value = readVarint(source, offset) ?: break
                        add(Field(number, varint = value.first)); offset = value.second
                    }
                    1 -> { if (offset + 8 > source.size) break; offset += 8 }
                    2 -> {
                        val length = readVarint(source, offset) ?: break
                        offset = length.second
                        val end = (offset + length.first.toInt()).coerceAtMost(source.size)
                        add(Field(number, bytes = source.copyOfRange(offset, end))); offset = end
                    }
                    5 -> {
                        if (offset + 4 > source.size) break
                        val value = (source[offset].toInt() and 0xff) or
                                ((source[offset + 1].toInt() and 0xff) shl 8) or
                                ((source[offset + 2].toInt() and 0xff) shl 16) or
                                ((source[offset + 3].toInt() and 0xff) shl 24)
                        add(Field(number, fixed32 = value)); offset += 4
                    }
                    else -> break
                }
            }
        }

        fun string(number: Int): String? = fields.firstOrNull { it.number == number }?.bytes
            ?.takeIf(::isPrintableText)?.toString(Charsets.UTF_8)

        fun message(number: Int): ProtoReader? = fields.firstOrNull { it.number == number }?.bytes
            ?.takeUnless(::isPrintableText)?.let(::ProtoReader)

        fun messages(number: Int): List<ProtoReader> = fields.filter { it.number == number }
            .mapNotNull { it.bytes?.takeUnless(::isPrintableText)?.let(::ProtoReader) }

        fun printableStrings(): List<String> = fields.mapNotNull { field ->
            field.bytes?.takeIf(::isPrintableText)?.toString(Charsets.UTF_8)
        }
    }

    private data class Field(
        val number: Int,
        val varint: Long? = null,
        val bytes: ByteArray? = null,
        val fixed32: Int? = null
    ) {
        val floatValue: Float? get() = fixed32?.let(Float::fromBits)
    }

    private fun isPrintableText(bytes: ByteArray): Boolean = bytes.isNotEmpty() && bytes.all {
        val value = it.toInt() and 0xff
        value == 9 || value == 10 || value == 13 || value >= 32
    }

    private fun readVarint(bytes: ByteArray, start: Int): Pair<Long, Int>? {
        var result = 0L
        var shift = 0
        var offset = start
        while (offset < bytes.size && shift < 64) {
            val next = bytes[offset++].toInt() and 0xff
            result = result or ((next and 0x7f).toLong() shl shift)
            if (next and 0x80 == 0) return result to offset
            shift += 7
        }
        return null
    }
}
