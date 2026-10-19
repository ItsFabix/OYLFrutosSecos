package com.example.oylfrutossecos.data.repository

import com.example.oylfrutossecos.data.local.OylDatabase
import com.example.oylfrutossecos.data.model.AppNotice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationRepository(private val database: OylDatabase) {
    suspend fun list(userId: Long): List<AppNotice> = withContext(Dispatchers.IO) {
        database.readNotices(userId)
    }
    suspend fun markRead(userId: Long, id: Long) = withContext(Dispatchers.IO) {
        database.markNoticeRead(userId, id)
    }
    suspend fun markAllRead(userId: Long) = withContext(Dispatchers.IO) {
        database.markAllNoticesRead(userId)
    }
    suspend fun createTest(userId: Long): AppNotice = withContext(Dispatchers.IO) {
        database.addTestNotice(userId)
    }
}
