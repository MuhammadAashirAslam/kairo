package com.example.kairo

import com.example.kairo.data.DocumentChunk
import com.example.kairo.data.SystemPersona
import com.example.kairo.domain.MatchType
import com.example.kairo.domain.RagPromptBuilder
import com.example.kairo.domain.ScoredChunk
import org.junit.Assert.assertTrue
import org.junit.Test

class RagPromptBuilderTest {

    private val builder = RagPromptBuilder()

    @Test
    fun testGeneralPersonaPrompt_containsExpectedInstructions() {
        val prompt = builder.buildPrompt(
            userQuestion = "What is on-device AI?",
            retrievedChunks = emptyList(),
            hasLoadedDocument = false,
            persona = SystemPersona.GENERAL
        )

        assertTrue(prompt.contains("helpful, secure, and privacy-first on-device AI assistant"))
        assertTrue(prompt.contains("Question: What is on-device AI?"))
        assertTrue(prompt.contains("Answer:"))
    }

    @Test
    fun testClinicalConsultationPersonaPrompt_containsMedicalHarnessRules() {
        val prompt = builder.buildPrompt(
            userQuestion = "What are the patient's reported symptoms and medication history?",
            retrievedChunks = emptyList(),
            hasLoadedDocument = false,
            persona = SystemPersona.CLINICAL
        )

        assertTrue(prompt.contains("Kairo Clinical"))
        assertTrue(prompt.contains("medical consultation assistant"))
        assertTrue(prompt.contains("synthesizing patient notes"))
    }

    @Test
    fun testCodingPersonaPrompt_containsEngineeringInstructions() {
        val prompt = builder.buildPrompt(
            userQuestion = "Write a Kotlin coroutine flow handler.",
            retrievedChunks = emptyList(),
            hasLoadedDocument = false,
            persona = SystemPersona.CODING
        )

        assertTrue(prompt.contains("Kairo Code"))
        assertTrue(prompt.contains("software engineering assistant"))
        assertTrue(prompt.contains("clean, robust, and well-structured code"))
    }

    @Test
    fun testResearchPersonaPrompt_containsAnalystInstructions() {
        val prompt = builder.buildPrompt(
            userQuestion = "Summarize the primary statistical findings.",
            retrievedChunks = emptyList(),
            hasLoadedDocument = false,
            persona = SystemPersona.RESEARCH
        )

        assertTrue(prompt.contains("Kairo Analyst"))
        assertTrue(prompt.contains("document research and synthesis"))
        assertTrue(prompt.contains("rigorous fidelity to the source materials"))
    }

    @Test
    fun testCustomPersonaPrompt_usesProvidedCustomInstructions() {
        val customText = "You are a specialized legal contract auditor. Verify all clause liabilities strictly."
        val prompt = builder.buildPrompt(
            userQuestion = "Is there an indemnification clause?",
            retrievedChunks = emptyList(),
            hasLoadedDocument = false,
            persona = SystemPersona.CUSTOM,
            customPrompt = customText
        )

        assertTrue(prompt.contains(customText))
        assertTrue(prompt.contains("Question: Is there an indemnification clause?"))
    }

    @Test
    fun testPromptWithDocumentContext_formatsCitationsAndGroundingRules() {
        val chunk1 = DocumentChunk(
            id = "doc_c0",
            chunkIndex = 0,
            text = "Patient was prescribed Metformin 500mg BID with no adverse allergic reactions noted.",
            wordCount = 12,
            breadcrumb = "Clinical Notes > Medication History"
        )
        val chunk2 = DocumentChunk(
            id = "doc_c1",
            chunkIndex = 1,
            text = "Blood pressure measured at 122/78 mmHg during routine triage.",
            wordCount = 9,
            breadcrumb = "Vitals > Triaged Assessment"
        )

        val scoredChunks = listOf(
            ScoredChunk(chunk = chunk1, score = 0.92f, matchType = MatchType.HYBRID),
            ScoredChunk(chunk = chunk2, score = 0.81f, matchType = MatchType.SEMANTIC)
        )

        val prompt = builder.buildPrompt(
            userQuestion = "What medication is the patient taking?",
            retrievedChunks = scoredChunks,
            hasLoadedDocument = true,
            persona = SystemPersona.CLINICAL
        )

        assertTrue(prompt.contains("Grounding Rules:"))
        assertTrue(prompt.contains("Base your answer ONLY on the provided context sources below."))
        assertTrue(prompt.contains("[Source 1 | Clinical Notes > Medication History]"))
        assertTrue(prompt.contains("Metformin 500mg BID"))
        assertTrue(prompt.contains("[Source 2 | Vitals > Triaged Assessment]"))
        assertTrue(prompt.contains("Blood pressure measured at 122/78 mmHg"))
        assertTrue(prompt.contains("Question: What medication is the patient taking?"))
    }
}
