package com.example.a221060_amos_mrnelson_project2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_tasks")
data class TaskEntity(
    @PrimaryKey val taskId: String,
    val taskName: String,
    val description: String,
    val startTime: String, // Changed to String to match Task model
    val endTime: String,   // Changed to String to match Task model
    val priority: String,
    val reminder: String,  // Changed name to match Task model
    val locationAddress: String,
    val isCompleted: Boolean = false,
    val taskDifficulty: Float, // Changed to Float to match Task model
    val isSynced: Boolean = false // New field for offline sync
)

@Entity(tableName = "daily_burnout_metrics")
data class DailyMetricsEntity(
    @PrimaryKey val dateIsoString: String, // "YYYY-MM-DD"
    val burnoutScore: Float,
    val screenTimeMillis: Long,
    val workDurationMillis: Long,
    val restDurationMillis: Long,
    val completionRate: Float,
    val weightedCompletionRate: Float
)

fun TaskEntity.toTask() = com.example.a221060_amos_mrnelson_project2.data.Task(
    id = taskId,
    title = taskName,
    description = description,
    location = locationAddress,
    startTime = startTime,
    endTime = endTime,
    reminder = reminder,
    priority = priority,
    isCompleted = isCompleted,
    estimatedHours = taskDifficulty
)

fun com.example.a221060_amos_mrnelson_project2.data.Task.toEntity(isSynced: Boolean = false) = TaskEntity(
    taskId = id,
    taskName = title,
    description = description,
    locationAddress = location,
    startTime = startTime,
    endTime = endTime,
    reminder = reminder,
    priority = priority,
    isCompleted = isCompleted,
    taskDifficulty = estimatedHours,
    isSynced = isSynced
)

@Dao
interface AppDao {
    @Query("SELECT * FROM user_tasks ORDER BY startTime ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM user_tasks WHERE isSynced = 0")
    suspend fun getUnsyncedTasks(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM user_tasks WHERE taskId = :id")
    suspend fun deleteTask(id: String)

    @Query("SELECT * FROM daily_burnout_metrics ORDER BY dateIsoString DESC")
    fun getDailyMetrics(): Flow<List<DailyMetricsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyMetrics(metrics: DailyMetricsEntity)

    @Query("DELETE FROM daily_burnout_metrics WHERE dateIsoString < :cutoffDate")
    suspend fun pruneOldMetrics(cutoffDate: String)
}
