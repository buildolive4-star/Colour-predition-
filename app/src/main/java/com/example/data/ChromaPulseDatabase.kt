package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChromaPulseDao {
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun observeUserProfile(userId: String): Flow<UserProfile?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserProfile(userId: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUserProfile(profile: UserProfile): Long

    @Update
    suspend fun updateUserProfile(profile: UserProfile)

    @Query("SELECT * FROM predictions WHERE userId = :userId ORDER BY createdAtMs DESC")
    fun observePredictions(userId: String): Flow<List<PredictionRecord>>

    @Query("SELECT * FROM predictions WHERE id = :predictionId LIMIT 1")
    suspend fun getPredictionById(predictionId: String): PredictionRecord?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPrediction(prediction: PredictionRecord)

    @Update
    suspend fun updatePrediction(prediction: PredictionRecord)

    @Query("SELECT * FROM wallet_transactions WHERE userId = :userId ORDER BY createdAtMs DESC")
    fun observeWalletTransactions(userId: String): Flow<List<WalletTransaction>>

    @Query("SELECT * FROM wallet_transactions WHERE id = :txId LIMIT 1")
    suspend fun getTransactionById(txId: String): WalletTransaction?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWalletTransaction(tx: WalletTransaction): Long

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAtMs DESC")
    fun observeNotifications(userId: String): Flow<List<UserNotification>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNotification(notification: UserNotification): Long

    @Query("UPDATE notifications SET read = 1, updatedAtMs = :updatedAtMs WHERE id = :notificationId")
    suspend fun markNotificationRead(notificationId: String, updatedAtMs: Long)

    @Query("UPDATE notifications SET read = 1, updatedAtMs = :updatedAtMs WHERE userId = :userId AND read = 0")
    suspend fun markAllNotificationsRead(userId: String, updatedAtMs: Long)

    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun deleteNotification(notificationId: String)

    @Query("SELECT * FROM game_rounds ORDER BY endTimeMs DESC LIMIT 200")
    fun observeGameRounds(): Flow<List<GameRoundRecord>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGameRoundIfAbsent(round: GameRoundRecord): Long

    @Query("SELECT * FROM leaderboard ORDER BY totalVirtualPoints DESC, totalWins DESC, bestStreak DESC LIMIT 100")
    fun observeLeaderboard(): Flow<List<LeaderboardEntry>>

    @Query("SELECT * FROM leaderboard WHERE id = :userId LIMIT 1")
    suspend fun getLeaderboardEntry(userId: String): LeaderboardEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLeaderboardEntry(entry: LeaderboardEntry)
}

@Database(
    entities = [
        UserProfile::class,
        PredictionRecord::class,
        WalletTransaction::class,
        UserNotification::class,
        GameRoundRecord::class,
        LeaderboardEntry::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ChromaPulseDatabase : RoomDatabase() {
    abstract fun dao(): ChromaPulseDao

    companion object {
        @Volatile
        private var INSTANCE: ChromaPulseDatabase? = null

        fun getInstance(context: Context): ChromaPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChromaPulseDatabase::class.java,
                    "chromapulse_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
