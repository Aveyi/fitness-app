package ru.pacani.fitnessdiary.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Тренировка. [finishedAt] = null — тренировка идёт сейчас.
 * У пользователя не больше одной незавершённой тренировки (проверяет репозиторий).
 */
@Entity(
    tableName = "workouts",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("userId"), Index("startedAt")],
)
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val title: String,
    /** epoch millis */
    val startedAt: Long,
    /** epoch millis */
    val finishedAt: Long? = null,
    val note: String? = null,
)
