package pl.grzybtrop.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null,
    val distanceMeters: Double = 0.0,
    /** true, gdy użytkownik podał wagę (albo zaznaczył „nic nie znalazłem”) */
    val findsReported: Boolean = false,
)

@Entity(
    tableName = "track_points",
    foreignKeys = [ForeignKey(
        entity = TripEntity::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("tripId")],
)
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val latitude: Double,
    val longitude: Double,
    val timeMillis: Long,
)

@Entity(
    tableName = "finds",
    foreignKeys = [ForeignKey(
        entity = TripEntity::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("tripId")],
)
data class FindEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val species: String,
    val weightGrams: Int,
)