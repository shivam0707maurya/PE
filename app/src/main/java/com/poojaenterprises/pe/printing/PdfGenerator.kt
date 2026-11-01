package com.poojaenterprises.pe.printing

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.poojaenterprises.pe.data.BillRecord
import com.poojaenterprises.pe.data.PaperSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    private const val TAG = "PdfGenerator"
    private const val MM_TO_PT = 2.83465f // 1 mm = ~2.835 PDF points at 72 DPI

    /**
     * Generates a PDF matching the exact rendered receipt bitmap.
     * Saves to:
     * 1) Public Documents/ShopPrinter/Bills/ (via MediaStore or direct File)
     * 2) App files directory (for immediate FileProvider sharing via WhatsApp)
     */
    suspend fun generateAndSaveBillPdf(
        context: Context,
        bill: BillRecord,
        bitmap: Bitmap,
        paperSize: PaperSize
    ): File? = withContext(Dispatchers.IO) {
        try {
            val widthPt = (paperSize.widthMm * MM_TO_PT).toInt()
            val heightPt = ((bitmap.height.toFloat() / bitmap.width.toFloat()) * widthPt).toInt()

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(widthPt, heightPt, 1).create()
            val page = document.startPage(pageInfo)

            val canvas = page.canvas
            val destRect = RectF(0f, 0f, widthPt.toFloat(), heightPt.toFloat())
            val paint = Paint().apply {
                isFilterBitmap = true
                isAntiAlias = true
            }
            canvas.drawBitmap(bitmap, null, destRect, paint)
            document.finishPage(page)

            // 1. Sanitize file name: Customer-Address-dd-MM-yyyy.pdf
            val baseFileName = buildSanitizedFileName(bill)
            val fileName = "$baseFileName.pdf"

            // 2. Save local file in app files directory (for WhatsApp FileProvider)
            val appBillsDir = File(context.filesDir, "bills").apply { mkdirs() }
            val localPdfFile = File(appBillsDir, fileName)
            FileOutputStream(localPdfFile).use { out ->
                document.writeTo(out)
            }

            // 3. Save to public Documents/ShopPrinter/Bills/
            saveToPublicDocuments(context, fileName, localPdfFile)

            document.close()
            localPdfFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate bill PDF", e)
            null
        }
    }

    private fun buildSanitizedFileName(bill: BillRecord): String {
        val custName = sanitizeForFilename(if (bill.customerName.isNotBlank()) bill.customerName else "Customer")
        val addr = sanitizeForFilename(if (bill.customerAddress.isNotBlank()) bill.customerAddress.take(20) else "Shop")
        val dateStr = sanitizeForFilename(bill.date.replace("/", "-"))
        return "${custName}-${addr}-${dateStr}"
    }

    private fun sanitizeForFilename(input: String): String {
        return input.replace(Regex("[\\\\/:*?\"<>|\\s]+"), "-").trim('-')
    }

    private fun saveToPublicDocuments(context: Context, fileName: String, sourceFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/ShopPrinter/Bills")
                }
                val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outStream ->
                        sourceFile.inputStream().use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                }
            } else {
                val publicDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "ShopPrinter/Bills"
                ).apply { mkdirs() }
                val targetFile = File(publicDir, fileName)
                sourceFile.copyTo(targetFile, overwrite = true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not save copy to public Documents folder: ${e.message}")
        }
    }
}
