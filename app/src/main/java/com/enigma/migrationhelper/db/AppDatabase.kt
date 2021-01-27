package com.enigma.migrationhelper.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.enigma.migrationhelper.db.dao.UserDao
import com.enigma.migrationhelper.db.entities.User
import java.util.concurrent.Executors

@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun getUsers() = arrayOf(
            User(1, "Noman", "Trovar", "first.jpg"),
            User(2, "Aayan", "Maskio", "second.jpg"),
            User(3, "Tariqul", "Hanos", "third.jpg")
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
                .build()
        }
    }
}