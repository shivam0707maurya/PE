package com.poojaenterprises.pe.data

import kotlinx.serialization.Serializable
import java.util.UUID

// ==================== Enums ====================

@Serializable
enum class PaperSize(val widthMm: Int, val dots: Int, val label: String) {
    SIZE_80MM(80, 576, "80 mm (3 inch)"),
    SIZE_58MM(58, 384, "58 mm (2 inch)")
}

@Serializable
enum class UnitType(val label: String) {
    PCS("Pcs"),
    KG("Kg")
}

@Serializable
enum class WhatsAppNumberType(val label: String) {
    MY_NUMBER("My number"),
    CUSTOMER("Customer"),
    STAFF("Staff")
}

enum class PrinterStatus(val label: String) {
    IDLE("Idle"),
    PRINTING("Printing"),
    BUSY("Busy"),
    PAPER_COVER("Paper End / Cover Open"),
    OFFLINE("Offline")
}

@Serializable
enum class DividerType {
    SOLID,
    DASHED,
    DOTTED,
    THICK
}

// ==================== Entities ====================

@Serializable
data class Customer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val address: String = "",
    val whatsAppNumber: String = ""
)

@Serializable
data class Item(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val defaultUnit: UnitType = UnitType.PCS
)

@Serializable
data class WhatsAppNumber(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: WhatsAppNumberType = WhatsAppNumberType.CUSTOMER,
    val phoneNumber: String = ""
)

@Serializable
data class BusinessDetails(
    val name: String = "Pooja Enterprises",
    val headerLine2: String = "Building Material",
    val address: String = "पियरवा पोखरा चकिया रोड अहरौरा",
    val footer: String = "Thank you",
    val headerSize: Float = 28f,
    val addressSize: Float = 16f,
    val itemSize: Float = 16f,
    val footerSize: Float = 16f,
    val tableHeading: String = "Item | Qty"
)

@Serializable
data class PrinterSettings(
    val ipAddress: String = "192.168.1.100",
    val port: Int = 9100,
    val paperSize: PaperSize = PaperSize.SIZE_80MM,
    val leftMarginMm: Int = 5,
    val rightMarginMm: Int = 5,
    val autoCut: Boolean = true
)

@Serializable
data class PrintElementToggles(
    val businessName: Boolean = true,
    val headerLine2: Boolean = true,
    val address: Boolean = true,
    val date: Boolean = true,
    val time: Boolean = true,
    val customerName: Boolean = true,
    val customerAddress: Boolean = true,
    val tableHeading: Boolean = true,
    val itemQtyUnit: Boolean = true,
    val footer: Boolean = true,
    val separators: Boolean = true,
    val autoCut: Boolean = true,
    val whatsappSending: Boolean = true,
    val pdfHistory: Boolean = true,
    val printPreview: Boolean = true
)

// ==================== Bill Items & Bill ====================

@Serializable
data class BillItem(
    val itemName: String,
    val quantity: Double,
    val unit: UnitType
) {
    /**
     * Requirement: Kg quantities always print with 3 decimals (25 -> 25.000 Kg, 250.2 -> 250.200 Kg).
     * Pcs prints as a whole number (10 Pcs).
     */
    val formattedQuantity: String
        get() = when (unit) {
            UnitType.KG -> String.format(java.util.Locale.US, "%.3f", quantity)
            UnitType.PCS -> quantity.toLong().toString()
        }

    val displaySummary: String
        get() = "$itemName $formattedQuantity ${unit.label}"
}

data class BillItemInput(
    val id: String = UUID.randomUUID().toString(),
    var itemName: String = "",
    var quantityStr: String = "",
    var unit: UnitType = UnitType.PCS
)

