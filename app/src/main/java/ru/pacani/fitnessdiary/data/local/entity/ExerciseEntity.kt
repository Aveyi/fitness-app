package ru.pacani.fitnessdiary.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Справочник упражнений: упражнения из wger (общие, [userId] = null)
 * и собственные упражнения пользователя ([isCustom] = true).
 */
@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["remoteId"], unique = true),
        Index("userId"),
        Index("category"),
    ],
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** id в wger; null у собственных упражнений. */
    val remoteId: Int? = null,
    val name: String,
    /** Описание техники, HTML уже очищен. */
    val description: String? = null,
    val category: String,
    /** Оборудование через запятую. */
    val equipment: String? = null,
    val isCustom: Boolean = false,
    val userId: Long? = null,
)
