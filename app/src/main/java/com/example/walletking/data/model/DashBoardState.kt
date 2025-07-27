package com.example.walletking.data.model

data class DashboardState(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val expenseCategories: List<ExpenseCategoryData> = emptyList(),
    val lastUpdateTime: Long = System.currentTimeMillis()  // Added this field
)
