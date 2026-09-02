package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PrintJobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrintJobDao {

    @Query("SELECT * FROM print_jobs ORDER BY timestamp DESC")
    fun getAllJobs(): Flow<List<PrintJobEntity>>

    @Query("SELECT * FROM print_jobs WHERE id = :id LIMIT 1")
    suspend fun getJobById(id: Long): PrintJobEntity?

    @Query("SELECT * FROM print_jobs WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteJobs(): Flow<List<PrintJobEntity>>

    @Query("SELECT * FROM print_jobs WHERE source LIKE '%' || :source || '%' ORDER BY timestamp DESC")
    fun getJobsBySource(source: String): Flow<List<PrintJobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: PrintJobEntity): Long

    @Update
    suspend fun updateJob(job: PrintJobEntity)

    @Query("DELETE FROM print_jobs WHERE id = :id")
    suspend fun deleteJobById(id: Long)

    @Query("DELETE FROM print_jobs")
    suspend fun clearAllJobs()

    @Query("SELECT COUNT(*) FROM print_jobs")
    fun getJobCount(): Flow<Int>
}
