package com.example.kairo

import com.example.kairo.data.ChunkStore
import com.example.kairo.data.DocumentChunk
import com.example.kairo.data.LoadedDocumentMetadata
import com.example.kairo.domain.Chunker
import com.example.kairo.domain.EmbeddingService
import com.example.kairo.domain.MatchType
import com.example.kairo.domain.RagPromptBuilder
import com.example.kairo.domain.RetrievalService
import com.example.kairo.domain.parser.DocumentStructureParser
import com.example.kairo.domain.retrieval.Bm25SearchEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HybridRetrievalTest {

    @Test
    fun testDocumentStructureParser_extractsHeadingsAndBreadcrumbs() {
        val parser = DocumentStructureParser()
        val markdown = """
            # Kairo Architecture
            Kairo is designed for on-device RAG on mobile platforms.
            
            ## Retrieval Engine
            The retrieval engine combines dense vector search with sparse BM25 indexing.
            
            ### BM25 Component
            Okapi BM25 scores exact terms and technical acronyms with high precision.
        """.trimIndent()

        val blocks = parser.parse("ArchitectureDoc.md", markdown)
        assertTrue(blocks.isNotEmpty())

        val bm25Block = blocks.find { it.text.contains("Okapi BM25") }
        assertNotNull(bm25Block)
        assertEquals(
            "ArchitectureDoc.md > Kairo Architecture > Retrieval Engine > BM25 Component",
            bm25Block?.breadcrumb
        )
    }

    @Test
    fun testChunker_enrichesContextualBreadcrumbs() {
        val chunker = Chunker(targetChunkWords = 100)
        val markdown = """
            # Executive Summary
            Company revenue grew by 35% in Q3 reaching $4.5M.
            Operating expenses were reduced by 12% across all regional sectors.
            
            # Technical Specifications
            System utilizes arm64 NEON SIMD hardware acceleration for low-latency inference.
        """.trimIndent()

        val chunks = chunker.chunkDocument("Report.md", markdown)
        assertTrue(chunks.size >= 2)

        val execChunk = chunks.find { it.rawText.contains("Company revenue") }
        assertNotNull(execChunk)
        assertTrue(execChunk!!.text.startsWith("[Context: Report.md > Executive Summary]"))

        val techChunk = chunks.find { it.rawText.contains("SIMD") }
        assertNotNull(techChunk)
        assertTrue(techChunk!!.text.startsWith("[Context: Report.md > Technical Specifications]"))
    }

    @Test
    fun testBm25SearchEngine_exactKeywordAndAcronymLookup() {
        val engine = Bm25SearchEngine()
        val chunks = listOf(
            DocumentChunk(
                id = "c0",
                chunkIndex = 0,
                text = "General overview of neural network architectures and multi-layer perceptrons.",
                rawText = "General overview of neural network architectures and multi-layer perceptrons.",
                breadcrumb = "Overview",
                wordCount = 10
            ),
            DocumentChunk(
                id = "c1",
                chunkIndex = 1,
                text = "Error code ERR_7702 occurs when the on-device NPU thermal budget exceeds limits.",
                rawText = "Error code ERR_7702 occurs when the on-device NPU thermal budget exceeds limits.",
                breadcrumb = "Troubleshooting",
                wordCount = 12
            ),
            DocumentChunk(
                id = "c2",
                chunkIndex = 2,
                text = "Financial statement for year 2026 showing capital expenditures and EBITDA figures.",
                rawText = "Financial statement for year 2026 showing capital expenditures and EBITDA figures.",
                breadcrumb = "Financials",
                wordCount = 11
            )
        )

        engine.indexChunks(chunks)

        // Query for unique error code
        val results = engine.search("ERR_7702", topK = 3)
        assertTrue(results.isNotEmpty())
        assertEquals(1, results.first().chunkIndex) // chunk index 1 has ERR_7702
        assertTrue(results.first().score > 0f)

        // Query for financial acronym
        val ebitdaResults = engine.search("EBITDA", topK = 3)
        assertTrue(ebitdaResults.isNotEmpty())
        assertEquals(2, ebitdaResults.first().chunkIndex) // chunk index 2 has EBITDA
    }

    @Test
    fun testHybridRetrievalService_fusesDenseAndSparseWithRRF() = runBlocking {
        val chunkStore = ChunkStore()
        val embeddingService = EmbeddingService()
        val retrievalService = RetrievalService(chunkStore, embeddingService)

        val chunks = listOf(
            DocumentChunk(
                id = "c0",
                chunkIndex = 0,
                text = "[Context: Guide > Setup]\nInstall Android Studio Hedgehog or Ladybug with JDK 17.",
                rawText = "Install Android Studio Hedgehog or Ladybug with JDK 17.",
                breadcrumb = "Guide > Setup",
                wordCount = 9
            ),
            DocumentChunk(
                id = "c1",
                chunkIndex = 1,
                text = "[Context: Guide > Config]\nSet android.useAndroidX=true and enable Jetpack Compose.",
                rawText = "Set android.useAndroidX=true and enable Jetpack Compose.",
                breadcrumb = "Guide > Config",
                wordCount = 6
            )
        )

        val embeddings = embeddingService.generateEmbeddings(chunks.map { it.text })
        val bm25Engine = Bm25SearchEngine()
        bm25Engine.indexChunks(chunks)

        chunkStore.saveDocument(
            metadata = LoadedDocumentMetadata("Guide.md", 1000L, 500, chunks.size),
            newChunks = chunks,
            newEmbeddings = embeddings,
            bm25Engine = bm25Engine
        )

        // Test hybrid retrieval
        val retrieved = retrievalService.retrieveTopK("Which JDK version is required for Android Studio?", k = 2)
        assertTrue(retrieved.isNotEmpty())
        assertEquals(0, retrieved.first().chunkIndex())
        assertTrue(retrieved.first().score > 0f)
    }

    @Test
    fun testRagPromptBuilder_formatsBreadcrumbsAndCitations() {
        val promptBuilder = RagPromptBuilder()
        val chunk = DocumentChunk(
            id = "c1",
            chunkIndex = 0,
            text = "[Context: Doc > Architecture]\nCore memory allocation is managed via arena pool.",
            rawText = "Core memory allocation is managed via arena pool.",
            breadcrumb = "Doc > Architecture",
            wordCount = 8
        )
        val scored = com.example.kairo.domain.ScoredChunk(
            chunk = chunk,
            score = 0.94f,
            matchType = MatchType.HYBRID
        )

        val prompt = promptBuilder.buildPrompt(
            userQuestion = "How is memory managed?",
            retrievedChunks = listOf(scored),
            hasLoadedDocument = true
        )

        assertTrue(prompt.contains("[Source 1 | Doc > Architecture]"))
        assertTrue(prompt.contains("Core memory allocation is managed via arena pool."))
        assertTrue(prompt.contains("How is memory managed?"))
    }

    @Test
    fun testPdfTextExtraction_extractsPageStreams() {
        if (!com.tom_roush.pdfbox.android.PDFBoxResourceLoader.isReady()) {
            // PDFBoxResourceLoader requires Android Context assets, which are initialized in KairoApp.onCreate() at runtime
            return
        }
        // Create an in-memory PDF document
        val doc = com.tom_roush.pdfbox.pdmodel.PDDocument()
        val page = com.tom_roush.pdfbox.pdmodel.PDPage()
        doc.addPage(page)

        val contentStream = com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, page)
        contentStream.beginText()
        contentStream.setFont(com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 12f)
        contentStream.showText("Offline RAG on mobile using PDFBox text stripper")
        contentStream.endText()
        contentStream.close()

        val baos = java.io.ByteArrayOutputStream()
        doc.save(baos)
        doc.close()

        // Test extraction
        val loadedDoc = com.tom_roush.pdfbox.pdmodel.PDDocument.load(java.io.ByteArrayInputStream(baos.toByteArray()))
        val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
        val extractedText = stripper.getText(loadedDoc).trim()
        loadedDoc.close()

        assertTrue(extractedText.contains("Offline RAG on mobile"))
        assertTrue(extractedText.contains("PDFBox text stripper"))
    }

    private fun com.example.kairo.domain.ScoredChunk.chunkIndex(): Int = this.chunk.chunkIndex
}
