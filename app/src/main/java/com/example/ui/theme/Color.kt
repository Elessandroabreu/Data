package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Identidade visual DATA — Ink Slate + Superfície Limpa + Semântica de Validade
val InkPrimary = Color(0xFF0F172A)
val InkOnPrimary = Color(0xFFFFFFFF)
val InkPrimaryContainer = Color(0xFFE2E8F0)
val InkOnPrimaryContainer = Color(0xFF0F172A)

val SlateSecondary = Color(0xFF334155)
val SlateOnSecondary = Color(0xFFFFFFFF)
val SlateSecondaryContainer = Color(0xFFF1F5F9)
val SlateOnSecondaryContainer = Color(0xFF1E293B)

val EmeraldAccent = Color(0xFF0F766E)
val EmeraldOnAccent = Color(0xFFFFFFFF)
val EmeraldContainer = Color(0xFFCCFBF1)
val EmeraldOnContainer = Color(0xFF115E59)

val AppBackground = Color(0xFFF5F7FA)
val AppSurface = Color(0xFFFFFFFF)
val AppSurfaceVariant = Color(0xFFEEF2F6)
val AppOnSurface = Color(0xFF0F172A)
val AppOnSurfaceVariant = Color(0xFF64748B)
val AppOutline = Color(0xFFE2E8F0)

val DarkBackground = Color(0xFF0B1120)
val DarkSurface = Color(0xFF111827)
val DarkSurfaceVariant = Color(0xFF1E293B)
val DarkOnSurface = Color(0xFFF1F5F9)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)
val DarkOutline = Color(0xFF334155)

@Immutable
data class ExpiryPalette(
    val expiredRecentBg: Color,
    val expiredRecentBadge: Color,
    val expiredRecentText: Color,
    val dueTodayBg: Color,
    val dueTodayPillBg: Color,
    val dueTodayAccent: Color,
    val dueTodayGlow: Color,
    val dueTodayText: Color,
    val dueSoonBg: Color,
    val dueSoonBadge: Color,
    val dueSoonText: Color,
    val warningBg: Color,
    val warningBadge: Color,
    val warningText: Color,
    val safeBg: Color,
    val safeBadge: Color,
    val safeText: Color,
    val kpiLabelText: Color
)

val LightExpiryPalette = ExpiryPalette(
    expiredRecentBg = Color(0xFFFCE8E8),
    expiredRecentBadge = Color(0xFF8B1111),
    expiredRecentText = Color(0xFF740D0D),
    dueTodayBg = Color(0xFFFFF5F5),
    dueTodayPillBg = Color(0xFFFEE2E2),
    dueTodayAccent = Color(0xFFDC2626),
    dueTodayGlow = Color(0xFFFF6B6B),
    dueTodayText = Color(0xFF991B1B),
    dueSoonBg = Color(0xFFFDECEC),
    dueSoonBadge = Color(0xFFDC2626),
    dueSoonText = Color(0xFF991B1B),
    warningBg = Color(0xFFFEF3C7),
    warningBadge = Color(0xFFB45309),
    warningText = Color(0xFF92400E),
    safeBg = Color(0xFFDCFCE7),
    safeBadge = Color(0xFF15803D),
    safeText = Color(0xFF166534),
    kpiLabelText = Color(0xFF1E293B)
)

val DarkExpiryPalette = ExpiryPalette(
    expiredRecentBg = Color(0xFF351318),
    expiredRecentBadge = Color(0xFF991B1B),
    expiredRecentText = Color(0xFFFECACA),
    dueTodayBg = Color(0xFF2D1216),
    dueTodayPillBg = Color(0xFF47151C),
    dueTodayAccent = Color(0xFFEF4444),
    dueTodayGlow = Color(0xFFFCA5A5),
    dueTodayText = Color(0xFFFECACA),
    dueSoonBg = Color(0xFF3A161A),
    dueSoonBadge = Color(0xFFDC2626),
    dueSoonText = Color(0xFFFECACA),
    warningBg = Color(0xFF36250A),
    warningBadge = Color(0xFFD97706),
    warningText = Color(0xFFFDE68A),
    safeBg = Color(0xFF0F2D1C),
    safeBadge = Color(0xFF16A34A),
    safeText = Color(0xFFBBF7D0),
    kpiLabelText = Color(0xFFF1F5F9)
)

val LocalExpiryPalette = staticCompositionLocalOf { LightExpiryPalette }
