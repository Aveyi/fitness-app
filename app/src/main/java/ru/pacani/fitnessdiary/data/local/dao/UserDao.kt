package ru.pacani.fitnessdiary.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.pacani.fitnessdiary.data.local.entity.UserEntity

/** Только простые операции; правила регистрации и входа — в AuthRepository. */
@Dao
interface UserDao {

    /** Занятый email нарушает unique-индекс: Room бросит SQLiteConstraintException. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    /** [email] уже нормализован: нижний регистр, без пробелов по краям. */
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    fun observeById(id: Long): Flow<UserEntity?>

    @Query("UPDATE users SET heightCm = :heightCm, weightKg = :weightKg, goal = :goal WHERE id = :id")
    suspend fun updateProfile(id: Long, heightCm: Int?, weightKg: Double?, goal: String)
}
