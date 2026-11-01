package com.poojaenterprises.pe.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.poojaenterprises.pe.data.BillRecord
import com.poojaenterprises.pe.ui.theme.RedDark
import com.poojaenterprises.pe.ui.theme.RedPrimary
import com.poojaenterprises.pe.ui.viewmodel.MainViewModel
import com.poojaenterprises.pe.utils.WhatsAppHelper
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun HistoryScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val bills by viewModel.storage.bills.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val filteredBills = bills.filter {
        it.customerName.contains(searchQuery.trim(), ignoreCase = true) ||
                it.customerAddress.contains(searchQuery.trim(), ignoreCase = true) ||
                it.date.contains(searchQuery.trim(), ignoreCase = true)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bill History (${bills.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = RedDark,
                    fontWeight = FontWeight.Bold
                )

                if (bills.isNotEmpty()) {
                    IconButton(onClick = { showDeleteAllDialog = true }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Delete all history", tint = Color.Gray)
                    }
                }
            }
        }

        // Search Bar
        Box(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by customer name or date...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
        }

        // Bills List
        if (filteredBills.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (bills.isEmpty()) "No bills printed yet." else "No matching bills found.",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredBills, key = { it.id }) { bill ->
                    BillHistoryCard(
                        bill = bill,
                        onReprint = { viewModel.reprintBill(bill) },
                        onShareWhatsApp = {
                            val pdfFile = bill.pdfPath?.let { File(it) }
                            if (pdfFile != null && pdfFile.exists()) {
                                WhatsAppHelper.shareBillPdfToWhatsApp(
                                    context = context,
                                    pdfFile = pdfFile,
                                    phoneNumber = bill.customerWhatsAppNumber
                                )
                            } else {
                                Toast.makeText(context, "PDF file not found", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onViewPdf = {
                            val pdfFile = bill.pdfPath?.let { File(it) }
                            if (pdfFile != null && pdfFile.exists()) {
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        pdfFile
                                    )
                                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/pdf")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(viewIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "PDF file not found", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDelete = {
                            coroutineScope.launch {
                                viewModel.storage.deleteBill(bill.id)
                            }
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    // Delete All Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete All History") },
            text = { Text("Are you sure you want to delete all saved bills? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.storage.deleteAllBills()
                        }
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Text("Delete all")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BillHistoryCard(
    bill: BillRecord,
    onReprint: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onViewPdf: () -> Unit,
    onDelete: () -> Unit
) {
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
                    text = if (bill.customerName.isNotBlank()) bill.customerName else "Customer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = RedDark
                )
                Text(
                    text = "${bill.date} ${bill.time}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            if (bill.customerAddress.isNotBlank()) {
                Text(
                    text = bill.customerAddress,
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Items Summary
            Text(
                text = bill.items.joinToString(" • ") { it.displaySummary },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF222222),
                modifier = Modifier.padding(top = 6.dp)
            )

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onReprint,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reprint", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                OutlinedButton(
                    onClick = onShareWhatsApp,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp", fontSize = 12.sp)
                }

                if (bill.pdfPath != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onViewPdf, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "View PDF", tint = RedPrimary)
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                }
            }
        }
    }
}
