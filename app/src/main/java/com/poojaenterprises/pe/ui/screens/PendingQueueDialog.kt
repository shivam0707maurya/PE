package com.poojaenterprises.pe.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poojaenterprises.pe.data.PrintJobStatus
import com.poojaenterprises.pe.ui.theme.RedPrimary
import com.poojaenterprises.pe.ui.theme.StatusBusy
import com.poojaenterprises.pe.ui.theme.StatusOffline
import com.poojaenterprises.pe.ui.theme.StatusPrinting
import com.poojaenterprises.pe.ui.viewmodel.MainViewModel

@Composable
fun PendingQueueDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val jobs by viewModel.printQueueJobs.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Print Queue (${jobs.size})", fontWeight = FontWeight.Bold) },
        text = {
            if (jobs.isEmpty()) {
                Text("Print queue is empty.", color = Color.Gray)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(jobs, key = { it.id }) { job ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F9))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (job.bill.customerName.isNotBlank()) job.bill.customerName else "Bill",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    val (statusText, statusColor) = when (job.status) {
                                        PrintJobStatus.PRINTING -> "Printing..." to StatusPrinting
                                        PrintJobStatus.PENDING -> "Pending" to StatusBusy
                                        PrintJobStatus.FAILED -> "Failed" to StatusOffline
                                        else -> "In Queue" to Color.Gray
                                    }
                                    Text(
                                        text = statusText,
                                        color = statusColor,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }

                                Text(
                                    text = "${job.bill.date} ${job.bill.time} • ${job.bill.items.size} items",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 2.dp)
                                )

                                if (job.errorMessage != null) {
                                    Text(
                                        text = job.errorMessage,
                                        color = StatusOffline,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (job.status == PrintJobStatus.FAILED) {
                                        Button(
                                            onClick = { viewModel.printQueue.retryJob(job.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Retry", fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.printQueue.cancelJob(job.id) },
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("Cancel", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
