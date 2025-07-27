package com.example.walletking.ui.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.walletking.data.database.AppDatabase
import com.example.walletking.data.model.Category
import com.example.walletking.data.model.DashboardState
import com.example.walletking.data.model.ExpenseCategoryData
import com.example.walletking.data.model.Transaction
import com.example.walletking.data.model.TransactionFilter
import com.example.walletking.data.model.TransactionType
import com.example.walletking.data.repository.FinanceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FinanceRepository

    // Trigger for updating UI components
    private val _transactionUpdateTrigger = MutableSharedFlow<Unit>()

    // Transaction filtering
    private val _transactionFilter = MutableStateFlow(TransactionFilter.ALL)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FinanceRepository(database.transactionDao(), database.categoryDao())
    }

    // Filtered transactions with update trigger
    val filteredTransactions = combine(
        _transactionFilter,
        _transactionUpdateTrigger.onStart { emit(Unit) }
    ) { filter, _ ->
        when (filter) {
            TransactionFilter.ALL -> repository.allTransactions
            TransactionFilter.INCOME -> repository.getTransactionsByType(TransactionType.INCOME)
            TransactionFilter.EXPENSE -> repository.getTransactionsByType(TransactionType.EXPENSE)
        }
    }.flatMapLatest { it }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // Dashboard state with update trigger
    val dashboardState: StateFlow<DashboardState> = combine(
        repository.getTransactionsByType(TransactionType.INCOME),
        repository.getTransactionsByType(TransactionType.EXPENSE),
        repository.getCategoryTotals(TransactionType.EXPENSE),
        _transactionUpdateTrigger.onStart { emit(Unit) }
    ) { incomeTransactions, expenseTransactions, categoryTotals, _ ->
        val totalIncome = incomeTransactions.sumOf { it.amount }
        val totalExpense = expenseTransactions.sumOf { it.amount }

        DashboardState(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            balance = totalIncome - totalExpense,
            expenseCategories = categoryTotals.map { categoryTotal ->
                ExpenseCategoryData(
                    category = categoryTotal.name,
                    amount = categoryTotal.total,
                    percentage = if (totalExpense > 0) (categoryTotal.total / totalExpense) * 100 else 0.0
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )

    // Existing functions for categories
    val allCategories = repository.allCategories

    fun getCategoriesByType(type: TransactionType) = repository.getCategoriesByType(type)

    fun getCategoryTotals(type: TransactionType, startDate: Long = 0) =
        repository.getCategoryTotals(type, startDate)

    // Transaction operations with update triggering
    fun addTransaction(transaction: Transaction) = viewModelScope.launch {
        repository.addTransaction(transaction)
        _transactionUpdateTrigger.emit(Unit)
    }

    fun deleteTransaction(transaction: Transaction) = viewModelScope.launch {
        repository.deleteTransaction(transaction)
        _transactionUpdateTrigger.emit(Unit)
    }

    fun setTransactionFilter(filter: TransactionFilter) {
        _transactionFilter.value = filter
    }

    // Category operations
    fun addCategory(category: Category) = viewModelScope.launch {
        repository.addCategory(category)
    }
}