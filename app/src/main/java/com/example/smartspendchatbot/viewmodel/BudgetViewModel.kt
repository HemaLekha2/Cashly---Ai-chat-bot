package com.example.smartspendchatbot.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartspendchatbot.ai.OpenAIHelper
import com.example.smartspendchatbot.data.local.BudgetEntity
import com.example.smartspendchatbot.data.local.ExpenseEntity
import com.example.smartspendchatbot.data.repository.BudgetRepository
import com.example.smartspendchatbot.model.Expense
import com.example.smartspendchatbot.model.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val repository: BudgetRepository,
    private val aiHelper: OpenAIHelper
) : ViewModel() {

    var isMonthly = mutableStateOf(true)
    var weeklyBudget = mutableDoubleStateOf(0.0)
    var monthlyBudget = mutableDoubleStateOf(0.0)
    private var expenses = mutableStateListOf<Expense>() // <-- make this public if your UI needs it
    var chatMessages = mutableStateListOf<Message>()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            repository.getBudget()?.let {
                isMonthly.value = it.isMonthly
                weeklyBudget.doubleValue = it.weeklyBudget
                monthlyBudget.doubleValue = it.monthlyBudget
            }
            expenses.clear()
            expenses.addAll(repository.getAllExpenses().map { entity ->
                // entity.date is String, Expense expects LocalDate
                Expense(entity.amount, LocalDate.parse(entity.date), entity.description)
            })
        }
    }

    fun setBudget(amount: Double) {
        if (isMonthly.value) monthlyBudget.doubleValue = amount else weeklyBudget.doubleValue = amount
        viewModelScope.launch {
            repository.setBudget(
                BudgetEntity(
                    isMonthly = isMonthly.value,
                    weeklyBudget = weeklyBudget.doubleValue,
                    monthlyBudget = monthlyBudget.doubleValue
                )
            )
        }
    }

    fun addExpense(amount: Double, description: String = "") {
        val exp = Expense(amount, LocalDate.now(), description)
        expenses.add(exp)
        viewModelScope.launch {
            repository.addExpense(
                ExpenseEntity(
                    amount = amount,
                    date = exp.date.toString(), // LocalDate to String for Room
                    description = description
                )
            )
        }
    }

    fun getTotalSpent(): Double = expenses.sumOf { it.amount }
    fun getRemaining(): Double {
        val totalBudget = if (isMonthly.value) monthlyBudget.doubleValue else weeklyBudget.doubleValue
        return (totalBudget - getTotalSpent()).coerceAtLeast(0.0)
    }
    fun getDailyBudget(): Double {
        val totalBudget = if (isMonthly.value) monthlyBudget.doubleValue else weeklyBudget.doubleValue
        val today = LocalDate.now()
        val daysLeft = if (isMonthly.value) (today.lengthOfMonth() - today.dayOfMonth + 1)
        else (7 - today.dayOfWeek.value + 1)
        return (getRemaining() / daysLeft.coerceAtLeast(1)).coerceAtLeast(0.0)
    }

    fun sendUserMessage(text: String) {
        chatMessages.add(Message(text, true))
        viewModelScope.launch {
            val aiReply = aiHelper.getChatbotReply(text)
            chatMessages.add(Message(aiReply, false))
        }
    }
}