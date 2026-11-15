package com.learnlock.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM child_profile WHERE id = 1")
    fun observeProfile(): Flow<ChildProfileEntity?>

    @Query("SELECT * FROM child_profile WHERE id = 1")
    suspend fun getProfile(): ChildProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ChildProfileEntity)
}

@Dao
interface ParentAuthDao {
    @Query("SELECT * FROM parent_auth WHERE id = 1")
    suspend fun get(): ParentAuthEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(auth: ParentAuthEntity)
}

@Dao
interface StatsDao {
    @Insert
    suspend fun insert(attempt: TaskAttemptEntity)

    @Query(
        """
        SELECT * FROM task_attempt
        WHERE timestampMillis >= :fromMillis
        ORDER BY timestampMillis DESC
        """
    )
    fun observeSince(fromMillis: Long): Flow<List<TaskAttemptEntity>>

    @Query(
        """
        SELECT AVG(CASE WHEN correct = 1 THEN 1.0 ELSE 0.0 END) FROM task_attempt
        WHERE timestampMillis >= :fromMillis AND usedParentUnlock = 0
        """
    )
    suspend fun accuracySince(fromMillis: Long): Double?

    @Query(
        """
        SELECT * FROM task_attempt
        WHERE usedParentUnlock = 0
        ORDER BY timestampMillis DESC
        LIMIT :limit
        """
    )
    suspend fun recentLearningAttempts(limit: Int): List<TaskAttemptEntity>
}
