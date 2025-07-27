package com.example.walletking.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateFormatter {
    private val fullFormatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    private val shortFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

    fun formatFull(timeStamp:Long) :String {
        return fullFormatter.format(Date(timeStamp))
    }
    fun formatShort(timeStamp:Long) :String {
        return shortFormatter.format(Date(timeStamp))
    }

    fun getMonthShort(): Long{
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH,1)
        calendar.set(Calendar.HOUR_OF_DAY,0)
        calendar.set(Calendar.MINUTE,0)
        calendar.set(Calendar.SECOND,0)
        calendar.set(Calendar.MILLISECOND,0)
        return calendar.timeInMillis

    }
}