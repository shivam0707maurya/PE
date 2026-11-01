package com.poojaenterprises.pe

import android.app.Application
import com.poojaenterprises.pe.data.StorageManager
import com.poojaenterprises.pe.printing.PrintQueueManager

class PEApplication : Application() {

    lateinit var storageManager: StorageManager
        private set

    lateinit var printQueueManager: PrintQueueManager
        private set

    override fun onCreate() {
        super.onCreate()
        storageManager = StorageManager(this)
        printQueueManager = PrintQueueManager(this, storageManager)
        printQueueManager.start()
    }
}
