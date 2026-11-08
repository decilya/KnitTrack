package com.knittrac.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Сидер системных символов: fresh install через callback и upgrade 6→7.
 */
@RunWith(RobolectricTestRunner::class)
class Migration6To7Test {

    private lateinit var context: Context
    private val dbName = "migration-6-7-test.db"
    private val codes = setOf("K", "P", "O", "K2tog", "SSK", "SL")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @Test
    fun freshInstall_hasDefaultSymbols() {
        val room = openCurrent()
        val symbols = readSymbols(room)
        assertEquals(codes, symbols.map { it.code }.toSet())
        symbols.forEach { assertNull(it.ownerPatternId) }
        val ssk = symbols.first { it.code == "SSK" }
        assertEquals(1, ssk.glyph.length)
        assertEquals("\\", ssk.glyph)
        room.close()
    }

    @Test
    fun migrate6To7_insertsDefaultSymbols() {
        createLegacyV6()
        val room = openCurrent()
        assertEquals(codes, readSymbols(room).map { it.code }.toSet())
        room.close()
    }

    @Test
    fun migrate6To7_idempotent() {
        createLegacyV6()
        val room = openCurrent()
        assertEquals(6, readSymbols(room).size)
        room.close()
    }

    /** Создаёт legacy БД v6 через [LegacySchemaHelper] (без Room onCreate). */
    private fun createLegacyV6() {
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        LegacySchemaHelper.createVersion(db, 6)
        db.close()
    }

    private fun openCurrent(): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10)
            .addCallback(AppDatabase.DATABASE_CALLBACK)
            .build()
            .also { it.openHelper.writableDatabase }

    private fun readSymbols(room: AppDatabase): List<SeedSymbol> {
        val out = mutableListOf<SeedSymbol>()
        room.query("SELECT code, owner_pattern_id, text_glyph FROM symbols", null).use { cursor ->
            val code = cursor.getColumnIndexOrThrow("code")
            val owner = cursor.getColumnIndexOrThrow("owner_pattern_id")
            val glyph = cursor.getColumnIndexOrThrow("text_glyph")
            while (cursor.moveToNext()) {
                out += SeedSymbol(
                    code = cursor.getString(code),
                    ownerPatternId = if (cursor.isNull(owner)) null else cursor.getLong(owner),
                    glyph = cursor.getString(glyph)
                )
            }
        }
        return out
    }

    private data class SeedSymbol(val code: String, val ownerPatternId: Long?, val glyph: String)
}
