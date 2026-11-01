package com.poojaenterprises.pe.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class StorageManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("pe_printer_prefs", Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    // StateFlows
    private val _isSetupComplete = MutableStateFlow(prefs.getBoolean(KEY_SETUP_COMPLETE, false))
    val isSetupComplete: StateFlow<Boolean> = _isSetupComplete.asStateFlow()

    private val _businessDetails = MutableStateFlow(loadBusinessDetails())
    val businessDetails: StateFlow<BusinessDetails> = _businessDetails.asStateFlow()

    private val _printerSettings = MutableStateFlow(loadPrinterSettings())
    val printerSettings: StateFlow<PrinterSettings> = _printerSettings.asStateFlow()

    private val _printToggles = MutableStateFlow(loadPrintToggles())
    val printToggles: StateFlow<PrintElementToggles> = _printToggles.asStateFlow()

    private val _customers = MutableStateFlow(loadCustomers())
    val customers: StateFlow<List<Customer>> = _customers.asStateFlow()

    private val _items = MutableStateFlow(loadItems())
    val items: StateFlow<List<Item>> = _items.asStateFlow()

    private val _whatsAppNumbers = MutableStateFlow(loadWhatsAppNumbers())
    val whatsAppNumbers: StateFlow<List<WhatsAppNumber>> = _whatsAppNumbers.asStateFlow()

    private val _styles = MutableStateFlow(loadStyles())
    val styles: StateFlow<List<BillStyle>> = _styles.asStateFlow()

    private val _selectedStyleId = MutableStateFlow(prefs.getString(KEY_SELECTED_STYLE_ID, "style_simple") ?: "style_simple")
    val selectedStyleId: StateFlow<String> = _selectedStyleId.asStateFlow()

    private val _bills = MutableStateFlow<List<BillRecord>>(emptyList())
    val bills: StateFlow<List<BillRecord>> = _bills.asStateFlow()

    private val _printQueue = MutableStateFlow<List<PrintJob>>(emptyList())
    val printQueue: StateFlow<List<PrintJob>> = _printQueue.asStateFlow()

    init {
        scope.launch {
            _bills.value = loadBillsFromFile()
            _printQueue.value = loadQueueFromFile()
        }
    }

    // ==================== Setup ====================

    fun setSetupComplete(complete: Boolean) {
        prefs.edit().putBoolean(KEY_SETUP_COMPLETE, complete).apply()
        _isSetupComplete.value = complete
    }

    // ==================== Business Details ====================

    private fun loadBusinessDetails(): BusinessDetails {
        val str = prefs.getString(KEY_BUSINESS_DETAILS, null) ?: return BusinessDetails()
        return try { json.decodeFromString(str) } catch (e: Exception) { BusinessDetails() }
    }

    fun saveBusinessDetails(details: BusinessDetails) {
        prefs.edit().putString(KEY_BUSINESS_DETAILS, json.encodeToString(details)).apply()
        _businessDetails.value = details
    }

    // ==================== Printer Settings ====================

    private fun loadPrinterSettings(): PrinterSettings {
        val str = prefs.getString(KEY_PRINTER_SETTINGS, null) ?: return PrinterSettings()
        return try { json.decodeFromString(str) } catch (e: Exception) { PrinterSettings() }
    }

    fun savePrinterSettings(settings: PrinterSettings) {
        prefs.edit().putString(KEY_PRINTER_SETTINGS, json.encodeToString(settings)).apply()
        _printerSettings.value = settings
    }

    // ==================== Toggles ====================

    private fun loadPrintToggles(): PrintElementToggles {
        val str = prefs.getString(KEY_PRINT_TOGGLES, null) ?: return PrintElementToggles()
        return try { json.decodeFromString(str) } catch (e: Exception) { PrintElementToggles() }
    }

    fun savePrintToggles(toggles: PrintElementToggles) {
        prefs.edit().putString(KEY_PRINT_TOGGLES, json.encodeToString(toggles)).apply()
        _printToggles.value = toggles
    }

    // ==================== Customers ====================

    private fun loadCustomers(): List<Customer> {
        val str = prefs.getString(KEY_CUSTOMERS, null) ?: return emptyList()
        return try { json.decodeFromString(str) } catch (e: Exception) { emptyList() }
    }

    fun saveCustomers(list: List<Customer>) {
        prefs.edit().putString(KEY_CUSTOMERS, json.encodeToString(list)).apply()
        _customers.value = list
    }

    fun saveCustomer(customer: Customer) {
        val list = _customers.value.toMutableList()
        val index = list.indexOfFirst { it.name.trim().equals(customer.name.trim(), ignoreCase = true) }
        if (index >= 0) {
            list[index] = customer
        } else {
            list.add(customer)
        }
        saveCustomers(list)
    }

    fun deleteCustomer(id: String) {
        val list = _customers.value.filter { it.id != id }
        saveCustomers(list)
    }

    fun deleteAllCustomers() {
        saveCustomers(emptyList())
    }

    // ==================== Items ====================

    private fun loadItems(): List<Item> {
        val str = prefs.getString(KEY_ITEMS, null) ?: return defaultItems()
        return try { json.decodeFromString(str) } catch (e: Exception) { defaultItems() }
    }

    private fun defaultItems(): List<Item> = listOf(
        Item(name = "सरिया", defaultUnit = UnitType.KG),
        Item(name = "सीमेंट", defaultUnit = UnitType.PCS),
        Item(name = "बालू", defaultUnit = UnitType.KG),
        Item(name = "गिट्टी", defaultUnit = UnitType.KG),
        Item(name = "ईंट", defaultUnit = UnitType.PCS),
        Item(name = "तार", defaultUnit = UnitType.KG)
    )

    fun saveItems(list: List<Item>) {
        prefs.edit().putString(KEY_ITEMS, json.encodeToString(list)).apply()
        _items.value = list
    }

    fun saveItem(item: Item) {
        val list = _items.value.toMutableList()
        val index = list.indexOfFirst { it.name.trim().equals(item.name.trim(), ignoreCase = true) }
        if (index >= 0) {
            list[index] = item
        } else {
            list.add(item)
        }
        saveItems(list)
    }

    fun deleteItem(id: String) {
        val list = _items.value.filter { it.id != id }
        saveItems(list)
    }

    fun deleteAllItems() {
        saveItems(emptyList())
    }

    // ==================== WhatsApp Numbers ====================

    private fun loadWhatsAppNumbers(): List<WhatsAppNumber> {
        val str = prefs.getString(KEY_WHATSAPP_NUMBERS, null) ?: return emptyList()
        return try { json.decodeFromString(str) } catch (e: Exception) { emptyList() }
    }

    fun saveWhatsAppNumbers(list: List<WhatsAppNumber>) {
        prefs.edit().putString(KEY_WHATSAPP_NUMBERS, json.encodeToString(list)).apply()
        _whatsAppNumbers.value = list
    }

    fun saveWhatsAppNumber(number: WhatsAppNumber) {
        val list = _whatsAppNumbers.value.toMutableList()
        val index = list.indexOfFirst { it.id == number.id }
        if (index >= 0) {
            list[index] = number
        } else {
            list.add(number)
        }
        saveWhatsAppNumbers(list)
    }

    fun deleteWhatsAppNumber(id: String) {
        val list = _whatsAppNumbers.value.filter { it.id != id }
        saveWhatsAppNumbers(list)
    }

    fun deleteAllWhatsAppNumbers() {
        saveWhatsAppNumbers(emptyList())
    }

    // ==================== Styles ====================

    private fun loadStyles(): List<BillStyle> {
        val str = prefs.getString(KEY_STYLES, null) ?: return defaultStyles()
        return try {
            val list = json.decodeFromString<List<BillStyle>>(str)
            if (list.isEmpty()) defaultStyles() else list
        } catch (e: Exception) {
            defaultStyles()
        }
    }

    private fun defaultStyles(): List<BillStyle> = listOf(
        BillStyle.simpleStyle(),
        BillStyle.tableStyle(),
        BillStyle.compactStyle()
    )

    fun saveStyles(list: List<BillStyle>) {
        prefs.edit().putString(KEY_STYLES, json.encodeToString(list)).apply()
        _styles.value = list
    }

    fun selectStyle(styleId: String) {
        prefs.edit().putString(KEY_SELECTED_STYLE_ID, styleId).apply()
        _selectedStyleId.value = styleId
    }

    fun addCustomStyle(style: BillStyle) {
        val list = _styles.value.filter { it.id != style.id } + style
        saveStyles(list)
    }

    fun deleteStyle(styleId: String) {
        val list = _styles.value.filter { it.id != styleId && !it.isBuiltIn }
        saveStyles(list)
        if (_selectedStyleId.value == styleId) {
            selectStyle("style_simple")
        }
    }

    fun getSelectedStyle(): BillStyle {
        val currentId = _selectedStyleId.value
        return _styles.value.find { it.id == currentId } ?: BillStyle.simpleStyle()
    }

    // ==================== Bills (History) ====================

    private fun billsFile(): File = File(context.filesDir, "bills_history.json")

    private fun loadBillsFromFile(): List<BillRecord> {
        val file = billsFile()
        if (!file.exists()) return emptyList()
        return try {
            json.decodeFromString(file.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveBill(bill: BillRecord) = withContext(Dispatchers.IO) {
        val current = _bills.value.toMutableList()
        current.add(0, bill) // newest first
        _bills.value = current
        try {
            billsFile().writeText(json.encodeToString(current))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteBill(billId: String) = withContext(Dispatchers.IO) {
        val current = _bills.value.filter { it.id != billId }
        _bills.value = current
        try {
            billsFile().writeText(json.encodeToString(current))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteAllBills() = withContext(Dispatchers.IO) {
        _bills.value = emptyList()
        try {
            billsFile().delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==================== Print Queue File Persistence ====================

    private fun queueFile(): File = File(context.filesDir, "print_queue.json")

    private fun loadQueueFromFile(): List<PrintJob> {
        val file = queueFile()
        if (!file.exists()) return emptyList()
        return try {
            json.decodeFromString(file.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun persistQueue(jobs: List<PrintJob>) = withContext(Dispatchers.IO) {
        _printQueue.value = jobs
        try {
            queueFile().writeText(json.encodeToString(jobs))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==================== Backup Export & Import ====================

    fun createBackupData(): BackupData {
        return BackupData(
            version = 1,
            timestamp = System.currentTimeMillis(),
            businessDetails = _businessDetails.value,
            printerSettings = _printerSettings.value,
            printElementToggles = _printToggles.value,
            customers = _customers.value,
            items = _items.value,
            whatsAppNumbers = _whatsAppNumbers.value,
            styles = _styles.value,
            selectedStyleId = _selectedStyleId.value,
            bills = _bills.value
        )
    }

    fun exportBackupJson(): String {
        return json.encodeToString(createBackupData())
    }

    suspend fun importBackupJson(jsonString: String, merge: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val backup = json.decodeFromString<BackupData>(jsonString)
            if (backup.version <= 0) {
                return@withContext Result.failure(Exception("Invalid backup format"))
            }

            if (merge) {
                // Merge customers
                val existingCusts = _customers.value.toMutableList()
                backup.customers.forEach { bCust ->
                    if (existingCusts.none { it.name.trim().equals(bCust.name.trim(), ignoreCase = true) }) {
                        existingCusts.add(bCust)
                    }
                }
                saveCustomers(existingCusts)

                // Merge items
                val existingItems = _items.value.toMutableList()
                backup.items.forEach { bItem ->
                    if (existingItems.none { it.name.trim().equals(bItem.name.trim(), ignoreCase = true) }) {
                        existingItems.add(bItem)
                    }
                }
                saveItems(existingItems)

                // Merge numbers
                val existingNumbers = _whatsAppNumbers.value.toMutableList()
                backup.whatsAppNumbers.forEach { bNum ->
                    if (existingNumbers.none { it.phoneNumber == bNum.phoneNumber }) {
                        existingNumbers.add(bNum)
                    }
                }
                saveWhatsAppNumbers(existingNumbers)

                // Merge bills
                val existingBills = _bills.value.toMutableList()
                backup.bills.forEach { bBill ->
                    if (existingBills.none { it.id == bBill.id }) {
                        existingBills.add(bBill)
                    }
                }
                _bills.value = existingBills.sortedByDescending { it.createdAt }
                billsFile().writeText(json.encodeToString(_bills.value))
            } else {
                // Replace everything
                saveBusinessDetails(backup.businessDetails)
                savePrinterSettings(backup.printerSettings)
                savePrintToggles(backup.printElementToggles)
                saveCustomers(backup.customers)
                saveItems(backup.items)
                saveWhatsAppNumbers(backup.whatsAppNumbers)
                saveStyles(backup.styles)
                selectStyle(backup.selectedStyleId)
                _bills.value = backup.bills.sortedByDescending { it.createdAt }
                billsFile().writeText(json.encodeToString(_bills.value))
            }

            setSetupComplete(true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val KEY_SETUP_COMPLETE = "pe_setup_complete"
        private const val KEY_BUSINESS_DETAILS = "pe_business_details"
        private const val KEY_PRINTER_SETTINGS = "pe_printer_settings"
        private const val KEY_PRINT_TOGGLES = "pe_print_toggles"
        private const val KEY_CUSTOMERS = "pe_customers"
        private const val KEY_ITEMS = "pe_items"
        private const val KEY_WHATSAPP_NUMBERS = "pe_whatsapp_numbers"
        private const val KEY_STYLES = "pe_styles"
        private const val KEY_SELECTED_STYLE_ID = "pe_selected_style_id"
    }
}
