package ru.pacani.fitnessdiary.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Пользователь приложения. Авторизация локальная (см. context.md, раздел 8).
 *
 * [email] хранится в нижнем регистре и без пробелов по краям.
 * [goal] — имя значения enum `Goal`: STRENGTH, MASS, WEIGHT_LOSS, MAINTENANCE.
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)],
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val passwordHash: String,
    val salt: String,
    val heightCm: Int? = null,
    val weightKg: Double? = null,
    val goal: String,
    val createdAt: Long,
)
