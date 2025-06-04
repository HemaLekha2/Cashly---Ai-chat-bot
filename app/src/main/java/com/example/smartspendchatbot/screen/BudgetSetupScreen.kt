package com.example.smartspendchatbot.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.smartspendchatbot.viewmodel.BudgetViewModel

@Composable
fun BudgetSetupScreen(
    viewModel: BudgetViewModel,
    onContinue: () -> Unit
) {
    var budgetInput by remember { mutableStateOf("") }
    var expenseInput by remember { mutableStateOf("") }
    var expenseDesc by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Budget Type Toggle
        Row(Modifier.padding(bottom = 16.dp)) {
            Text("Budget Type:", modifier = Modifier.padding(end = 8.dp))
            Switch(
                checked = viewModel.isMonthly.value,
                onCheckedChange = { viewModel.isMonthly.value = it }
            )
            Text(
                if (viewModel.isMonthly.value) "Monthly" else "Weekly",
                Modifier.padding(start = 8.dp)
            )
        }

        // Budget Input
        Text(
            if (viewModel.isMonthly.value) "Monthly Budget (₹)" else "Weekly Budget (₹)",
            style = MaterialTheme.typography.headlineSmall
        )
        OutlinedTextField(
            value = budgetInput,
            onValueChange = { budgetInput = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                budgetInput.toDoubleOrNull()?.let {
                    viewModel.setBudget(it)
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Set Budget")
        }

        // Expense Input
        Divider(Modifier.padding(vertical = 24.dp))
        Text("Add Expense", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = expenseInput,
            onValueChange = { expenseInput = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("Amount (₹)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
        OutlinedTextField(
            value = expenseDesc,
            onValueChange = { expenseDesc = it },
            label = { Text("Description") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
        Button(
            onClick = {
                expenseInput.toDoubleOrNull()?.let {
                    viewModel.addExpense(it, expenseDesc)
                    expenseInput = ""
                    expenseDesc = ""
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Add Expense")
        }

        // Budget Summary
        Divider(Modifier.padding(vertical = 24.dp))
        Text("Budget Summary", style = MaterialTheme.typography.titleLarge)
        Text("Total Spent: ₹${"%.2f".format(viewModel.getTotalSpent())}",
            modifier = Modifier.padding(top = 8.dp))
        Text("Remaining: ₹${"%.2f".format(viewModel.getRemaining())}")

        // Continue Button
        Button(
            onClick = onContinue,
            modifier = Modifier
                .padding(top = 32.dp)
                .fillMaxWidth(),
            enabled = viewModel.monthlyBudget.value > 0 || viewModel.weeklyBudget.value > 0
        ) {
            Text("Continue to Chatbot")
        }
    }
}