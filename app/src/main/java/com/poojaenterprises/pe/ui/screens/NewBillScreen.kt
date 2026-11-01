package com.poojaenterprises.pe.ui.screens

import androidx.compose.material3.TextButton
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poojaenterprises.pe.data.Customer
import com.poojaenterprises.pe.data.PrinterStatus
import com.poojaenterprises.pe.data.UnitType
import com.poojaenterprises.pe.data.WhatsAppNumber
import com.poojaenterprises.pe.data.WhatsAppNumberType
import com.poojaenterprises.pe.ui.theme.GoldPrimary
import com.poojaenterprises.pe.ui.theme.RedDark
import com.poojaenterprises.pe.ui.theme.RedPrimary
import com.poojaenterprises.pe.ui.theme.StatusBusy
import com.poojaenterprises.pe.ui.theme.StatusIdle
import com.poojaenterprises.pe.ui.theme.StatusOffline
import com.poojaenterprises.pe.ui.theme.StatusPaper
import com.poojaenterprises.pe.ui.theme.StatusPrinting
import com.poojaenterprises.pe.ui.viewmodel.MainViewModel
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NewBillScreen(viewModel: MainViewModel) {
    val context = LocalContext.current

    // Observe state
    val toggles by viewModel.storage.printToggles.collectAsState()
    val printerStatus by viewModel.printerStatus.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    val savedCustomers by viewModel.storage.customers.collectAsState()
    val savedItems by viewModel.storage.items.collectAsState()
    val savedNumbers by viewModel.storage.whatsAppNumbers.collectAsState()

    val billDate by viewModel.billDate.collectAsState()
    val billTime by viewModel.billTime.collectAsState()
    val customerName by viewModel.customerName.collectAsState()
    val customerAddress by viewModel.customerAddress.collectAsState()
    val items by viewModel.billItems.collectAsState()

    val sendWhatsApp by viewModel.sendWhatsApp.collectAsState()
    val selectedNumber by viewModel.selectedWhatsAppNumber.collectAsState()
    val customPhone by viewModel.customWhatsAppPhone.collectAsState()
    val preferBusiness by viewModel.preferWhatsAppBusiness.collectAsState()

    val showPreview by viewModel.showPreviewDialog.collectAsState()
    val previewBmp by viewModel.previewBitmap.collectAsState()
    val showQueue by viewModel.showQueueDialog.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9FA))
    ) {
        // ==================== Header Status Bar ====================
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 2.dp,
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "PE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = RedPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "New Bill",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = Color(0xFF333333)
                    )
                }

                // Printer Status indicator & Pending badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (statusColor, statusLabel) = when (printerStatus) {
                        PrinterStatus.IDLE -> StatusIdle to "Idle"
                        PrinterStatus.PRINTING -> StatusPrinting to "Printing"
                        PrinterStatus.BUSY -> StatusBusy to "Busy"
                        PrinterStatus.PAPER_COVER -> StatusPaper to "Paper / Cover"
                        PrinterStatus.OFFLINE -> StatusOffline to "Offline"
                    }

                    // Status Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = statusColor.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(statusColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusLabel,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Pending count button
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (pendingCount > 0) StatusBusy.copy(alpha = 0.2f) else Color(0xFFEEEEEE),
                        modifier = Modifier.clickable { viewModel.showQueueDialog.value = true }
                    ) {
                        Text(
                            text = "Pending: $pendingCount",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (pendingCount > 0) StatusBusy else Color.DarkGray
                        )
                    }
                }
            }
        }

        // ==================== Main Bill Form (Scrollable) ====================
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Date & Time Row (Only if enabled)
            if (toggles.date || toggles.time) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (toggles.date) {
                        OutlinedTextField(
                            value = billDate,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    val c = Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            viewModel.billDate.value = String.format("%02d-%02d-%04d", d, m + 1, y)
                                        },
                                        c.get(Calendar.YEAR),
                                        c.get(Calendar.MONTH),
                                        c.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = RedPrimary)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    if (toggles.time) {
                        OutlinedTextField(
                            value = billTime,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Time") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    val c = Calendar.getInstance()
                                    TimePickerDialog(
                                        context,
                                        { _, h, m ->
                                            val amPm = if (h >= 12) "PM" else "AM"
                                            val hour12 = if (h % 12 == 0) 12 else h % 12
                                            viewModel.billTime.value = String.format("%02d:%02d %s", hour12, m, amPm)
                                        },
                                        c.get(Calendar.HOUR_OF_DAY),
                                        c.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                }) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = RedPrimary)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Customer Name & Suggestions (Only if enabled)
            if (toggles.customerName) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Customer Name",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = RedDark
                            )
                            // "Save customer" button
                            TextButton(onClick = { viewModel.saveCurrentCustomer() }) {
                                Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save customer", fontSize = 13.sp)
                            }
                        }

                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { viewModel.onCustomerNameChanged(it) },
                            placeholder = { Text("Type name (Hindi / English)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Customer Autocomplete Suggestions
                        val suggestions = if (customerName.isNotBlank()) {
                            savedCustomers.filter {
                                it.name.contains(customerName.trim(), ignoreCase = true)
                            }.take(5)
                        } else emptyList()

                        if (suggestions.isNotEmpty()) {
                            Text(
                                text = "Suggestions:",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                suggestions.forEach { cust ->
                                    AssistChip(
                                        onClick = { viewModel.onCustomerSelected(cust) },
                                        label = { Text(cust.name) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Customer Address (Only if enabled)
            if (toggles.customerAddress) {
                OutlinedTextField(
                    value = customerAddress,
                    onValueChange = { viewModel.customerAddress.value = it },
                    label = { Text("Customer Address") },
                    placeholder = { Text("Address (auto-filled for saved customers)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 2
                )
            }

            // ==================== Items Table ====================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Items (सामान)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = RedDark,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Suggestion chips from saved items
                    if (savedItems.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            savedItems.take(8).forEach { sItem ->
                                AssistChip(
                                    onClick = {
                                        // Find first empty item row or add new
                                        val emptyIdx = items.indexOfFirst { it.itemName.isBlank() }
                                        if (emptyIdx >= 0) {
                                            viewModel.updateItemName(emptyIdx, sItem.name)
                                        } else {
                                            viewModel.addItemRow()
                                            viewModel.updateItemName(items.size, sItem.name)
                                        }
                                    },
                                    label = { Text("${sItem.name} (${sItem.defaultUnit.label})") }
                                )
                            }
                        }
                    }

                    // Items rows
                    items.forEachIndexed { index, rowItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Item Name Field
                            OutlinedTextField(
                                value = rowItem.itemName,
                                onValueChange = { viewModel.updateItemName(index, it) },
                                label = { Text("Item ${index + 1}") },
                                placeholder = { Text("सरिया / सीमेंट") },
                                modifier = Modifier.weight(1.8f),
                                shape = RoundedCornerShape(8.dp)
                            )

                            // Quantity Field
                            OutlinedTextField(
                                value = rowItem.quantityStr,
                                onValueChange = { viewModel.updateItemQuantity(index, it) },
                                label = { Text("Qty") },
                                placeholder = { Text(if (rowItem.unit == UnitType.KG) "250.2" else "10") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(8.dp)
                            )

                            // Unit Selector (Pcs / Kg)
                            Column(modifier = Modifier.weight(1.1f)) {
                                Row(
                                    modifier = Modifier
                                        .background(Color(0xFFF0F0F2), RoundedCornerShape(8.dp))
                                        .padding(2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (rowItem.unit == UnitType.PCS) RedPrimary else Color.Transparent,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable { viewModel.updateItemUnit(index, UnitType.PCS) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Pcs",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (rowItem.unit == UnitType.PCS) Color.White else Color.DarkGray
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (rowItem.unit == UnitType.KG) RedPrimary else Color.Transparent,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable { viewModel.updateItemUnit(index, UnitType.KG) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Kg",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (rowItem.unit == UnitType.KG) Color.White else Color.DarkGray
                                        )
                                    }
                                }
                            }

                            // Remove row button (only if more than 1 row)
                            if (items.size > 1) {
                                IconButton(
                                    onClick = { viewModel.removeItemRow(index) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove row", tint = Color.Gray)
                                }
                            }
                        }
                    }

                    // Add Row button
                    OutlinedButton(
                        onClick = { viewModel.addItemRow() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add row")
                    }
                }
            }

            // ==================== WhatsApp Sending Option ====================
            if (toggles.whatsappSending) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.sendWhatsApp.value = !sendWhatsApp }
                        ) {
                            Checkbox(
                                checked = sendWhatsApp,
                                onCheckedChange = { viewModel.sendWhatsApp.value = it }
                            )
                            Text(
                                text = "Send on WhatsApp",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = RedDark
                            )
                        }

                        // WhatsApp Number Chooser (ONLY VISIBLE WHEN TICKED)
                        if (sendWhatsApp) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Choose number:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray
                            )

                            // Quick chips from saved numbers
                            if (savedNumbers.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    savedNumbers.forEach { num ->
                                        FilterChip(
                                            selected = selectedNumber?.id == num.id,
                                            onClick = {
                                                viewModel.selectedWhatsAppNumber.value = num
                                                viewModel.customWhatsAppPhone.value = num.phoneNumber
                                            },
                                            label = { Text("${num.name} (${num.type.label})") }
                                        )
                                    }
                                }
                            }

                            // Custom or Selected phone number input
                            OutlinedTextField(
                                value = customPhone,
                                onValueChange = {
                                    viewModel.customWhatsAppPhone.value = it
                                    viewModel.selectedWhatsAppNumber.value = null
                                },
                                label = { Text("Phone Number (+91 default)") },
                                placeholder = { Text("e.g. 9876543210") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            // WhatsApp app preference switch
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                FilterChip(
                                    selected = !preferBusiness,
                                    onClick = { viewModel.preferWhatsAppBusiness.value = false },
                                    label = { Text("WhatsApp") }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilterChip(
                                    selected = preferBusiness,
                                    onClick = { viewModel.preferWhatsAppBusiness.value = true },
                                    label = { Text("WhatsApp Business") }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // ==================== Bottom Print Button ====================
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            color = Color.White
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = { viewModel.onPrintTapped() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Print",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Preview Dialog
    if (showPreview && previewBmp != null) {
        PrintPreviewDialog(
            bitmap = previewBmp!!,
            onConfirm = { viewModel.confirmAndEnqueueBill() },
            onDismiss = { viewModel.showPreviewDialog.value = false }
        )
    }

    // Pending Queue Dialog
    if (showQueue) {
        PendingQueueDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showQueueDialog.value = false }
        )
    }
}
