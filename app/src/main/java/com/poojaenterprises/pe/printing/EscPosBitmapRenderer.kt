package com.poojaenterprises.pe.printing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.poojaenterprises.pe.data.BillItem
import com.poojaenterprises.pe.data.BillRecord
import com.poojaenterprises.pe.data.BillStyle
import com.poojaenterprises.pe.data.BusinessDetails
import com.poojaenterprises.pe.data.DividerType
import com.poojaenterprises.pe.data.PrintElementToggles
import com.poojaenterprises.pe.data.PrinterSettings

object EscPosBitmapRenderer {

    // 203 DPI thermal printer standard dots/mm (~7.2 dots/mm for 576 dots / 80 mm)
    private fun mmToDots(mm: Int, totalDots: Int, totalMm: Int): Int {
        return (mm * totalDots) / totalMm
    }

    fun renderBillToBitmap(
        bill: BillRecord,
        businessDetails: BusinessDetails,
        printerSettings: PrinterSettings,
        toggles: PrintElementToggles,
        style: BillStyle
    ): Bitmap {
        val totalDots = printerSettings.paperSize.dots
        val totalMm = printerSettings.paperSize.widthMm

        val leftMargin = mmToDots(printerSettings.leftMarginMm, totalDots, totalMm)
        val rightMargin = mmToDots(printerSettings.rightMarginMm, totalDots, totalMm)
        val contentWidth = totalDots - leftMargin - rightMargin

        val paint = Paint().apply {
            isAntiAlias = true
            isSubpixelText = true
            color = Color.BLACK
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        // Two passes: 1) calculate total height, 2) draw on canvas
        val calculatedHeight = measureBillHeight(
            bill = bill,
            businessDetails = businessDetails,
            toggles = toggles,
            style = style,
            contentWidth = contentWidth,
            paint = paint
        )

        val totalHeight = calculatedHeight + 60 // top/bottom padding + feed buffer
        val bitmap = Bitmap.createBitmap(totalDots, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        var currentY = 24 // initial top padding

        // Draw elements in strict order based on Section 8 requirements
        // 1. Business Name (header)
        if (toggles.businessName && businessDetails.name.isNotBlank()) {
            paint.textSize = businessDetails.headerSize
            paint.isFakeBoldText = true
            paint.textAlign = Paint.Align.CENTER
            val x = (leftMargin + totalDots - rightMargin) / 2f
            canvas.drawText(businessDetails.name, x, (currentY + businessDetails.headerSize), paint)
            currentY += (businessDetails.headerSize * 1.35f).toInt()
        }

        // 2. Header Line 2 (optional)
        if (toggles.headerLine2 && businessDetails.headerLine2.isNotBlank()) {
            paint.textSize = businessDetails.addressSize
            paint.isFakeBoldText = false
            paint.textAlign = Paint.Align.CENTER
            val x = (leftMargin + totalDots - rightMargin) / 2f
            canvas.drawText(businessDetails.headerLine2, x, (currentY + businessDetails.addressSize), paint)
            currentY += (businessDetails.addressSize * 1.3f).toInt()
        }

        // 3. Address
        if (toggles.address && businessDetails.address.isNotBlank()) {
            paint.textSize = businessDetails.addressSize
            paint.isFakeBoldText = false
            paint.textAlign = Paint.Align.CENTER
            val x = (leftMargin + totalDots - rightMargin) / 2f
            val addressLines = wrapText(businessDetails.address, contentWidth, paint)
            for (line in addressLines) {
                canvas.drawText(line, x, (currentY + businessDetails.addressSize), paint)
                currentY += (businessDetails.addressSize * 1.25f).toInt()
            }
        }

        // 4. Separator Line
        if (toggles.separators) {
            currentY += 8
            currentY = drawDivider(canvas, leftMargin, contentWidth, currentY, DividerType.THICK)
            currentY += 8
        }

        // 5. Date and Time (same row: date left, time right)
        if (toggles.date || toggles.time) {
            paint.textSize = businessDetails.itemSize * 0.95f
            paint.isFakeBoldText = false
            val textY = currentY + paint.textSize

            if (toggles.date) {
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("Date: ${bill.date}", leftMargin.toFloat(), textY, paint)
            }
            if (toggles.time) {
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("Time: ${bill.time}", (leftMargin + contentWidth).toFloat(), textY, paint)
            }
            currentY += (paint.textSize * 1.4f).toInt()
        }

        // 6. Customer Name
        if (toggles.customerName && bill.customerName.isNotBlank()) {
            paint.textSize = businessDetails.itemSize
            paint.isFakeBoldText = true
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(bill.customerName, leftMargin.toFloat(), (currentY + paint.textSize), paint)
            currentY += (paint.textSize * 1.35f).toInt()
        }

        // 7. Customer Address
        if (toggles.customerAddress && bill.customerAddress.isNotBlank()) {
            paint.textSize = businessDetails.itemSize * 0.9f
            paint.isFakeBoldText = false
            paint.textAlign = Paint.Align.LEFT
            val custAddrLines = wrapText(bill.customerAddress, contentWidth, paint)
            for (line in custAddrLines) {
                canvas.drawText(line, leftMargin.toFloat(), (currentY + paint.textSize), paint)
                currentY += (paint.textSize * 1.25f).toInt()
            }
        }

        // 8. Separator Line
        if (toggles.separators) {
            currentY += 8
            currentY = drawDivider(canvas, leftMargin, contentWidth, currentY, DividerType.SOLID)
            currentY += 8
        }

        // 9. Table heading: Item | Qty
        if (toggles.tableHeading && businessDetails.tableHeading.isNotBlank()) {
            paint.textSize = businessDetails.itemSize
            paint.isFakeBoldText = true
            val headingY = currentY + paint.textSize

            val parts = businessDetails.tableHeading.split("|").map { it.trim() }
            val leftHeading = parts.getOrNull(0) ?: "Item"
            val rightHeading = parts.getOrNull(1) ?: "Qty"

            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(leftHeading, leftMargin.toFloat(), headingY, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(rightHeading, (leftMargin + contentWidth).toFloat(), headingY, paint)

            currentY += (paint.textSize * 1.4f).toInt()
        }

        // 10. Separator Line
        if (toggles.separators) {
            currentY += 6
            currentY = drawDivider(canvas, leftMargin, contentWidth, currentY, DividerType.SOLID)
            currentY += 8
        }

        // 11. Item Rows (Item name left, Qty + Unit right)
        if (toggles.itemQtyUnit && bill.items.isNotEmpty()) {
            paint.textSize = businessDetails.itemSize

            for ((index, item) in bill.items.withIndex()) {
                val rowY = currentY + paint.textSize
                val rightText = "${item.formattedQuantity} ${item.unit.label}"

                paint.isFakeBoldText = false
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(rightText, (leftMargin + contentWidth).toFloat(), rowY, paint)

                val rightWidth = paint.measureText(rightText)
                val availableLeftWidth = contentWidth - rightWidth - 20

                // Format item display depending on style
                val leftText = if (style.id == "style_table") {
                    "${index + 1}. ${item.itemName}"
                } else {
                    item.itemName
                }

                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(leftText, leftMargin.toFloat(), rowY, paint)

                // For Compact style: draw dotted leader line between name and quantity
                if (style.id == "style_compact") {
                    val leftWidth = paint.measureText(leftText)
                    val dotStartX = leftMargin + leftWidth + 10
                    val dotEndX = (leftMargin + contentWidth) - rightWidth - 10
                    if (dotEndX > dotStartX) {
                        val dotPaint = Paint(paint).apply {
                            strokeWidth = 2f
                            pathEffect = DashPathEffect(floatArrayOf(3f, 6f), 0f)
                        }
                        canvas.drawLine(dotStartX, rowY - 4, dotEndX, rowY - 4, dotPaint)
                    }
                }

                currentY += (paint.textSize * 1.45f).toInt()
            }
        }

        // 12. Separator Line
        if (toggles.separators) {
            currentY += 8
            currentY = drawDivider(canvas, leftMargin, contentWidth, currentY, DividerType.THICK)
            currentY += 8
        }

        // 13. Footer
        if (toggles.footer && businessDetails.footer.isNotBlank()) {
            paint.textSize = businessDetails.footerSize
            paint.isFakeBoldText = false
            paint.textAlign = Paint.Align.CENTER
            val x = (leftMargin + totalDots - rightMargin) / 2f
            canvas.drawText(businessDetails.footer, x, (currentY + businessDetails.footerSize), paint)
            currentY += (businessDetails.footerSize * 1.5f).toInt()
        }

        return bitmap
    }

    private fun measureBillHeight(
        bill: BillRecord,
        businessDetails: BusinessDetails,
        toggles: PrintElementToggles,
        style: BillStyle,
        contentWidth: Int,
        paint: Paint
    ): Int {
        var h = 30 // initial margin

        if (toggles.businessName && businessDetails.name.isNotBlank()) {
            h += (businessDetails.headerSize * 1.35f).toInt()
        }
        if (toggles.headerLine2 && businessDetails.headerLine2.isNotBlank()) {
            h += (businessDetails.addressSize * 1.3f).toInt()
        }
        if (toggles.address && businessDetails.address.isNotBlank()) {
            paint.textSize = businessDetails.addressSize
            val lines = wrapText(businessDetails.address, contentWidth, paint)
            h += (lines.size * businessDetails.addressSize * 1.25f).toInt()
        }
        if (toggles.separators) h += 24
        if (toggles.date || toggles.time) h += (businessDetails.itemSize * 1.4f).toInt()
        if (toggles.customerName && bill.customerName.isNotBlank()) h += (businessDetails.itemSize * 1.35f).toInt()
        if (toggles.customerAddress && bill.customerAddress.isNotBlank()) {
            paint.textSize = businessDetails.itemSize * 0.9f
            val lines = wrapText(bill.customerAddress, contentWidth, paint)
            h += (lines.size * paint.textSize * 1.25f).toInt()
        }
        if (toggles.separators) h += 24
        if (toggles.tableHeading && businessDetails.tableHeading.isNotBlank()) h += (businessDetails.itemSize * 1.4f).toInt()
        if (toggles.separators) h += 22
        if (toggles.itemQtyUnit) h += (bill.items.size * businessDetails.itemSize * 1.45f).toInt()
        if (toggles.separators) h += 24
        if (toggles.footer && businessDetails.footer.isNotBlank()) h += (businessDetails.footerSize * 1.5f).toInt()

        return h
    }

    private fun drawDivider(canvas: Canvas, left: Int, width: Int, y: Int, type: DividerType): Int {
        val paint = Paint().apply {
            color = Color.BLACK
            strokeWidth = when (type) {
                DividerType.THICK -> 3.5f
                DividerType.SOLID -> 1.5f
                DividerType.DASHED -> 2f
                DividerType.DOTTED -> 2f
            }
        }
        when (type) {
            DividerType.DASHED -> {
                paint.pathEffect = DashPathEffect(floatArrayOf(10f, 6f), 0f)
            }
            DividerType.DOTTED -> {
                paint.pathEffect = DashPathEffect(floatArrayOf(3f, 4f), 0f)
            }
            else -> {}
        }
        val startX = left.toFloat()
        val endX = (left + width).toFloat()
        canvas.drawLine(startX, y.toFloat(), endX, y.toFloat(), paint)
        return y + paint.strokeWidth.toInt()
    }

    private fun wrapText(text: String, maxWidth: Int, paint: Paint): List<String> {
        val lines = mutableListOf<String>()
        val paragraphs = text.split("\n")
        for (p in paragraphs) {
            if (paint.measureText(p) <= maxWidth) {
                lines.add(p)
                continue
            }
            val words = p.split(" ")
            var currentLine = StringBuilder()
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(testLine) <= maxWidth) {
                    currentLine = StringBuilder(testLine)
                } else {
                    if (currentLine.isNotEmpty()) {
                        lines.add(currentLine.toString())
                    }
                    currentLine = StringBuilder(word)
                }
            }
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
            }
        }
        return lines
    }
}
