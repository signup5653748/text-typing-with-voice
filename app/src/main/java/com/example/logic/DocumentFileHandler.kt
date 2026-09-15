package com.example.logic

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * Supported document formats in the editor.
 */
enum class SupportedFileType(val extension: String, val mimeType: String, val label: String) {
    TXT("txt", "text/plain", "Plain Text (.txt)"),
    MD("md", "text/markdown", "Markdown (.md)"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Word Document (.docx)");

    companion object {
        fun fromFileName(fileName: String): SupportedFileType {
            val lower = fileName.lowercase()
            return when {
                lower.endsWith(".docx") -> DOCX
                lower.endsWith(".md") -> MD
                else -> TXT
            }
        }

        fun fromMimeType(mimeType: String?): SupportedFileType {
            if (mimeType == null) return TXT
            val lower = mimeType.lowercase()
            return when {
                lower.contains("wordprocessingml") || lower.contains("docx") -> DOCX
                lower.contains("markdown") -> MD
                else -> TXT
            }
        }
    }
}

/**
 * Handles reading and writing across supported file formats (.txt, .md, .docx).
 * For .docx, parses and rebuilds standard Office Open XML (word/document.xml)
 * preserving paragraph structure, tables, and existing package relationships.
 */
object DocumentFileHandler {

    private const val W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"

    /**
     * Reads document text content based on file type.
     */
    fun readDocument(inputStream: InputStream, fileType: SupportedFileType): String {
        return when (fileType) {
            SupportedFileType.TXT,
            SupportedFileType.MD -> {
                // Ensure standard newlines are preserved
                inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            }
            SupportedFileType.DOCX -> {
                readDocx(inputStream)
            }
        }
    }

    /**
     * Writes document text content back based on file type.
     * If existingInputStream is provided (for overwrite/save on existing docx),
     * existing docx assets and structure are updated cleanly.
     */
    fun writeDocument(
        text: String,
        outputStream: OutputStream,
        fileType: SupportedFileType,
        existingInputStream: InputStream? = null
    ) {
        when (fileType) {
            SupportedFileType.TXT,
            SupportedFileType.MD -> {
                // Write text with UTF-8 encoding
                outputStream.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
                    writer.write(text)
                    writer.flush()
                }
            }
            SupportedFileType.DOCX -> {
                if (existingInputStream != null) {
                    try {
                        val existingBytes = existingInputStream.readBytes()
                        if (existingBytes.isNotEmpty()) {
                            updateExistingDocx(existingBytes, text, outputStream)
                            return
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                // Generate fresh valid .docx structure
                createNewDocx(text, outputStream)
            }
        }
    }

    /**
     * Extracts text and preserves paragraph breaks from a DOCX zip archive.
     */
    private fun readDocx(inputStream: InputStream): String {
        val zipIn = ZipInputStream(inputStream)
        var documentXmlBytes: ByteArray? = null

        var entry: ZipEntry? = zipIn.nextEntry
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                documentXmlBytes = zipIn.readBytes()
                break
            }
            entry = zipIn.nextEntry
        }

        if (documentXmlBytes == null || documentXmlBytes.isEmpty()) {
            return ""
        }

        return parseDocumentXml(documentXmlBytes)
    }

    /**
     * Parses word/document.xml to extract paragraphs (<w:p>), line breaks (<w:br>),
     * and tabs (<w:tab>), combining them with newline separators.
     */
    private fun parseDocumentXml(xmlBytes: ByteArray): String {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(ByteArrayInputStream(xmlBytes))

        val bodyNode = doc.getElementsByTagNameNS(W_NS, "body").item(0)
            ?: doc.getElementsByTagName("w:body").item(0)
            ?: doc.documentElement

        val paragraphs = mutableListOf<String>()
        extractParagraphs(bodyNode, paragraphs)

        return paragraphs.joinToString("\n")
    }

    private fun extractParagraphs(node: Node, outParagraphs: MutableList<String>) {
        val childNodes = node.childNodes
        for (i in 0 until childNodes.length) {
            val child = childNodes.item(i)
            val localName = child.localName ?: child.nodeName.substringAfterLast(':')

            if (localName == "p") {
                val pText = extractTextFromParagraphNode(child)
                outParagraphs.add(pText)
            } else if (localName == "tbl") {
                // For tables, extract each row/cell
                extractParagraphs(child, outParagraphs)
            } else if (child.hasChildNodes()) {
                extractParagraphs(child, outParagraphs)
            }
        }
    }

