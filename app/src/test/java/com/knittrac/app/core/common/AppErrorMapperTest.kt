package com.knittrac.app.core.common

import android.database.sqlite.SQLiteException
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/**
 * Юнит-тесты [AppErrorMapper].
 *
 * Robolectric нужен: [android.database.sqlite.SQLiteException] — Android-класс,
 * на android.jar-заглушке конструктор бросает Stub!.
 */
@RunWith(RobolectricTestRunner::class)
class AppErrorMapperTest {

    @Test
    fun map_ioException_returnsIoError() {
        val result = AppErrorMapper.map(IOException("boom"))
        assertTrue(result is AppError.IoError)
    }

    @Test
    fun map_illegalArgument_returnsValidationError() {
        val result = AppErrorMapper.map(IllegalArgumentException("boom"))
        assertTrue(result is AppError.ValidationError)
    }

    @Test
    fun map_sqliteException_returnsDatabaseError() {
        val result = AppErrorMapper.map(SQLiteException("boom"))
        assertTrue(result is AppError.DatabaseError)
    }

    @Test
    fun map_runtimeException_returnsUnknownError() {
        val result = AppErrorMapper.map(RuntimeException("boom"))
        assertTrue(result is AppError.UnknownError)
    }
}
