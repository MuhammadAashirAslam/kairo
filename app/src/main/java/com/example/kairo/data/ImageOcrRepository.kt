package com.example.kairo.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private suspend fun <T> Task<T>.awaitTask(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) continuation.resume(result)
        }
        addOnFailureListener { error ->
            if (continuation.isActive) continuation.resumeWithException(error)
        }
        addOnCanceledListener {
            continuation.cancel()
        }
    }

data class ExtractedImageText(
    val imageName: String,
    val fullText: String,
    val lineCount: Int,
    val blockCount: Int,
    val uriString: String? = null
)

class ImageOcrRepository {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun extractTextFromUri(context: Context, uri: Uri, displayName: String = "Scanned Image"): Result<ExtractedImageText> =
        withContext(Dispatchers.IO) {
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                val visionText = recognizer.process(inputImage).awaitTask()

                val rawText = visionText.text.trim()
                if (rawText.isBlank()) {
                    return@withContext Result.failure(
                        Exception("No text could be recognized in the selected image. Please ensure the image contains visible text.")
                    )
                }

                // Format text with markdown section header for RAG ingestion
                val structuredContent = buildString {
                    appendLine("# Image Document: $displayName")
                    appendLine()
                    visionText.textBlocks.forEach { block ->
                        appendLine(block.text.trim())
                        appendLine()
                    }
                }.trim()

                val totalLines = visionText.textBlocks.sumOf { it.lines.size }

                Result.success(
                    ExtractedImageText(
                        imageName = displayName,
                        fullText = structuredContent,
                        lineCount = totalLines,
                        blockCount = visionText.textBlocks.size,
                        uriString = uri.toString()
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun extractTextFromBitmap(bitmap: Bitmap, displayName: String = "Camera Photo"): Result<ExtractedImageText> =
        withContext(Dispatchers.IO) {
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val visionText = recognizer.process(inputImage).awaitTask()

                val rawText = visionText.text.trim()
                if (rawText.isBlank()) {
                    return@withContext Result.failure(
                        Exception("No text could be recognized in the captured photo. Please ensure the camera is focused on visible text.")
                    )
                }

                val structuredContent = buildString {
                    appendLine("# Camera Capture: $displayName")
                    appendLine()
                    visionText.textBlocks.forEach { block ->
                        appendLine(block.text.trim())
                        appendLine()
                    }
                }.trim()

                val totalLines = visionText.textBlocks.sumOf { it.lines.size }

                Result.success(
                    ExtractedImageText(
                        imageName = displayName,
                        fullText = structuredContent,
                        lineCount = totalLines,
                        blockCount = visionText.textBlocks.size,
                        uriString = null
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
