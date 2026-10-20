package ru.pacani.fitnessdiary.domain.model

import java.time.Instant

/** Цель тренировок. В БД хранится имя значения (users.goal). */
enum class Goal {
    STRENGTH,
    MASS,
    WEIGHT_LOSS,
    MAINTENANCE,
}

/** Пользователь без пароля и соли: наружу из репозитория они не уходят. */
data class User(
    val id: Long,
    val name: String,
    val email: String,
    val heightCm: Int?,
    val weightKg: Double?,
    val goal: Goal,
    val createdAt: Instant,
)
