package com.poojaenterprises.pe.printing

import android.graphics.Bitmap
import android.util.Log
import com.poojaenterprises.pe.data.PrinterStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

object EscPosRasterSender {

    private const val TAG = "EscPosRasterSender"
    private const val CHUNK_ROWS = 200 // send 200 raster rows per chunk
    private const val CONNECT_TIMEOUT_MS = 4000
    private const val READ_TIMEOUT_MS = 2500

    // ESC/POS Commands
    private val CMD_INIT = byteArrayOf(0x1B, 0x40) // ESC @ (initialize)
    private val CMD_FEED_4 = byteArrayOf(0x1B, 0x64, 0x04) // ESC d 4 (feed 4 lines)
    private val CMD_CUT = byteArrayOf(0x1D, 0x56, 0x42, 0x00) // GS V 66 0 (partial cut)

    // Real-time status requests (DLE EOT n)
    private val CMD_STATUS_1 = byteArrayOf(0x10, 0x04, 0x01) // Printer status
    private val CMD_STATUS_2 = byteArrayOf(0x10, 0x04, 0x02) // Offline cause
    private val CMD_STATUS_4 = byteArrayOf(0x10, 0x04, 0x04) // Roll paper sensor

    suspend fun sendBitmap(
        ipAddress: String,
        port: Int,
        bitmap: Bitmap,
        autoCut: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        var outputStream: OutputStream? = null

        try {
            socket = Socket()
            socket.connect(InetSocketAddress(ipAddress, port), CONNECT_TIMEOUT_MS)
            socket.soTimeout = 8000
            outputStream = socket.getOutputStream()

            // 1. Initialize printer
            outputStream.write(CMD_INIT)
            outputStream.flush()

            // 2. Convert and send bitmap in 200-row chunks
            val width = bitmap.width
            val height = bitmap.height
            val bytesPerRow = (width + 7) / 8

            var startY = 0
            while (startY < height) {
                val currentChunkRows = minOf(CHUNK_ROWS, height - startY)
                val chunkRaster = convertChunkToRaster(bitmap, startY, currentChunkRows, bytesPerRow)

                // GS v 0 0 xL xH yL yH
                val xL = (bytesPerRow % 256).toByte()
                val xH = (bytesPerRow / 256).toByte()
                val yL = (currentChunkRows % 256).toByte()
                val yH = (currentChunkRows / 256).toByte()

                val header = byteArrayOf(0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH)
                outputStream.write(header)
                outputStream.write(chunkRaster)
                outputStream.flush()

                startY += currentChunkRows
                // Small yield between chunks
                Thread.sleep(20)
            }

            // 3. Feed 4 lines
            outputStream.write(CMD_FEED_4)
            outputStream.flush()

            // 4. Cut if enabled
            if (autoCut) {
                outputStream.write(CMD_CUT)
                outputStream.flush()
            }

            // Short pause to ensure buffer is dispatched
            Thread.sleep(150)
            Result.success(Unit)
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Printer connection timeout to $ipAddress:$port", e)
            Result.failure(Exception("Printer connection timeout. Verify IP and same Wi-Fi router."))
        } catch (e: Exception) {
            Log.e(TAG, "Print failed", e)
            Result.failure(Exception("Printer error: ${e.message}"))
        } finally {
            try { outputStream?.close() } catch (ignored: Exception) {}
            try { socket?.close() } catch (ignored: Exception) {}
        }
    }

    /**
     * Converts a slice of the Bitmap into 1-bit ESC/POS raster data.
     * Uses luminance thresholding: dark pixels (luminance < 160) = bit 1 (print black dot).
     */
    private fun convertChunkToRaster(
        bitmap: Bitmap,
        startY: Int,
        numRows: Int,
        bytesPerRow: Int
    ): ByteArray {
        val width = bitmap.width
        val raster = ByteArray(bytesPerRow * numRows)
        val pixelRow = IntArray(width)

        for (row in 0 until numRows) {
            val y = startY + row
            bitmap.getPixels(pixelRow, 0, width, 0, y, width, 1)

            val rowOffset = row * bytesPerRow
            for (x in 0 until width) {
                val pixel = pixelRow[x]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                // Standard perceptual luminance formula
                val luminance = (r * 299 + g * 587 + b * 114) / 1000

                // If dark pixel -> set bit to 1 (print dot)
                if (luminance < 160) {
                    val byteIdx = x / 8
                    val bitIdx = 7 - (x % 8)
                    raster[rowOffset + byteIdx] = (raster[rowOffset + byteIdx].toInt() or (1 shl bitIdx)).toByte()
                }
            }
        }
        return raster
    }

    /**
     * Poll printer status: queries DLE EOT 1..4.
     * Fallback to socket reachability if printer does not answer real-time status.
     */
    suspend fun queryPrinterStatus(ipAddress: String, port: Int): PrinterStatus = withContext(Dispatchers.IO) {
        if (ipAddress.isBlank()) return@withContext PrinterStatus.OFFLINE

        var socket: Socket? = null
        try {
            socket = Socket()
            socket.connect(InetSocketAddress(ipAddress, port), 1200)
            socket.soTimeout = 800

            val out = socket.getOutputStream()
            val inStream = socket.getInputStream()

            // Try DLE EOT 2 (offline cause) and DLE EOT 4 (paper end)
            out.write(CMD_STATUS_2)
            out.flush()

            val buffer = ByteArray(4)
            val bytesRead = try { inStream.read(buffer) } catch (e: Exception) { 0 }

            if (bytesRead > 0) {
                val statusByte = buffer[0].toInt()
                // Bit 2 (0x04): Cover open
                // Bit 5 (0x20): Paper end / out of paper
                // Bit 6 (0x40): Error
                if ((statusByte and 0x04) != 0 || (statusByte and 0x20) != 0) {
                    return@withContext PrinterStatus.PAPER_COVER
                }
                if ((statusByte and 0x40) != 0) {
                    return@withContext PrinterStatus.BUSY
                }
            }

            // Socket is open and connected -> IDLE
            PrinterStatus.IDLE
        } catch (e: Exception) {
            PrinterStatus.OFFLINE
        } finally {
            try { socket?.close() } catch (ignored: Exception) {}
        }
    }
}
