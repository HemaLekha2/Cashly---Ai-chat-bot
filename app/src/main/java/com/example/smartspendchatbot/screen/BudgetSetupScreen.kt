package com.example.smartspendchatbot.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.smartspendchatbot.viewmodel.BudgetViewModel
import java.util.Locale

@Composable
fun BudgetSetupScreen(
    viewModel: BudgetViewModel,
    onContinue: () -> Unit
) {
    var incomeInput by remember { mutableStateOf(viewModel.monthlyIncome.doubleValue.takeIf { it > 0 }?.toString() ?: "") }
    var budgetInput by remember { mutableStateOf(
        (if (viewModel.isMonthly.value) viewModel.monthlyBudget.doubleValue else viewModel.weeklyBudget.doubleValue)
            .takeIf { it > 0 }?.toString() ?: ""
    ) }
    var expenseInput by remember { mutableStateOf("") }
    var expenseDesc by remember { mutableStateOf("") }

    LaunchedEffect(viewModel.isMonthly.value) {
        budgetInput = (if (viewModel.isMonthly.value) viewModel.monthlyBudget.doubleValue else viewModel.weeklyBudget.doubleValue)
            .takeIf { it > 0 }?.toString() ?: ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Setup Your Budget", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = incomeInput,
            onValueChange = { incomeInput = it },
            label = { Text("Monthly Income (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), // Fixed: NumberDecimal -> Decimal
            modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Budget Period:", modifier = Modifier.padding(end = 8.dp))
            Text(
                text = if (viewModel.isMonthly.value) "Monthly" else "Weekly",
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = viewModel.isMonthly.value,
                onCheckedChange = { viewModel.isMonthly.value = it }
            )
        }

        OutlinedTextField(
            value = budgetInput,
            onValueChange = { budgetInput = it },
            label = { Text(if (viewModel.isMonthly.value) "Monthly Budget (₹)" else "Weekly Budget (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), // Fixed: NumberDecimal -> Decimal
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val income = incomeInput.toDoubleOrNull() ?: 0.0
                val budget = budgetInput.toDoubleOrNull() ?: 0.0
                viewModel.saveBudgetSettings(income, budget, viewModel.isMonthly.value)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Budget Settings")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Add Expense", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = expenseInput,
            onValueChange = { expenseInput = it },
            label = { Text("Amount (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), // Fixed: NumberDecimal -> Decimal
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = expenseDesc,
            onValueChange = { expenseDesc = it },
            label = { Text("Description (e.g., Groceries, Rent)") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                expenseInput.toDoubleOrNull()?.let {
                    viewModel.addExpense(it, expenseDesc.ifBlank { "Uncategorized" })
                    expenseInput = ""
                    expenseDesc = ""
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Add Expense")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Current Summary", style = MaterialTheme.typography.titleLarge)
        Text("Total Spent (Current Period): ₹${String.format(Locale.US, "%.2f", viewModel.getTotalSpent())}")
        Text("Remaining Budget: ₹${String.format(Locale.US, "%.2f", viewModel.getRemaining())}")
        Text("Suggested Daily Spend: ₹${String.format(Locale.US, "%.2f", viewModel.getDailyBudget())}")

        Button(
            onClick = onContinue,
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth(),
            enabled = viewModel.monthlyIncome.doubleValue > 0
        ) {
            Text("Go to Chatbot / Get Plan")
        }
    }
}