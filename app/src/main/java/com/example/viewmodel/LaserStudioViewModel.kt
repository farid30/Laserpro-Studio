package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.RectF
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gcode.GCodeGenerator
import com.example.machine.GrblMachineController
import com.example.machine.SafetyManager
import com.example.model.DitherMode
import com.example.model.GCodeJob
import com.example.model.LaserLayer
import com.example.model.LaserShape
import com.example.model.MachineProfile
import com.example.model.MaterialPreset
import com.example.model.Point2D
import com.example.model.ProjectFileManager
import com.example.model.SampleProjects
import com.example.parser.DxfImporter
import com.example.parser.ImageProcessor
import com.example.parser.SvgImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.UUID

enum class DrawingTool(val displayName: String) {
    SELECT("Select / Transform"),
    RECTANGLE("Rectangle"),
    CIRCLE("Circle"),
    LINE("Line"),
    POLYGON("Polygon"),
    TEXT("Text"),
    RASTER_IMAGE("Image Engrave"),
    MEASURE("Measure Distance")
}

enum class StudioScreen(val title: String) {
    WORKSPACE("Design CAD"),
    PREVIEW("G-Code & Toolpath"),
    CONTROL("Machine Control"),
    MATERIALS("Materials Library"),
    SETTINGS("Machine Settings")
}

class LaserStudioViewModel(application: Application) : AndroidViewModel(application) {

    val machineController = GrblMachineController(application.applicationContext, viewModelScope)

    private val _currentScreen = MutableStateFlow(StudioScreen.WORKSPACE)
    val currentScreen: StateFlow<StudioScreen> = _currentScreen.asStateFlow()

    private val _projectName = MutableStateFlow("Laser Project 1")
    val projectName: StateFlow<String> = _projectName.asStateFlow()

    private val _shapes = MutableStateFlow<List<LaserShape>>(emptyList())
    val shapes: StateFlow<List<LaserShape>> = _shapes.asStateFlow()

    private val _layers = MutableStateFlow(LaserLayer.DEFAULT_LAYERS)
    val layers: StateFlow<List<LaserLayer>> = _layers.asStateFlow()

    private val _activeLayerId = MutableStateFlow(LaserLayer.DEFAULT_LAYERS.first().id)
    val activeLayerId: StateFlow<String> = _activeLayerId.asStateFlow()

    private val _selectedShapeIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedShapeIds: StateFlow<Set<String>> = _selectedShapeIds.asStateFlow()

    private val _activeTool = MutableStateFlow(DrawingTool.SELECT)
    val activeTool: StateFlow<DrawingTool> = _activeTool.asStateFlow()

    private val _machineProfile = MutableStateFlow(MachineProfile.DEFAULT)
    val machineProfile: StateFlow<MachineProfile> = _machineProfile.asStateFlow()

    private val _materialPresets = MutableStateFlow(MaterialPreset.DEFAULT_PRESETS)
    val materialPresets: StateFlow<List<MaterialPreset>> = _materialPresets.asStateFlow()

    private val _generatedJob = MutableStateFlow<GCodeJob?>(null)
    val generatedJob: StateFlow<GCodeJob?> = _generatedJob.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Undo / Redo history
    private val undoStack = mutableListOf<List<LaserShape>>()
    private val redoStack = mutableListOf<List<LaserShape>>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Zoom and Pan
    private val _zoomScale = MutableStateFlow(1.5f) // 1mm = 1.5dp on screen initially
    val zoomScale: StateFlow<Float> = _zoomScale.asStateFlow()

    private val _panOffsetX = MutableStateFlow(40f)
    val panOffsetX: StateFlow<Float> = _panOffsetX.asStateFlow()

    private val _panOffsetY = MutableStateFlow(40f)
    val panOffsetY: StateFlow<Float> = _panOffsetY.asStateFlow()

    init {
        // Load default sample project on start
        loadSampleProject(SampleProjects.getSampleProjects()[0])
    }

    fun setScreen(screen: StudioScreen) {
        if (screen == StudioScreen.PREVIEW) {
            generateGCodeJob()
        }
        _currentScreen.value = screen
    }

    fun setTool(tool: DrawingTool) {
        _activeTool.value = tool
        if (tool != DrawingTool.SELECT) {
            _selectedShapeIds.value = emptySet()
        }
    }

    fun setActiveLayer(layerId: String) {
        _activeLayerId.value = layerId
        // Also assign selected shapes to this layer if any
        if (_selectedShapeIds.value.isNotEmpty()) {
            recordHistory()
            _shapes.value = _shapes.value.map {
                if (_selectedShapeIds.value.contains(it.id)) it.copyWithLayer(layerId) else it
            }
        }
    }

    fun selectShape(id: String?, addToSelection: Boolean = false) {
        if (id == null) {
            _selectedShapeIds.value = emptySet()
            return
        }
        if (addToSelection) {
            val set = _selectedShapeIds.value.toMutableSet()
            if (set.contains(id)) set.remove(id) else set.add(id)
            _selectedShapeIds.value = set
        } else {
            _selectedShapeIds.value = setOf(id)
        }
    }

