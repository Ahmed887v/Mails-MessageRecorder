package com.newrecorder.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormat {

    private val timeOnly = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateTime = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

    /** صياغة مختصرة للطابع الزمني: اليوم يعرض الوقت فقط، وما قبله يعرض التاريخ والوقت. */
    fun relative(timestamp: Long): String {
        if (timestamp <= 0) return ""
        val now = System.currentTimeMillis()
        val sameDay = isSameDay(timestamp, now)
        return if (sameDay) timeOnly.format(Date(timestamp)) else dateTime.format(Date(timestamp))
    }

    private fun isSameDay(a: Long, b: Long): Boolean {
        val ca = java.util.Calendar.getInstance().apply { timeInMillis = a }
        val cb = java.util.Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(java.util.Calendar.YEAR) == cb.get(java.util.Calendar.YEAR) &&
            ca.get(java.util.Calendar.DAY_OF_YEAR) == cb.get(java.util.Calendar.DAY_OF_YEAR)
    }
}
