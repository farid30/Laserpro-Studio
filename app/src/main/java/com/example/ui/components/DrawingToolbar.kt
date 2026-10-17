package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LaserBorder
import com.example.ui.theme.LaserPrimary
import com.example.ui.theme.LaserSurfaceElevated
import com.example.viewmodel.DrawingTool

@Composable
fun DrawingToolbar(
    modifier: Modifier = Modifier,
    isVertical: Boolean = false,
    activeTool: DrawingTool,
    onToolSelected: (DrawingTool) -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    hasSelection: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onAlign: (String) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit
) {
    var alignMenuOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .testTag("drawing_toolbar"),
        color = LaserSurfaceElevated,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, LaserBorder)
    ) {
        if (isVertical) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(52.dp)
                    .padding(vertical = 6.dp, horizontal = 4.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Drawing tools
                ToolItem(DrawingTool.SELECT, Icons.Default.NearMe, activeTool == DrawingTool.SELECT) { onToolSelected(it) }
                ToolItem(DrawingTool.RECTANGLE, Icons.Default.CropSquare, activeTool == DrawingTool.RECTANGLE) { onToolSelected(it) }
                ToolItem(DrawingTool.CIRCLE, Icons.Default.RadioButtonUnchecked, activeTool == DrawingTool.CIRCLE) { onToolSelected(it) }
                ToolItem(DrawingTool.LINE, Icons.Default.HorizontalRule, activeTool == DrawingTool.LINE) { onToolSelected(it) }
                ToolItem(DrawingTool.POLYGON, Icons.Default.Category, activeTool == DrawingTool.POLYGON) { onToolSelected(it) }
                ToolItem(DrawingTool.TEXT, Icons.Default.TextFields, activeTool == DrawingTool.TEXT) { onToolSelected(it) }
                ToolItem(DrawingTool.RASTER_IMAGE, Icons.Default.Image, activeTool == DrawingTool.RASTER_IMAGE) { onToolSelected(it) }
                ToolItem(DrawingTool.MEASURE, Icons.Default.Straighten, activeTool == DrawingTool.MEASURE) { onToolSelected(it) }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = LaserBorder)

                // Actions
                ActionItem(Icons.AutoMirrored.Filled.Undo, "Undo", canUndo) { onUndo() }
                ActionItem(Icons.AutoMirrored.Filled.Redo, "Redo", canRedo) { onRedo() }
                ActionItem(Icons.Default.ContentCopy, "Duplicate", hasSelection) { onDuplicate() }
                ActionItem(Icons.Default.Delete, "Delete", hasSelection, tint = MaterialTheme.colorScheme.error) { onDelete() }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = LaserBorder)

                // Zoom controls
                ActionItem(Icons.Default.ZoomIn, "Zoom In", true) { onZoomIn() }
                ActionItem(Icons.Default.ZoomOut, "Zoom Out", true) { onZoomOut() }
                ActionItem(Icons.Default.ZoomOutMap, "Fit Bed", true) { onResetZoom() }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ToolItem(DrawingTool.SELECT, Icons.Default.NearMe, activeTool == DrawingTool.SELECT) { onToolSelected(it) }
                ToolItem(DrawingTool.RECTANGLE, Icons.Default.CropSquare, activeTool == DrawingTool.RECTANGLE) { onToolSelected(it) }
                ToolItem(DrawingTool.CIRCLE, Icons.Default.RadioButtonUnchecked, activeTool == DrawingTool.CIRCLE) { onToolSelected(it) }
                ToolItem(DrawingTool.LINE, Icons.Default.HorizontalRule, activeTool == DrawingTool.LINE) { onToolSelected(it) }
                ToolItem(DrawingTool.POLYGON, Icons.Default.Category, activeTool == DrawingTool.POLYGON) { onToolSelected(it) }
                ToolItem(DrawingTool.TEXT, Icons.Default.TextFields, activeTool == DrawingTool.TEXT) { onToolSelected(it) }
                ToolItem(DrawingTool.RASTER_IMAGE, Icons.Default.Image, activeTool == DrawingTool.RASTER_IMAGE) { onToolSelected(it) }
                ToolItem(DrawingTool.MEASURE, Icons.Default.Straighten, activeTool == DrawingTool.MEASURE) { onToolSelected(it) }

                VerticalDivider(modifier = Modifier.padding(horizontal = 4.dp), color = LaserBorder)

                ActionItem(Icons.AutoMirrored.Filled.Undo, "Undo", canUndo) { onUndo() }
                ActionItem(Icons.AutoMirrored.Filled.Redo, "Redo", canRedo) { onRedo() }
                ActionItem(Icons.Default.ContentCopy, "Duplicate", hasSelection) { onDuplicate() }
                ActionItem(Icons.Default.Delete, "Delete", hasSelection, tint = MaterialTheme.colorScheme.error) { onDelete() }

                // Align Dropdown
                Box {
                    IconButton(
                        onClick = { alignMenuOpen = true },
                        enabled = hasSelection,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Default.FormatAlignLeft,
                            contentDescription = "Align",
                            tint = if (hasSelection) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = alignMenuOpen,
                        onDismissRequest = { alignMenuOpen = false }
                    ) {
                        DropdownMenuItem(text = { Text("Align Left") }, onClick = { onAlign("LEFT"); alignMenuOpen = false })
                        DropdownMenuItem(text = { Text("Align Center Horizontal") }, onClick = { onAlign("CENTER_H"); alignMenuOpen = false })
                        DropdownMenuItem(text = { Text("Align Right") }, onClick = { onAlign("RIGHT"); alignMenuOpen = false })
                        DropdownMenuItem(text = { Text("Align Top") }, onClick = { onAlign("TOP"); alignMenuOpen = false })
                        DropdownMenuItem(text = { Text("Align Center Vertical") }, onClick = { onAlign("CENTER_V"); alignMenuOpen = false })
                        DropdownMenuItem(text = { Text("Align Bottom") }, onClick = { onAlign("BOTTOM"); alignMenuOpen = false })
                    }
                }

                VerticalDivider(modifier = Modifier.padding(horizontal = 4.dp), color = LaserBorder)

                ActionItem(Icons.Default.ZoomIn, "Zoom In", true) { onZoomIn() }
                ActionItem(Icons.Default.ZoomOut, "Zoom Out", true) { onZoomOut() }
                ActionItem(Icons.Default.ZoomOutMap, "Fit Bed", true) { onResetZoom() }
            }
        }
    }
}

@Composable
private fun ToolItem(
    tool: DrawingTool,
    icon: ImageVector,
    isSelected: Boolean,
    onSelect: (DrawingTool) -> Unit
) {
    FilledIconButton(
        onClick = { onSelect(tool) },
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (isSelected) LaserPrimary else Color.Transparent,
            contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .size(38.dp)
            .testTag("tool_${tool.name.lowercase()}")
    ) {
        Icon(icon, contentDescription = tool.displayName, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    desc: String,
    enabled: Boolean,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(38.dp)
            .testTag("action_${desc.lowercase().replace(' ', '_')}")
    ) {
        Icon(
            icon,
            contentDescription = desc,
            tint = if (enabled) tint else tint.copy(alpha = 0.35f),
            modifier = Modifier.size(20.dp)
        )
    }
}
