package com.poojaenterprises.pe.printing

import android.content.Context
import android.util.Log
import com.poojaenterprises.pe.data.BillRecord
import com.poojaenterprises.pe.data.PrintJob
import com.poojaenterprises.pe.data.PrintJobStatus
import com.poojaenterprises.pe.data.PrinterStatus
import com.poojaenterprises.pe.data.StorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PrintQueueManager(
    private val context: Context,
    private val storage: StorageManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val queueMutex = Mutex()

    private val _printerStatus = MutableStateFlow(PrinterStatus.OFFLINE)
    val printerStatus: StateFlow<PrinterStatus> = _printerStatus.asStateFlow()

    private val _isPrinting = MutableStateFlow(false)
    val isPrinting: StateFlow<Boolean> = _isPrinting.asStateFlow()

    private var pollJob: Job? = null
    private var workerJob: Job? = null

    // Callback invoked when a bill prints successfully, so WhatsApp or UI notifications can be handled
    var onBillPrintedSuccessfully: ((BillRecord, java.io.File?) -> Unit)? = null

    fun start() {
        startStatusPolling()
        startQueueWorker()
    }

    private fun startStatusPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                val settings = storage.printerSettings.value
                if (!_isPrinting.value) {
                    val status = EscPosRasterSender.queryPrinterStatus(settings.ipAddress, settings.port)
                    _printerStatus.value = status
                }
                delay(3500) // poll every 3.5 seconds
            }
        }
    }

    private fun startQueueWorker() {
        workerJob?.cancel()
        workerJob = scope.launch {
            while (isActive) {
                processNextJob()
                delay(1000)
            }
        }
    }

    fun enqueueBill(bill: BillRecord) {
        scope.launch {
            queueMutex.withLock {
                val currentQueue = storage.printQueue.value.toMutableList()
                val job = PrintJob(bill = bill, status = PrintJobStatus.PENDING)
                currentQueue.add(job)
                storage.persistQueue(currentQueue)
            }
            processNextJob()
        }
    }

    fun retryJob(jobId: String) {
        scope.launch {
            queueMutex.withLock {
                val currentQueue = storage.printQueue.value.map { job ->
                    if (job.id == jobId) {
                        job.copy(status = PrintJobStatus.PENDING, errorMessage = null)
                    } else job
                }
                storage.persistQueue(currentQueue)
            }
            processNextJob()
        }
    }

    fun cancelJob(jobId: String) {
        scope.launch {
            queueMutex.withLock {
                val currentQueue = storage.printQueue.value.filter { it.id != jobId }
                storage.persistQueue(currentQueue)
            }
        }
    }

    private suspend fun processNextJob() {
        if (_isPrinting.value) return

        val nextJob = storage.printQueue.value.firstOrNull { it.status == PrintJobStatus.PENDING } ?: return

        queueMutex.withLock {
            _isPrinting.value = true
            _printerStatus.value = PrinterStatus.PRINTING
            updateJobStatus(nextJob.id, PrintJobStatus.PRINTING)
        }

        val settings = storage.printerSettings.value
        val business = storage.businessDetails.value
        val toggles = storage.printToggles.value
        val style = storage.getSelectedStyle()

        // 1. Render receipt to Bitmap
        val bitmap = EscPosBitmapRenderer.renderBillToBitmap(
            bill = nextJob.bill,
            businessDetails = business,
            printerSettings = settings,
            toggles = toggles,
            style = style
        )

        // 2. Transmit to LAN printer
        val printResult = EscPosRasterSender.sendBitmap(
            ipAddress = settings.ipAddress,
            port = settings.port,
            bitmap = bitmap,
            autoCut = toggles.autoCut && settings.autoCut
        )

        queueMutex.withLock {
            if (printResult.isSuccess) {
                // Remove from pending queue
                val updatedQueue = storage.printQueue.value.filter { it.id != nextJob.id }
                storage.persistQueue(updatedQueue)

                _printerStatus.value = PrinterStatus.IDLE
                _isPrinting.value = false

                // 3. Generate PDF and save to history
                scope.launch {
                    val pdfFile = if (toggles.pdfHistory) {
                        PdfGenerator.generateAndSaveBillPdf(context, nextJob.bill, bitmap, settings.paperSize)
                    } else null

                    val record = nextJob.bill.copy(pdfPath = pdfFile?.absolutePath)
                    storage.saveBill(record)

                    onBillPrintedSuccessfully?.invoke(record, pdfFile)
                }
            } else {
                val errorMsg = printResult.exceptionOrNull()?.message ?: "Print failed"
                Log.e("PrintQueueManager", "Print job ${nextJob.id} failed: $errorMsg")
                _printerStatus.value = PrinterStatus.OFFLINE
                _isPrinting.value = false
                updateJobStatus(nextJob.id, PrintJobStatus.FAILED, errorMsg)
            }
        }
    }

    private suspend fun updateJobStatus(jobId: String, status: PrintJobStatus, errorMessage: String? = null) {
        val updated = storage.printQueue.value.map { job ->
            if (job.id == jobId) {
                job.copy(status = status, errorMessage = errorMessage)
            } else job
        }
        storage.persistQueue(updated)
    }

    fun getPendingCount(): Int {
        return storage.printQueue.value.count { it.status == PrintJobStatus.PENDING || it.status == PrintJobStatus.PRINTING }
    }
}
