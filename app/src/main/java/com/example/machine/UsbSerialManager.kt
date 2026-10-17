package com.example.machine

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class UsbSerialManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager

    private var connection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var inEndpoint: UsbEndpoint? = null
    private var outEndpoint: UsbEndpoint? = null

    private var readJob: Job? = null

    private val _dataReceived = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val dataReceived: SharedFlow<String> = _dataReceived

    var connectedDeviceName: String? = null
        private set

    val isConnected: Boolean
        get() = connection != null && outEndpoint != null

    data class DiscoveredUsbDevice(
        val device: UsbDevice,
        val displayName: String,
        val vendorIdHex: String,
        val productIdHex: String
    )

    fun listDevices(): List<DiscoveredUsbDevice> {
        val manager = usbManager ?: return emptyList()
        val deviceList = manager.deviceList.values
        return deviceList.map { dev ->
            val vId = String.format("%04X", dev.vendorId)
            val pId = String.format("%04X", dev.productId)
            val chipName = identifyChip(dev.vendorId)
            val name = if (dev.productName != null) "${dev.productName} ($chipName)" else "$chipName Controller ($vId:$pId)"
            DiscoveredUsbDevice(dev, name, vId, pId)
        }
    }

    private fun identifyChip(vendorId: Int): String {
        return when (vendorId) {
            0x1A86 -> "CH340/CH341 (Standard GRBL)"
            0x0403 -> "FTDI Serial"
            0x10C4 -> "CP2102/CP2104"
            0x067B -> "Prolific PL2303"
            0x2341, 0x2A03 -> "Arduino Uno/Nano GRBL"
            else -> "USB-to-Serial"
        }
    }

    fun connectDevice(device: UsbDevice, baudRate: Int = 115200): Boolean {
        val manager = usbManager ?: return false
        disconnect()

        val conn = manager.openDevice(device) ?: return false

        // Locate bulk endpoints
        var foundInterface: UsbInterface? = null
        var epIn: UsbEndpoint? = null
        var epOut: UsbEndpoint? = null

        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            for (e in 0 until iface.endpointCount) {
                val ep = iface.getEndpoint(e)
                if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (ep.direction == UsbConstants.USB_DIR_IN && epIn == null) {
                        epIn = ep
                    } else if (ep.direction == UsbConstants.USB_DIR_OUT && epOut == null) {
                        epOut = ep
                    }
                }
            }
            if (epIn != null && epOut != null) {
                foundInterface = iface
                break
            }
        }

        if (foundInterface == null || epIn == null || epOut == null) {
            conn.close()
            return false
        }

        conn.claimInterface(foundInterface, true)

        connection = conn
        usbInterface = foundInterface
        inEndpoint = epIn
        outEndpoint = epOut
        connectedDeviceName = device.productName ?: "USB GRBL Controller"

        // Set line coding for CDC-ACM if applicable: baud rate, 8 data bits, no parity, 1 stop bit
        setBaudRateCdc(conn, baudRate)

        startReading(conn, epIn)
        return true
    }

    private fun setBaudRateCdc(conn: UsbDeviceConnection, baudRate: Int) {
        val lineCoding = ByteArray(7)
        lineCoding[0] = (baudRate and 0xFF).toByte()
        lineCoding[1] = ((baudRate shr 8) and 0xFF).toByte()
        lineCoding[2] = ((baudRate shr 16) and 0xFF).toByte()
        lineCoding[3] = ((baudRate shr 24) and 0xFF).toByte()
        lineCoding[4] = 0 // 1 Stop bit
        lineCoding[5] = 0 // No parity
        lineCoding[6] = 8 // 8 data bits

        conn.controlTransfer(0x21, 0x20, 0, 0, lineCoding, lineCoding.size, 500)
        // Set control line state DTR/RTS
        conn.controlTransfer(0x21, 0x22, 0x03, 0, null, 0, 500)
    }

    private fun startReading(conn: UsbDeviceConnection, epIn: UsbEndpoint) {
        readJob?.cancel()
        readJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(256)
            val lineBuilder = StringBuilder()

            while (isActive && connection != null) {
                val bytesRead = conn.bulkTransfer(epIn, buffer, buffer.size, 100)
                if (bytesRead > 0) {
                    val str = String(buffer, 0, bytesRead, Charsets.UTF_8)
                    lineBuilder.append(str)
                    while (lineBuilder.contains("\n")) {
                        val lineEnd = lineBuilder.indexOf("\n")
                        val line = lineBuilder.substring(0, lineEnd + 1)
                        lineBuilder.delete(0, lineEnd + 1)
                        _dataReceived.emit(line)
                    }
                }
            }
        }
    }

    fun sendString(data: String): Boolean {
        val conn = connection ?: return false
        val epOut = outEndpoint ?: return false
        val bytes = data.toByteArray(Charsets.UTF_8)
        val written = conn.bulkTransfer(epOut, bytes, bytes.size, 500)
        return written >= 0
    }

    fun disconnect() {
        readJob?.cancel()
        readJob = null
        try {
            usbInterface?.let { connection?.releaseInterface(it) }
            connection?.close()
        } catch (_: Exception) {}
        connection = null
        usbInterface = null
        inEndpoint = null
        outEndpoint = null
        connectedDeviceName = null
    }
}
