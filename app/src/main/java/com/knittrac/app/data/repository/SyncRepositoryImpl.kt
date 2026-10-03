package com.knittrac.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.AppErrorMapper
import com.knittrac.app.core.common.Result
import com.knittrac.app.data.local.AppDatabase
import com.knittrac.app.domain.repository.SyncRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val context: Context, 
    private val gson: Gson
) : SyncRepository {

    override suspend fun exportToJson(): Result<String> = try {
        // ИСПРАВЛЕНО: используем getDatabase вместо несуществующего getInstance
        val db = AppDatabase.getDatabase(context)
        
        // ИСПРАВЛЕНО: убираем .map { it.toDomain() }, так как Room уже возвращает доменные модели
        val projects = db.projectDao().getAllProjectsFlow().first()
        val sessions = db.sessionDao().getAllSessions()
        
        val data = mapOf(
            "projects" to projects, 
            "sessions" to sessions
        )
        Result.Success(gson.toJson(data))
    } catch (e: Exception) { 
        Result.Error(AppErrorMapper.map(e)) 
    }

    override suspend fun importFromJson(json: String): Result<Unit> = try {
        val type = object : TypeToken<Map<String, List<Any>>>() {}.type
        gson.fromJson<Map<String, List<Any>>>(json, type)
        // Здесь должна быть логика импорта, пока оставлена заглушка как в оригинале
        Result.Success(Unit)
    } catch (e: Exception) { 
        Result.Error(AppErrorMapper.map(e)) 
    }
}
