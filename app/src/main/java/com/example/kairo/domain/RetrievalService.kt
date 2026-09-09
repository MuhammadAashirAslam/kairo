package com.example.kairo.domain

import com.example.kairo.data.ChunkStore
import com.example.kairo.data.DocumentChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

enum class MatchType {
    HYBRID,
    KEYWORD,
    SEMANTIC
}

data class ScoredChunk(
    val chunk: DocumentChunk,
    val score: Float,
    val matchType: MatchType = MatchType.HYBRID,
    val bm25Score: Float = 0f,
    val denseScore: Float = 0f
)

/**
 * Production-grade Hybrid Retrieval Engine combining Dense Neural Vector Search
 * and Sparse Okapi BM25 with Reciprocal Rank Fusion (RRF) and MMR Diversity Filtering.
 */
class RetrievalService(
    private val chunkStore: ChunkStore,
    private val embeddingService: EmbeddingService,
    private val rrfConstant: Float = 60f,
    private val mmrLambda: Float = 0.75f
) {

    suspend fun retrieveTopK(
        query: String,
        k: Int = 3,
        minScoreThreshold: Float = 0.05f
    ): List<ScoredChunk> = withContext(Dispatchers.Default) {
        val chunks = chunkStore.getChunks()
        val embeddings = chunkStore.getEmbeddings()
        val bm25Engine = chunkStore.getBm25Engine()

        if (chunks.isEmpty() || embeddings.isEmpty() || chunks.size != embeddings.size) {
            return@withContext emptyList()
        }

        val numDocs = chunks.size

        // 1. Dense Vector Scoring
        val queryVec = embeddingService.generateEmbedding(query)
        val denseScores = FloatArray(numDocs)
        for (i in 0 until numDocs) {
            denseScores[i] = embeddingService.cosineSimilarity(queryVec, embeddings[i])
        }

        // Rank dense results (1-based rank)
        val denseRankedIndices = (0 until numDocs)
            .sortedByDescending { denseScores[it] }

        val denseRankMap = mutableMapOf<Int, Int>()
        for (rank in denseRankedIndices.indices) {
            denseRankMap[denseRankedIndices[rank]] = rank + 1
        }

        // 2. Sparse BM25 Scoring
        val bm25Results = bm25Engine?.search(query, topK = numDocs) ?: emptyList()
        val bm25ScoreMap = bm25Results.associate { it.chunkIndex to it.score }

        val bm25RankMap = mutableMapOf<Int, Int>()
        for (rank in bm25Results.indices) {
            bm25RankMap[bm25Results[rank].chunkIndex] = rank + 1
        }

        // 3. Reciprocal Rank Fusion (RRF)
        val rrfScores = FloatArray(numDocs)
        val matchTypes = Array(numDocs) { MatchType.SEMANTIC }

        for (i in 0 until numDocs) {
            val denseRank = denseRankMap[i]
            val bm25Rank = bm25RankMap[i]

            var score = 0f
            var isDenseHit = false
            var isBm25Hit = false

            if (denseRank != null && denseScores[i] > 0.05f) {
                score += 1.0f / (rrfConstant + denseRank)
                isDenseHit = true
            }

            if (bm25Rank != null && (bm25ScoreMap[i] ?: 0f) > 0.001f) {
                score += 1.0f / (rrfConstant + bm25Rank)
                isBm25Hit = true
            }

            rrfScores[i] = score
            matchTypes[i] = when {
                isDenseHit && isBm25Hit -> MatchType.HYBRID
                isBm25Hit -> MatchType.KEYWORD
                else -> MatchType.SEMANTIC
            }
        }

        // 4. Candidate Pool Selection (Top 3x K for MMR re-ranking)
        val candidatePoolSize = (k * 3).coerceAtMost(numDocs)
        val candidateIndices = (0 until numDocs)
            .sortedByDescending { rrfScores[it] }
            .take(candidatePoolSize)
            .toMutableList()

        if (candidateIndices.isEmpty()) return@withContext emptyList()

        // 5. Maximal Marginal Relevance (MMR) for Diversity
        val selectedIndices = mutableListOf<Int>()
        val maxRrf = rrfScores[candidateIndices[0]].coerceAtLeast(0.0001f)

        while (selectedIndices.size < k && candidateIndices.isNotEmpty()) {
            var bestIdx = -1
            var bestMmrScore = -Float.MAX_VALUE

            for (candIdx in candidateIndices) {
                val relevance = rrfScores[candIdx] / maxRrf

                // Max similarity with already selected items
                var maxRedundancy = 0f
                if (selectedIndices.isNotEmpty()) {
                    val candVec = embeddings[candIdx]
                    for (selIdx in selectedIndices) {
                        val selVec = embeddings[selIdx]
                        val sim = embeddingService.cosineSimilarity(candVec, selVec)
                        if (sim > maxRedundancy) {
                            maxRedundancy = sim
                        }
                    }
                }

                // MMR formula
                val mmr = mmrLambda * relevance - (1.0f - mmrLambda) * maxRedundancy
                if (mmr > bestMmrScore) {
                    bestMmrScore = mmr
                    bestIdx = candIdx
                }
            }

            if (bestIdx != -1) {
                selectedIndices.add(bestIdx)
                candidateIndices.remove(bestIdx)
            } else {
                break
            }
        }

        // Map selected indices to ScoredChunk objects with normalized scores
        val scored = selectedIndices.map { idx ->
            // Raw relevance in 0..1, computed before display coercion; the
            // user-configured similarity threshold must apply to this value.
            val relevance = (rrfScores[idx] / maxRrf) * 0.5f + denseScores[idx].coerceIn(0f, 1f) * 0.5f

            ScoredChunk(
                chunk = chunks[idx],
                // Normalize score to intuitive 0.0 - 1.0 confidence range
                score = relevance.coerceIn(0.1f, 0.99f),
                matchType = matchTypes[idx],
                bm25Score = bm25ScoreMap[idx] ?: 0f,
                denseScore = denseScores[idx]
            ) to relevance
        }

        val filtered = scored.filter { it.second >= minScoreThreshold }.map { it.first }

        // Never return empty-handed when an index exists: keep the single best
        // chunk as grounding fallback even if nothing clears the threshold.
        if (filtered.isEmpty() && scored.isNotEmpty()) listOf(scored.first().first) else filtered
    }
}
