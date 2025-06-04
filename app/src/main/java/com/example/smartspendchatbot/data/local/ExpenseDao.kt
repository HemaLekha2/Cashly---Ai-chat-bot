package com.example.smartspendchatbot.data.local


import androidx.room.*

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expense ORDER BY date DESC")
    suspend fun getAllExpenses(): List<ExpenseEntity>

    @Insert
    suspend fun insertExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expense")
    suspend fun clearExpenses()
}