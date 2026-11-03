package br.com.mobilibus.tracker.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val passwordHash: String
)

@Entity(
    tableName = "favorite_vehicles",
    primaryKeys = ["userId", "vehicleId"],
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class FavoriteVehicleEntity(
    val userId: Long,
    val vehicleId: String,
    val label: String,
    val line: String,
    val destination: String,
    val latitude: Double?,
    val longitude: Double?,
    val savedAt: Long = System.currentTimeMillis()
)

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_vehicles WHERE userId = :userId ORDER BY savedAt DESC")
    fun observeForUser(userId: Long): Flow<List<FavoriteVehicleEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_vehicles WHERE userId = :userId AND vehicleId = :vehicleId)")
    fun isFavorite(userId: Long, vehicleId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteVehicleEntity)

    @Query("DELETE FROM favorite_vehicles WHERE userId = :userId AND vehicleId = :vehicleId")
    suspend fun delete(userId: Long, vehicleId: String)
}

@Database(entities = [UserEntity::class, FavoriteVehicleEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun favoriteDao(): FavoriteDao
}
