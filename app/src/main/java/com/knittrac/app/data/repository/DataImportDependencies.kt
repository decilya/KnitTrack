package com.knittrac.app.data.repository

import com.google.gson.Gson
import com.knittrac.app.data.local.AppDatabase
import com.knittrac.app.data.local.ProjectDao
import com.knittrac.app.data.local.SessionDao
import javax.inject.Inject

/**
 * Parameter Object для инъекции зависимостей в [DataImporterImpl].
 * 
 * Позволяет сократить количество параметров в конструкторе (правило LongParameterList)
 * и группирует зависимости, необходимые для атомарного импорта данных.
 */
class DataImportDependencies @Inject constructor(
    val database: AppDatabase,
    val projectDao: ProjectDao,
    val sessionDao: SessionDao,
    val gson: Gson
)
