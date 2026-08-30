package com.knittrac.app.data.local
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.knittrac.app.data.local.dao.ProjectDao
import com.knittrac.app.data.local.dao.SessionDao
import com.knittrac.app.data.local.entity.ProjectEntity
import com.knittrac.app.data.local.entity.SessionEntity
@Database(entities = [ProjectEntity::class, SessionEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun sessionDao(): SessionDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase = INSTANCE ?: synchronized(this) { Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "knittrac_db").build().also { INSTANCE = it } }
    }
}
