package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val seededDefaults: Boolean = false,
    val notificationsEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Market(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ProductItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val marketId: String = "",
    val marketName: String = "",
    val expiryDate: String = "", // YYYY-MM-DD
    val notes: String = "",
    val photoBase64: String? = null,
    val status: String = STATUS_ACTIVE,
    val archiveReason: String? = null,
    val archivedDate: String? = null, // YYYY-MM-DD
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        const val STATUS_ACTIVE = "active"
        const val STATUS_RESOLVED = "resolved"
        const val STATUS_ARCHIVED_AUTO = "archived_auto"
        const val STATUS_ARCHIVED_MANUAL = "archived_manual"

        const val REASON_AUTO = "auto"
        const val REASON_MANUAL = "manual"
        const val REASON_RESOLVED = "resolved"
    }
}

enum class ExpiryUrgency(val sortOrder: Int) {
    EXPIRED_RECENT(0),   // -3 a -1 dias (vermelho escuro)
    DUE_TODAY(1),        // 0 dias (vermelho piscando + contorno girando)
    DUE_IN_3_DAYS(2),    // 1 a 3 dias (vermelho)
    DUE_IN_7_DAYS(3),    // 4 a 7 dias (amarelo)
    SAFE(4),             // > 7 dias (verde)
    SHOULD_AUTO_ARCHIVE(5) // < -3 dias
}

val DEFAULT_MARKETS = listOf(
    "Giassi 13",
    "Komprão 66",
    "Komprão 70",
    "Komprão 86",
    "Manenti",
    "Moniari",
    "Líder",
    "Combo",
    "Depieri Oficinas",
    "Depieri F. Silva"
)