    fun selectAll() {
        _selectedShapeIds.value = _shapes.value.map { it.id }.toSet()
    }

    fun addShape(shape: LaserShape) {
        recordHistory()
        _shapes.value = _shapes.value + shape
        _selectedShapeIds.value = setOf(shape.id)
    }

    fun updateShape(shape: LaserShape) {
        recordHistory()
        _shapes.value = _shapes.value.map { if (it.id == shape.id) shape else it }
    }

    fun deleteSelected() {
        if (_selectedShapeIds.value.isEmpty()) return
        recordHistory()
        _shapes.value = _shapes.value.filterNot { _selectedShapeIds.value.contains(it.id) }
        _selectedShapeIds.value = emptySet()
    }

    fun duplicateSelected() {
        if (_selectedShapeIds.value.isEmpty()) return
        recordHistory()
        val toDuplicate = _shapes.value.filter { _selectedShapeIds.value.contains(it.id) }
        val newShapes = toDuplicate.map {
            when (it) {
                is LaserShape.Rectangle -> it.copy(id = UUID.randomUUID().toString(), x = it.x + 10f, y = it.y + 10f)
                is LaserShape.Circle -> it.copy(id = UUID.randomUUID().toString(), centerX = it.centerX + 10f, centerY = it.centerY + 10f)
                is LaserShape.Ellipse -> it.copy(id = UUID.randomUUID().toString(), centerX = it.centerX + 10f, centerY = it.centerY + 10f)
                is LaserShape.Line -> it.copy(id = UUID.randomUUID().toString(), x1 = it.x1 + 10f, y1 = it.y1 + 10f, x2 = it.x2 + 10f, y2 = it.y2 + 10f)
                is LaserShape.Polygon -> it.copy(id = UUID.randomUUID().toString(), points = it.points.map { p -> Point2D(p.x + 10f, p.y + 10f) })
                is LaserShape.VectorPath -> it.copy(id = UUID.randomUUID().toString(), subPaths = it.subPaths.map { sub -> sub.map { p -> Point2D(p.x + 10f, p.y + 10f) } })
                is LaserShape.Text -> it.copy(id = UUID.randomUUID().toString(), x = it.x + 10f, y = it.y + 10f)
                is LaserShape.RasterImage -> it.copy(id = UUID.randomUUID().toString(), x = it.x + 10f, y = it.y + 10f)
            }
        }
        _shapes.value = _shapes.value + newShapes
        _selectedShapeIds.value = newShapes.map { it.id }.toSet()
    }

    fun translateSelected(dx: Float, dy: Float) {
        if (_selectedShapeIds.value.isEmpty()) return
        _shapes.value = _shapes.value.map {
            if (_selectedShapeIds.value.contains(it.id)) it.translate(dx, dy) else it
        }
    }

    fun commitTransformation() {
        recordHistory()
    }

    fun scaleSelected(scaleFactor: Float) {
        if (_selectedShapeIds.value.isEmpty()) return
        val b = getSelectionBounds() ?: return
        recordHistory()
        val cx = b.centerX()
        val cy = b.centerY()
        _shapes.value = _shapes.value.map {
            if (_selectedShapeIds.value.contains(it.id)) it.scale(scaleFactor, scaleFactor, cx, cy) else it
        }
    }

    fun alignSelected(alignment: String) {
        val selected = _shapes.value.filter { _selectedShapeIds.value.contains(it.id) }
        if (selected.size < 2) return
        recordHistory()

        val bounds = getSelectionBounds() ?: return
        _shapes.value = _shapes.value.map { s ->
            if (!_selectedShapeIds.value.contains(s.id)) s
            else {
                val sb = s.bounds()
                val (dx, dy) = when (alignment) {
                    "LEFT" -> Pair(bounds.left - sb.left, 0f)
                    "CENTER_H" -> Pair(bounds.centerX() - sb.centerX(), 0f)
                    "RIGHT" -> Pair(bounds.right - sb.right, 0f)
                    "TOP" -> Pair(0f, bounds.top - sb.top)
                    "CENTER_V" -> Pair(0f, bounds.centerY() - sb.centerY())
                    "BOTTOM" -> Pair(0f, bounds.bottom - sb.bottom)
                    else -> Pair(0f, 0f)
                }
                s.translate(dx, dy)
            }
        }
    }

    fun getSelectionBounds(): RectF? {
        val selected = _shapes.value.filter { _selectedShapeIds.value.contains(it.id) }
        if (selected.isEmpty()) return null
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (s in selected) {
            val b = s.bounds()
            if (b.left < minX) minX = b.left
            if (b.top < minY) minY = b.top
            if (b.right > maxX) maxX = b.right
            if (b.bottom > maxY) maxY = b.bottom
        }
        return RectF(minX, minY, maxX, maxY)
    }

