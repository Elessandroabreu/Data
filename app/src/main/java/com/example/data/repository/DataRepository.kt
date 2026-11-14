package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.DEFAULT_MARKETS
import com.example.data.model.Market
import com.example.data.model.ProductItem
import com.example.data.model.UserSettings
import com.example.util.DateUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DataRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Usuário não autenticado. Faça login com o Google.")
    }

    private fun settingsDoc(uid: String) = db.collection("users").document(uid)
    private fun marketsCol(uid: String) = settingsDoc(uid).collection("markets")
    private fun productsCol(uid: String) = settingsDoc(uid).collection("products")

    fun observeSettings(userId: String): Flow<UserSettings?> = flow {
        val docRef = settingsDoc(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserSettings::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(ex, OperationType.GET, docRef.path, auth)
                    }
                    throw error
                }
        )
    }

    fun observeMarkets(userId: String): Flow<List<Market>> = flow {
        val colRef = marketsCol(userId)
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Market::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(ex, OperationType.LIST, colRef.path, auth)
                    }
                    throw error
                }
        )
    }

    fun observeProducts(userId: String): Flow<List<ProductItem>> = flow {
        val colRef = productsCol(userId)
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ProductItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(ex, OperationType.LIST, colRef.path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureDefaultMarketsSeeded(): Result<Unit> {
        return try {
            val uid = requireUserId()
            val userRef = settingsDoc(uid)
            val snapshot = userRef.get().await()
            val alreadySeeded = snapshot.exists() && (snapshot.getBoolean("seededDefaults") == true)
            if (alreadySeeded) {
                return Result.success(Unit)
            }

            val existingMarkets = marketsCol(uid).limit(1).get().await()
            val batch = db.batch()
            if (!snapshot.exists()) {
                batch.set(
                    userRef,
                    mapOf(
                        "userId" to uid,
                        "seededDefaults" to true,
                        "notificationsEnabled" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.update(
                    userRef,
                    mapOf(
                        "seededDefaults" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            if (existingMarkets.isEmpty) {
                DEFAULT_MARKETS.forEach { marketName ->
                    val mRef = marketsCol(uid).document()
                    batch.set(
                        mRef,
                        mapOf(
                            "userId" to uid,
                            "name" to marketName,
                            "active" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}", auth)
            Result.failure(e)
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean): Result<Unit> {
        return try {
            val uid = requireUserId()
            val ref = settingsDoc(uid)
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(
                    mapOf(
                        "notificationsEnabled" to enabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "userId" to uid,
                        "seededDefaults" to true,
                        "notificationsEnabled" to enabled,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}", auth)
            Result.failure(e)
        }
    }

    suspend fun addMarket(name: String): Result<String> {
        return try {
            val uid = requireUserId()
            val trimmed = name.trim()
            require(trimmed.isNotEmpty()) { "Informe o nome do mercado." }
            val docRef = marketsCol(uid).document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "name" to trimmed,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/${auth.currentUser?.uid}/markets", auth)
            Result.failure(e)
        }
    }

    suspend fun updateMarket(marketId: String, newName: String, active: Boolean): Result<Unit> {
        return try {
            val uid = requireUserId()
            val trimmed = newName.trim()
            require(trimmed.isNotEmpty()) { "Informe o nome do mercado." }
            val docRef = marketsCol(uid).document(marketId)
            docRef.update(
                mapOf(
                    "name" to trimmed,
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/markets/$marketId", auth)
            Result.failure(e)
        }
    }

    suspend fun setMarketActive(marketId: String, active: Boolean): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = marketsCol(uid).document(marketId)
            docRef.update(
                mapOf(
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/markets/$marketId", auth)
            Result.failure(e)
        }
    }

    suspend fun addProduct(
        name: String,
        marketId: String,
        marketName: String,
        expiryDateIso: String,
        notes: String,
        photoBase64: String?
    ): Result<String> {
        return try {
            val uid = requireUserId()
            val cleanName = name.trim()
            val cleanPhoto = photoBase64?.takeIf { it.isNotBlank() }
            require(cleanName.isNotEmpty() || cleanPhoto != null) {
                "Informe o nome do produto ou adicione uma foto."
            }
            val docRef = productsCol(uid).document()
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "name" to cleanName,
                "marketId" to marketId,
                "marketName" to marketName.trim(),
                "expiryDate" to expiryDateIso,
                "notes" to notes.trim(),
                "status" to ProductItem.STATUS_ACTIVE,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanPhoto != null) {
                payload["photoBase64"] = cleanPhoto
            }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/${auth.currentUser?.uid}/products", auth)
            Result.failure(e)
        }
    }

    suspend fun updateProduct(
        productId: String,
        name: String,
        marketId: String,
        marketName: String,
        expiryDateIso: String,
        notes: String,
        photoBase64: String?
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            val cleanName = name.trim()
            val cleanPhoto = photoBase64?.takeIf { it.isNotBlank() }
            require(cleanName.isNotEmpty() || cleanPhoto != null) {
                "Informe o nome do produto ou adicione uma foto."
            }
            val docRef = productsCol(uid).document(productId)
            val updates = mutableMapOf<String, Any>(
                "name" to cleanName,
                "marketId" to marketId,
                "marketName" to marketName.trim(),
                "expiryDate" to expiryDateIso,
                "notes" to notes.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanPhoto != null) {
                updates["photoBase64"] = cleanPhoto
            } else {
                updates["photoBase64"] = FieldValue.delete()
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/products/$productId", auth)
            Result.failure(e)
        }
    }

    suspend fun archiveProduct(
        productId: String,
        targetStatus: String,
        reason: String,
        archivedDateIso: String = DateUtils.todayIso()
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = productsCol(uid).document(productId)
            docRef.update(
                mapOf(
                    "status" to targetStatus,
                    "archiveReason" to reason,
                    "archivedDate" to archivedDateIso,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/products/$productId", auth)
            Result.failure(e)
        }
    }

    suspend fun autoArchiveExpiredProducts(
        products: List<ProductItem>,
        todayIso: String = DateUtils.todayIso()
    ): Result<Int> {
        return try {
            val uid = requireUserId()
            val toArchive = products.filter {
                val diff = DateUtils.daysUntilExpiry(it.expiryDate, todayIso)
                it.status == ProductItem.STATUS_ACTIVE && diff in -15L..-4L
            }
            if (toArchive.isEmpty()) return Result.success(0)

            toArchive.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { product ->
                    val docRef = productsCol(uid).document(product.id)
                    batch.update(
                        docRef,
                        mapOf(
                            "status" to ProductItem.STATUS_ARCHIVED_AUTO,
                            "archiveReason" to ProductItem.REASON_AUTO,
                            "archivedDate" to todayIso,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
            Result.success(toArchive.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/products", auth)
            Result.failure(e)
        }
    }

    suspend fun purgeExpiredOver15Days(
        products: List<ProductItem>,
        todayIso: String = DateUtils.todayIso()
    ): Result<Int> {
        return try {
            val uid = requireUserId()
            val toDelete = products.filter {
                DateUtils.daysUntilExpiry(it.expiryDate, todayIso) < -15L
            }
            if (toDelete.isEmpty()) return Result.success(0)

            toDelete.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { product ->
                    val docRef = productsCol(uid).document(product.id)
                    batch.delete(docRef)
                }
                batch.commit().await()
            }
            Result.success(toDelete.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/${auth.currentUser?.uid}/products", auth)
            Result.failure(e)
        }
    }

    suspend fun restoreProduct(productId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = productsCol(uid).document(productId)
            docRef.update(
                mapOf(
                    "status" to ProductItem.STATUS_ACTIVE,
                    "archiveReason" to FieldValue.delete(),
                    "archivedDate" to FieldValue.delete(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/products/$productId", auth)
            Result.failure(e)
        }
    }

    suspend fun deleteProductPermanently(productId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = productsCol(uid).document(productId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/${auth.currentUser?.uid}/products/$productId", auth)
            Result.failure(e)
        }
    }
}
