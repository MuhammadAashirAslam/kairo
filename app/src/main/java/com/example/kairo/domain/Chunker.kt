package com.example.kairo.domain

import com.example.kairo.data.DocumentChunk
import com.example.kairo.domain.parser.DocumentStructureParser
import com.example.kairo.domain.parser.StructuralBlock

/**
 * Production-grade Structure-Aware & Contextual Chunker.
 * Preserves paragraph and sentence boundaries while enriching each chunk
 * with its structural breadcrumb (Anthropic Contextual Retrieval SOTA).
 */
class Chunker(
    private val targetChunkWords: Int = 250,
    private val overlapWords: Int = 50,
    private val minChunkWords: Int = 40,
    private val maxChunkWords: Int = 380,
    private val parser: DocumentStructureParser = DocumentStructureParser()
) {

    /**
     * Chunk document using structural outline and contextual breadcrumbs.
     */
    fun chunkDocument(
        documentName: String,
        text: String,
        docIdPrefix: String = "chk"
    ): List<DocumentChunk> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()

        val blocks = parser.parse(documentName = documentName, rawContent = trimmed)
        if (blocks.isEmpty()) return emptyList()

        val chunks = mutableListOf<DocumentChunk>()
        var chunkIndex = 0

        var currentSentences = mutableListOf<String>()
        var currentWords = 0
        var currentBreadcrumb = blocks.firstOrNull()?.breadcrumb ?: documentName
        var currentHeading = blocks.firstOrNull()?.heading

        fun flushCurrentChunk() {
            if (currentSentences.isEmpty()) return
            val raw = currentSentences.joinToString(" ").trim()
            currentSentences.clear()
            val wordsInChunk = currentWords
            currentWords = 0

            if (raw.isNotEmpty()) {
                val enrichedText = if (currentBreadcrumb.isNotBlank()) {
                    "[Context: $currentBreadcrumb]\n$raw"
                } else {
                    raw
                }

                chunks.add(
                    DocumentChunk(
                        id = "${docIdPrefix}_$chunkIndex",
                        chunkIndex = chunkIndex,
                        text = enrichedText,
                        rawText = raw,
                        breadcrumb = currentBreadcrumb,
                        heading = currentHeading,
                        wordCount = wordsInChunk
                    )
                )
                chunkIndex++
            }
        }

        for (block in blocks) {
            val sentences = parser.splitIntoSentences(block.text)

            // If switching section heading/breadcrumb, flush accumulated sentences to keep chunks section-pure
            if (currentSentences.isNotEmpty() && currentBreadcrumb != block.breadcrumb) {
                flushCurrentChunk()
            }

            currentBreadcrumb = block.breadcrumb
            currentHeading = block.heading

            for (sentence in sentences) {
                val sentenceWords = sentence.split(Regex("""\s+""")).filter { it.isNotBlank() }.size

                // If adding this sentence exceeds maxChunkWords and we already have enough content, flush
                if (currentWords + sentenceWords > maxChunkWords && currentWords >= minChunkWords) {
                    flushCurrentChunk()
                }

                currentSentences.add(sentence)
                currentWords += sentenceWords

                // If target reached, flush
                if (currentWords >= targetChunkWords) {
                    flushCurrentChunk()
                }
            }
        }

        flushCurrentChunk()

        // Fallback safety: if no chunks were formed for any reason, wrap full text
        if (chunks.isEmpty() && trimmed.isNotEmpty()) {
            val words = trimmed.split(Regex("""\s+""")).filter { it.isNotBlank() }
            chunks.add(
                DocumentChunk(
                    id = "${docIdPrefix}_0",
                    chunkIndex = 0,
                    text = "[Context: $documentName]\n$trimmed",
                    rawText = trimmed,
                    breadcrumb = documentName,
                    heading = null,
                    wordCount = words.size
                )
            )
        }

        return chunks
    }

    /**
     * Backward-compatible overload for generic text without document title.
     */
    fun chunkText(text: String, docIdPrefix: String = "chk"): List<DocumentChunk> {
        return chunkDocument(documentName = "Document", text = text, docIdPrefix = docIdPrefix)
    }
}
