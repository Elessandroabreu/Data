package com.example.util

import com.example.data.model.ExpiryUrgency
import org.junit.Assert.assertEquals
import org.junit.Test

class DateUtilsTest {

    @Test
    fun classifyUrgency_matchesNovember14Example() {
        val today = "2026-11-14"

        // Antes de 11/11 -> arquivar automaticamente
        assertEquals(
            ExpiryUrgency.SHOULD_AUTO_ARCHIVE,
            DateUtils.classifyUrgency("2026-11-10", today)
        )

        // 11/11, 12/11, 13/11 -> vencidos recentes (vermelho escuro)
        assertEquals(ExpiryUrgency.EXPIRED_RECENT, DateUtils.classifyUrgency("2026-11-11", today))
        assertEquals(ExpiryUrgency.EXPIRED_RECENT, DateUtils.classifyUrgency("2026-11-12", today))
        assertEquals(ExpiryUrgency.EXPIRED_RECENT, DateUtils.classifyUrgency("2026-11-13", today))

        // 14/11 -> vence hoje (contorno girando e piscando)
        assertEquals(ExpiryUrgency.DUE_TODAY, DateUtils.classifyUrgency("2026-11-14", today))

        // 15/11 a 17/11 -> até 3 dias (vermelho)
        assertEquals(ExpiryUrgency.DUE_IN_3_DAYS, DateUtils.classifyUrgency("2026-11-15", today))
        assertEquals(ExpiryUrgency.DUE_IN_3_DAYS, DateUtils.classifyUrgency("2026-11-17", today))

        // 18/11 a 21/11 -> 4 a 7 dias (amarelo)
        assertEquals(ExpiryUrgency.DUE_IN_7_DAYS, DateUtils.classifyUrgency("2026-11-18", today))
        assertEquals(ExpiryUrgency.DUE_IN_7_DAYS, DateUtils.classifyUrgency("2026-11-21", today))

        // 22/11+ -> verde
        assertEquals(ExpiryUrgency.SAFE, DateUtils.classifyUrgency("2026-11-22", today))
    }

    @Test
    fun daysUntilExpiry_detects15DayPurgeThreshold() {
        val today = "2026-11-30"
        assertEquals(-15L, DateUtils.daysUntilExpiry("2026-11-15", today))
        assertEquals(-16L, DateUtils.daysUntilExpiry("2026-11-14", today))
    }

    @Test
    fun brDateFormattingAndParsing_workInDdMmYyyy() {
        assertEquals("14/11/2026", DateUtils.isoToBrDisplay("2026-11-14"))
        assertEquals("2026-11-14", DateUtils.brDisplayToIso("14/11/2026"))
        assertEquals(null, DateUtils.brDisplayToIso("31/02/2026"))
    }
}
