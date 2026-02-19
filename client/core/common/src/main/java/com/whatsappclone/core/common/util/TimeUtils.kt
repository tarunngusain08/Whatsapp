package com.whatsappclone.core.common.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeUtils {

    fun nowMillis(): Long = System.currentTimeMillis()

    fun formatChatTimestamp(epochMillis: Long): String {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = epochMillis }

        return when {
            isSameDay(now, target) -> formatTime(epochMillis, "HH:mm")
            isYesterday(now, target) -> "Yesterday"
            isSameWeek(now, target) -> formatTime(epochMillis, "EEE")
            else -> formatTime(epochMillis, "dd/MM/yy")
        }
    }

    fun formatMessageTime(epochMillis: Long): String {
        return formatTime(epochMillis, "HH:mm")
    }

    fun formatLastSeen(epochMillis: Long): String {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = epochMillis }
        val time = formatTime(epochMillis, "HH:mm")

        return when {
            isSameDay(now, target) -> "last seen today at $time"
            isYesterday(now, target) -> "last seen yesterday at $time"
            else -> "last seen ${formatTime(epochMillis, "dd/MM/yy")}"
        }
    }

    fun formatExportTimestamp(epochMillis: Long): String {
        return formatTime(epochMillis, "yyyy-MM-dd HH:mm:ss")
    }

    fun formatDateSeparator(epochMillis: Long): String {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = epochMillis }

        return when {
            isSameDay(now, target) -> "Today"
            isYesterday(now, target) -> "Yesterday"
            else -> formatTime(epochMillis, "MMMM d, yyyy")
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(now: Calendar, target: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(yesterday, target)
    }

    private fun isSameWeek(now: Calendar, target: Calendar): Boolean {
        return now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.WEEK_OF_YEAR) == target.get(Calendar.WEEK_OF_YEAR)
    }

    private fun formatTime(epochMillis: Long, pattern: String): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(epochMillis))
    }
}
