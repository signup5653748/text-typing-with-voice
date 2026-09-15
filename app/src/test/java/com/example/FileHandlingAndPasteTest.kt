package com.example

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.data.ActionButton
import com.example.logic.DocumentFileHandler
import com.example.logic.SupportedFileType
import com.example.logic.TextActionLogic
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileHandlingAndPasteTest {

    @Test
    fun paste_preservesMultilineAndParagraphs_fromVariousSources() {
        val initialText = "Hello World"
        val currentValue = TextFieldValue(text = initialText, selection = TextRange(5, 5)) // cursor between Hello and World

        // 1. Multiline text with standard \n (notes / chat app)
        val multilineFromNotes = "\nParagraph 1\n\nParagraph 2\n"
        val result1 = TextActionLogic.handleAction(
            action = ActionButton.PASTE,
            currentValue = currentValue,
            clipboardText = multilineFromNotes
        )
        assertEquals("Hello\nParagraph 1\n\nParagraph 2\n World", result1.text)

        // 2. Multiline text with Windows CRLF (\r\n) normalized to \n
        val multilineFromWindows = "Line A\r\nLine B\r\n\r\nLine C"
        val normalized = multilineFromWindows.replace("\r\n", "\n").replace("\r", "\n")
        val result2 = TextActionLogic.handleAction(
            action = ActionButton.PASTE,
            currentValue = TextFieldValue(text = "", selection = TextRange(0, 0)),
            clipboardText = normalized
        )
        assertEquals("Line A\nLine B\n\nLine C", result2.text)
    }

    @Test
    fun txtAndMd_roundTrip_preservesExactFormattingAndNewlines() {
        val sampleText = "# Title\n\n- Item 1\n- Item 2\n\n▫️ Heading with symbol\nFinal line."
        
        // Test .txt
        val txtOut = ByteArrayOutputStream()
        DocumentFileHandler.writeDocument(sampleText, txtOut, SupportedFileType.TXT)
        val txtIn = ByteArrayInputStream(txtOut.toByteArray())
        val readTxt = DocumentFileHandler.readDocument(txtIn, SupportedFileType.TXT)
        assertEquals(sampleText, readTxt)

        // Test .md
        val mdOut = ByteArrayOutputStream()
        DocumentFileHandler.writeDocument(sampleText, mdOut, SupportedFileType.MD)
        val mdIn = ByteArrayInputStream(mdOut.toByteArray())
        val readMd = DocumentFileHandler.readDocument(mdIn, SupportedFileType.MD)
        assertEquals(sampleText, readMd)
    }

    @Test
    fun docx_roundTrip_createsValidDocxAndPreservesParagraphStructure() {
        val sampleDoc = "Paragraph 1: Introduction\n\nParagraph 2: Second section\nParagraph 3: Conclusion"
        
        // Write new DOCX
        val docxOut = ByteArrayOutputStream()
        DocumentFileHandler.writeDocument(sampleDoc, docxOut, SupportedFileType.DOCX)
        val docxBytes = docxOut.toByteArray()
        assertTrue(docxBytes.isNotEmpty())

        // Read DOCX back
        val docxIn = ByteArrayInputStream(docxBytes)
        val readDocx = DocumentFileHandler.readDocument(docxIn, SupportedFileType.DOCX)
        assertEquals(sampleDoc, readDocx)

        // Modify and update existing DOCX
        val updatedText = "Paragraph 1: Updated\n\nParagraph 2: Second section\nParagraph 3: Conclusion\nNew Paragraph 4"
        val updateOut = ByteArrayOutputStream()
        DocumentFileHandler.writeDocument(
            text = updatedText,
            outputStream = updateOut,
            fileType = SupportedFileType.DOCX,
            existingInputStream = ByteArrayInputStream(docxBytes)
        )
        val updatedBytes = updateOut.toByteArray()
        val readUpdated = DocumentFileHandler.readDocument(ByteArrayInputStream(updatedBytes), SupportedFileType.DOCX)
        assertEquals(updatedText, readUpdated)
    }

    @Test
    fun fileTypeDetector_identifiesCorrectExtension() {
        assertEquals(SupportedFileType.TXT, SupportedFileType.fromFileName("notes.txt"))
        assertEquals(SupportedFileType.TXT, SupportedFileType.fromFileName("notes.TXT"))
        assertEquals(SupportedFileType.MD, SupportedFileType.fromFileName("README.md"))
        assertEquals(SupportedFileType.MD, SupportedFileType.fromFileName("doc.MD"))
        assertEquals(SupportedFileType.DOCX, SupportedFileType.fromFileName("report.docx"))
        assertEquals(SupportedFileType.DOCX, SupportedFileType.fromFileName("ASSIGNMENT.DOCX"))
        assertEquals(SupportedFileType.TXT, SupportedFileType.fromFileName("unknownfile"))
    }
}
