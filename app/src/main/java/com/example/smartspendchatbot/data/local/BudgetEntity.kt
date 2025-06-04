package com.example.smartspendchatbot.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget")
data class BudgetEntity(
    @PrimaryKey val id: Int = 0,
    val isMonthly: Boolean,
    val weeklyBudget: Double,
    val monthlyBudget: Double
)