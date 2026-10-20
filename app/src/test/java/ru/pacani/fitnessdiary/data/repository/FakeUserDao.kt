package ru.pacani.fitnessdiary.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.pacani.fitnessdiary.data.local.dao.UserDao
import ru.pacani.fitnessdiary.data.local.entity.UserEntity

/** UserDao в памяти для юнит-тестов. */
class FakeUserDao : UserDao {

    val users = MutableStateFlow<List<UserEntity>>(emptyList())
    private var nextId = 1L

    override suspend fun insert(user: UserEntity): Long {
        check(users.value.none { it.email == user.email }) { "UNIQUE constraint failed: users.email" }
        val id = nextId++
        users.update { it + user.copy(id = id) }
        return id
    }

    override suspend fun findByEmail(email: String): UserEntity? = users.value.firstOrNull { it.email == email }

    override suspend fun getById(id: Long): UserEntity? = users.value.firstOrNull { it.id == id }

    override fun observeById(id: Long): Flow<UserEntity?> = users.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun updateProfile(id: Long, heightCm: Int?, weightKg: Double?, goal: String) {
        users.update { list ->
            list.map { if (it.id == id) it.copy(heightCm = heightCm, weightKg = weightKg, goal = goal) else it }
        }
    }
}
