package com.example.llpclient.data.local.schema

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val passwordForRelogin: String? = null,
    val authToken: String,
    val refreshToken: String?,
    val lastLogin: Long
)