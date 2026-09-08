package com.example.kairo.domain.retrieval

import com.example.kairo.data.DocumentChunk
import java.util.Locale
import kotlin.math.ln

data class Bm25Result(
    val chunkIndex: Int,
    val score: Float
)

data class TermPosting(
    val chunkIndex: Int,
    val frequency: Int
)

/**
 * High-performance, in-memory Okapi BM25 Lexical Inverted Index Engine.
 * Provides exact keyword, technical identifier, acronym, and numeric search.
 */
class Bm25SearchEngine(
    private val k1: Float = 1.2f,
    private val b: Float = 0.75f
) {
    private var numDocs: Int = 0
    private var avgDocLength: Float = 0f
    private var docLengths: IntArray = IntArray(0)
    private val invertedIndex = mutableMapOf<String, MutableList<TermPosting>>()
    private val idfCache = mutableMapOf<String, Float>()

    private val stopWords = hashSetOf(
        "a", "about", "above", "after", "again", "against", "all", "am", "an", "and",
        "any", "are", "aren't", "as", "at", "be", "because", "been", "before", "being",
        "below", "between", "both", "but", "by", "can't", "cannot", "could", "couldn't",
        "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down", "during",
        "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't", "have",
        "haven't", "having", "he", "he'd", "he'll", "he's", "her", "here", "here's",
        "hers", "herself", "him", "himself", "his", "how", "how's", "i", "i'd", "i'll",
        "i'm", "i've", "if", "in", "into", "is", "isn't", "it", "it's", "its", "itself",
        "let's", "me", "more", "most", "mustn't", "my", "myself", "no", "nor", "not", "of",
        "off", "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves",
        "out", "over", "own", "same", "shan't", "she", "she'd", "she'll", "she's", "should",
        "shouldn't", "so", "some", "such", "than", "that", "that's", "the", "their",
        "theirs", "them", "themselves", "then", "there", "there's", "these", "they",
        "they'd", "they'll", "they're", "they've", "this", "those", "through", "to", "too",
        "under", "until", "up", "very", "was", "wasn't", "we", "we'd", "we'll", "we're",
        "we've", "were", "weren't", "what", "what's", "when", "when's", "where", "where's",
        "which", "while", "who", "who's", "whom", "why", "why's", "with", "won't", "would",
        "wouldn't", "you", "you'd", "you'll", "you're", "you've", "your", "yours",
        "yourself", "yourselves"
    )

    /**
     * Builds the BM25 index over a list of chunks in sub-millisecond time.
     */
    fun indexChunks(chunks: List<DocumentChunk>) {
        invertedIndex.clear()
        idfCache.clear()

        numDocs = chunks.size
        if (numDocs == 0) {
            avgDocLength = 0f
            docLengths = IntArray(0)
            return
        }

        docLengths = IntArray(numDocs)
        var totalLength = 0L

        // Phase 1: Tokenize each document and build postings
        for (i in chunks.indices) {
            val chunk = chunks[i]
            // Index both context breadcrumb and body text
            val textToIndex = "${chunk.breadcrumb} ${chunk.rawText}"
            val tokens = tokenize(textToIndex)
            val docLen = tokens.size
            docLengths[i] = docLen
            totalLength += docLen

            // Count term frequencies in this chunk
            val tfMap = mutableMapOf<String, Int>()
            for (token in tokens) {
                tfMap[token] = (tfMap[token] ?: 0) + 1
            }

            // Append to inverted index
            for ((term, count) in tfMap) {
                val postings = invertedIndex.getOrPut(term) { mutableListOf() }
                postings.add(TermPosting(chunkIndex = i, frequency = count))
            }
        }

        avgDocLength = if (numDocs > 0) totalLength.toFloat() / numDocs else 1f

        // Phase 2: Precompute smoothed IDF for all terms
        for ((term, postings) in invertedIndex) {
            val docFreq = postings.size
            // Probabilistic Okapi BM25 IDF with +0.5 smoothing
            val idf = ln(1.0 + (numDocs - docFreq + 0.5) / (docFreq + 0.5)).toFloat()
            idfCache[term] = idf.coerceAtLeast(0.01f)
        }
    }

    /**
     * Executes BM25 scoring for a search query.
     */
    fun search(query: String, topK: Int = 10): List<Bm25Result> {
        if (numDocs == 0) return emptyList()

        val queryTokens = tokenize(query)
        if (queryTokens.isEmpty()) return emptyList()

        // Accumulate scores for each chunk matching query terms
        val docScores = FloatArray(numDocs)

        for (term in queryTokens) {
            val postings = invertedIndex[term] ?: continue
            val idf = idfCache[term] ?: continue

            for (posting in postings) {
                val docIdx = posting.chunkIndex
                val tf = posting.frequency.toFloat()
                val docLen = docLengths[docIdx].toFloat()

                // Okapi BM25 formula
                val numerator = tf * (k1 + 1.0f)
                val denominator = tf + k1 * (1.0f - b + b * (docLen / avgDocLength))
                val termScore = idf * (numerator / denominator)

                docScores[docIdx] += termScore
            }
        }

        // Collect non-zero scores and sort descending
        val results = mutableListOf<Bm25Result>()
        for (i in 0 until numDocs) {
            if (docScores[i] > 0.0001f) {
                results.add(Bm25Result(chunkIndex = i, score = docScores[i]))
            }
        }

        return results.sortedByDescending { it.score }.take(topK.coerceAtLeast(1))
    }

    /**
     * Fast normalizing tokenizer: converts to lowercase, strips punctuation, and removes stopwords.
     */
    fun tokenize(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        val tokens = mutableListOf<String>()
        val len = text.length
        val sb = StringBuilder()

        for (i in 0 until len) {
            val ch = text[i]
            if (ch.isLetterOrDigit()) {
                sb.append(ch.lowercaseChar())
            } else if (sb.isNotEmpty()) {
                val token = sb.toString()
                sb.setLength(0)
                if (token.length > 1 && !stopWords.contains(token)) {
                    tokens.add(token)
                }
            }
        }

        if (sb.isNotEmpty()) {
            val token = sb.toString()
            if (token.length > 1 && !stopWords.contains(token)) {
                tokens.add(token)
            }
        }

        return tokens
    }
}
