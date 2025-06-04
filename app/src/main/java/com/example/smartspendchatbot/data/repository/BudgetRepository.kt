package com.example.smartspendchatbot.data.repository


import com.example.smartspendchatbot.data.local.BudgetDao
import com.example.smartspendchatbot.data.local.BudgetEntity
import com.example.smartspendchatbot.data.local.ExpenseDao
import com.example.smartspendchatbot.data.local.ExpenseEntity
import javax.inject.Inject

class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    private val expenseDao: ExpenseDao
) {
    suspend fun getBudget(): BudgetEntity? = budgetDao.getBudget()
    suspend fun setBudget(budget: BudgetEntity) = budgetDao.insertBudget(budget)
    suspend fun getAllExpenses(): List<ExpenseEntity> = expenseDao.getAllExpenses()
    suspend fun addExpense(expense: ExpenseEntity) = expenseDao.insertExpense(expense)
    suspend fun clearExpenses() = expenseDao.clearExpenses()
}