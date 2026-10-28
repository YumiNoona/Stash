package com.lockr.app

import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.util.JsonWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class DataExportFormat(val label: String, val mimeType: String, val extension: String) {
    JSON("JSON", "application/json", "json"),
    PDF("PDF", "application/pdf", "pdf"),
    EXCEL("Excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
    TEXT("Text", "text/plain", "txt")
}

/** Readable exports are user-directed files. Encrypted backups use LocalVaultStore separately. */
fun writeReadableExport(output: OutputStream, items: List<VaultItem>, format: DataExportFormat, includePrivate: Boolean) {
    when (format) {
        DataExportFormat.JSON -> writeJson(output, items, includePrivate)
        DataExportFormat.PDF -> writePdf(output, items, includePrivate)
        DataExportFormat.EXCEL -> writeXlsx(output, items, includePrivate)
        DataExportFormat.TEXT -> writeText(output, items, includePrivate)
    }
}

private val neverExport = setOf("securitycode", "cvv", "cvc", "cardcvc")
private val privateFields = setOf("password", "cardnumber", "idnumber", "documentnumber", "address", "extractedsnippet", "ocrtext", "content", "notes")

internal fun visibleMetadata(item: VaultItem, includePrivate: Boolean): Map<String, String> =
    item.metadata.filterKeys { key -> key.lowercase() !in neverExport && (includePrivate || key.lowercase() !in privateFields) }

private fun writeJson(output: OutputStream, items: List<VaultItem>, includePrivate: Boolean) {
    JsonWriter(OutputStreamWriter(output, StandardCharsets.UTF_8)).use { json ->
        json.setIndent("  ")
        json.beginObject()
        json.name("format").value("Stash readable export")
        json.name("version").value(1)
        json.name("items").beginArray()
        items.forEach { item ->
            json.beginObject()
            json.name("title").value(item.title)
            json.name("category").value(item.category.label)
            json.name("detail").value(item.detail)
            json.name("saved").value(item.time)
            json.name("lastFour").value(item.lastFour)
            json.name("metadata").beginObject()
            visibleMetadata(item, includePrivate).forEach { (key, value) -> json.name(key).value(value) }
            json.endObject()
            json.name("attachments").beginArray()
            item.attachments.forEach { attachment ->
                json.beginObject()
                json.name("name").value(attachment.name)
                json.name("mimeType").value(attachment.mimeType)
                json.name("byteCount").value(attachment.byteCount)
                if (includePrivate) json.name("base64Data").value(attachment.base64Data)
                json.endObject()
            }
            json.endArray()
            json.endObject()
        }
        json.endArray()
        json.endObject()
    }
}

private fun writeText(output: OutputStream, items: List<VaultItem>, includePrivate: Boolean) {
    OutputStreamWriter(output, StandardCharsets.UTF_8).buffered().use { writer ->
        writer.appendLine("STASH · LOCAL EXPORT")
        writer.appendLine("${items.size} saved items")
        items.forEachIndexed { index, item ->
            writer.appendLine()
            writer.appendLine("${index + 1}. ${item.title}")
            writer.appendLine("Type: ${item.category.label}  ·  Saved: ${item.time}")
            if (item.detail.isNotBlank()) writer.appendLine("Detail: ${item.detail}")
            if (item.lastFour.isNotBlank()) writer.appendLine("Last four: ${item.lastFour}")
            visibleMetadata(item, includePrivate).forEach { (key, value) -> writer.appendLine("${exportFieldLabel(key)}: $value") }
            if (item.attachments.isNotEmpty()) writer.appendLine("Files: ${item.attachments.joinToString { it.name }}")
        }
    }
}

private fun exportSummary(item: VaultItem, includePrivate: Boolean): String = buildString {
    if (item.detail.isNotBlank()) append(item.detail).append(" | ")
    if (item.lastFour.isNotBlank()) append("•••• ").append(item.lastFour).append(" | ")
    visibleMetadata(item, includePrivate).forEach { (key, value) -> append(exportFieldLabel(key)).append(": ").append(value.replace('\n', ' ')).append(" | ") }
}.trim().trimEnd('|', ' ')

private fun exportFieldLabel(key: String): String = key.replace(Regex("([a-z])([A-Z])"), "$1 $2")
    .replaceFirstChar { it.uppercase() }

private fun writePdf(output: OutputStream, items: List<VaultItem>, includePrivate: Boolean) {
    val document = PdfDocument()
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(24, 35, 51); textSize = 21f; isFakeBoldText = true }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(24, 35, 51); textSize = 12f; isFakeBoldText = true }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(77, 91, 111); textSize = 10f }
    var pageNumber = 0
    var page: PdfDocument.Page? = null
    var y = 0f
    fun newPage() {
        page?.let(document::finishPage)
        pageNumber++
        page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        y = 47f
        page!!.canvas.drawText("Stash", 40f, y, titlePaint)
        y += 24f
        page!!.canvas.drawText("Local vault export · ${items.size} items", 40f, y, bodyPaint)
        y += 27f
    }
    fun line(text: String, paint: Paint) {
        if (y > 786f) newPage()
        page!!.canvas.drawText(text, 40f, y, paint)
        y += if (paint === labelPaint) 19f else 15f
    }
    fun wrapped(text: String, paint: Paint) {
        text.replace('\n', ' ').split(' ').fold(StringBuilder()) { current, word ->
            val proposed = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(proposed) > 510f && current.isNotEmpty()) {
                line(current.toString(), paint)
                current.clear().append(word)
            } else current.clear().append(proposed)
            current
        }.takeIf { it.isNotEmpty() }?.let { line(it.toString(), paint) }
    }
    try {
        newPage()
        items.forEachIndexed { index, item ->
            if (y > 745f) newPage()
            wrapped("${index + 1}. ${item.title}", labelPaint)
            line("${item.category.label}  ·  ${item.time}", bodyPaint)
            exportSummary(item, includePrivate).takeIf(String::isNotBlank)?.let { wrapped(it, bodyPaint) }
            if (item.attachments.isNotEmpty()) wrapped("Files: ${item.attachments.joinToString { it.name }}", bodyPaint)
            y += 10f
        }
        page?.let(document::finishPage)
        document.writeTo(output)
    } finally {
        document.close()
    }
}

private fun writeXlsx(output: OutputStream, items: List<VaultItem>, includePrivate: Boolean) {
    ZipOutputStream(output).use { zip ->
        fun entry(path: String, content: String) {
            zip.putNextEntry(ZipEntry(path))
            zip.write(content.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
        }
        entry("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>""")
        entry("_rels/.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
        entry("xl/workbook.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Stash items" sheetId="1" r:id="rId1"/></sheets></workbook>""")
        entry("xl/_rels/workbook.xml.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>""")
        zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
        val writer = OutputStreamWriter(zip, StandardCharsets.UTF_8)
        writer.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        fun row(index: Int, values: List<String>) {
            writer.write("<row r=\"$index\">")
            values.forEachIndexed { column, value ->
                val reference = "${'A' + column}$index"
                writer.write("<c r=\"$reference\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${xmlEscape(value.take(32_767))}</t></is></c>")
            }
            writer.write("</row>")
        }
        row(1, listOf("Title", "Category", "Saved", "Detail", "Last four", "Information", "Files"))
        items.forEachIndexed { index, item ->
            row(index + 2, listOf(item.title, item.category.label, item.time, item.detail, item.lastFour,
                exportSummary(item, includePrivate), item.attachments.joinToString { it.name }))
        }
        writer.write("</sheetData></worksheet>")
        writer.flush()
        zip.closeEntry()
    }
}

internal fun xmlEscape(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;")
    .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
