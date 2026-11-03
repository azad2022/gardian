package com.example.data.repository

import com.example.data.database.LogDao
import com.example.data.database.LogEntry
import kotlinx.coroutines.flow.Flow

class LogRepository(private val logDao: LogDao) {
    val allLogs: Flow<List<LogEntry>> = logDao.getAllLogs()

    fun getLogsByType(type: String): Flow<List<LogEntry>> {
        return logDao.getLogsByType(type)
    }

    suspend fun insertLog(entry: LogEntry) {
        logDao.insertLog(entry)
    }

    suspend fun clearLogs() {
        logDao.clearLogs()
    }
}
