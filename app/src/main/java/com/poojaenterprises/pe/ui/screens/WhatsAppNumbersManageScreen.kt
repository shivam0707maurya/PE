package com.poojaenterprises.pe.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poojaenterprises.pe.data.WhatsAppNumber
import com.poojaenterprises.pe.data.WhatsAppNumberType
import com.poojaenterprises.pe.ui.theme.RedDark
import com.poojaenterprises.pe.ui.theme.RedPrimary
import com.poojaenterprises.pe.ui.viewmodel.MainViewModel

@Composable
fun WhatsAppNumbersManageScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val numbers by viewModel.storage.whatsAppNumbers.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var editingNumber by remember { mutableStateOf<WhatsAppNumber?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }

    val filtered = numbers.filter {
        it.name.contains(searchQuery.trim(), ignoreCase = true) ||
                it.phoneNumber.contains(searchQuery.trim())
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF9F9FA))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 2.dp, color = Color.White) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Saved WhatsApp Numbers (${numbers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = RedDark
                    )
                }
            }

            // Search
            Box(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or number...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // List
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RedDark)
                                Text("${item.type.label} • ${item.phoneNumber}", fontSize = 13.sp, color = Color.DarkGray)
                            }
                            Row {
                                IconButton(onClick = { editingNumber = item }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                                }
                                IconButton(onClick = { viewModel.storage.deleteWhatsAppNumber(item.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }

        // FAB to add number
        FloatingActionButton(
            onClick = { isAddingNew = true },
            containerColor = RedPrimary,
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Number")
        }
    }

    // Add / Edit Dialog
    if (isAddingNew || editingNumber != null) {
        val target = editingNumber
        var name by remember { mutableStateOf(target?.name ?: "") }
        var phone by remember { mutableStateOf(target?.phoneNumber ?: "") }
        var type by remember { mutableStateOf(target?.type ?: WhatsAppNumberType.CUSTOMER) }

        AlertDialog(
            onDismissRequest = {
                isAddingNew = false
                editingNumber = null
            },
            title = { Text(if (target == null) "New WhatsApp Number" else "Edit WhatsApp Number") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number (+91 default)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Number Type:", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WhatsAppNumberType.values().forEach { nType ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = type == nType, onClick = { type = nType })
                                Text(nType.label, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            val newOrUpdated = WhatsAppNumber(
                                id = target?.id ?: java.util.UUID.randomUUID().toString(),
                                name = name.trim(),
                                phoneNumber = phone.trim(),
                                type = type
                            )
                            viewModel.storage.saveWhatsAppNumber(newOrUpdated)
                        }
                        isAddingNew = false
                        editingNumber = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    isAddingNew = false
                    editingNumber = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
