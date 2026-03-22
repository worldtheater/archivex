package com.worldtheater.archive.data.local

import androidx.room.Database
import androidx.room.ConstructedBy
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.worldtheater.archive.util.log.L

@ConstructedBy(AppDataBaseConstructor::class)
@Database(entities = [Note::class], version = 1, exportSchema = false)
abstract class AppDataBase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {

        private const val TAG = "AppDataBase"

        lateinit var db: AppDataBase
        private var initialized: Boolean = false

        fun init(builder: Builder<AppDataBase>) {
            L.d(TAG, "init >>>")
            if (!initialized) {
                db = builder
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                initialized = true
            }
            L.d(TAG, "init <<<")
        }

    }
}

@Suppress("KotlinNoActualForExpect")
expect object AppDataBaseConstructor : RoomDatabaseConstructor<AppDataBase> {
    override fun initialize(): AppDataBase
}
