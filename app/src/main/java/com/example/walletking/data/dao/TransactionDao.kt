package com.example.walletking.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.walletking.data.model.CategoryTotal
import com.example.walletking.data.model.Transaction
import com.example.walletking.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>

    @Query("SELECT CAST(SUM(amount) as DOUBLE) FROM transactions WHERE type = :type")
    suspend fun getTotalByType(type: TransactionType): Double?

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsList(): List<Transaction>

    @Query("""
    SELECT 
        category as categoryName,
        CAST(SUM(amount) as DOUBLE) as totalAmount,
        MAX(date) as transactionDate
    FROM transactions
    WHERE type = :type
    GROUP BY category
""")
    fun getCategoryTotals(type: TransactionType): Flow<List<CategoryTotal>>

    @Insert
    suspend fun insert(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)
}