    private fun extractTextFromParagraphNode(pNode: Node): String {
        val sb = StringBuilder()
        fun traverse(node: Node) {
            val local = node.localName ?: node.nodeName.substringAfterLast(':')
            when (local) {
                "t" -> sb.append(node.textContent ?: "")
                "tab" -> sb.append("\t")
                "br", "cr" -> sb.append("\n")
                else -> {
                    val children = node.childNodes
                    for (i in 0 until children.length) {
                        traverse(children.item(i))
                    }
                }
            }
        }
        traverse(pNode)
        return sb.toString()
    }

    /**
     * Creates a new valid .docx file from scratch with the given text paragraphs.
     */
    private fun createNewDocx(text: String, outputStream: OutputStream) {
        val paragraphs = text.split("\n")
        val documentXml = buildDocumentXml(paragraphs)

        val zipOut = ZipOutputStream(outputStream)

        // [Content_Types].xml
        zipOut.putNextEntry(ZipEntry("[Content_Types].xml"))
        zipOut.write(CONTENT_TYPES_XML.toByteArray(StandardCharsets.UTF_8))
        zipOut.closeEntry()

        // _rels/.rels
        zipOut.putNextEntry(ZipEntry("_rels/.rels"))
        zipOut.write(ROOT_RELS_XML.toByteArray(StandardCharsets.UTF_8))
        zipOut.closeEntry()

        // word/_rels/document.xml.rels
        zipOut.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
        zipOut.write(DOCUMENT_RELS_XML.toByteArray(StandardCharsets.UTF_8))
        zipOut.closeEntry()

        // word/document.xml
        zipOut.putNextEntry(ZipEntry("word/document.xml"))
        zipOut.write(documentXml.toByteArray(StandardCharsets.UTF_8))
        zipOut.closeEntry()

        // word/styles.xml
        zipOut.putNextEntry(ZipEntry("word/styles.xml"))
        zipOut.write(DEFAULT_STYLES_XML.toByteArray(StandardCharsets.UTF_8))
        zipOut.closeEntry()

        zipOut.finish()
        zipOut.flush()
    }

    /**
     * Updates an existing .docx file by replacing word/document.xml with updated paragraphs
     * while preserving all other zip entries (styles, headers, footers, relationships, settings).
     */
    private fun updateExistingDocx(existingBytes: ByteArray, text: String, outputStream: OutputStream) {
        val paragraphs = text.split("\n")
        val updatedDocumentXml = buildDocumentXml(paragraphs)

        val zipIn = ZipInputStream(ByteArrayInputStream(existingBytes))
        val zipOut = ZipOutputStream(outputStream)

        var hasWrittenDocumentXml = false
        var entry = zipIn.nextEntry
        while (entry != null) {
            val entryName = entry.name
            zipOut.putNextEntry(ZipEntry(entryName))

            if (entryName == "word/document.xml") {
                zipOut.write(updatedDocumentXml.toByteArray(StandardCharsets.UTF_8))
                hasWrittenDocumentXml = true
            } else {
                val buffer = ByteArray(4096)
                var read: Int
                while (zipIn.read(buffer).also { read = it } != -1) {
                    zipOut.write(buffer, 0, read)
                }
            }
            zipOut.closeEntry()
            entry = zipIn.nextEntry
        }

        if (!hasWrittenDocumentXml) {
            zipOut.putNextEntry(ZipEntry("word/document.xml"))
            zipOut.write(updatedDocumentXml.toByteArray(StandardCharsets.UTF_8))
            zipOut.closeEntry()
        }

        zipOut.finish()
        zipOut.flush()
    }

    /**
     * Generates a valid WordprocessingML document.xml body containing each paragraph.
     */
    private fun buildDocumentXml(paragraphs: List<String>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
        sb.append("<w:body>")

        for (para in paragraphs) {
            sb.append("<w:p>")
            if (para.isNotEmpty()) {
                sb.append("<w:r>")
                sb.append("<w:t xml:space=\"preserve\">")
                sb.append(escapeXml(para))
                sb.append("</w:t>")
                sb.append("</w:r>")
            }
            sb.append("</w:p>")
        }

        sb.append("""<w:sectPr><w:pgSz w:w="12240" w:h="15840"/><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="720" w:footer="720" w:gutter="0"/></w:sectPr>""")
        sb.append("</w:body>")
        sb.append("</w:document>")
        return sb.toString()
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private const val CONTENT_TYPES_XML =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>"""

    private const val ROOT_RELS_XML =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

    private const val DOCUMENT_RELS_XML =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private const val DEFAULT_STYLES_XML =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:docDefaults>
    <w:rPrDefault>
      <w:rPr>
        <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/>
        <w:sz w:val="22"/>
        <w:szCs w:val="22"/>
        <w:lang w:val="en-US"/>
      </w:rPr>
    </w:rPrDefault>
  </w:docDefaults>
</w:styles>"""
}
