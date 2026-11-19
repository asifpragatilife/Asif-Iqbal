package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

enum class ExportFormat(val extension: String, val mimeType: String, val label: String) {
    PDF("pdf", "application/pdf", "PDF Document"),
    JPG("jpg", "image/jpeg", "JPG Image"),
    TXT("txt", "text/plain", "Plain Text (.txt)"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Word (.docx)"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Excel (.xlsx)"),
}

object DocumentExportEngine {

    private fun exportsDir(context: Context): File =
        File(context.cacheDir, "exports").apply { mkdirs() }

    /**
     * Builds a real multi-page PDF file from page bitmaps.
     */
    fun buildPdfFromBitmaps(
        context: Context,
        bitmaps: List<Bitmap>,
        outputName: String,
        jpegQuality: Int = 90,
    ): File {
        val safeName = outputName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "document" }
        val outFile = File(exportsDir(context), "$safeName.pdf")
        val pdf = PdfDocument()
        try {
            bitmaps.forEachIndexed { idx, rawBmp ->
                val targetW = 1240
                val ratio = rawBmp.height.toFloat() / rawBmp.width.coerceAtLeast(1).toFloat()
                val targetH = (targetW * ratio).toInt().coerceIn(400, 2200)
                val scaled = Bitmap.createScaledBitmap(rawBmp, targetW, targetH, true)

                // Re-compress through JPEG stream when compression quality < 95 for real file-size savings
                val compressedBmp = if (jpegQuality < 95) {
                    val baos = java.io.ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, jpegQuality, baos)
                    val bytes = baos.toByteArray()
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: scaled
                } else {
                    scaled
                }

                val pageInfo = PdfDocument.PageInfo.Builder(targetW, targetH, idx + 1).create()
                val page = pdf.startPage(pageInfo)
                page.canvas.drawColor(Color.WHITE)
                page.canvas.drawBitmap(compressedBmp, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
                pdf.finishPage(page)

                if (compressedBmp !== scaled) compressedBmp.recycle()
                if (scaled !== rawBmp) scaled.recycle()
            }
            FileOutputStream(outFile).use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
        return outFile
    }

    /**
     * Imports pages from a PDF Uri via Android PdfRenderer as crisp high-resolution bitmaps.
     */
    fun renderPdfUriToBitmaps(context: Context, uri: Uri, maxPages: Int = 12): List<Bitmap> {
        val tempFile = File(context.cacheDir, "import_${System.currentTimeMillis()}.pdf")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output -> input.copyTo(output) }
        } ?: return emptyList()

        val result = mutableListOf<Bitmap>()
        try {
            ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val count = renderer.pageCount.coerceAtMost(maxPages)
                    for (i in 0 until count) {
                        renderer.openPage(i).use { page ->
                            val scale = 2
                            val bmp = Bitmap.createBitmap(
                                page.width * scale,
                                page.height * scale,
                                Bitmap.Config.ARGB_8888,
                            )
                            val canvas = Canvas(bmp)
                            canvas.drawColor(Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            result.add(bmp)
                        }
                    }
                }
            }
        } finally {
            tempFile.delete()
        }
        return result
    }

    /**
     * Permanently flattens a signature bitmap onto a page bitmap.
     */
    fun flattenSignatureOntoBitmap(
        pageBitmap: Bitmap,
        signatureBitmap: Bitmap,
        centerXNorm: Float,
        centerYNorm: Float,
        widthNorm: Float,
    ): Bitmap {
        val out = pageBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val sigW = (out.width * widthNorm).coerceAtLeast(48f)
        val aspect = signatureBitmap.height.toFloat() / signatureBitmap.width.coerceAtLeast(1).toFloat()
        val sigH = sigW * aspect
        val cx = out.width * centerXNorm
        val cy = out.height * centerYNorm
        val dest = RectF(cx - sigW / 2f, cy - sigH / 2f, cx + sigW / 2f, cy + sigH / 2f)
        canvas.drawBitmap(signatureBitmap, null, dest, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        return out
    }

    /**
     * Permanently burns opaque black boxes into underlying bitmap pixels (irreversible redaction).
     */
    fun burnRedactionsOntoBitmap(
        pageBitmap: Bitmap,
        boxes: List<NormalizedRect>,
    ): Bitmap {
        val out = pageBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val paint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = false
        }
        boxes.forEach { box ->
            val rect = Rect(
                (box.left * out.width).toInt().coerceIn(0, out.width),
                (box.top * out.height).toInt().coerceIn(0, out.height),
                (box.right * out.width).toInt().coerceIn(0, out.width),
                (box.bottom * out.height).toInt().coerceIn(0, out.height),
            )
            canvas.drawRect(rect, paint)
        }
        return out
    }

    fun exportJpg(context: Context, bitmap: Bitmap, outputName: String, quality: Int = 92): File {
        val safeName = outputName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "page" }
        val outFile = File(exportsDir(context), "$safeName.jpg")
        FileOutputStream(outFile).use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
        return outFile
    }

    fun exportTxt(context: Context, title: String, ocrText: String, outputName: String): File {
        val safeName = outputName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "text" }
        val outFile = File(exportsDir(context), "$safeName.txt")
        val content = buildString {
            appendLine(title)
            appendLine("=".repeat(title.length.coerceAtLeast(12)))
            appendLine()
            append(ocrText.ifBlank { "(No OCR text extracted yet)" })
        }
        outFile.writeText(content)
        return outFile
    }

    /**
     * Generates a valid OOXML Word (.docx) archive 100% offline.
     */
    fun exportDocx(context: Context, title: String, ocrText: String, outputName: String): File {
        val safeName = outputName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "document" }
        val outFile = File(exportsDir(context), "$safeName.docx")
        val paragraphs = (listOf(title, "") + ocrText.ifBlank { "Scanned document exported by AsifAraf." }.lines())
            .joinToString("") { line ->
                "<w:p><w:r><w:t xml:space=\"preserve\">${escapeXml(line)}</w:t></w:r></w:p>"
            }

        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

        val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

        val docXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>$paragraphs<w:sectPr/></w:body>