    fun updateLayer(layer: LaserLayer) {
        _layers.value = _layers.value.map { if (it.id == layer.id) layer else it }
    }

    fun toggleLayerEnabled(layerId: String) {
        _layers.value = _layers.value.map {
            if (it.id == layerId) it.copy(enabled = !it.enabled) else it
        }
    }

    fun applyMaterialPresetToActiveLayer(preset: MaterialPreset) {
        val active = _layers.value.find { it.id == _activeLayerId.value } ?: return
        val updated = active.copy(
            mode = preset.recommendedMode,
            speedMmMin = preset.speedMmMin,
            maxPowerPercent = preset.maxPowerPercent,
            passes = preset.passes,
            airAssist = preset.airAssist,
            lineIntervalMm = preset.lineIntervalMm
        )
        updateLayer(updated)
        _userMessage.value = "Applied preset '${preset.name}' to ${active.name}"
    }

    fun updateMachineProfile(profile: MachineProfile) {
        _machineProfile.value = profile
        _userMessage.value = "Machine profile '${profile.name}' updated"
    }

    fun generateGCodeJob(): GCodeJob {
        val job = GCodeGenerator.generateJob(_shapes.value, _layers.value, _machineProfile.value)
        _generatedJob.value = job
        return job
    }

    fun frameSelectedOrAll() {
        val bounds = getSelectionBounds() ?: run {
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            for (s in _shapes.value) {
                val b = s.bounds()
                if (b.left < minX) minX = b.left
                if (b.top < minY) minY = b.top
                if (b.right > maxX) maxX = b.right
                if (b.bottom > maxY) maxY = b.bottom
            }
            if (minX < Float.MAX_VALUE) RectF(minX, minY, maxX, maxY) else RectF(0f, 0f, 50f, 50f)
        }
        machineController.frameBoundingBox(bounds)
    }

    fun loadSampleProject(sample: SampleProjects.SampleProject) {
        recordHistory()
        _projectName.value = sample.title
        _shapes.value = sample.shapes
        _layers.value = sample.layers
        _activeLayerId.value = sample.layers.first().id
        _selectedShapeIds.value = emptySet()
        _userMessage.value = "Loaded: ${sample.title}"
    }

    fun importSvg(content: String) {
        recordHistory()
        val result = SvgImporter.parseSvgString(content, _activeLayerId.value)
        _shapes.value = _shapes.value + result.shapes
        _selectedShapeIds.value = result.shapes.map { it.id }.toSet()
        _userMessage.value = result.description
    }

    fun importDxf(content: String) {
        recordHistory()
        val result = DxfImporter.parseDxfString(content, _activeLayerId.value)
        _shapes.value = _shapes.value + result.shapes
        _selectedShapeIds.value = result.shapes.map { it.id }.toSet()
        _userMessage.value = result.description
    }

    fun importRasterBitmap(bitmap: Bitmap) {
        recordHistory()
        val processed = ImageProcessor.processBitmapToMatrix(
            bitmap = bitmap,
            ditherMode = DitherMode.FLOYD_STEINBERG,
            threshold = 128
        )
        val imageShape = LaserShape.RasterImage(
            layerId = _activeLayerId.value,
            name = "Engrave Image",
            x = 20f,
            y = 20f,
            width = 60f,
            height = 60f * (processed.heightPx.toFloat() / processed.widthPx.toFloat()),
            ditherMode = DitherMode.FLOYD_STEINBERG,
            pixelWidth = processed.widthPx,
            pixelHeight = processed.heightPx
        )
        _shapes.value = _shapes.value + imageShape
        _selectedShapeIds.value = setOf(imageShape.id)
        _userMessage.value = "Imported raster image (${processed.widthPx}x${processed.heightPx} px) for engraving"
    }

    fun recordHistory() {
        undoStack.add(_shapes.value)
        if (undoStack.size > 30) undoStack.removeAt(0)
        redoStack.clear()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = false
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = _shapes.value
        val prev = undoStack.removeAt(undoStack.size - 1)
        redoStack.add(current)
        _shapes.value = prev
        _selectedShapeIds.value = emptySet()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = true
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = _shapes.value
        val next = redoStack.removeAt(redoStack.size - 1)
        undoStack.add(current)
        _shapes.value = next
        _selectedShapeIds.value = emptySet()
        _canUndo.value = true
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun clearProject() {
        recordHistory()
        _shapes.value = emptyList()
        _selectedShapeIds.value = emptySet()
        _projectName.value = "New Laser Project"
    }

    fun updateZoomPan(zoomDelta: Float, panDx: Float, panDy: Float) {
        val newZoom = (_zoomScale.value * zoomDelta).coerceIn(0.2f, 15f)
        _zoomScale.value = newZoom
        _panOffsetX.value += panDx
        _panOffsetY.value += panDy
    }

    fun resetView() {
        _zoomScale.value = 1.5f
        _panOffsetX.value = 40f
        _panOffsetY.value = 40f
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}
