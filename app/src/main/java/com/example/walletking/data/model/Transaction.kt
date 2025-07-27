package com.example.walletking.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.walletking.data.database.TransactionTypeConverter

@Entity(tableName = "transactions")
@TypeConverters(TransactionTypeConverter::class)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Double,
    val description: String,
    val category: String,
    @TypeConverters(TransactionTypeConverter::class)
    val type: TransactionType,
    val date: Long = System.currentTimeMillis()
)
