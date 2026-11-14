package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ProductItem
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun seedDefaultMarkets_authenticatedUser_createsInitialTenMarkets() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = DataRepository(firestore, auth)

        val seedResult = withTimeout(TIMEOUT_MS) { repo.ensureDefaultMarketsSeeded() }
        assertTrue(seedResult.isSuccess)

        val markets = withTimeout(TIMEOUT_MS) {
            repo.observeMarkets(uid).first { it.isNotEmpty() }
        }
        assertTrue(markets.size >= 10)
        assertTrue(markets.any { it.name == "Giassi 13" })
    }

    @Test
    fun productLifecycle_createArchiveAndRestore_succeedsForOwner() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = DataRepository(firestore, auth)
        withTimeout(TIMEOUT_MS) { repo.ensureDefaultMarketsSeeded() }.getOrThrow()

        val market = withTimeout(TIMEOUT_MS) {
            repo.observeMarkets(uid).first { it.isNotEmpty() }
        }.first()

        val productId = withTimeout(TIMEOUT_MS) {
            repo.addProduct(
                name = "Iogurte Grego",
                marketId = market.id,
                marketName = market.name,
                expiryDateIso = "2026-11-10",
                notes = "Gôndola refrigerada",
                photoBase64 = null
            ).getOrThrow()
        }
        assertTrue(productId.isNotEmpty())

        val createdList = withTimeout(TIMEOUT_MS) {
            repo.observeProducts(uid).first { list -> list.any { it.id == productId } }
        }
        val created = createdList.first { it.id == productId }
        assertEquals(ProductItem.STATUS_ACTIVE, created.status)

        // Auto-archive product expired > 3 days ago
        val archivedCount = withTimeout(TIMEOUT_MS) {
            repo.autoArchiveExpiredProducts(listOf(created), todayIso = "2026-11-14").getOrThrow()
        }
        assertEquals(1, archivedCount)

        val afterArchive = withTimeout(TIMEOUT_MS) {
            repo.observeProducts(uid).first { list ->
                list.any { it.id == productId && it.status == ProductItem.STATUS_ARCHIVED_AUTO }
            }
        }
        assertTrue(afterArchive.any { it.id == productId && it.archiveReason == ProductItem.REASON_AUTO })

        // Restore back to active
        withTimeout(TIMEOUT_MS) { repo.restoreProduct(productId).getOrThrow() }
        val afterRestore = withTimeout(TIMEOUT_MS) {
            repo.observeProducts(uid).first { list ->
                list.any { it.id == productId && it.status == ProductItem.STATUS_ACTIVE }
            }
        }
        val restored = afterRestore.first { it.id == productId }
        assertEquals(null, restored.archiveReason)
        assertEquals(null, restored.archivedDate)

        // Purge products expired > 15 days ago
        val purgedCount = withTimeout(TIMEOUT_MS) {
            repo.purgeExpiredOver15Days(listOf(restored), todayIso = "2026-11-26").getOrThrow()
        }
        assertEquals(1, purgedCount)

        val remaining = withTimeout(TIMEOUT_MS) {
            repo.observeProducts(uid).first { list -> list.none { it.id == productId } }
        }
        assertTrue(remaining.none { it.id == productId })
    }

    @Test
    fun updateMarket_renamesLinkedProductsAndSeedIsIdempotent() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = DataRepository(firestore, auth)

        withTimeout(TIMEOUT_MS) { repo.ensureDefaultMarketsSeeded() }.getOrThrow()
        withTimeout(TIMEOUT_MS) { repo.ensureDefaultMarketsSeeded() }.getOrThrow()

        val markets = withTimeout(TIMEOUT_MS) {
            repo.observeMarkets(uid).first { it.size >= 10 }
        }
        assertEquals(10, markets.size)

        val targetMarket = markets.first()
        val productId = withTimeout(TIMEOUT_MS) {
            repo.addProduct(
                name = "Leite Integral",
                marketId = targetMarket.id,
                marketName = targetMarket.name,
                expiryDateIso = "2026-11-20",
                notes = "",
                photoBase64 = null
            ).getOrThrow()
        }

        withTimeout(TIMEOUT_MS) {
            repo.updateMarket(targetMarket.id, "Mercado Renomeado", true).getOrThrow()
        }

        val updatedProducts = withTimeout(TIMEOUT_MS) {
            repo.observeProducts(uid).first { list ->
                list.any { it.id == productId && it.marketName == "Mercado Renomeado" }
            }
        }
        assertEquals("Mercado Renomeado", updatedProducts.first { it.id == productId }.marketName)
    }

    @Test
    fun observeProducts_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        signInTestUser(BOB_EMAIL)
        val bobRepo = DataRepository(firestore, auth)

        var caught: Exception? = null
        try {
            withTimeout(TIMEOUT_MS) { bobRepo.observeProducts(aliceUid).first() }
        } catch (e: Exception) {
            caught = e
        }
        val firestoreException = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    @Test
    fun observeProducts_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = DataRepository(firestore, auth)

        var caught: Exception? = null
        try {
            withTimeout(TIMEOUT_MS) { repo.observeProducts("some_user_123").first() }
        } catch (e: Exception) {
            caught = e
        }
        val firestoreException = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_data@test.com"
        const val BOB_EMAIL = "bob_data@test.com"
        const val TIMEOUT_MS = 5000L
    }
}
