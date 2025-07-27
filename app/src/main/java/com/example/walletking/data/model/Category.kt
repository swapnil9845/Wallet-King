package com.example.walletking.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val name : String,
    val type: TransactionType,
    val color: Int
)
