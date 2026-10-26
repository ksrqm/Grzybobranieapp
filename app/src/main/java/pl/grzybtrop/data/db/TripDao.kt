package pl.grzybtrop.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Jeden wiersz na liście „Ostatnie spacery”. */
data class TripListItem(
    val id: Long,
    val name: String,
    val startedAtMillis: Long,
    val distanceMeters: Double,
    val findsReported: Boolean,
    val totalWeightGrams: Int,
)

@Dao
interface TripDao {
    @Insert suspend fun insertTrip(trip: TripEntity): Long
    @Update suspend fun updateTrip(trip: TripEntity)
    @Insert suspend fun insertPoint(point: TrackPointEntity)
    @Insert suspend fun insertFinds(finds: List<FindEntity>)

    @Query("SELECT COUNT(*) FROM trips")
    suspend fun countTrips(): Int

    @Query(
        """
        SELECT t.id, t.name, t.startedAtMillis, t.distanceMeters, t.findsReported,
               COALESCE(SUM(f.weightGrams), 0) AS totalWeightGrams
        FROM trips t
        LEFT JOIN finds f ON f.tripId = t.id
        WHERE t.endedAtMillis IS NOT NULL
        GROUP BY t.id
        ORDER BY t.startedAtMillis DESC
        LIMIT :limit
        """
    )
    fun observeRecentTrips(limit: Int): Flow<List<TripListItem>>
}