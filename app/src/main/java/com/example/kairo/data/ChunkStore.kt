package com.example.kairo.data

import com.example.kairo.domain.retrieval.Bm25SearchEngine

data class DocumentChunk(
    val id: String,
    val chunkIndex: Int,
    val text: String,
    val rawText: String = text,
    val breadcrumb: String = "",
    val heading: String? = null,
    val wordCount: Int,
    val startCharOffset: Int = 0
)

data class LoadedDocumentMetadata(
    val name: String,
    val sizeBytes: Long,
    val characterCount: Int,
    val chunkCount: Int,
    val loadedTimestamp: Long = System.currentTimeMillis()
)

class ChunkStore {
    private var metadata: LoadedDocumentMetadata? = null
    private val chunks = mutableListOf<DocumentChunk>()
    private val embeddings = mutableListOf<FloatArray>()
    private var bm25Engine: Bm25SearchEngine? = null

    @Synchronized
    fun saveDocument(
        metadata: LoadedDocumentMetadata,
        newChunks: List<DocumentChunk>,
        newEmbeddings: List<FloatArray>,
        bm25Engine: Bm25SearchEngine? = null
    ) {
        this.metadata = metadata
        this.chunks.clear()
        this.chunks.addAll(newChunks)
        this.embeddings.clear()
        this.embeddings.addAll(newEmbeddings)
        this.bm25Engine = bm25Engine
    }

    @Synchronized
    fun getDocumentMetadata(): LoadedDocumentMetadata? = metadata

    @Synchronized
    fun getChunks(): List<DocumentChunk> = chunks.toList()

    @Synchronized
    fun getEmbeddings(): List<FloatArray> = embeddings.toList()

    @Synchronized
    fun getBm25Engine(): Bm25SearchEngine? = bm25Engine

    @Synchronized
    fun isDocumentLoaded(): Boolean = metadata != null && chunks.isNotEmpty()

    @Synchronized
    fun getChunkCount(): Int = chunks.size

    @Synchronized
    fun getDocumentName(): String? = metadata?.name

    @Synchronized
    fun clear() {
        metadata = null
        chunks.clear()
        embeddings.clear()
        bm25Engine = null
    }
}
