package com.example.util

import com.example.data.model.ExpiryUrgency
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs

object DateUtils {
    private const val MILLIS_PER_DAY = 86_400_000L

    fun todayIso(): String {
        val cal = Calendar.getInstance()
        return formatIso(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun formatIso(year: Int, month1to12: Int, day: Int): String {
        return String.format("%04d-%02d-%02d", year, month1to12, day)
    }

    fun isoToBrDisplay(isoDate: String): String {
        val parts = isoDate.split("-")
        if (parts.size != 3) return isoDate
        return "${parts[2]}/${parts[1]}/${parts[0]}"
    }

    fun brDisplayToIso(brDate: String): String? {
        val digits = brDate.filter { it.isDigit() }
        if (digits.length != 8) return null
        val day = digits.substring(0, 2).toIntOrNull() ?: return null
        val month = digits.substring(2, 4).toIntOrNull() ?: return null
        val year = digits.substring(4, 8).toIntOrNull() ?: return null
        if (year < 2000 || year > 2100 || month !in 1..12 || day !in 1..31) return null
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            isLenient = false
            clear()
            set(year, month - 1, day, 0, 0, 0)
        }
        return try {
            cal.timeInMillis // triggers lenient validation
            formatIso(year, month, day)
        } catch (_: Exception) {
            null
        }
    }

    fun isoToUtcMidnightMillis(isoDate: String): Long? {
        val parts = isoDate.split("-")
        if (parts.size != 3) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            isLenient = false
            clear()
            set(year, month - 1, day, 0, 0, 0)
        }
        return try {
            cal.timeInMillis
        } catch (_: Exception) {
            null
        }
    }

    fun utcMillisToIso(utcMillis: Long): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        return formatIso(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun daysUntilExpiry(expiryIso: String, todayIso: String = todayIso()): Long {
        val expiryMs = isoToUtcMidnightMillis(expiryIso) ?: return 0L
        val todayMs = isoToUtcMidnightMillis(todayIso) ?: return 0L
        return (expiryMs - todayMs) / MILLIS_PER_DAY
    }

    fun classifyUrgency(expiryIso: String, todayIso: String = todayIso()): ExpiryUrgency {
        val diff = daysUntilExpiry(expiryIso, todayIso)
        return when {
            diff < -3L -> ExpiryUrgency.SHOULD_AUTO_ARCHIVE
            diff in -3L..-1L -> ExpiryUrgency.EXPIRED_RECENT
            diff == 0L -> ExpiryUrgency.DUE_TODAY
            diff in 1L..3L -> ExpiryUrgency.DUE_IN_3_DAYS
            diff in 4L..7L -> ExpiryUrgency.DUE_IN_7_DAYS
            else -> ExpiryUrgency.SAFE
        }
    }

    fun formatRemainingDaysLabel(daysUntil: Long): String {
        return when {
            daysUntil < -1L -> "Vencido há ${abs(daysUntil)} dias"
            daysUntil == -1L -> "Vencido há 1 dia"
            daysUntil == 0L -> "Vence hoje • Atenção máxima"
            daysUntil == 1L -> "Vence amanhã (1 dia)"
            else -> "Vence em $daysUntil dias"
        }
    }

    fun urgencyStatusTitle(urgency: ExpiryUrgency): String {
        return when (urgency) {
            ExpiryUrgency.EXPIRED_RECENT -> "Vencido recente"
            ExpiryUrgency.DUE_TODAY -> "Vence no dia"
            ExpiryUrgency.DUE_IN_3_DAYS -> "Urgente (até 3 dias)"
            ExpiryUrgency.DUE_IN_7_DAYS -> "Atenção (4 a 7 dias)"
            ExpiryUrgency.SAFE -> "Em dia (+7 dias)"
            ExpiryUrgency.SHOULD_AUTO_ARCHIVE -> "Vencido (+3 dias)"
        }
    }
}
