package com.example.machine

enum class MachineState(val displayName: String) {
    DISCONNECTED("Disconnected"),
    IDLE("Idle"),
    RUN("Running"),
    HOLD("Hold / Paused"),
    JOG("Jogging"),
    ALARM("Alarm / Locked"),
    DOOR("Door Open Safety Interlock"),
    CHECK("G-Code Check Mode"),
    HOME("Homing Cycle")
}

enum class ConnectionType {
    NONE,
    USB_SERIAL,
    MOCK_SIMULATOR
}

data class GrblStatus(
    val state: MachineState = MachineState.DISCONNECTED,
    val connectionType: ConnectionType = ConnectionType.NONE,
    val isConnected: Boolean = false,
    val deviceName: String = "No Controller",
    val mposX: Float = 0f,
    val mposY: Float = 0f,
    val mposZ: Float = 0f,
    val wposX: Float = 0f,
    val wposY: Float = 0f,
    val wposZ: Float = 0f,
    val feedRate: Float = 0f,
    val spindleSpeed: Int = 0,
    val bufferPlanner: Int = 15,
    val bufferRx: Int = 128,
    val alarmCode: Int? = null,
    val alarmMessage: String? = null,
    val jobProgress: Float = 0f, // 0.0 to 1.0
    val isStreamingJob: Boolean = false,
    val linesProcessed: Int = 0,
    val totalLinesInJob: Int = 0
)
