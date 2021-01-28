package com.enigma.migrationhelper.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.enigma.migrationhelper.db.dao.UserDao
import com.enigma.migrationhelper.db.entities.User
import java.util.concurrent.Executors

@Database(entities = [User::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun getUsers() = arrayOf(
            User(1, "Noman", "Trovar", "first.jpg", "123123131"),
            User(2, "Aayan", "Maskio", "second.jpg", "431313131"),
            User(3, "Tariqul", "Hanos", "third.jpg", "456321211")
        )

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "network")
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        //pre-populate data
                        Executors.newSingleThreadExecutor().execute {
                            instance?.userDao()?.insertAll(*getUsers())
                        }
                    }
                })
                .addMigrations(MIGRATION_1_2)
                .build()
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.alterTable("User", emptySet(), setOf(ColumnData("phone_number", "TEXT")))
            }
        }
    }
}