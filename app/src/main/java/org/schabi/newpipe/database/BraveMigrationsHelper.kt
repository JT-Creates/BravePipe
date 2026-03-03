package org.schabi.newpipe.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * BravePipeLegacy cannot upgrade to room 2.7.2 and has to use 2.6.1 for SDK 19 compatibility.
 *
 * commit 0747b3a0a59857fcce69eade2266d9c83271c58e
 * ('Use "factory" method for creating db migrations')
 * uses the factory method that 2.7.2 provides but is missing on version 2.6.1.
 *
 * This object replicate the factory method that we use instead
 */
object BraveMigrationsHelper {

    fun Migration(
        from: Int,
        to: Int,
        block: (SupportSQLiteDatabase) -> Unit
    ): Migration = object : Migration(from, to) {
        override fun migrate(db: SupportSQLiteDatabase) = block(db)
    }
}
