package com.aaa.scouting.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(

    @PrimaryKey
    val id: Int,

    val username: String,

    val fullName: String,

    val email: String?,

    val passwordHash: String?,

    val role: String,

    val active: Boolean,

    val lastLoginAt: String?,

    val createdAt: String?,

    val updatedAt: String?
)