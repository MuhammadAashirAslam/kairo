package com.example.kairo.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class DocumentData(
    val name: String,
    val sizeBytes: Long,
    val content: String,
    val characterCount: Int = content.length
)

class DocumentRepository(private val context: Context) {

    suspend fun readDocument(uri: Uri): Result<DocumentData> = withContext(Dispatchers.IO) {
        try {
            var fileName = "document.txt"
            var fileSize = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            val mimeType = context.contentResolver.getType(uri).orEmpty()
            val isPdf = fileName.lowercase().endsWith(".pdf") || mimeType.contains("pdf", ignoreCase = true)

            val content = if (isPdf) {
                readPdfContent(uri)
            } else {
                readTextContent(uri)
            }

            if (content.isEmpty()) {
                return@withContext Result.failure(Exception("The selected file is empty"))
            }

            if (fileSize == 0L) {
                fileSize = content.toByteArray(Charsets.UTF_8).size.toLong()
            }

            Result.success(
                DocumentData(
                    name = fileName,
                    sizeBytes = fileSize,
                    content = content
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun readPdfContent(uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open PDF file input stream")

        return inputStream.use { stream ->
            PDDocument.load(stream).use { document ->
                val numPages = document.numberOfPages
                if (numPages == 0) {
                    throw Exception("The PDF document has no pages")
                }

                val sb = StringBuilder()
                val stripper = PDFTextStripper()

                for (page in 1..numPages) {
                    stripper.startPage = page
                    stripper.endPage = page
                    val pageText = stripper.getText(document).trim()

                    if (pageText.isNotEmpty()) {
                        sb.append("# Page ").append(page).append("\n")
                        sb.append(pageText).append("\n\n")
                    }
                }

                val extracted = sb.toString().trim()
                if (extracted.isEmpty()) {
                    throw Exception("No text could be extracted from this PDF (it may contain scanned image pages or be password-protected)")
                }
                extracted
            }
        }
    }

    private fun readTextContent(uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open file input stream")

        return inputStream.use { stream ->
            val sb = StringBuilder()
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    sb.append(line).append("\n")
                    line = reader.readLine()
                }
            }
            sb.toString().trim()
        }
    }
}
