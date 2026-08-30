package com.knittrac.app.data.repository
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.data.local.AppDatabase
import com.knittrac.app.data.mapper.ProjectMapper.toDomain
import com.knittrac.app.data.mapper.SessionMapper.toDomain
import com.knittrac.app.domain.repository.SyncRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class SyncRepositoryImpl @Inject constructor(private val context: Context, private val gson: Gson) : SyncRepository {
    override suspend fun exportToJson(): Result<String> = try {
        val db = AppDatabase.getInstance(context)
        val data = mapOf("projects" to db.projectDao().getAllProjects().first().map { it.toDomain() }, "sessions" to db.sessionDao().getAllSessions().map { it.toDomain() })
        Result.Success(gson.toJson(data))
    } catch (e: Exception) { Result.Error(AppError.DatabaseError(e)) }
    override suspend fun importFromJson(json: String): Result<Unit> = try {
        val type = object : TypeToken<Map<String, List<Any>>>() {}.type
        gson.fromJson<Map<String, List<Any>>>(json, type)
        Result.Success(Unit)
    } catch (e: Exception) { Result.Error(AppError.DatabaseError(e)) }
}
