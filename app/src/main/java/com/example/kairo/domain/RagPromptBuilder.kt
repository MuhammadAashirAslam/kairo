package com.example.kairo.domain

import com.example.kairo.data.SystemPersona

class RagPromptBuilder {

    fun getPersonaInstructions(persona: SystemPersona, customPrompt: String = ""): String {
        return when (persona) {
            SystemPersona.GENERAL ->
                "You are Kairo, a helpful, secure, and privacy-first on-device AI assistant running locally on Android.\nAnswer the user's question directly, clearly, and completely. When providing code or step-by-step instructions, output the entire solution without cutting off prematurely."

            SystemPersona.CLINICAL ->
                "You are Kairo Clinical, an on-device medical consultation assistant.\nYou assist physicians and clinicians by synthesizing patient notes, medical records, symptoms, and clinical data.\nProvide structured, objective insights, highlight potential contraindications or considerations, and ground all medical facts strictly in the provided data."

            SystemPersona.CODING ->
                "You are Kairo Code, an expert software engineering assistant.\nYou provide clean, robust, and well-structured code solutions with concise architectural explanations. Always output complete implementations without skipping necessary code."

            SystemPersona.RESEARCH ->
                "You are Kairo Analyst, a document research and synthesis assistant.\nYou provide deep, analytical summaries and extract key data points, statistics, and conclusions with rigorous fidelity to the source materials."

            SystemPersona.CUSTOM ->
                if (customPrompt.isNotBlank()) customPrompt.trim()
                else "You are Kairo, a helpful, secure, and privacy-first on-device AI assistant running locally on Android."
        }
    }

    fun buildPrompt(
        userQuestion: String,
        retrievedChunks: List<ScoredChunk>,
        hasLoadedDocument: Boolean = true,
        persona: SystemPersona = SystemPersona.GENERAL,
        customPrompt: String = "",
        additionalContext: String? = null
    ): String {
        val personaInstructions = getPersonaInstructions(persona, customPrompt)

        // Optional image/OCR context attached directly to the prompt. This is
        // independent of the document index, so image text grounds the answer
        // even while background ingestion is still running.
        val ocrSection = additionalContext?.trim().takeUnless { it.isNullOrEmpty() }
            ?.let { "Attached image text (on-device OCR):\n$it\n\n" } ?: ""

        if (!hasLoadedDocument) {
            return """
$personaInstructions

${ocrSection}Question: ${userQuestion.trim()}

Answer:
""".trimIndent()
        }

        val contextSection = if (retrievedChunks.isEmpty()) {
            "(No relevant document context found in the loaded document.)"
        } else {
            retrievedChunks.mapIndexed { index, scored ->
                val sourceLabel = if (scored.chunk.breadcrumb.isNotBlank()) {
                    "[Source ${index + 1} | ${scored.chunk.breadcrumb}]"
                } else {
                    "[Source ${index + 1}]"
                }
                "$sourceLabel\n${scored.chunk.rawText.trim()}"
            }.joinToString("\n\n")
        }

        return """
$personaInstructions

Grounding Rules:
1. Base your answer ONLY on the provided context sources below.
2. Cite the source whenever stating facts (e.g., "[Source 1]").
3. Be direct, clear, and complete. If providing code, analysis, or step-by-step instructions, provide the full response without cutting off prematurely.
4. If the answer cannot be determined from the context, state clearly that the document does not contain this information — do not guess or fabricate.

Context:
$contextSection

${ocrSection}Question: ${userQuestion.trim()}

Answer:
""".trimIndent()
    }
}
