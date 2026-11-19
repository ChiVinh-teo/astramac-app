package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

sealed interface IngestedMaterial {
    data class TextMaterial(
        val fileName: String,
        val text: String
    ) : IngestedMaterial

    data class PdfMaterial(
        val fileName: String,
        val pdfBytes: ByteArray
    ) : IngestedMaterial

    data class ImageMaterial(
        val fileName: String,
        val bitmap: Bitmap
    ) : IngestedMaterial
}

object DocumentIngestionHelper {
    private const val MAX_INLINE_BYTES = 18 * 1024 * 1024 // < 20MB limit
    private const val MAX_TEXT_CHARS = 40_000

    suspend fun loadBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 1600): Bitmap =
        withContext(Dispatchers.IO) {
            val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = false
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                } ?: throw IllegalStateException("Không thể đọc ảnh đã chọn.")
            }

            val width = rawBitmap.width
            val height = rawBitmap.height
            val longest = max(width, height)
            val scaled = if (longest > maxDimension) {
                val ratio = maxDimension.toFloat() / longest.toFloat()
                Bitmap.createScaledBitmap(
                    rawBitmap,
                    (width * ratio).toInt().coerceAtLeast(1),
                    (height * ratio).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                rawBitmap
            }

            // Compress/re-verify memory size
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            if (out.size() > MAX_INLINE_BYTES) {
                val smaller = Bitmap.createScaledBitmap(
                    scaled,
                    (scaled.width * 0.6f).toInt().coerceAtLeast(1),
                    (scaled.height * 0.6f).toInt().coerceAtLeast(1),
                    true
                )
                return@withContext smaller
            }
            scaled
        }

    suspend fun readDocumentFromUri(context: Context, uri: Uri): IngestedMaterial =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri).orEmpty()
            val fileName = resolveFileName(context, uri)

            if (mimeType.startsWith("image/")) {
                val bmp = loadBitmapFromUri(context, uri)
                return@withContext IngestedMaterial.ImageMaterial(fileName, bmp)
            }

            val bytes = resolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArrayOutputStream()
                val chunk = ByteArray(8192)
                var total = 0
                while (true) {
                    val read = input.read(chunk)
                    if (read <= 0) break
                    total += read
                    if (total > MAX_INLINE_BYTES) {
                        throw IllegalArgumentException(
                            "Tệp tài liệu vượt quá giới hạn 18MB. Vui lòng chọn tệp nhỏ hơn hoặc tách chương."
                        )
                    }
                    buffer.write(chunk, 0, read)
                }
                buffer.toByteArray()
            } ?: throw IllegalStateException("Không thể mở tệp tài liệu đã chọn.")

            if (mimeType == "application/pdf" || fileName.endsWith(".pdf", ignoreCase = true)) {
                return@withContext IngestedMaterial.PdfMaterial(fileName, bytes)
            }

            val decodedText = bytes.toString(Charsets.UTF_8).trim()
            if (decodedText.isBlank()) {
                throw IllegalArgumentException("Tệp văn bản trống hoặc không hỗ trợ mã hóa.")
            }
            IngestedMaterial.TextMaterial(fileName, decodedText.take(MAX_TEXT_CHARS))
        }

    fun resolveFileName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        return uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { "Tài liệu" } ?: "Tài liệu"
    }
}
