package com.poojaenterprises.pe.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poojaenterprises.pe.data.PaperSize
import com.poojaenterprises.pe.ui.theme.GoldPrimary
import com.poojaenterprises.pe.ui.theme.RedDark
import com.poojaenterprises.pe.ui.theme.RedPrimary
import com.poojaenterprises.pe.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentPage by remember { mutableIntStateOf(1) }

    // Page 2 State pre-filled with shop details
    var businessName by remember { mutableStateOf("Pooja Enterprises") }
    var headerLine2 by remember { mutableStateOf("Building Material") }
    var address by remember { mutableStateOf("पियरवा पोखरा चकिया रोड अहरौरा") }
    var printerIp by remember { mutableStateOf("192.168.1.100") }
    var paperSize by remember { mutableStateOf(PaperSize.SIZE_80MM) }

    // Auto scan dialog state
    var showScanDialog by remember { mutableStateOf(false) }
    val isScanning by viewModel.isScanning.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val foundIps by viewModel.foundPrinterIps.collectAsState()

    // Backup import launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val content = context.contentResolver.openInputStream(uri)?.use {
                        it.bufferedReader().readText()
                    }
                    if (content != null) {
                        val result = viewModel.storage.importBackupJson(content, merge = false)
                        if (result.isSuccess) {
                            Toast.makeText(context, "Backup restored successfully", Toast.LENGTH_SHORT).show()
                            viewModel.storage.setSetupComplete(true)
                        } else {
                            Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to read backup: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFDF7))
            .padding(24.dp)
    ) {
        if (currentPage == 1) {
            // ==================== Page 1: Import Backup or Skip ====================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo Icon Card
                Card(
                    modifier = Modifier.size(96.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = RedPrimary)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "PE",
                            color = GoldPrimary,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Welcome to PE",
                    style = MaterialTheme.typography.titleLarge,
                    color = RedDark,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Fast LAN Thermal Bill Printer for Pooja Enterprises",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 40.dp)
                )

                // Import Backup button
                Button(
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Import backup", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Skip button
                OutlinedButton(
                    onClick = { currentPage = 2 },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Skip", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = RedPrimary)
                }
            }
        } else {
            // ==================== Page 2: Shop Details & Printer IP ====================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Shop & Printer Setup",
                    style = MaterialTheme.typography.titleLarge,
                    color = RedDark,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Enter details for receipt printing. You can edit these later in Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = { Text("Business Name (Hindi / English)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = headerLine2,
                    onValueChange = { headerLine2 = it },
                    label = { Text("Header Line 2 (Optional)") },
                    placeholder = { Text("e.g. Building Material") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address (पियरवा पोखरा चकिया रोड अहरौरा)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Printer IP with Auto Scan button
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = printerIp,
                            onValueChange = { printerIp = it },
                            label = { Text("Printer IP Address") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Button(
                            onClick = {
                                showScanDialog = true
                                viewModel.startSubnetScan()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Auto scan", color = Color.White)
                        }
                    }
                }

                // Paper Size Selector
                Text(text = "Paper Size", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        RadioButton(
                            selected = paperSize == PaperSize.SIZE_80MM,
                            onClick = { paperSize = PaperSize.SIZE_80MM }
                        )
                        Text(text = "80 mm (3 inch)")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        RadioButton(
                            selected = paperSize == PaperSize.SIZE_58MM,
                            onClick = { paperSize = PaperSize.SIZE_58MM }
                        )
                        Text(text = "58 mm (2 inch)")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Finish button
                Button(
                    onClick = {
                        val currentBusiness = viewModel.storage.businessDetails.value.copy(
                            name = businessName.trim(),
                            headerLine2 = headerLine2.trim(),
                            address = address.trim()
                        )
                        val currentPrinter = viewModel.storage.printerSettings.value.copy(
                            ipAddress = printerIp.trim(),
                            paperSize = paperSize
                        )
                        viewModel.storage.saveBusinessDetails(currentBusiness)
                        viewModel.storage.savePrinterSettings(currentPrinter)
                        viewModel.storage.setSetupComplete(true)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Finish", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
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
                        Text("No printers found. Make sure the phone and printer are on the same Wi-Fi router.")
                    } else {
                        Text("Select your printer IP:", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        foundIps.forEach { ip ->
                            TextButton(
                                onClick = {
                                    printerIp = ip
                                    showScanDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
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
                TextButton(onClick = { showScanDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
