package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "event_logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // e.g., "SIM_CHANGE", "SMS_CMD", "ALARM", "LOC", "USB", "SHUTDOWN", "POWER", "BT_GUARDIAN"
    val description: String, // Farsi description of the event
    val location: String? = null, // GPS coordinates or fallback info
    val details: String? = null // additional technical details
)
