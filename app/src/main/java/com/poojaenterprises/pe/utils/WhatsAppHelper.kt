package com.poojaenterprises.pe.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object WhatsAppHelper {

    fun formatPhoneNumber(input: String): String {
        // Strip everything except digits
        var clean = input.replace(Regex("[^0-9]"), "")
        if (clean.startsWith("0")) {
            clean = clean.substring(1)
        }
        // Default to India country code +91 if 10 digits
        if (clean.length == 10) {
            clean = "91$clean"
        }
        return clean
    }

    fun isWhatsAppInstalled(context: Context, isBusiness: Boolean): Boolean {
        val packageName = if (isBusiness) "com.whatsapp.w4b" else "com.whatsapp"
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Opens WhatsApp with the PDF file attached to the specified phone number.
     * The owner taps Send manually.
     */
    fun shareBillPdfToWhatsApp(
        context: Context,
        pdfFile: File,
        phoneNumber: String,
        preferBusiness: Boolean = false
    ) {
        try {
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val cleanNumber = formatPhoneNumber(phoneNumber)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Target specific phone number in WhatsApp via JID: <country_code><number>@s.whatsapp.net
            if (cleanNumber.isNotBlank()) {
                intent.putExtra("jid", "$cleanNumber@s.whatsapp.net")
            }

            // Choose WhatsApp Business if preferred or available
            val hasNormal = isWhatsAppInstalled(context, false)
            val hasBusiness = isWhatsAppInstalled(context, true)

            val targetPkg = when {
                preferBusiness && hasBusiness -> "com.whatsapp.w4b"
                hasNormal -> "com.whatsapp"
                hasBusiness -> "com.whatsapp.w4b"
                else -> null
            }

            if (targetPkg != null) {
                intent.setPackage(targetPkg)
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open WhatsApp: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
