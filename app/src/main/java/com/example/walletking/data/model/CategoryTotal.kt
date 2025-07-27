package com.example.walletking.data.model

import androidx.room.ColumnInfo

data class CategoryTotal(
    @ColumnInfo(name = "categoryName")  // This will be the unified column name
    val name: String,
    @ColumnInfo(name = "totalAmount")   // This will be the unified column name
    val total: Double,

    @ColumnInfo(name = "transactionDate")  // Add date column
    val date: Long?= null  // Using Long to store timestamp
)
