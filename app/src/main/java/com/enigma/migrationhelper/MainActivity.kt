package com.enigma.migrationhelper

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.enigma.migrationhelper.db.AppDatabase

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Thread {
            AppDatabase.getInstance(this).userDao().getAll().forEach {
                Log.d("zzzzzzz", "onCreate: $it")
            }
        }.start()
    }
}