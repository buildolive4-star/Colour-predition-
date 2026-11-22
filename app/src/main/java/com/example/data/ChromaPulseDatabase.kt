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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface ChromaPulseDao {
    // Players
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun observeUserProfile(userId: String): Flow<UserProfile?>

    @Query("SELECT * FROM users ORDER BY createdAtMs DESC")
    fun observeAllUsers(): Flow<List<UserProfile>>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserProfile(userId: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUserProfile(profile: UserProfile): Long

    @Update
    suspend fun updateUserProfile(profile: UserProfile)

    // Color Predictions
    @Query("SELECT * FROM predictions WHERE userId = :userId ORDER BY createdAtMs DESC")
    fun observePredictions(userId: String): Flow<List<PredictionRecord>>

    @Query("SELECT * FROM predictions ORDER BY createdAtMs DESC LIMIT 200")
    fun observeAllPredictions(): Flow<List<PredictionRecord>>

    @Query("SELECT * FROM predictions WHERE id = :predictionId LIMIT 1")
    suspend fun getPredictionById(predictionId: String): PredictionRecord?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPrediction(prediction: PredictionRecord)

    @Update
    suspend fun updatePrediction(prediction: PredictionRecord)

    // Multi-Game Arcade Plays
    @Query("SELECT * FROM arcade_plays WHERE userId = :userId ORDER BY createdAtMs DESC LIMIT 200")
    fun observeArcadePlays(userId: String): Flow<List<ArcadeGameRecord>>

    @Query("SELECT * FROM arcade_plays ORDER BY createdAtMs DESC LIMIT 200")
    fun observeAllArcadePlays(): Flow<List<ArcadeGameRecord>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertArcadePlay(play: ArcadeGameRecord)

    // Wallet Ledger
    @Query("SELECT * FROM wallet_transactions WHERE userId = :userId ORDER BY createdAtMs DESC")
    fun observeWalletTransactions(userId: String): Flow<List<WalletTransaction>>

    @Query("SELECT * FROM wallet_transactions ORDER BY createdAtMs DESC LIMIT 250")
    fun observeAllWalletTransactions(): Flow<List<WalletTransaction>>

    @Query("SELECT * FROM wallet_transactions WHERE id = :txId LIMIT 1")
    suspend fun getTransactionById(txId: String): WalletTransaction?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWalletTransaction(tx: WalletTransaction): Long

    // Notifications
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

    // Game Rounds
    @Query("SELECT * FROM game_rounds ORDER BY endTimeMs DESC LIMIT 200")
    fun observeGameRounds(): Flow<List<GameRoundRecord>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGameRoundIfAbsent(round: GameRoundRecord): Long

    // Leaderboard
    @Query("SELECT * FROM leaderboard ORDER BY totalVirtualPoints DESC, totalWins DESC, bestStreak DESC LIMIT 100")
    fun observeLeaderboard(): Flow<List<LeaderboardEntry>>

    @Query("SELECT * FROM leaderboard WHERE id = :userId LIMIT 1")
    suspend fun getLeaderboardEntry(userId: String): LeaderboardEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLeaderboardEntry(entry: LeaderboardEntry)

    // Admin Accounts (Super Admin + Co-Admins)
    @Query("SELECT * FROM admins ORDER BY CASE WHEN role = 'SUPER_ADMIN' THEN 0 ELSE 1 END, createdAtMs DESC")
    fun observeAdmins(): Flow<List<AdminAccount>>

    @Query("SELECT * FROM admins WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getAdminByEmail(email: String): AdminAccount?

    @Query("SELECT * FROM admins WHERE id = :adminId LIMIT 1")
    suspend fun getAdminById(adminId: String): AdminAccount?

    @Query("SELECT COUNT(*) FROM admins WHERE role = 'SUPER_ADMIN'")
    suspend fun countSuperAdmins(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAdmin(admin: AdminAccount)

    @Update
    suspend fun updateAdmin(admin: AdminAccount)

    @Query("DELETE FROM admins WHERE id = :adminId AND role != 'SUPER_ADMIN'")
    suspend fun deleteCoAdmin(adminId: String)

    // Platform Config
    @Query("SELECT * FROM platform_config WHERE id = 'default' LIMIT 1")
    fun observePlatformConfig(): Flow<PlatformConfig?>

    @Query("SELECT * FROM platform_config WHERE id = 'default' LIMIT 1")
    suspend fun getPlatformConfig(): PlatformConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlatformConfig(config: PlatformConfig)
}

@Database(
    entities = [
        UserProfile::class,
        PredictionRecord::class,
        WalletTransaction::class,
        UserNotification::class,
        GameRoundRecord::class,
        LeaderboardEntry::class,
        AdminAccount::class,
        PlatformConfig::class,
        ArcadeGameRecord::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ChromaPulseDatabase : RoomDatabase() {
    abstract fun dao(): ChromaPulseDao

    companion object {
        @Volatile
        private var INSTANCE: ChromaPulseDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `admins` (
                        `id` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `passwordHash` TEXT NOT NULL,
                        `passwordSalt` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `canManagePlayers` INTEGER NOT NULL,
                        `canManageGames` INTEGER NOT NULL,
                        `canSendBroadcasts` INTEGER NOT NULL,
                        `createdByEmail` TEXT NOT NULL,
                        `createdAtMs` INTEGER NOT NULL,
                        `updatedAtMs` INTEGER NOT NULL,
                        `lastLoginAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_admins_email` ON `admins` (`email`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `platform_config` (
                        `id` TEXT NOT NULL,
                        `maintenanceMode` INTEGER NOT NULL,
                        `colorGameEnabled` INTEGER NOT NULL,
                        `crashGameEnabled` INTEGER NOT NULL,
                        `minesGameEnabled` INTEGER NOT NULL,
                        `diceGameEnabled` INTEGER NOT NULL,
                        `wheelGameEnabled` INTEGER NOT NULL,
                        `dragonTigerEnabled` INTEGER NOT NULL,
                        `dailyBonusCoins` INTEGER NOT NULL,
                        `welcomeBonusCoins` INTEGER NOT NULL,
                        `announcementBanner` TEXT NOT NULL,
                        `updatedByEmail` TEXT NOT NULL,
                        `updatedAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `arcade_plays` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `gameId` TEXT NOT NULL,
                        `gameTitle` TEXT NOT NULL,
                        `stakeCoins` INTEGER NOT NULL,
                        `multiplier` REAL NOT NULL,
                        `rewardCoins` INTEGER NOT NULL,
                        `outcomeSummary` TEXT NOT NULL,
                        `didWin` INTEGER NOT NULL,
                        `createdAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): ChromaPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChromaPulseDatabase::class.java,
                    "chromapulse_local.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