</w:document>"""

        ZipOutputStream(FileOutputStream(outFile)).use { zip ->
            zip.putEntry("[Content_Types].xml", contentTypes)
            zip.putEntry("_rels/.rels", rels)
            zip.putEntry("word/document.xml", docXml)
        }
        return outFile
    }

    /**
     * Generates a valid OOXML Excel (.xlsx) spreadsheet 100% offline from OCR lines/columns.
     */
    fun exportXlsx(context: Context, title: String, ocrText: String, outputName: String): File {
        val safeName = outputName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "sheet" }
        val outFile = File(exportsDir(context), "$safeName.xlsx")

        val rows = mutableListOf(listOf(title))
        ocrText.ifBlank { "Item,Value\nScanned Page,1" }.lines().filter { it.isNotBlank() }.forEach { line ->
            val cols = when {
                line.contains("|") -> line.split("|")
                line.contains("\t") -> line.split("\t")
                line.contains(",") -> line.split(",")
                else -> line.split(Regex("\\s{2,}")).takeIf { it.size > 1 } ?: listOf(line)
            }.map { it.trim() }
            rows.add(cols)
        }

        val sheetRowsXml = rows.mapIndexed { rIdx, cols ->
            val rowNum = rIdx + 1
            val cellsXml = cols.mapIndexed { cIdx, value ->
                val colLetter = ('A' + (cIdx % 26))
                "<c r=\"$colLetter$rowNum\" t=\"inlineStr\"><is><t>${escapeXml(value)}</t></is></c>"
            }.joinToString("")
            "<row r=\"$rowNum\">$cellsXml</row>"
        }.joinToString("")

        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""

        val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

        val workbook = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets><sheet name="Extracted" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""

        val wbRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>"""

        val sheet1 = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>$sheetRowsXml</sheetData>
</worksheet>"""

        ZipOutputStream(FileOutputStream(outFile)).use { zip ->
            zip.putEntry("[Content_Types].xml", contentTypes)
            zip.putEntry("_rels/.rels", rels)
            zip.putEntry("xl/workbook.xml", workbook)
            zip.putEntry("xl/_rels/workbook.xml.rels", wbRels)
            zip.putEntry("xl/worksheets/sheet1.xml", sheet1)
        }
        return outFile
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share via $title").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun ZipOutputStream.putEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun escapeXml(input: String): String = input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
