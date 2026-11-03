package br.com.mobilibus.tracker

import android.content.Context
import androidx.room.Room
import br.com.mobilibus.tracker.data.local.AppDatabase
import br.com.mobilibus.tracker.data.local.SessionStore
import br.com.mobilibus.tracker.data.remote.RemoteGraph
import br.com.mobilibus.tracker.data.repository.RoomAuthRepository
import br.com.mobilibus.tracker.data.repository.RoomFavoriteRepository
import br.com.mobilibus.tracker.domain.repository.AuthRepository
import br.com.mobilibus.tracker.domain.repository.FavoriteRepository
import br.com.mobilibus.tracker.domain.repository.VehicleRepository

class AppGraph(context: Context) {
    private val database = Room.databaseBuilder(context, AppDatabase::class.java, "mobilibus.db").build()
    val session = SessionStore(context)
    val authRepository: AuthRepository = RoomAuthRepository(database.userDao(), session)
    val favoriteRepository: FavoriteRepository = RoomFavoriteRepository(database.favoriteDao())
    val vehicleRepository: VehicleRepository = RemoteGraph.vehicleRepository
}
