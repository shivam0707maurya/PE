package com.poojaenterprises.pe.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poojaenterprises.pe.data.BillStyle
import com.poojaenterprises.pe.data.PaperSize
import com.poojaenterprises.pe.ui.theme.GoldPrimary
import com.poojaenterprises.pe.ui.theme.RedDark
import com.poojaenterprises.pe.ui.theme.RedPrimary
import com.poojaenterprises.pe.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateToCustomers: () -> Unit,
    onNavigateToItems: () -> Unit,
    onNavigateToNumbers: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val business by viewModel.storage.businessDetails.collectAsState()
    val printer by viewModel.storage.printerSettings.collectAsState()
    val toggles by viewModel.storage.printToggles.collectAsState()
    val styles by viewModel.storage.styles.collectAsState()
    val selectedStyleId by viewModel.storage.selectedStyleId.collectAsState()

    val isScanning by viewModel.isScanning.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val foundIps by viewModel.foundPrinterIps.collectAsState()

    var showScanDialog by remember { mutableStateOf(false) }
    var showImportMergeDialog by remember { mutableStateOf(false) }
    var pendingImportJson by remember { mutableStateOf<String?>(null) }

    // Launcher for style json import
    val styleImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                if (content != null) {
                    val customStyle = Json { ignoreUnknownKeys = true }.decodeFromString<BillStyle>(content)
                    viewModel.storage.addCustomStyle(customStyle.copy(isBuiltIn = false))
                    viewModel.storage.selectStyle(customStyle.id)
                    Toast.makeText(context, "Style '${customStyle.name}' imported", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Invalid style JSON: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Launcher for backup import
    val backupImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                if (content != null) {
                    pendingImportJson = content
                    showImportMergeDialog = true
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9FA))
    ) {
        // Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 2.dp,
            color = Color.White
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = RedDark,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==================== 1. What to Print ====================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "What to Print",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = RedDark,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    ToggleRow("Business name", toggles.businessName) {
                        viewModel.storage.savePrintToggles(toggles.copy(businessName = it))
                    }
                    ToggleRow("Header line 2", toggles.headerLine2) {
                        viewModel.storage.savePrintToggles(toggles.copy(headerLine2 = it))
                    }
                    ToggleRow("Address", toggles.address) {
                        viewModel.storage.savePrintToggles(toggles.copy(address = it))
                    }
                    ToggleRow("Date", toggles.date) {
                        viewModel.storage.savePrintToggles(toggles.copy(date = it))
                    }
                    ToggleRow("Time", toggles.time) {
                        viewModel.storage.savePrintToggles(toggles.copy(time = it))
                    }
                    ToggleRow("Customer name", toggles.customerName) {
                        viewModel.storage.savePrintToggles(toggles.copy(customerName = it))
                    }
                    ToggleRow("Customer address", toggles.customerAddress) {
                        viewModel.storage.savePrintToggles(toggles.copy(customerAddress = it))
                    }
                    ToggleRow("Table heading", toggles.tableHeading) {
                        viewModel.storage.savePrintToggles(toggles.copy(tableHeading = it))
                    }
                    ToggleRow("Item quantity & unit", toggles.itemQtyUnit) {
                        viewModel.storage.savePrintToggles(toggles.copy(itemQtyUnit = it))
                    }
                    ToggleRow("Footer", toggles.footer) {
                        viewModel.storage.savePrintToggles(toggles.copy(footer = it))
                    }
                    ToggleRow("Separator lines", toggles.separators) {
                        viewModel.storage.savePrintToggles(toggles.copy(separators = it))
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "App Switches",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    ToggleRow("Auto cut", toggles.autoCut) {
                        viewModel.storage.savePrintToggles(toggles.copy(autoCut = it))
                    }
                    ToggleRow("WhatsApp sending", toggles.whatsappSending) {
                        viewModel.storage.savePrintToggles(toggles.copy(whatsappSending = it))
                    }
                    ToggleRow("Saving PDF history", toggles.pdfHistory) {
                        viewModel.storage.savePrintToggles(toggles.copy(pdfHistory = it))
                    }
                    ToggleRow("Print preview", toggles.printPreview) {
                        viewModel.storage.savePrintToggles(toggles.copy(printPreview = it))
                    }
                }
            }

            // ==================== 2. Business Details ====================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Business Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = RedDark
                    )

                    OutlinedTextField(
                        value = business.name,
                        onValueChange = { viewModel.storage.saveBusinessDetails(business.copy(name = it)) },
                        label = { Text("Business Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = business.headerLine2,
                        onValueChange = { viewModel.storage.saveBusinessDetails(business.copy(headerLine2 = it)) },
                        label = { Text("Header Line 2") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = business.address,
                        onValueChange = { viewModel.storage.saveBusinessDetails(business.copy(address = it)) },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = business.tableHeading,
                        onValueChange = { viewModel.storage.saveBusinessDetails(business.copy(tableHeading = it)) },
                        label = { Text("Table Heading (Left | Right)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = business.footer,
                        onValueChange = { viewModel.storage.saveBusinessDetails(business.copy(footer = it)) },
                        label = { Text("Footer Text") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // ==================== 3. Printer Settings ====================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Printer Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = RedDark
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = printer.ipAddress,
                            onValueChange = { viewModel.storage.savePrinterSettings(printer.copy(ipAddress = it)) },
                            label = { Text("Printer IP") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Button(
                            onClick = {
                                showScanDialog = true
                                viewModel.startSubnetScan()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Auto scan", color = Color.White)
                        }
                    }

                    // Paper Size
                    Text(text = "Paper Size", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = printer.paperSize == PaperSize.SIZE_80MM,
                                onClick = { viewModel.storage.savePrinterSettings(printer.copy(paperSize = PaperSize.SIZE_80MM)) }
                            )
                            Text("80 mm")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = printer.paperSize == PaperSize.SIZE_58MM,
                                onClick = { viewModel.storage.savePrinterSettings(printer.copy(paperSize = PaperSize.SIZE_58MM)) }
                            )
                            Text("58 mm")
                        }
                    }

                    // Test Print button
                    Button(
                        onClick = { viewModel.testPrint() },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test print")
                    }
                }
            }

            // ==================== 4. Bill Style ====================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Bill Style",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = RedDark,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    styles.forEach { style ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.storage.selectStyle(style.id) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedStyleId == style.id,
                                onClick = { viewModel.storage.selectStyle(style.id) }
                            )
                            Text(
                                text = style.name,
                                fontWeight = if (selectedStyleId == style.id) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (!style.isBuiltIn) {
                                IconButton(onClick = { viewModel.storage.deleteStyle(style.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete style", tint = Color.Gray)
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { styleImportLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import style")
                    }
                }
            }

            // ==================== 5. Saved Data Management ====================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Saved Data Management",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = RedDark,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    SettingNavItem(
                        icon = Icons.Default.People,
                        title = "Customers",
                        subtitle = "${viewModel.storage.customers.collectAsState().value.size} saved",
                        onClick = onNavigateToCustomers
                    )
                    SettingNavItem(
                        icon = Icons.Default.Inventory,
                        title = "Items & Units",
                        subtitle = "${viewModel.storage.items.collectAsState().value.size} saved",
                        onClick = onNavigateToItems
                    )
                    SettingNavItem(
                        icon = Icons.Default.ContactPhone,
                        title = "WhatsApp Numbers",
                        subtitle = "${viewModel.storage.whatsAppNumbers.collectAsState().value.size} saved",
                        onClick = onNavigateToNumbers
                    )
                }
            }

            // ==================== 6. Backup & Restore ====================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Backup & Restore",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = RedDark
                    )

                    // Export Backup Button
                    Button(
                        onClick = {
                            val jsonString = viewModel.storage.exportBackupJson()
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "PE_Shop_Backup.json")
                                putExtra(Intent.EXTRA_TEXT, jsonString)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Export backup"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export backup")
                    }

                    // Import Backup Button
                    OutlinedButton(
                        onClick = { backupImportLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import backup")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Auto scan dialog
    if (showScanDialog) {
        AlertDialog(
            onDismissRequest = { showScanDialog = false },
            title = { Text("LAN Subnet Scan (Port 9100)") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isScanning) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Scanning Wi-Fi subnet: $scanProgress%")
                        }
                    } else if (foundIps.isEmpty()) {
                        Text("No printers found on current Wi-Fi subnet.")
                    } else {
                        Text("Select found printer IP:", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        foundIps.forEach { ip ->
                            TextButton(
                                onClick = {
                                    viewModel.storage.savePrinterSettings(printer.copy(ipAddress = ip))
                                    showScanDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                    Icon(Icons.Default.Print, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(ip, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showScanDialog = false }) { Text("Close") }
            }
        )
    }

    // Backup Merge or Replace dialog
    if (showImportMergeDialog && pendingImportJson != null) {
        AlertDialog(
            onDismissRequest = { showImportMergeDialog = false },
            title = { Text("Restore Backup") },
            text = { Text("Do you want to merge with existing data or replace everything?") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val res = viewModel.storage.importBackupJson(pendingImportJson!!, merge = true)
                            if (res.isSuccess) {
                                Toast.makeText(context, "Backup merged successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                            showImportMergeDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) {
                    Text("Merge")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            val res = viewModel.storage.importBackupJson(pendingImportJson!!, merge = false)
                            if (res.isSuccess) {
                                Toast.makeText(context, "Backup replaced successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                            showImportMergeDialog = false
                        }
                    }
                ) {
                    Text("Replace all")
                }
            }
        )
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
}
