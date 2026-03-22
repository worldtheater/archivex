package com.worldtheater.archive.util

import android.annotation.SuppressLint
import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {

    private val dateFormatter: ThreadLocal<SimpleDateFormat> =
        object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat {
                return newDateFormatter()
            }
        }

    private val calendar = Calendar.getInstance()

    @SuppressLint("SimpleDateFormat")
    private fun newDateFormatter(): SimpleDateFormat {
        return SimpleDateFormat("yyyy/MM/dd HH:mm")
    }

    fun formatDateTime(time: Long): String {
        val formatter = dateFormatter.get() ?: newDateFormatter()
        val locale = Locale.getDefault()
        val isSimplifiedZh =
            locale.language == "zh" && (locale.country == "CN" || locale.toLanguageTag()
                .contains("Hans"))
        val pattern = if (isSimplifiedZh) "yyyy/MM/dd HH:mm" else "MM/dd/yyyy HH:mm"
        formatter.applyPattern(pattern)
        return formatter.format(Date(time))
    }

    @SuppressLint("SimpleDateFormat")
    fun parseLegacyDate(dateString: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy/MM/dd/HH/mm")
            format.parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    fun isTimeInRange(
        currentHour: Int = calendar.get(Calendar.HOUR_OF_DAY),
        startHour: Int,
        endHour: Int
    ) = currentHour in startHour..endHour

    fun getRelativeTime(date: Date): String {
        return DateUtils.getRelativeTimeSpanString(
            date.time,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }
}
