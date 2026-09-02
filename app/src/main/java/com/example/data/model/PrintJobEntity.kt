package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted ESC/POS Print Job record.
 */
@Entity(tableName = "print_jobs")
data class PrintJobEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val clientInfo: String,
    val source: String, // "TCP (9100)", "HTTP (9101)", "USB (ADB)", "Virtual Terminal", "Demo"
    val totalBytes: Int,
    val cutCount: Int,
    val paperWidthMm: Int = 80, // 80 or 58
    val rawBytes: ByteArray,
    val isFavorite: Boolean = false,
    val drawerKicked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PrintJobEntity
        return id == other.id && rawBytes.contentEquals(other.rawBytes)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + rawBytes.contentHashCode()
        return result
    }
}
