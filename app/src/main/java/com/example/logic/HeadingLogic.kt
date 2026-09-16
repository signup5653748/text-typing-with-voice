package com.example.logic

import kotlin.math.min

data class HeadingItem(
    val lineNumber: Int,
    val level: Int,
    val symbolsText: String,
    val headingText: String,
    val lineStartOffset: Int,
    val textStartOffset: Int
)

object HeadingLogic {
    // Unicode white small square '\u25AB' and other square markers
    private val HEADING_SQUARE_CHARS = charArrayOf(
        '\u25AB', // ▫ White small square
        '\u25FD', // ◽ White medium small square
        '\u25FB', // ◻ White medium square
        '\u25A1', // □ White square
        '\u2B1C'  // ⬜ White large square
    )

    private const val VARIATION_SELECTOR_16 = '\uFE0F'

    fun isHeadingSquareChar(c: Char): Boolean {
        return c in HEADING_SQUARE_CHARS
    }

    /**
     * Inspects the start of a line to determine if it is a heading.
     * Returns Pair(level, symbolCharLength) where level is the number of square symbols (up to 15),
     * and symbolCharLength is the total characters (including variation selectors) occupied by the symbols.
     */
    fun getHeadingLevelAndLength(line: CharSequence): Pair<Int, Int> {
        var level = 0
        var index = 0
        val length = line.length

        while (index < length && level < 15) {
            val c = line[index]
            if (isHeadingSquareChar(c)) {
                level++
                index++
                // Skip optional variation selector \uFE0F
                if (index < length && line[index] == VARIATION_SELECTOR_16) {
                    index++
                }
            } else {
                break
            }
        }

        return if (level > 0) Pair(level, index) else Pair(0, 0)
    }

    /**
     * Strips leading heading symbols (and one optional trailing space) from a single line.
     */
    fun stripLeadingSymbols(line: String): String {
        val (level, symbolLength) = getHeadingLevelAndLength(line)
        if (level == 0) return line
        var start = symbolLength
        // Also strip leading whitespace after symbols if present
        while (start < line.length && (line[start] == ' ' || line[start] == '\t')) {
            start++
        }
        return line.substring(start)
    }

    /**
     * Strips leading heading symbols from all lines in a block of text for TTS playback.
     */
    fun stripHeadingSymbolsForTTS(text: String): String {
        if (text.isEmpty()) return text
        val lines = text.split("\n")
        return lines.joinToString("\n") { line ->
            stripLeadingSymbols(line)
        }
    }

    /**
     * Replaces heading square symbols and variation selectors with spaces so that
     * the text length and character indices match the original document 1:1, ensuring
     * TTS spoken word highlights line up with exact document coordinates.
     */
    fun maskHeadingSymbolsForTTS(text: String): String {
        if (text.isEmpty()) return text
        val chars = text.toCharArray()
        for (i in chars.indices) {
            val c = chars[i]
            if (isHeadingSquareChar(c) || c == VARIATION_SELECTOR_16) {
                chars[i] = ' '
            }
        }
        return String(chars)
    }

    /**
     * Scans the document text line by line and collects all heading items.
     * Returns a list of HeadingItem with line numbers, levels (1..15), symbols, and text.
     */
    fun scanHeadings(text: String): List<HeadingItem> {
        if (text.isEmpty()) return emptyList()

        val result = mutableListOf<HeadingItem>()
        var lineStart = 0
        var lineNumber = 1
        val len = text.length

        while (lineStart <= len) {
            val nextNewline = text.indexOf('\n', startIndex = lineStart)
            val lineEnd = if (nextNewline >= 0) nextNewline else len
            val line = text.substring(lineStart, lineEnd)

            val (level, symbolLength) = getHeadingLevelAndLength(line)
            if (level > 0) {
                val symbols = line.substring(0, min(symbolLength, line.length))
                var contentStartInLine = symbolLength
                while (contentStartInLine < line.length && (line[contentStartInLine] == ' ' || line[contentStartInLine] == '\t')) {
                    contentStartInLine++
                }
                val headingContent = line.substring(contentStartInLine).trimEnd()

                result.add(
                    HeadingItem(
                        lineNumber = lineNumber,
                        level = level.coerceIn(1, 15),
                        symbolsText = "▫".repeat(level.coerceIn(1, 15)),
                        headingText = headingContent.ifBlank { "Heading $lineNumber" },
                        lineStartOffset = lineStart,
                        textStartOffset = lineStart + contentStartInLine
                    )
                )
            }

            if (nextNewline < 0) break
            lineStart = nextNewline + 1
            lineNumber++
        }

        return result
    }
}
