package com.example.llpclient.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.llpclient.data.local.schema.UserDao
import com.example.llpclient.data.local.schema.UserEntity

@Database(entities = [UserEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}