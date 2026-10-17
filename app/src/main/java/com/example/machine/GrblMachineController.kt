package com.example.machine

import android.content.Context
import android.graphics.RectF
import android.hardware.usb.UsbDevice
import com.example.model.GCodeJob
import com.example.model.MachineProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class GrblMachineController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    val usbSerialManager = UsbSerialManager(context, scope)
    val mockSimulator = MockGrblSimulator(scope)

    private val _status = MutableStateFlow(GrblStatus())
    val status: StateFlow<GrblStatus> = _status.asStateFlow()

    private val _consoleLogs = MutableStateFlow<List<String>>(emptyList())
    val consoleLogs: StateFlow<List<String>> = _consoleLogs.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    private var streamingJob: Job? = null
    private var pollJob: Job? = null

    init {
        // Collect incoming from USB
        scope.launch {
            usbSerialManager.dataReceived.collect { line ->
                processIncomingLine(line)
            }
        }
        // Collect incoming from Mock
        scope.launch {
            mockSimulator.incomingResponses.collect { line ->
                processIncomingLine(line)
            }
        }
    }

    fun connectUsb(device: UsbDevice, baudRate: Int = 115200) {
        disconnect()
        val success = usbSerialManager.connectDevice(device, baudRate)
        if (success) {
            _status.value = _status.value.copy(
                isConnected = true,
                connectionType = ConnectionType.USB_SERIAL,
                deviceName = usbSerialManager.connectedDeviceName ?: "USB GRBL",
                state = MachineState.IDLE
            )
            appendLog("[INFO] Connected to USB GRBL controller at $baudRate baud")
            // Send soft-reset and query status, ensure laser is OFF on connect
            scope.launch {
                delay(500)
                sendCommand("\u0018") // Soft reset
                delay(200)
                sendCommand("M5 ; Safety: laser off on connect")
                sendCommand("?")
            }
            startStatusPolling()
        } else {
            appendLog("[ERROR] Failed to open USB device connection")
        }
    }

    fun connectMock() {
        disconnect()
        mockSimulator.connect()
        _status.value = _status.value.copy(
            isConnected = true,
            connectionType = ConnectionType.MOCK_SIMULATOR,
            deviceName = "Mock GRBL Controller (Offline Simulation)",
            state = MachineState.IDLE
        )
        appendLog("[INFO] Connected to Mock GRBL Controller (Offline Simulator)")
        startStatusPolling()
    }

    fun disconnect() {
        stopStreamingJob()
        pollJob?.cancel()
        pollJob = null

        if (usbSerialManager.isConnected) {
            // Ensure laser is turned off before disconnecting
            usbSerialManager.sendString("M5\r\n")
            usbSerialManager.disconnect()
        }
        if (mockSimulator.isConnected) {
            mockSimulator.disconnect()
        }

        _status.value = GrblStatus(
            state = MachineState.DISCONNECTED,
            connectionType = ConnectionType.NONE,
            isConnected = false
        )
        appendLog("[INFO] Machine disconnected")
    }

    private fun startStatusPolling() {
        pollJob?.cancel()
        pollJob = scope.launch(Dispatchers.Default) {
            while (isActive && _status.value.isConnected) {
                delay(300)
                if (_status.value.connectionType == ConnectionType.USB_SERIAL) {
                    usbSerialManager.sendString("?")
                } else if (_status.value.connectionType == ConnectionType.MOCK_SIMULATOR) {
                    mockSimulator.emitStatusReport()
                }
            }
        }
    }

    fun sendCommand(cmd: String) {
        val clean = cmd.trim()
        if (clean.isEmpty()) return
        appendLog("> $clean")

        if (_status.value.connectionType == ConnectionType.USB_SERIAL) {
            usbSerialManager.sendString("$clean\r\n")
        } else if (_status.value.connectionType == ConnectionType.MOCK_SIMULATOR) {
            scope.launch {
                mockSimulator.handleCommand(clean)
            }
        } else {
            appendLog("[WARN] Cannot send command: No machine connected")
        }
    }

    fun emergencyStop() {
        appendLog("[EMERGENCY STOP ACTIVATED] Halting machine immediately!")
        // Immediate feed hold and soft-reset
        if (_status.value.connectionType == ConnectionType.USB_SERIAL) {
            usbSerialManager.sendString("!\r\n")
            usbSerialManager.sendString("\u0018\r\n")
            usbSerialManager.sendString("M5\r\n")
        } else if (_status.value.connectionType == ConnectionType.MOCK_SIMULATOR) {
            scope.launch {
                mockSimulator.handleCommand("!")
                mockSimulator.handleCommand("\u0018")
                mockSimulator.handleCommand("M5")
            }
        }
        stopStreamingJob()
        _status.value = _status.value.copy(state = MachineState.ALARM, isStreamingJob = false)
    }

    fun jog(dx: Float, dy: Float, dz: Float = 0f, feedRateMmMin: Float = 1000f) {
        val jogCmd = String.format(
            Locale.US,
            "\$J=G91 G21 X%.3f Y%.3f Z%.3f F%.0f",
            dx, dy, dz, feedRateMmMin
        )
        sendCommand(jogCmd)
    }

    fun homeMachine() {
        sendCommand("\$H")
    }

    fun unlockAlarm() {
        sendCommand("\$X")
    }

    fun setWorkOriginZero() {
        sendCommand("G10 L20 P1 X0 Y0 Z0")
    }

    fun moveToOrigin() {
        sendCommand("G0 X0 Y0")
    }

    fun pauseJob() {
        sendCommand("!")
        _status.value = _status.value.copy(state = MachineState.HOLD)
        appendLog("[INFO] Job execution paused")
    }

    fun resumeJob() {
        sendCommand("~")
        _status.value = _status.value.copy(state = MachineState.RUN)
        appendLog("[INFO] Job execution resumed")
    }

    fun frameBoundingBox(bounds: RectF, feedRate: Float = 3000f) {
        scope.launch {
            appendLog("[INFO] Framing design boundary: X[%.1f..%.1f] Y[%.1f..%.1f] mm".format(Locale.US, bounds.left, bounds.right, bounds.top, bounds.bottom))
            sendCommand("G0 X%.3f Y%.3f F%.0f".format(Locale.US, bounds.left, bounds.top, feedRate))
            delay(400)
            sendCommand("G0 X%.3f Y%.3f F%.0f".format(Locale.US, bounds.right, bounds.top, feedRate))
            delay(400)
            sendCommand("G0 X%.3f Y%.3f F%.0f".format(Locale.US, bounds.right, bounds.bottom, feedRate))
            delay(400)
            sendCommand("G0 X%.3f Y%.3f F%.0f".format(Locale.US, bounds.left, bounds.bottom, feedRate))
            delay(400)
            sendCommand("G0 X%.3f Y%.3f F%.0f".format(Locale.US, bounds.left, bounds.top, feedRate))
            appendLog("[INFO] Framing complete")
        }
    }

    fun startJob(job: GCodeJob) {
        if (!_status.value.isConnected) {
            appendLog("[ERROR] Cannot start job: Machine is not connected")
            return
        }

        stopStreamingJob()
        val lines = job.rawGCode.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith(";") }
        if (lines.isEmpty()) {
            appendLog("[WARN] No executable G-code lines in job")
            return
        }

        appendLog("[INFO] Starting job stream (${lines.size} commands)...")
        _status.value = _status.value.copy(
            isStreamingJob = true,
            totalLinesInJob = lines.size,
            linesProcessed = 0,
            jobProgress = 0f,
            state = MachineState.RUN
        )

        streamingJob = scope.launch(Dispatchers.IO) {
            for ((index, line) in lines.withIndex()) {
                if (!isActive || !_status.value.isStreamingJob) break

                // Wait if machine is in HOLD state
                while (isActive && _status.value.state == MachineState.HOLD) {
                    delay(100)
                }

                sendCommand(line)

                // Simple flow control delay based on feedrate or line type
                val delayMs = if (line.startsWith("G1") || line.startsWith("G0")) 15L else 5L
                delay(delayMs)

                val progress = (index + 1).toFloat() / lines.size.toFloat()
                _status.value = _status.value.copy(
                    linesProcessed = index + 1,
                    jobProgress = progress
                )
            }

            if (isActive && _status.value.isStreamingJob) {
                appendLog("[INFO] Job completed successfully!")
                _status.value = _status.value.copy(
                    isStreamingJob = false,
                    jobProgress = 1.0f,
                    state = MachineState.IDLE
                )
            }
        }
    }

    fun stopStreamingJob() {
        streamingJob?.cancel()
        streamingJob = null
        if (_status.value.isStreamingJob) {
            sendCommand("M5") // Laser off immediately
            _status.value = _status.value.copy(isStreamingJob = false, state = MachineState.IDLE)
            appendLog("[INFO] Job streaming stopped")
        }
    }

    private fun processIncomingLine(raw: String) {
        val line = raw.trim()
        if (line.isEmpty()) return

        if (!line.startsWith("<")) {
            appendLog("< $line")
        }

        if (line.startsWith("<") && line.endsWith(">")) {
            parseGrblStatusLine(line)
        } else if (line.startsWith("ALARM:")) {
            val code = line.removePrefix("ALARM:").trim().toIntOrNull()
            _status.value = _status.value.copy(
                state = MachineState.ALARM,
                alarmCode = code,
                alarmMessage = getAlarmDescription(code ?: 0)
            )
        } else if (line.startsWith("error:")) {
            appendLog("[CONTROLLER ERROR] $line")
        }
    }

    private fun parseGrblStatusLine(line: String) {
        // e.g. <Idle|MPos:0.000,0.000,0.000|WPos:0.000,0.000,0.000|FS:0,0>
        val content = line.substring(1, line.length - 1)
        val parts = content.split("|")
        if (parts.isEmpty()) return

        val stateStr = parts[0]
        val newState = when {
            stateStr.startsWith("Idle") -> MachineState.IDLE
            stateStr.startsWith("Run") -> MachineState.RUN
            stateStr.startsWith("Hold") -> MachineState.HOLD
            stateStr.startsWith("Jog") -> MachineState.JOG
            stateStr.startsWith("Alarm") -> MachineState.ALARM
            stateStr.startsWith("Door") -> MachineState.DOOR
            stateStr.startsWith("Check") -> MachineState.CHECK
            stateStr.startsWith("Home") -> MachineState.HOME
            else -> _status.value.state
        }

        var mX = _status.value.mposX
        var mY = _status.value.mposY
        var mZ = _status.value.mposZ
        var wX = _status.value.wposX
        var wY = _status.value.wposY
        var wZ = _status.value.wposZ
        var feed = _status.value.feedRate
        var spindle = _status.value.spindleSpeed

        for (i in 1 until parts.size) {
            val part = parts[i]
            when {
                part.startsWith("MPos:") -> {
                    val coords = part.removePrefix("MPos:").split(",")
                    if (coords.size >= 2) {
                        mX = coords[0].toFloatOrNull() ?: mX
                        mY = coords[1].toFloatOrNull() ?: mY
                        if (coords.size >= 3) mZ = coords[2].toFloatOrNull() ?: mZ
                    }
                }
                part.startsWith("WPos:") -> {
                    val coords = part.removePrefix("WPos:").split(",")
                    if (coords.size >= 2) {
                        wX = coords[0].toFloatOrNull() ?: wX
                        wY = coords[1].toFloatOrNull() ?: wY
                        if (coords.size >= 3) wZ = coords[2].toFloatOrNull() ?: wZ
                    }
                }
                part.startsWith("FS:") -> {
                    val fs = part.removePrefix("FS:").split(",")
                    if (fs.size >= 2) {
                        feed = fs[0].toFloatOrNull() ?: feed
                        spindle = fs[1].toIntOrNull() ?: spindle
                    }
                }
            }
        }

        _status.value = _status.value.copy(
            state = newState,
            mposX = mX,
            mposY = mY,
            mposZ = mZ,
            wposX = wX,
            wposY = wY,
            wposZ = wZ,
            feedRate = feed,
            spindleSpeed = spindle
        )
    }

    private fun getAlarmDescription(code: Int): String {
        return when (code) {
            1 -> "Hard limit triggered. Machine position likely lost."
            2 -> "Soft limit alarm. Motion exceeds machine travel limits."
            3 -> "Reset while in motion. Machine position lost."
            4 -> "Probe fail. Probe did not contact work."
            5 -> "Probe fail. Probe did not clear work."
            6 -> "Homing fail. Reset during active homing cycle."
            7 -> "Homing fail. Safety door was opened during homing cycle."
            8 -> "Homing fail. Pull-off travel failed to clear limit switch."
            9 -> "Homing fail. Could not find limit switch within search distance."
            else -> "Machine alarm state $code. Issue \$X to unlock after inspecting hardware."
        }
    }

    private fun appendLog(msg: String) {
        val current = _consoleLogs.value.toMutableList()
        current.add(msg)
        if (current.size > 200) current.removeAt(0)
        _consoleLogs.value = current
    }

    fun clearLogs() {
        _consoleLogs.value = emptyList()
    }
}
