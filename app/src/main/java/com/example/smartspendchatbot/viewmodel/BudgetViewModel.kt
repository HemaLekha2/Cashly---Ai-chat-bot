package com.example.smartspendchatbot.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartspendchatbot.ai.GoogleAiHelper // Renamed from OpenAIHelper
import com.example.smartspendchatbot.data.local.BudgetEntity
import com.example.smartspendchatbot.data.local.ExpenseEntity
import com.example.smartspendchatbot.data.repository.BudgetRepository
import com.example.smartspendchatbot.model.Expense
import com.example.smartspendchatbot.model.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val repository: BudgetRepository,
    private val aiHelper: GoogleAiHelper // Use the refactored helper
) : ViewModel() {

    // Budget State
    var monthlyIncome = mutableDoubleStateOf(0.0)
    var isMonthly = mutableStateOf(true)
    var weeklyBudget = mutableDoubleStateOf(0.0)
    var monthlyBudget = mutableDoubleStateOf(0.0)

    // Expense State
    private var _expenses = mutableStateListOf<Expense>()
    val expenses: List<Expense> get() = _expenses // Public read-only view

    // Chat State
    var chatMessages = mutableStateListOf<Message>()
    var isLoadingAiResponse = mutableStateOf(false)

    init {
        loadData()
        // Add initial bot message if needed
        if (chatMessages.isEmpty()) {
            chatMessages.add(Message("Hello! How can I help you with your budget today? Ask me for a budget plan or expense analysis.", false))
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getBudget()?.let {
                monthlyIncome.doubleValue = it.monthlyIncome
                isMonthly.value = it.isMonthly
                weeklyBudget.doubleValue = it.weeklyBudget
                monthlyBudget.doubleValue = it.monthlyBudget
            }
            _expenses.clear()
            _expenses.addAll(repository.getAllExpenses().mapNotNull { entity ->
                try {
                    // Safely parse the date string using ISO_LOCAL_DATE
                    Expense(entity.amount, LocalDate.parse(entity.date, DateTimeFormatter.ISO_LOCAL_DATE), entity.description)
                } catch (e: DateTimeParseException) {
                    Log.e("BudgetViewModel", "Failed to parse date: ${entity.date}", e)
                    null // Skip invalid entries
                } catch (e: Exception) {
                    Log.e("BudgetViewModel", "Error mapping expense entity: ${entity.id}", e)
                    null
                }
            })
        }
    }

    // Function to save income and budget settings
    fun saveBudgetSettings(income: Double, budgetAmount: Double, isMonthlyBudget: Boolean) {
        monthlyIncome.doubleValue = income
        isMonthly.value = isMonthlyBudget
        if (isMonthlyBudget) {
            monthlyBudget.doubleValue = budgetAmount
            weeklyBudget.doubleValue = 0.0 // Clear weekly if setting monthly
        } else {
            weeklyBudget.doubleValue = budgetAmount
            monthlyBudget.doubleValue = 0.0 // Clear monthly if setting weekly
        }

        viewModelScope.launch {
            repository.setBudget(
                BudgetEntity(
                    id = 0, // Use fixed ID for the single budget entry
                    monthlyIncome = monthlyIncome.doubleValue,
                    isMonthly = isMonthly.value,
                    weeklyBudget = weeklyBudget.doubleValue,
                    monthlyBudget = monthlyBudget.doubleValue
                )
            )
        }
    }

    fun addExpense(amount: Double, description: String) {
        val today = LocalDate.now()
        val exp = Expense(amount, today, description)
        _expenses.add(exp)
        viewModelScope.launch {
            repository.addExpense(
                ExpenseEntity(
                    amount = amount,
                    date = today.format(DateTimeFormatter.ISO_LOCAL_DATE), // Format date to String
                    description = description
                )
            )
        }
    }

    fun getTotalSpent(): Double = _expenses.sumOf { it.amount }

    fun getRemaining(): Double {
        val totalBudget = if (isMonthly.value) monthlyBudget.doubleValue else weeklyBudget.doubleValue
        return (totalBudget - getTotalSpent()).coerceAtLeast(0.0)
    }

    fun getDailyBudget(): Double {
        val totalBudget = if (isMonthly.value) monthlyBudget.doubleValue else weeklyBudget.doubleValue
        val today = LocalDate.now()
        val daysInPeriod = if (isMonthly.value) today.lengthOfMonth() else 7
        val dayOfPeriod = if (isMonthly.value) today.dayOfMonth else today.dayOfWeek.value
        val daysLeft = (daysInPeriod - dayOfPeriod + 1).coerceAtLeast(1)

        return (getRemaining() / daysLeft).coerceAtLeast(0.0)
    }

    fun sendUserMessage(text: String) {
        val trimmedText = text.trim()
        if (trimmedText.isBlank()) return

        chatMessages.add(Message(trimmedText, true))

        if (trimmedText.contains("plan", ignoreCase = true) || trimmedText.contains("analyze", ignoreCase = true) || trimmedText.contains("optimise", ignoreCase = true) || trimmedText.contains("optimize", ignoreCase = true)) {
            requestBudgetPlanOrAnalysis(trimmedText)
        } else {
            getBasicChatReply(trimmedText)
        }
    }

    private fun requestBudgetPlanOrAnalysis(userRequest: String) {
        if (isLoadingAiResponse.value) return

        isLoadingAiResponse.value = true
        // Add loading message safely
        val loadingMessage = Message("Analyzing your finances and preparing advice...", false, isLoading = true)
        chatMessages.add(loadingMessage)
        val loadingMessageIndex = chatMessages.lastIndex

        viewModelScope.launch {
            try {
                val currentIncome = monthlyIncome.doubleValue
                val currentExpenses = repository.getAllExpenses().mapNotNull { entity ->
                    try {
                        Expense(entity.amount, LocalDate.parse(entity.date, DateTimeFormatter.ISO_LOCAL_DATE), entity.description)
                    } catch (e: Exception) { null }
                }

                if (currentIncome <= 0) {
                    // Remove loading message using index
                    if (loadingMessageIndex >= 0 && loadingMessageIndex < chatMessages.size) {
                        chatMessages.removeAt(loadingMessageIndex)
                    }
                    chatMessages.add(Message("Please set your monthly income first in the budget setup screen.", false))
                    isLoadingAiResponse.value = false
                    return@launch
                }

                val expenseSummary = currentExpenses
                    .sortedByDescending { it.date }
                    .take(20)
                    .joinToString("\n") { "- ${it.date}: ${it.description} (₹${it.amount})" }

                val financialContext = """
                Monthly Income: ₹$currentIncome
                Recent Expenses:
                $expenseSummary
                User Request: $userRequest
                """

                val aiReply = aiHelper.getFinancialAdvice(financialContext)

                // Update chat: Remove loading message and add AI response using index
                if (loadingMessageIndex >= 0 && loadingMessageIndex < chatMessages.size) {
                    chatMessages.removeAt(loadingMessageIndex)
                }
                chatMessages.add(Message(aiReply, false))

            } catch (e: Exception) {
                Log.e("BudgetViewModel", "Error getting financial advice", e)
                // Handle errors: Remove loading message using index
                 if (loadingMessageIndex >= 0 && loadingMessageIndex < chatMessages.size) {
                    chatMessages.removeAt(loadingMessageIndex)
                }
                chatMessages.add(Message("Sorry, I encountered an error trying to get advice: ${e.message}", false))
            } finally {
                isLoadingAiResponse.value = false
            }
        }
    }

    private fun getBasicChatReply(text: String) {
         if (isLoadingAiResponse.value) return
         isLoadingAiResponse.value = true
         // Add loading message safely
         val loadingMessage = Message("Thinking...", false, isLoading = true)
         chatMessages.add(loadingMessage)
         val loadingMessageIndex = chatMessages.lastIndex

         viewModelScope.launch {
             try {
                 val aiReply = aiHelper.getChatbotReply(text)
                 // Remove loading message using index
                 if (loadingMessageIndex >= 0 && loadingMessageIndex < chatMessages.size) {
                    chatMessages.removeAt(loadingMessageIndex)
                 }
                 chatMessages.add(Message(aiReply, false))
             } catch (e: Exception) {
                 Log.e("BudgetViewModel", "Error getting basic chat reply", e)
                 // Remove loading message using index
                 if (loadingMessageIndex >= 0 && loadingMessageIndex < chatMessages.size) {
                    chatMessages.removeAt(loadingMessageIndex)
                 }
                 chatMessages.add(Message("Sorry, I couldn't process that: ${e.message}", false))
             } finally {
                 isLoadingAiResponse.value = false
             }
         }
    }
}
