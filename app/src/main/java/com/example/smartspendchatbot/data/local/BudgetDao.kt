package com.example.smartspendchatbot.data.local

import androidx.room.*

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budget LIMIT 1")
    suspend fun getBudget(): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)
}