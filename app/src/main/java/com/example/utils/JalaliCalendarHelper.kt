package com.example.utils

import java.util.Calendar
import java.util.TimeZone

object JalaliCalendarHelper {
    fun getJalaliDateString(timestamp: Long): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"))
        calendar.timeInMillis = timestamp
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val jalali = gregorianToJalali(year, month, day)
        
        val farsiYear = toPersianNumber(jalali.year.toString())
        val farsiMonth = toPersianNumber(String.format("%02d", jalali.month))
        val farsiDay = toPersianNumber(String.format("%02d", jalali.day))
        val farsiTime = toPersianNumber(String.format("%02d:%02d", hour, minute))

        return "$farsiYear/$farsiMonth/$farsiDay - $farsiTime"
    }

    private class JalaliDate(val year: Int, val month: Int, val day: Int)

    private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 335)
        val jy = gy - 1600
        val jm = gm - 1
        val jd = gd - 1

        var gDayNo = 365 * jy + (jy + 3) / 4 - (jy + 99) / 100 + (jy + 399) / 400
        gDayNo += gDaysInMonth[jm]
        if (jm > 1 && ((jy % 4 == 0 && jy % 100 != 0) || (jy % 400 == 0))) {
            gDayNo++
        }
        gDayNo += jd

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jyRes = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jyRes += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        val jmRes: Int
        val jdRes: Int
        if (jDayNo < 186) {
            jmRes = 1 + jDayNo / 31
            jdRes = 1 + jDayNo % 31
        } else {
            jmRes = 7 + (jDayNo - 186) / 30
            jdRes = 1 + (jDayNo - 186) % 30
        }
        return JalaliDate(jyRes, jmRes, jdRes)
    }

    fun toPersianNumber(input: String): String {
        return input.map { char ->
            when (char) {
                '0' -> '۰'
                '1' -> '۱'
                '2' -> '۲'
                '3' -> '۳'
                '4' -> '۴'
                '5' -> '۵'
                '6' -> '۶'
                '7' -> '۷'
                '8' -> '۸'
                '9' -> '۹'
                else -> char
            }
        }.joinToString("")
    }
}
