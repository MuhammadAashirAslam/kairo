package com.example.kairo.domain.parser

/**
 * Structural element of a parsed document.
 */
sealed class StructuralElement {
    data class Heading(val level: Int, val title: String, val rawText: String) : StructuralElement()
    data class Paragraph(val text: String, val sentences: List<String>) : StructuralElement()
    data class ListItem(val text: String) : StructuralElement()
    data class CodeBlock(val language: String, val content: String) : StructuralElement()
}

/**
 * A logical block of text with hierarchical context attached.
 */
data class StructuralBlock(
    val breadcrumb: String,
    val heading: String?,
    val text: String,
    val wordCount: Int,
    val isHeaderBlock: Boolean = false
)

class DocumentStructureParser {

    private val headingRegex = Regex("""^(#{1,6})\s+(.+)$""")

    /**
     * Parses raw document text into hierarchical structural blocks with breadcrumbs.
     */
    fun parse(documentName: String, rawContent: String): List<StructuralBlock> {
        val cleanContent = rawContent.replace("\r\n", "\n").replace("\r", "\n")
        if (cleanContent.isBlank()) return emptyList()

        val lines = cleanContent.lines()
        val blocks = mutableListOf<StructuralBlock>()

        // Stack to track active heading hierarchy [H1, H2, H3...]
        val headingStack = mutableListOf<Pair<Int, String>>()
        val currentParagraphLines = mutableListOf<String>()

        fun currentBreadcrumb(): String {
            if (headingStack.isEmpty()) return documentName
            val path = headingStack.joinToString(" > ") { it.second }
            return "$documentName > $path"
        }

        fun flushParagraph() {
            if (currentParagraphLines.isEmpty()) return
            val paraText = currentParagraphLines.joinToString("\n").trim()
            currentParagraphLines.clear()

            if (paraText.isNotEmpty()) {
                val words = paraText.split(Regex("""\s+""")).filter { it.isNotBlank() }
                blocks.add(
                    StructuralBlock(
                        breadcrumb = currentBreadcrumb(),
                        heading = headingStack.lastOrNull()?.second,
                        text = paraText,
                        wordCount = words.size,
                        isHeaderBlock = false
                    )
                )
            }
        }

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // Check for Markdown '#' headings
            val headingMatch = headingRegex.matchEntire(trimmed)
            if (headingMatch != null) {
                flushParagraph()
                val level = headingMatch.groupValues[1].length
                val title = headingMatch.groupValues[2].trim()

                updateHeadingStack(headingStack, level, title)
                i++
                continue
            }

            // Check for Underline headings (=== for H1, --- for H2)
            if (i + 1 < lines.size && trimmed.isNotBlank()) {
                val nextLine = lines[i + 1].trim()
                if (nextLine.length >= 3 && nextLine.all { it == '=' }) {
                    flushParagraph()
                    updateHeadingStack(headingStack, 1, trimmed)
                    i += 2
                    continue
                } else if (nextLine.length >= 3 && nextLine.all { it == '-' }) {
                    flushParagraph()
                    updateHeadingStack(headingStack, 2, trimmed)
                    i += 2
                    continue
                }
            }

            // Empty line indicates paragraph break
            if (trimmed.isEmpty()) {
                flushParagraph()
            } else {
                currentParagraphLines.add(line)
            }
            i++
        }

        flushParagraph()
        return blocks
    }

    private fun updateHeadingStack(stack: MutableList<Pair<Int, String>>, level: Int, title: String) {
        // Pop headings with greater or equal level
        while (stack.isNotEmpty() && stack.last().first >= level) {
            stack.removeAt(stack.lastIndex)
        }
        stack.add(level to title)
    }

    /**
     * Splits a paragraph into individual clean sentences.
     */
    fun splitIntoSentences(text: String): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()

        // Match sentence boundary: period, question mark, exclamation mark followed by whitespace
        val sentenceRegex = Regex("""(?<=[.!?])\s+(?=[A-Z0-9"'\(\[])""")
        val parts = trimmed.split(sentenceRegex)

        return parts.map { it.trim() }.filter { it.isNotEmpty() }
    }
}
