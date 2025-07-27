package com.example.walletking.ui.transactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.walletking.data.database.AppDatabase
import com.example.walletking.data.model.DashboardState
import com.example.walletking.data.model.ExpenseCategoryData
import com.example.walletking.data.model.Transaction
import com.example.walletking.data.model.TransactionFilter
import com.example.walletking.data.model.TransactionType
import com.example.walletking.data.repository.FinanceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class TransactionsViewModel(
    application: Application
) : AndroidViewModel(application) {

    // Initialize repository first
    private val repository: FinanceRepository = FinanceRepository(
        AppDatabase.getDatabase(application).transactionDao(),
        AppDatabase.getDatabase(application).categoryDao()
    )
    private val _dashboardState = MutableStateFlow(DashboardState())
    val dashboardState = _dashboardState.asStateFlow()

    private val _recentTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val recentTransactions = _recentTransactions.asStateFlow()

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent = _errorEvent.asSharedFlow()
    private val _transactionFilter = MutableStateFlow(TransactionFilter.ALL)

    // Now we can safely use repository since it's already initialized

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions = _transactionFilter
        .flatMapLatest { filter ->
            when (filter) {
                TransactionFilter.ALL -> repository.allTransactions
                TransactionFilter.INCOME -> repository.getTransactionsByType(TransactionType.INCOME)
                TransactionFilter.EXPENSE -> repository.getTransactionsByType(TransactionType.EXPENSE)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun setTransactionFilter(filter: TransactionFilter) {
        _transactionFilter.value = filter
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            updateDashboardData()
        }
    }
    private fun updateDashboardData() {
        viewModelScope.launch {
            try {
                // Get all transactions
                val allTransactions = repository.getAllTransactions()

                // Calculate totals
                var totalIncome = 0.0
                var totalExpense = 0.0
                val categoryExpenses = mutableMapOf<String, Double>()

                allTransactions.forEach { transaction ->
                    when (transaction.type) {
                        TransactionType.INCOME -> totalIncome += transaction.amount
                        TransactionType.EXPENSE -> {
                            totalExpense += transaction.amount
                            // Update category-wise expenses
                            categoryExpenses[transaction.category] =
                                (categoryExpenses[transaction.category] ?: 0.0) + transaction.amount
                        }
                    }
                }

                // Calculate current balance
                val currentBalance = totalIncome - totalExpense

                // Create expense categories data for pie chart
                val expenseCategories = categoryExpenses.map { (category, amount) ->
                    ExpenseCategoryData(
                        category = category,
                        amount = amount,
                        percentage = (amount / totalExpense) * 100
                    )
                }.sortedByDescending { it.amount }

                // Update dashboard state
                _dashboardState.update { currentState ->
                    currentState.copy(
                        balance = currentBalance,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense,
                        expenseCategories = expenseCategories,
                        lastUpdateTime = System.currentTimeMillis()
                    )
                }

                // Update recent transactions
                val recentTransactions = allTransactions
                    .sortedByDescending { it.date }
                    .take(5)  // Get last 5 transactions
                _recentTransactions.update { recentTransactions }

            } catch (e: Exception) {
                // Handle error
                _errorEvent.emit("Failed to update dashboard: ${e.message}")
            }
        }
    }
}