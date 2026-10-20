package ru.pacani.fitnessdiary.data.local

import androidx.room.withTransaction

/**
 * Выполняет блок в одной транзакции. В приложении — Room, в юнит-тестах —
 * простая реализация, которая просто вызывает блок.
 */
interface TransactionRunner {
    suspend fun <R> inTransaction(block: suspend () -> R): R
}

class RoomTransactionRunner(private val db: AppDatabase) : TransactionRunner {
    override suspend fun <R> inTransaction(block: suspend () -> R): R = db.withTransaction { block() }
}
