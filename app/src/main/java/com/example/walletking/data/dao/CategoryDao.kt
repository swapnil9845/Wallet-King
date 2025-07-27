package com.example.walletking.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.walletking.data.model.Category
import com.example.walletking.data.model.CategoryTotal
import com.example.walletking.data.model.TransactionType

import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE type = :type")
    fun getCategoriesByType(type: TransactionType): Flow<List<Category>>

    @Query("""
        SELECT 
            c.name as categoryName, 
            CAST(SUM(t.amount) as DOUBLE) as totalAmount
        FROM categories c
        LEFT JOIN transactions t ON c.name = t.category 
        WHERE c.type = :type
        AND (t.date >= :startDate OR t.date IS NULL)
        GROUP BY c.name
    """)
    fun getCategoryTotals(type: TransactionType, startDate: Long = 0): Flow<List<CategoryTotal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category)
}