@Serializable
data class BillRecord(
    val id: String = UUID.randomUUID().toString(),
    val date: String, // dd-MM-yyyy
    val time: String, // hh:mm a
    val customerName: String,
    val customerAddress: String,
    val customerWhatsAppNumber: String = "",
    val items: List<BillItem>,
    val pdfPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// ==================== Receipt Styles ====================

@Serializable
data class StyleBlock(
    val type: String, // header, header2, address, separator, datetime, customer, customer_address, table_heading, items, footer
    val enabled: Boolean = true,
    val alignment: String = "center", // left, center, right
    val fontSize: Float = 16f,
    val bold: Boolean = false,
    val dividerType: DividerType = DividerType.SOLID
)

@Serializable
data class BillStyle(
    val id: String = UUID.randomUUID().toString(),
    val name: String, // Simple, Table, Compact, or Custom
    val blocks: List<StyleBlock>,
    val isBuiltIn: Boolean = true
) {
    companion object {
        fun simpleStyle(): BillStyle = BillStyle(
            id = "style_simple",
            name = "Simple",
            isBuiltIn = true,
            blocks = listOf(
                StyleBlock("header", alignment = "center", fontSize = 28f, bold = true),
                StyleBlock("header2", alignment = "center", fontSize = 16f),
                StyleBlock("address", alignment = "center", fontSize = 16f),
                StyleBlock("separator", dividerType = DividerType.THICK),
                StyleBlock("datetime", alignment = "left", fontSize = 16f),
                StyleBlock("customer", alignment = "left", fontSize = 16f, bold = true),
                StyleBlock("customer_address", alignment = "left", fontSize = 15f),
                StyleBlock("separator", dividerType = DividerType.SOLID),
                StyleBlock("table_heading", alignment = "left", fontSize = 16f, bold = true),
                StyleBlock("separator", dividerType = DividerType.SOLID),
                StyleBlock("items", alignment = "left", fontSize = 16f),
                StyleBlock("separator", dividerType = DividerType.THICK),
                StyleBlock("footer", alignment = "center", fontSize = 16f)
            )
        )

        fun tableStyle(): BillStyle = BillStyle(
            id = "style_table",
            name = "Table",
            isBuiltIn = true,
            blocks = listOf(
                StyleBlock("header", alignment = "center", fontSize = 26f, bold = true),
                StyleBlock("header2", alignment = "center", fontSize = 16f),
                StyleBlock("address", alignment = "center", fontSize = 16f),
                StyleBlock("separator", dividerType = DividerType.THICK),
                StyleBlock("datetime", alignment = "left", fontSize = 15f),
                StyleBlock("customer", alignment = "left", fontSize = 16f, bold = true),
                StyleBlock("customer_address", alignment = "left", fontSize = 15f),
                StyleBlock("separator", dividerType = DividerType.THICK),
                StyleBlock("table_heading", alignment = "left", fontSize = 16f, bold = true),
                StyleBlock("separator", dividerType = DividerType.SOLID),
                StyleBlock("items", alignment = "left", fontSize = 16f),
                StyleBlock("separator", dividerType = DividerType.THICK),
                StyleBlock("footer", alignment = "center", fontSize = 15f)
            )
        )

        fun compactStyle(): BillStyle = BillStyle(
            id = "style_compact",
            name = "Compact",
            isBuiltIn = true,
            blocks = listOf(
                StyleBlock("header", alignment = "left", fontSize = 22f, bold = true),
                StyleBlock("header2", alignment = "left", fontSize = 14f),
                StyleBlock("address", alignment = "left", fontSize = 14f),
                StyleBlock("separator", dividerType = DividerType.DASHED),
                StyleBlock("datetime", alignment = "left", fontSize = 14f),
                StyleBlock("customer", alignment = "left", fontSize = 15f, bold = true),
                StyleBlock("customer_address", alignment = "left", fontSize = 14f),
                StyleBlock("separator", dividerType = DividerType.DASHED),
                StyleBlock("table_heading", alignment = "left", fontSize = 14f, bold = true),
                StyleBlock("separator", dividerType = DividerType.DOTTED),
                StyleBlock("items", alignment = "left", fontSize = 14f),
                StyleBlock("separator", dividerType = DividerType.DASHED),
                StyleBlock("footer", alignment = "left", fontSize = 14f)
            )
        )
    }
}

// ==================== Backup ====================

@Serializable
data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val businessDetails: BusinessDetails,
    val printerSettings: PrinterSettings,
    val printElementToggles: PrintElementToggles,
    val customers: List<Customer>,
    val items: List<Item>,
    val whatsAppNumbers: List<WhatsAppNumber>,
    val styles: List<BillStyle>,
    val selectedStyleId: String = "style_simple",
    val bills: List<BillRecord> = emptyList()
)

// ==================== Print Queue ====================

@Serializable
enum class PrintJobStatus {
    PENDING,
    PRINTING,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Serializable
data class PrintJob(
    val id: String = UUID.randomUUID().toString(),
    val bill: BillRecord,
    val status: PrintJobStatus = PrintJobStatus.PENDING,
    val retryCount: Int = 0,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
