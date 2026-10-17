package com.example.machine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class MockGrblSimulator(
    private val scope: CoroutineScope
) {
    private val _incomingResponses = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val incomingResponses: SharedFlow<String> = _incomingResponses

    var isConnected = false
        private set

    var state = MachineState.IDLE
    var mposX = 0f
    var mposY = 0f
    var mposZ = 0f
    var wposX = 0f
    var wposY = 0f
    var wposZ = 0f
    var laserPower = 0
    var feedRate = 0f

    private var statusPollJob: Job? = null
    private var isStreaming = false

    fun connect(): Boolean {
        isConnected = true
        state = MachineState.IDLE
        scope.launch {
            _incomingResponses.emit("\r\nGrbl 1.1h ['\$' for help]\r\n")
            _incomingResponses.emit("[MSG:'\$H'|'\$X' to unlock]\r\n")
            _incomingResponses.emit("ok\r\n")
        }
        startStatusTimer()
        return true
    }

    fun disconnect() {
        isConnected = false
        state = MachineState.DISCONNECTED
        statusPollJob?.cancel()
        statusPollJob = null
        laserPower = 0
    }

    private fun startStatusTimer() {
        statusPollJob?.cancel()
        statusPollJob = scope.launch(Dispatchers.Default) {
            while (isActive && isConnected) {
                delay(200)
                // Periodically emit simulated status if queried or idle
                emitStatusReport()
            }
        }
    }

    suspend fun emitStatusReport() {
        val stateStr = when (state) {
            MachineState.IDLE -> "Idle"
            MachineState.RUN -> "Run"
            MachineState.HOLD -> "Hold:0"
            MachineState.JOG -> "Jog"
            MachineState.ALARM -> "Alarm"
            MachineState.DOOR -> "Door:0"
            MachineState.CHECK -> "Check"
            MachineState.HOME -> "Home"
            MachineState.DISCONNECTED -> "Idle"
        }
        val report = String.format(
            Locale.US,
            "<%s|MPos:%.3f,%.3f,%.3f|WPos:%.3f,%.3f,%.3f|FS:%.0f,%d|Bf:15,128>\r\n",
            stateStr, mposX, mposY, mposZ, wposX, wposY, wposZ, feedRate, laserPower
        )
        _incomingResponses.emit(report)
    }

    suspend fun handleCommand(rawCmd: String) {
        val cmd = rawCmd.trim()
        if (cmd.isEmpty()) return

        when {
            cmd == "?" -> {
                emitStatusReport()
            }
            cmd == "\$X" -> {
                state = MachineState.IDLE
                _incomingResponses.emit("[MSG:Caution: Unlocked]\r\nok\r\n")
            }
            cmd == "\$H" -> {
                state = MachineState.HOME
                delay(300)
                mposX = 0f
                mposY = 0f
                mposZ = 0f
                wposX = 0f
                wposY = 0f
                wposZ = 0f
                state = MachineState.IDLE
                _incomingResponses.emit("ok\r\n")
            }
            cmd == "!" -> { // Pause / Feed hold
                state = MachineState.HOLD
            }
            cmd == "~" -> { // Resume / Cycle start
                state = if (isStreaming) MachineState.RUN else MachineState.IDLE
            }
            cmd.startsWith("\u0018") || cmd == "RESET" -> { // Soft reset
                state = MachineState.IDLE
                laserPower = 0
                _incomingResponses.emit("\r\nGrbl 1.1h ['\$' for help]\r\nok\r\n")
            }
            cmd == "\$\$" -> {
                // Return GRBL standard configuration settings
                val settings = listOf(
                    "\$0=10 (Step pulse time, microseconds)",
                    "\$1=255 (Step idle delay, milliseconds)",
                    "\$2=0 (Step pulse invert, mask)",
                    "\$3=0 (Step direction invert, mask)",
                    "\$10=1 (Status report mask)",
                    "\$11=0.010 (Junction deviation, mm)",
                    "\$12=0.002 (Arc tolerance, mm)",
                    "\$13=0 (Report inches, boolean)",
                    "\$20=0 (Soft limits, boolean)",
                    "\$21=0 (Hard limits, boolean)",
                    "\$22=1 (Homing cycle, boolean)",
                    "\$23=3 (Homing dir invert, mask)",
                    "\$24=25.000 (Homing feed, mm/min)",
                    "\$25=500.000 (Homing seek, mm/min)",
                    "\$26=250 (Homing debounce, milliseconds)",
                    "\$27=1.000 (Homing pull-off, mm)",
                    "\$30=1000 (Max spindle speed / laser max PWM)",
                    "\$31=0 (Min spindle speed)",
                    "\$32=1 (Laser-mode enable, boolean)",
                    "\$100=80.000 (X-axis travel resolution, step/mm)",
                    "\$101=80.000 (Y-axis travel resolution, step/mm)",
                    "\$102=80.000 (Z-axis travel resolution, step/mm)",
                    "\$110=10000.000 (X-axis maximum rate, mm/min)",
                    "\$111=10000.000 (Y-axis maximum rate, mm/min)",
                    "\$112=3000.000 (Z-axis maximum rate, mm/min)",
                    "\$120=500.000 (X-axis acceleration, mm/sec^2)",
                    "\$121=500.000 (Y-axis acceleration, mm/sec^2)",
                    "\$122=200.000 (Z-axis acceleration, mm/sec^2)",
                    "\$130=400.000 (X-axis maximum travel, mm)",
                    "\$131=400.000 (Y-axis maximum travel, mm)",
                    "\$132=50.000 (Z-axis maximum travel, mm)"
                )
                for (s in settings) {
                    _incomingResponses.emit("$s\r\n")
                }
                _incomingResponses.emit("ok\r\n")
            }
            cmd.startsWith("\$J=") -> {
                // Jog command
                state = MachineState.JOG
                parseAndApplyMovement(cmd.removePrefix("\$J="))
                delay(50)
                state = MachineState.IDLE
                _incomingResponses.emit("ok\r\n")
            }
            cmd.startsWith("G10 L20") || cmd.startsWith("G92") -> {
                // Set origin
                wposX = 0f
                wposY = 0f
                _incomingResponses.emit("ok\r\n")
            }
            cmd.startsWith("G0") || cmd.startsWith("G1") -> {
                parseAndApplyMovement(cmd)
                _incomingResponses.emit("ok\r\n")
            }
            cmd.startsWith("M3") || cmd.startsWith("M4") -> {
                val sMatch = Regex("""S(\d+)""").find(cmd)
                laserPower = sMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1000
                _incomingResponses.emit("ok\r\n")
            }
            cmd.startsWith("M5") -> {
                laserPower = 0
                _incomingResponses.emit("ok\r\n")
            }
            else -> {
                _incomingResponses.emit("ok\r\n")
            }
        }
    }

    private fun parseAndApplyMovement(gcodeLine: String) {
        val xMatch = Regex("""X([-\d.]+)""").find(gcodeLine)
        val yMatch = Regex("""Y([-\d.]+)""").find(gcodeLine)
        val fMatch = Regex("""F([-\d.]+)""").find(gcodeLine)
        val sMatch = Regex("""S(\d+)""").find(gcodeLine)

        val isIncremental = gcodeLine.contains("G91")

        if (xMatch != null) {
            val v = xMatch.groupValues[1].toFloatOrNull() ?: 0f
            if (isIncremental) {
                mposX += v
                wposX += v
            } else {
                mposX = v
                wposX = v
            }
        }
        if (yMatch != null) {
            val v = yMatch.groupValues[1].toFloatOrNull() ?: 0f
            if (isIncremental) {
                mposY += v
                wposY += v
            } else {
                mposY = v
                wposY = v
            }
        }
        if (fMatch != null) {
            feedRate = fMatch.groupValues[1].toFloatOrNull() ?: feedRate
        }
        if (sMatch != null) {
            laserPower = sMatch.groupValues[1].toIntOrNull() ?: laserPower
        }
    }
}
