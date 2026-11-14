package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

object ImageUtils {
    private const val MAX_DIMENSION_PX = 480
    private const val MAX_BYTES = 120_000

    fun createCameraTempUri(context: Context): Uri {
        val dir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
        val file = File(dir, "produto_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    suspend fun compressUriToBase64(context: Context, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                resolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOptions)
                } ?: return@withContext Result.failure(
                    IllegalStateException("Não foi possível abrir a imagem selecionada.")
                )

                if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Arquivo de imagem inválido ou corrompido.")
                    )
                }

                var sampleSize = 1
                val maxSide = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
                while (maxSide / sampleSize > MAX_DIMENSION_PX * 2) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }
                val decoded = resolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                } ?: return@withContext Result.failure(
                    IllegalStateException("Não foi possível ler a imagem.")
                )

                val scaled = scaleDownIfNeeded(decoded, MAX_DIMENSION_PX)
                if (scaled !== decoded) {
                    decoded.recycle()
                }

                var quality = 78
                var bytes: ByteArray
                do {
                    val out = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    bytes = out.toByteArray()
                    quality -= 15
                } while (bytes.size > MAX_BYTES && quality >= 30)

                scaled.recycle()

                if (bytes.size > 180_000) {
                    return@withContext Result.failure(
                        IllegalStateException("A foto ficou muito grande. Tente aproximar menos ou escolher outra.")
                    )
                }

                val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
                Result.success(encoded)
            } catch (e: SecurityException) {
                Result.failure(IllegalStateException("Sem permissão para acessar a foto selecionada.", e))
            } catch (e: Exception) {
                Result.failure(IllegalStateException("Erro ao processar a foto. Tente novamente.", e))
            }
        }

    fun decodeBase64ToBitmap(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val longest = maxOf(width, height)
        if (longest <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / longest.toFloat()
        val newW = (width * ratio).toInt().coerceAtLeast(1)
        val newH = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}
