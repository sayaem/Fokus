package com.example.admission

import com.example.data.model.AdmissionEventEntity
import com.example.data.model.AdmissionEventType
import com.example.data.model.ApplicationStatus
import com.example.ui.localization.AppLanguage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class CountdownResult(
    val label: String,
    val isUrgent: Boolean = false,
    val isPassed: Boolean = false,
    val isUnconfirmed: Boolean = false,
    val formattedDate: String = "",
    val daysRemaining: Long = 0L,
    val hoursRemaining: Long = 0L,
    val minutesRemaining: Long = 0L
)

object AdmissionCountdownEngine {

    val DHAKA_TIMEZONE: TimeZone = TimeZone.getTimeZone("Asia/Dhaka")

    fun calculateCountdown(
        event: AdmissionEventEntity,
        applicationStatus: ApplicationStatus? = null,
        language: AppLanguage = AppLanguage.ENGLISH,
        nowMillis: Long = System.currentTimeMillis()
    ): CountdownResult {
        if (!event.isDateConfirmed) {
            val unconfirmedLabel = if (language == AppLanguage.BANGLA) "তারিখ এখনো নির্ধারিত হয়নি" else "Date not confirmed"
            return CountdownResult(
                label = unconfirmedLabel,
                isUnconfirmed = true,
                formattedDate = if (language == AppLanguage.BANGLA) "অনির্ধারিত" else "TBA"
            )
        }

        val targetMillis = event.deadlineDateTimeMillis ?: event.startDateTimeMillis
        if (targetMillis == null || targetMillis == 0L) {
            val unconfirmedLabel = if (language == AppLanguage.BANGLA) "তারিখ এখনো নির্ধারিত হয়নি" else "Date not confirmed"
            return CountdownResult(
                label = unconfirmedLabel,
                isUnconfirmed = true,
                formattedDate = if (language == AppLanguage.BANGLA) "অনির্ধারিত" else "TBA"
            )
        }

        val formattedDate = formatDate(targetMillis, event.isAllDay, language)

        val diff = targetMillis - nowMillis

        // If target is in the past
        if (diff <= 0) {
            val isDeadlineType = event.eventType == AdmissionEventType.APPLICATION_DEADLINE ||
                    event.eventType == AdmissionEventType.PAYMENT_DEADLINE
            val isIncomplete = applicationStatus != null &&
                    applicationStatus != ApplicationStatus.SUBMITTED &&
                    applicationStatus != ApplicationStatus.CONFIRMED

            val passedLabel = if (isDeadlineType && isIncomplete) {
                if (language == AppLanguage.BANGLA) "আবেদনের সময় শেষ" else "Deadline passed"
            } else {
                if (language == AppLanguage.BANGLA) "সময় অতিক্রান্ত" else "Event passed"
            }

            return CountdownResult(
                label = passedLabel,
                isUrgent = isDeadlineType && isIncomplete,
                isPassed = true,
                formattedDate = formattedDate
            )
        }

        val totalMinutes = diff / (60 * 1000L)
        val totalHours = diff / (3600 * 1000L)
        val days = diff / (24 * 3600 * 1000L)
        val remainingHours = (diff % (24 * 3600 * 1000L)) / (3600 * 1000L)
        val remainingMinutes = (diff % (3600 * 1000L)) / (60 * 1000L)

        // Check if event is on the same calendar day
        val calNow = Calendar.getInstance(DHAKA_TIMEZONE).apply { timeInMillis = nowMillis }
        val calTarget = Calendar.getInstance(DHAKA_TIMEZONE).apply { timeInMillis = targetMillis }

        val isSameDay = calNow.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR) &&
                calNow.get(Calendar.DAY_OF_YEAR) == calTarget.get(Calendar.DAY_OF_YEAR)

        val isUrgent = days <= 3

        val label = when {
            isSameDay -> {
                if (language == AppLanguage.BANGLA) {
                    if (totalHours > 0) "আজ (${formatBanglaDigits(totalHours.toString())}ঘ বাকি)" else "আজ (${formatBanglaDigits(remainingMinutes.toString())}মি বাকি)"
                } else {
                    if (totalHours > 0) "Today (${totalHours}h left)" else "Today (${remainingMinutes}m left)"
                }
            }
            days >= 1 -> {
                if (language == AppLanguage.BANGLA) {
                    "${formatBanglaDigits(days.toString())} দিন বাকি"
                } else {
                    if (days == 1L) "1 day left" else "$days days left"
                }
            }
            totalHours >= 1 -> {
                if (language == AppLanguage.BANGLA) {
                    "${formatBanglaDigits(totalHours.toString())} ঘণ্টা ${formatBanglaDigits(remainingMinutes.toString())} মি বাকি"
                } else {
                    "${totalHours}h ${remainingMinutes}m left"
                }
            }
            else -> {
                if (language == AppLanguage.BANGLA) {
                    "${formatBanglaDigits(totalMinutes.coerceAtLeast(1).toString())} মিনিট বাকি"
                } else {
                    "${totalMinutes.coerceAtLeast(1)}m left"
                }
            }
        }

        return CountdownResult(
            label = label,
            isUrgent = isUrgent,
            isPassed = false,
            formattedDate = formattedDate,
            daysRemaining = days,
            hoursRemaining = remainingHours,
            minutesRemaining = remainingMinutes
        )
    }

    fun formatDate(millis: Long, isAllDay: Boolean, language: AppLanguage): String {
        val sdfPattern = if (isAllDay) "dd MMM yyyy" else "dd MMM yyyy, hh:mm a"
        val sdf = SimpleDateFormat(sdfPattern, Locale.ENGLISH).apply {
            timeZone = DHAKA_TIMEZONE
        }
        val formatted = sdf.format(Date(millis))

        return if (language == AppLanguage.BANGLA) {
            translateDateToBangla(formatted)
        } else {
            formatted
        }
    }

    fun formatShortDate(millis: Long, language: AppLanguage): String {
        val sdf = SimpleDateFormat("dd MMM", Locale.ENGLISH).apply {
            timeZone = DHAKA_TIMEZONE
        }
        val formatted = sdf.format(Date(millis))
        return if (language == AppLanguage.BANGLA) translateDateToBangla(formatted) else formatted
    }

    private fun translateDateToBangla(englishDateStr: String): String {
        var result = englishDateStr
        val monthReplacements = listOf(
            "Jan" to "জানু",
            "Feb" to "ফেব্রু",
            "Mar" to "মার্চ",
            "Apr" to "এপ্রিল",
            "May" to "মে",
            "Jun" to "জুন",
            "Jul" to "জুলাই",
            "Aug" to "আগস্ট",
            "Sep" to "সেপ্টে",
            "Oct" to "অক্টো",
            "Nov" to "নভে",
            "Dec" to "ডিসে",
            "AM" to "সকাল",
            "PM" to "বিকাল"
        )
        for ((eng, bng) in monthReplacements) {
            result = result.replace(eng, bng, ignoreCase = true)
        }
        return formatBanglaDigits(result)
    }

    fun formatBanglaDigits(input: String): String {
        val bengaliDigits = arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val sb = StringBuilder()
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(bengaliDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }
}
