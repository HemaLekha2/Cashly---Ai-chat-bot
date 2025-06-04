package com.example.smartspendchatbot.model

import java.time.LocalDate

data class Expense(
    val amount: Double,
    val date: LocalDate = LocalDate.now(),
    val description: String = ""
)