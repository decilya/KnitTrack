package com.knittrac.app.data.repository

import com.google.gson.Gson
import com.knittrac.app.core.common.TimeProvider
import com.knittrac.app.data.local.AppDatabase
import com.knittrac.app.data.local.ProjectDao
import com.knittrac.app.data.local.SessionDao
import javax.inject.Inject

/**
 * Parameter Object для инъекции зависимостей в [DataExporterImpl].
 * 
 * Позволяет сократить количество параметров в конструкторе (правило LongParameterList)
 * и группирует зависимости, необходимые для атомарного экспорта данных.
 */
class DataExportDependencies @Inject constructor(
    val projectDao: ProjectDao,
    val sessionDao: SessionDao,
    val gson: Gson,
    val appDatabase: AppDatabase,
    val timeProvider: TimeProvider
)
