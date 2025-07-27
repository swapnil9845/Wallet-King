package com.example.walletking.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.walletking.data.database.AppDatabase
import com.example.walletking.data.model.CategoryTotal
import com.example.walletking.data.model.DashboardState
import com.example.walletking.data.model.ExpenseCategoryData
import com.example.walletking.data.model.TransactionType
import com.example.walletking.data.model.MonthlyInsights
import com.example.walletking.data.repository.FinanceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FinanceRepository = FinanceRepository(
        AppDatabase.getDatabase(application).transactionDao(),
        AppDatabase.getDatabase(application).categoryDao()
    )

    private val _updateTrigger = MutableSharedFlow<Unit>(replay = 1)
    private val _selectedDate = MutableStateFlow<Pair<Int, Int>?>(null)
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()










    private val monthlyInsightsFlow = combine(
        repository.getCategoryTotals(TransactionType.EXPENSE),
        repository.getTransactionsByType(TransactionType.EXPENSE),
        repository.getTransactionsByType(TransactionType.INCOME),
        _selectedDate
    ) { categoryTotals, expenses, incomes, selectedDate ->
        val currentCalendar = Calendar.getInstance()
        val selectedYear = selectedDate?.first ?: currentCalendar.get(Calendar.YEAR)
        val selectedMonth = selectedDate?.second ?: currentCalendar.get(Calendar.MONTH)

        // Filter transactions for selected month
        val monthExpenses = expenses.filter { transaction ->
            val calendar = Calendar.getInstance().apply {
                timeInMillis = transaction.date
            }
            calendar.get(Calendar.YEAR) == selectedYear &&
                    calendar.get(Calendar.MONTH) == selectedMonth
        }

        val monthIncomes = incomes.filter { transaction ->
            val calendar = Calendar.getInstance().apply {
                timeInMillis = transaction.date
            }
            calendar.get(Calendar.YEAR) == selectedYear &&
                    calendar.get(Calendar.MONTH) == selectedMonth
        }

        // Calculate insights
        val topExpense = categoryTotals.maxByOrNull { it.total }
        val totalMonthExpense = monthExpenses.sumOf { it.amount }
        val totalMonthIncome = monthIncomes.sumOf { it.amount }

        // Get days in month
        val calendar = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth, 1)
        }
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Calculate daily average
        val dailyAverage = totalMonthExpense / daysInMonth

        // Calculate savings rate
        val savingsRate = if (totalMonthIncome > 0) {
            ((totalMonthIncome - totalMonthExpense) / totalMonthIncome) * 100
        } else 0.0

        // Calculate expense trend
        val lastMonthExpenses = expenses.filter { transaction ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = transaction.date
            }
            if (selectedMonth == 0) {
                cal.get(Calendar.YEAR) == selectedYear - 1 &&
                        cal.get(Calendar.MONTH) == 11
            } else {
                cal.get(Calendar.YEAR) == selectedYear &&
                        cal.get(Calendar.MONTH) == selectedMonth - 1
            }
        }.sumOf { it.amount }

        val expenseTrend = if (lastMonthExpenses > 0) {
            ((totalMonthExpense - lastMonthExpenses) / lastMonthExpenses) * 100
        } else 0.0

        MonthlyInsights(
            topExpenseCategory = topExpense?.name ?: "",
            topExpenseAmount = topExpense?.total ?: 0.0,
            dailyAverageExpense = dailyAverage,
            savingsRate = savingsRate,
            expenseTrendPercentage = expenseTrend,

            )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MonthlyInsights()
    )

    // Expose the insights
    val monthlyInsights: StateFlow<MonthlyInsights> = monthlyInsightsFlow










    private val expenseCategoriesFlow = combine(
        repository.getCategoryTotals(TransactionType.EXPENSE),
        _selectedDate,
        _updateTrigger.onStart { emit(Unit) }
    ) { categoryTotals, selectedDate, _ ->
        android.util.Log.d("DashboardViewModel", "Starting data processing")

        try {
            val currentCalendar = Calendar.getInstance()
            val filteredTotals = if (selectedDate != null) {
                val (year, month) = selectedDate
                categoryTotals.filter { categoryTotal ->
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = categoryTotal.date ?: System.currentTimeMillis()
                    }
                    calendar.get(Calendar.YEAR) == year &&
                            calendar.get(Calendar.MONTH) == month
                }
            } else {
                categoryTotals.filter { categoryTotal ->
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = categoryTotal.date ?: System.currentTimeMillis()
                    }
                    calendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
                            calendar.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH)
                }
            }

            android.util.Log.d("DashboardViewModel", "Filtered totals size: ${filteredTotals.size}")

            val totalExpense = filteredTotals.sumOf { it.total }
            filteredTotals.map { categoryTotal ->
                ExpenseCategoryData(
                    category = categoryTotal.name,
                    amount = categoryTotal.total,
                    percentage = if (totalExpense > 0) (categoryTotal.total / totalExpense) * 100 else 0.0
                )
            }
        } finally {
            android.util.Log.d("DashboardViewModel", "Processing complete, setting loading to false")
            _isLoading.value = false
        }
    }

    val pieChartState: StateFlow<List<ExpenseCategoryData>> = expenseCategoriesFlow
        .onStart {
            android.util.Log.d("DashboardViewModel", "Starting pie chart state collection")
            _isLoading.value = true
        }
        .catch { error ->
            android.util.Log.e("DashboardViewModel", "Error in categories flow", error)
            _isLoading.value = false
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setSelectedMonth(year: Int, month: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            android.util.Log.d("DashboardViewModel", "Setting month: $year-$month")
            _selectedDate.value = year to month
            _updateTrigger.emit(Unit)
        }
    }

    fun resetDateFilter() {
        viewModelScope.launch {
            _isLoading.value = true
            android.util.Log.d("DashboardViewModel", "Resetting date filter")
            _selectedDate.value = null
            _updateTrigger.emit(Unit)
        }
    }

    fun getFormattedSelectedDate(): String {
        return _selectedDate.value?.let { (year, month) ->
            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
            }
            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            monthFormat.format(calendar.time)
        } ?: "Current Month"
    }


    init {
        viewModelScope.launch {
            _updateTrigger.emit(Unit)
        }
    }
}