package com.example.smartspendchatbot.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BudgetEntity::class, ExpenseEntity::class],
    version = 2
)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun budgetDao(): BudgetDao
    abstract fun expenseDao(): ExpenseDao
}

