package com.example.data.repository

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.dao.PrintJobDao
import com.example.data.model.PrintJobEntity
import com.example.escpos.EscPosElement
import com.example.escpos.EscPosParser
import com.example.escpos.ParsedReceipt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class PrintJobRepository(private val dao: PrintJobDao) {

    val allJobs: Flow<List<PrintJobEntity>> = dao.getAllJobs()
    val favoriteJobs: Flow<List<PrintJobEntity>> = dao.getFavoriteJobs()
    val jobCount: Flow<Int> = dao.getJobCount()

    // Shared flow for real-time notification of newly received prints across the app
    private val _newPrintEvent = MutableSharedFlow<PrintJobEntity>(extraBufferCapacity = 10)
    val newPrintEvent = _newPrintEvent.asSharedFlow()

    suspend fun savePrintJob(
        rawBytes: ByteArray,
        clientInfo: String,
        source: String,
        paperWidthMm: Int = 80
    ): PrintJobEntity {
        val parser = EscPosParser(if (paperWidthMm == 58) 384 else 576)
        val parsed = parser.parse(rawBytes, clientInfo)

        val hasDrawerKick = parsed.elements.any { it is EscPosElement.DrawerKick }

        val entity = PrintJobEntity(
            title = parsed.title,
            clientInfo = clientInfo,
            source = source,
            totalBytes = rawBytes.size,
            cutCount = parsed.cutCount,
            paperWidthMm = paperWidthMm,
            rawBytes = rawBytes,
            drawerKicked = hasDrawerKick,
            timestamp = System.currentTimeMillis()
        )

        val id = dao.insertJob(entity)
        val savedJob = entity.copy(id = id)
        _newPrintEvent.tryEmit(savedJob)
        return savedJob
    }

    suspend fun getJobById(id: Long): PrintJobEntity? = dao.getJobById(id)

    suspend fun toggleFavorite(job: PrintJobEntity) {
        dao.updateJob(job.copy(isFavorite = !job.isFavorite))
    }

    suspend fun deleteJob(id: Long) = dao.deleteJobById(id)

    suspend fun clearAll() = dao.clearAllJobs()

    companion object {
        @Volatile
        private var INSTANCE: PrintJobRepository? = null

        fun getInstance(context: Context): PrintJobRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                val instance = PrintJobRepository(db.printJobDao())
                INSTANCE = instance
                instance
            }
        }
    }
}
