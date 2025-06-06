package com.example.smartspendchatbot.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val date: String, // Consider storing as Long (Epoch Millis) or ensure consistent ISO format
    val description: String,
    val category: String = "Uncategorized" // Added category field
)