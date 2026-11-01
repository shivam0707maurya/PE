package com.poojaenterprises.pe.ui.screens

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
import com.poojaenterprises.pe.data.Customer
import com.poojaenterprises.pe.ui.theme.RedDark
import com.poojaenterprises.pe.ui.theme.RedPrimary
import com.poojaenterprises.pe.ui.viewmodel.MainViewModel

@Composable
fun CustomersManageScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val customers by viewModel.storage.customers.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }

    val filtered = customers.filter {
        it.name.contains(searchQuery.trim(), ignoreCase = true) ||
                it.address.contains(searchQuery.trim(), ignoreCase = true)
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
                        text = "Saved Customers (${customers.size})",
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
                    placeholder = { Text("Search customer name...") },
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
                items(filtered, key = { it.id }) { cust ->
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
                                Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RedDark)
                                if (cust.address.isNotBlank()) {
                                    Text(cust.address, fontSize = 13.sp, color = Color.DarkGray)
                                }
                                if (cust.whatsAppNumber.isNotBlank()) {
                                    Text("WhatsApp: ${cust.whatsAppNumber}", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                            Row {
                                IconButton(onClick = { editingCustomer = cust }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                                }
                                IconButton(onClick = { viewModel.storage.deleteCustomer(cust.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }

        // FAB to add customer
        FloatingActionButton(
            onClick = { isAddingNew = true },
            containerColor = RedPrimary,
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Customer")
        }
    }

    // Add / Edit Dialog
    if (isAddingNew || editingCustomer != null) {
        val target = editingCustomer
        var name by remember { mutableStateOf(target?.name ?: "") }
        var address by remember { mutableStateOf(target?.address ?: "") }
        var phone by remember { mutableStateOf(target?.whatsAppNumber ?: "") }

        AlertDialog(
            onDismissRequest = {
                isAddingNew = false
                editingCustomer = null
            },
            title = { Text(if (target == null) "New Customer" else "Edit Customer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("WhatsApp Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val newOrUpdated = Customer(
                                id = target?.id ?: java.util.UUID.randomUUID().toString(),
                                name = name.trim(),
                                address = address.trim(),
                                whatsAppNumber = phone.trim()
                            )
                            viewModel.storage.saveCustomer(newOrUpdated)
                        }
                        isAddingNew = false
                        editingCustomer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    isAddingNew = false
                    editingCustomer = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
