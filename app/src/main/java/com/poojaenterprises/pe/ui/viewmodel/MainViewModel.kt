package com.poojaenterprises.pe.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.poojaenterprises.pe.PEApplication
import com.poojaenterprises.pe.data.BillItem
import com.poojaenterprises.pe.data.BillItemInput
import com.poojaenterprises.pe.data.BillRecord
import com.poojaenterprises.pe.data.Customer
import com.poojaenterprises.pe.data.Item
import com.poojaenterprises.pe.data.PrinterStatus
import com.poojaenterprises.pe.data.UnitType
import com.poojaenterprises.pe.data.WhatsAppNumber
import com.poojaenterprises.pe.data.WhatsAppNumberType
import com.poojaenterprises.pe.printing.EscPosBitmapRenderer
import com.poojaenterprises.pe.printing.LanScanner
import com.poojaenterprises.pe.utils.WhatsAppHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PEApplication
    val storage = app.storageManager
    val printQueue = app.printQueueManager

    // Setup state
    val isSetupComplete: StateFlow<Boolean> = storage.isSetupComplete

    // Status & Queue
    val printerStatus: StateFlow<PrinterStatus> = printQueue.printerStatus
    val printQueueJobs = storage.printQueue
    val pendingCount: StateFlow<Int> = storage.printQueue.map { list ->
        list.count { it.status == com.poojaenterprises.pe.data.PrintJobStatus.PENDING || it.status == com.poojaenterprises.pe.data.PrintJobStatus.PRINTING }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    // Current Bill Form Inputs
    val billDate = MutableStateFlow(SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date()))
    val billTime = MutableStateFlow(SimpleDateFormat("hh:mm a", Locale.US).format(Date()))
    val customerName = MutableStateFlow("")
    val customerAddress = MutableStateFlow("")
    val billItems = MutableStateFlow(listOf(BillItemInput()))

    // WhatsApp Inputs
    val sendWhatsApp = MutableStateFlow(false)
    val selectedWhatsAppNumber = MutableStateFlow<WhatsAppNumber?>(null)
    val customWhatsAppPhone = MutableStateFlow("")
    val preferWhatsAppBusiness = MutableStateFlow(false)

    // Print Preview state
    val previewBitmap = MutableStateFlow<Bitmap?>(null)
    val showPreviewDialog = MutableStateFlow(false)

    // Lan Scanner state
    val isScanning = MutableStateFlow(false)
    val scanProgress = MutableStateFlow(0)
    val foundPrinterIps = MutableStateFlow<List<String>>(emptyList())

    // Pending Queue Dialog state
    val showQueueDialog = MutableStateFlow(false)

    init {
        // Wire up print completion for WhatsApp trigger
        printQueue.onBillPrintedSuccessfully = { bill, pdfFile ->
            if (sendWhatsApp.value && pdfFile != null) {
                val targetPhone = selectedWhatsAppNumber.value?.phoneNumber?.ifBlank { null }
                    ?: customWhatsAppPhone.value.ifBlank { null }
                    ?: bill.customerWhatsAppNumber.ifBlank { null }

                if (!targetPhone.isNullOrBlank()) {
                    viewModelScope.launch {
                        WhatsAppHelper.shareBillPdfToWhatsApp(
                            context = getApplication(),
                            pdfFile = pdfFile,
                            phoneNumber = targetPhone,
                            preferBusiness = preferWhatsAppBusiness.value
                        )
                    }
                }
            }
        }
    }

    // ==================== Customer Selection & Suggestions ====================

    fun onCustomerNameChanged(query: String) {
        customerName.value = query
    }

    fun onCustomerSelected(customer: Customer) {
        customerName.value = customer.name
        customerAddress.value = customer.address
        if (customer.whatsAppNumber.isNotBlank()) {
            customWhatsAppPhone.value = customer.whatsAppNumber
            // Check if there is a matching saved WhatsAppNumber
            val match = storage.whatsAppNumbers.value.find { it.phoneNumber == customer.whatsAppNumber }
            if (match != null) {
                selectedWhatsAppNumber.value = match
            }
        }
    }

    fun saveCurrentCustomer() {
        val name = customerName.value.trim()
        if (name.isBlank()) {
            Toast.makeText(getApplication(), "Enter customer name first", Toast.LENGTH_SHORT).show()
            return
        }
        val phone = selectedWhatsAppNumber.value?.phoneNumber ?: customWhatsAppPhone.value
        val customer = Customer(
            name = name,
            address = customerAddress.value.trim(),
            whatsAppNumber = phone
        )
        storage.saveCustomer(customer)
        Toast.makeText(getApplication(), "Customer saved", Toast.LENGTH_SHORT).show()
    }

    // ==================== Item Rows Management ====================

    fun addItemRow() {
        billItems.value = billItems.value + BillItemInput()
    }

    fun removeItemRow(index: Int) {
        if (billItems.value.size > 1 && index in billItems.value.indices) {
            val updated = billItems.value.toMutableList()
            updated.removeAt(index)
            billItems.value = updated
        }
    }

    fun updateItemName(index: Int, name: String) {
        if (index in billItems.value.indices) {
            val updated = billItems.value.toMutableList()
            // If item name matches saved item, auto-select default unit
            val matchedItem = storage.items.value.find { it.name.trim().equals(name.trim(), ignoreCase = true) }
            val unit = matchedItem?.defaultUnit ?: updated[index].unit
            updated[index] = updated[index].copy(itemName = name, unit = unit)
            billItems.value = updated
        }
    }

    fun updateItemQuantity(index: Int, qtyStr: String) {
        if (index in billItems.value.indices) {
            val updated = billItems.value.toMutableList()
            updated[index] = updated[index].copy(quantityStr = qtyStr)
            billItems.value = updated
        }
    }

    fun updateItemUnit(index: Int, unit: UnitType) {
        if (index in billItems.value.indices) {
            val updated = billItems.value.toMutableList()
            updated[index] = updated[index].copy(unit = unit)
            billItems.value = updated
        }
    }

    // ==================== Print Action ====================

    fun onPrintTapped() {
        // Validate items
        val validItems = billItems.value.filter { it.itemName.isNotBlank() }
        if (validItems.isEmpty()) {
            Toast.makeText(getApplication(), "Please add at least one item", Toast.LENGTH_SHORT).show()
            return
        }

        val parsedBillItems = validItems.map { input ->
            val qty = input.quantityStr.toDoubleOrNull() ?: 1.0
            BillItem(itemName = input.itemName.trim(), quantity = qty, unit = input.unit)
        }

        val phone = selectedWhatsAppNumber.value?.phoneNumber ?: customWhatsAppPhone.value
        val bill = BillRecord(
            date = billDate.value,
            time = billTime.value,
            customerName = customerName.value.trim(),
            customerAddress = customerAddress.value.trim(),
            customerWhatsAppNumber = phone,
            items = parsedBillItems
        )

        val toggles = storage.printToggles.value
        val bitmap = EscPosBitmapRenderer.renderBillToBitmap(
            bill = bill,
            businessDetails = storage.businessDetails.value,
            printerSettings = storage.printerSettings.value,
            toggles = toggles,
            style = storage.getSelectedStyle()
        )

        if (toggles.printPreview) {
            previewBitmap.value = bitmap
            showPreviewDialog.value = true
        } else {
            confirmAndEnqueueBill(bill)
        }
    }

    fun confirmAndEnqueueBill(customBill: BillRecord? = null) {
        showPreviewDialog.value = false

        val billToPrint: BillRecord = customBill ?: run {
            val validItems = billItems.value.filter { it.itemName.isNotBlank() }.map { input ->
                val qty = input.quantityStr.toDoubleOrNull() ?: 1.0
                BillItem(itemName = input.itemName.trim(), quantity = qty, unit = input.unit)
            }
            val phone = selectedWhatsAppNumber.value?.phoneNumber ?: customWhatsAppPhone.value
            BillRecord(
                date = billDate.value,
                time = billTime.value,
                customerName = customerName.value.trim(),
                customerAddress = customerAddress.value.trim(),
                customerWhatsAppNumber = phone,
                items = validItems
            )
        }

        // Auto-save customer name if new
        if (billToPrint.customerName.isNotBlank()) {
            val existing = storage.customers.value.find { it.name.trim().equals(billToPrint.customerName.trim(), ignoreCase = true) }
            if (existing == null) {
                storage.saveCustomer(
                    Customer(
                        name = billToPrint.customerName,
                        address = billToPrint.customerAddress,
                        whatsAppNumber = billToPrint.customerWhatsAppNumber
                    )
                )
            }
        }

        // Auto-save new item names
        billToPrint.items.forEach { bItem ->
            val existing = storage.items.value.find { it.name.trim().equals(bItem.itemName.trim(), ignoreCase = true) }
            if (existing == null) {
                storage.saveItem(Item(name = bItem.itemName, defaultUnit = bItem.unit))
            }
        }

        // Enqueue to print queue
        printQueue.enqueueBill(billToPrint)
        Toast.makeText(getApplication(), "Bill sent to print queue", Toast.LENGTH_SHORT).show()

        // Reset form for next bill (keep date/time current)
        billDate.value = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())
        billTime.value = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
        customerName.value = ""
        customerAddress.value = ""
        billItems.value = listOf(BillItemInput())
        sendWhatsApp.value = false
        selectedWhatsAppNumber.value = null
        customWhatsAppPhone.value = ""
    }

    fun reprintBill(bill: BillRecord) {
        printQueue.enqueueBill(bill)
        Toast.makeText(getApplication(), "Reprint queued", Toast.LENGTH_SHORT).show()
    }

    fun testPrint() {
        val testBill = BillRecord(
            date = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date()),
            time = SimpleDateFormat("hh:mm a", Locale.US).format(Date()),
            customerName = "Test Customer (ग्राहक टेस्ट)",
            customerAddress = "पियरवा पोखरा चकिया रोड अहरौरा",
            items = listOf(
                BillItem(itemName = "सरिया (TMT Rebar)", quantity = 250.2, unit = UnitType.KG),
                BillItem(itemName = "सीमेंट (UltraTech)", quantity = 20.0, unit = UnitType.PCS)
            )
        )
        printQueue.enqueueBill(testBill)
        Toast.makeText(getApplication(), "Test print queued", Toast.LENGTH_SHORT).show()
    }

    // ==================== Lan Scanner ====================

    fun startSubnetScan() {
        viewModelScope.launch {
            isScanning.value = true
            scanProgress.value = 0
            foundPrinterIps.value = emptyList()

            val port = storage.printerSettings.value.port
            val ips = LanScanner.scanSubnetForPrinters(
                context = getApplication(),
                targetPort = port,
                onProgress = { scanProgress.value = it }
            )

            foundPrinterIps.value = ips
            isScanning.value = false
            if (ips.isEmpty()) {
                Toast.makeText(getApplication(), "No printers found on current Wi-Fi subnet", Toast.LENGTH_LONG).show()
            }
        }
    }
}
