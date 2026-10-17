package com.example.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

object ProjectFileManager {

    data class LoadedProject(
        val name: String,
        val shapes: List<LaserShape>,
        val layers: List<LaserLayer>,
        val machineProfile: MachineProfile
    )

    fun serializeProject(
        projectName: String,
        shapes: List<LaserShape>,
        layers: List<LaserLayer>,
        machineProfile: MachineProfile
    ): String {
        val root = JSONObject()
        root.put("version", "1.0")
        root.put("name", projectName)

        // Machine profile
        val mp = JSONObject()
        mp.put("name", machineProfile.name)
        mp.put("bedWidthMm", machineProfile.bedWidthMm.toDouble())
        mp.put("bedHeightMm", machineProfile.bedHeightMm.toDouble())
        mp.put("maxSpeedMmMin", machineProfile.maxSpeedMmMin.toDouble())
        mp.put("laserMaxPwm", machineProfile.laserMaxPwm)
        root.put("machineProfile", mp)

        // Layers
        val layersArr = JSONArray()
        for (l in layers) {
            val lo = JSONObject()
            lo.put("id", l.id)
            lo.put("name", l.name)
            lo.put("colorHex", l.colorHex)
            lo.put("mode", l.mode.name)
            lo.put("speedMmMin", l.speedMmMin.toDouble())
            lo.put("maxPowerPercent", l.maxPowerPercent.toDouble())
            lo.put("passes", l.passes)
            lo.put("airAssist", l.airAssist)
            lo.put("lineIntervalMm", l.lineIntervalMm.toDouble())
            lo.put("enabled", l.enabled)
            layersArr.put(lo)
        }
        root.put("layers", layersArr)

        // Shapes
        val shapesArr = JSONArray()
        for (s in shapes) {
            val so = JSONObject()
            so.put("id", s.id)
            so.put("layerId", s.layerId)
            so.put("name", s.name)
            so.put("rotationDeg", s.rotationDeg.toDouble())

            when (s) {
                is LaserShape.Rectangle -> {
                    so.put("type", "RECTANGLE")
                    so.put("x", s.x.toDouble())
                    so.put("y", s.y.toDouble())
                    so.put("width", s.width.toDouble())
                    so.put("height", s.height.toDouble())
                    so.put("cornerRadius", s.cornerRadius.toDouble())
                }
                is LaserShape.Circle -> {
                    so.put("type", "CIRCLE")
                    so.put("centerX", s.centerX.toDouble())
                    so.put("centerY", s.centerY.toDouble())
                    so.put("radius", s.radius.toDouble())
                }
                is LaserShape.Ellipse -> {
                    so.put("type", "ELLIPSE")
                    so.put("centerX", s.centerX.toDouble())
                    so.put("centerY", s.centerY.toDouble())
                    so.put("radiusX", s.radiusX.toDouble())
                    so.put("radiusY", s.radiusY.toDouble())
                }
                is LaserShape.Line -> {
                    so.put("type", "LINE")
                    so.put("x1", s.x1.toDouble())
                    so.put("y1", s.y1.toDouble())
                    so.put("x2", s.x2.toDouble())
                    so.put("y2", s.y2.toDouble())
                }
                is LaserShape.Polygon -> {
                    so.put("type", "POLYGON")
                    so.put("isClosed", s.isClosed)
                    val pts = JSONArray()
                    for (p in s.points) {
                        val po = JSONObject()
                        po.put("x", p.x.toDouble())
                        po.put("y", p.y.toDouble())
                        pts.put(po)
                    }
                    so.put("points", pts)
                }
                is LaserShape.VectorPath -> {
                    so.put("type", "PATH")
                    so.put("isClosed", s.isClosed)
                    val subArr = JSONArray()
                    for (sub in s.subPaths) {
                        val pts = JSONArray()
                        for (p in sub) {
                            val po = JSONObject()
                            po.put("x", p.x.toDouble())
                            po.put("y", p.y.toDouble())
                            pts.put(po)
                        }
                        subArr.put(pts)
                    }
                    so.put("subPaths", subArr)
                }
                is LaserShape.Text -> {
                    so.put("type", "TEXT")
                    so.put("text", s.text)
                    so.put("x", s.x.toDouble())
                    so.put("y", s.y.toDouble())
                    so.put("heightMm", s.heightMm.toDouble())
                }
                is LaserShape.RasterImage -> {
                    so.put("type", "RASTER")
                    so.put("x", s.x.toDouble())
                    so.put("y", s.y.toDouble())
                    so.put("width", s.width.toDouble())
                    so.put("height", s.height.toDouble())
                }
            }
            shapesArr.put(so)
        }
        root.put("shapes", shapesArr)

        return root.toString(2)
    }

    fun deserializeProject(jsonStr: String): LoadedProject {
        val root = JSONObject(jsonStr)
        val name = root.optString("name", "Untitled Laser Project")

        // Parse layers
        val layers = mutableListOf<LaserLayer>()
        val layersArr = root.optJSONArray("layers")
        if (layersArr != null) {
            for (i in 0 until layersArr.length()) {
                val lo = layersArr.getJSONObject(i)
                val id = lo.getString("id")
                val lName = lo.optString("name", "Layer $id")
                val colorHex = lo.optLong("colorHex", 0xFF00E5FF)
                val mode = LayerMode.valueOf(lo.optString("mode", "CUT"))
                val speed = lo.optDouble("speedMmMin", 600.0).toFloat()
                val power = lo.optDouble("maxPowerPercent", 80.0).toFloat()
                val passes = lo.optInt("passes", 1)
                val air = lo.optBoolean("airAssist", true)
                val interval = lo.optDouble("lineIntervalMm", 0.1).toFloat()
                val enabled = lo.optBoolean("enabled", true)
                layers.add(LaserLayer(id, lName, colorHex, mode, speed, 0f, power, passes, interval, air, 2.5f, 0f, enabled))
            }
        }
        if (layers.isEmpty()) {
            layers.addAll(LaserLayer.DEFAULT_LAYERS)
        }

        // Parse shapes
        val shapes = mutableListOf<LaserShape>()
        val shapesArr = root.optJSONArray("shapes")
        if (shapesArr != null) {
            for (i in 0 until shapesArr.length()) {
                val so = shapesArr.getJSONObject(i)
                val type = so.optString("type", "")
                val id = so.optString("id")
                val layerId = so.optString("layerId", layers.first().id)
                val sName = so.optString("name", "Object")

                when (type) {
                    "RECTANGLE" -> {
                        shapes.add(
                            LaserShape.Rectangle(
                                id = id,
                                layerId = layerId,
                                name = sName,
                                x = so.getDouble("x").toFloat(),
                                y = so.getDouble("y").toFloat(),
                                width = so.getDouble("width").toFloat(),
                                height = so.getDouble("height").toFloat(),
                                cornerRadius = so.optDouble("cornerRadius", 0.0).toFloat()
                            )
                        )
                    }
                    "CIRCLE" -> {
                        shapes.add(
                            LaserShape.Circle(
                                id = id,
                                layerId = layerId,
                                name = sName,
                                centerX = so.getDouble("centerX").toFloat(),
                                centerY = so.getDouble("centerY").toFloat(),
                                radius = so.getDouble("radius").toFloat()
                            )
                        )
                    }
                    "ELLIPSE" -> {
                        shapes.add(
                            LaserShape.Ellipse(
                                id = id,
                                layerId = layerId,
                                name = sName,
                                centerX = so.getDouble("centerX").toFloat(),
                                centerY = so.getDouble("centerY").toFloat(),
                                radiusX = so.getDouble("radiusX").toFloat(),
                                radiusY = so.getDouble("radiusY").toFloat()
                            )
                        )
                    }
                    "LINE" -> {
                        shapes.add(
                            LaserShape.Line(
                                id = id,
                                layerId = layerId,
                                name = sName,
                                x1 = so.getDouble("x1").toFloat(),
                                y1 = so.getDouble("y1").toFloat(),
                                x2 = so.getDouble("x2").toFloat(),
                                y2 = so.getDouble("y2").toFloat()
                            )
                        )
                    }
                    "POLYGON" -> {
                        val ptsArr = so.optJSONArray("points")
                        val pts = mutableListOf<Point2D>()
                        if (ptsArr != null) {
                            for (p in 0 until ptsArr.length()) {
                                val po = ptsArr.getJSONObject(p)
                                pts.add(Point2D(po.getDouble("x").toFloat(), po.getDouble("y").toFloat()))
                            }
                        }
                        shapes.add(
                            LaserShape.Polygon(
                                id = id,
                                layerId = layerId,
                                name = sName,
                                points = pts,
                                isClosed = so.optBoolean("isClosed", true)
                            )
                        )
                    }
                    "TEXT" -> {
                        shapes.add(
                            LaserShape.Text(
                                id = id,
                                layerId = layerId,
                                name = sName,
                                text = so.getString("text"),
                                x = so.getDouble("x").toFloat(),
                                y = so.getDouble("y").toFloat(),
                                heightMm = so.getDouble("heightMm").toFloat()
                            )
                        )
                    }
                }
            }
        }

        val profile = MachineProfile()
        return LoadedProject(name, shapes, layers, profile)
    }

    fun exportToSvg(shapes: List<LaserShape>, layers: List<LaserLayer>, bedWidth: Float, bedHeight: Float): String {
        val svg = StringBuilder()
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"${bedWidth}mm\" height=\"${bedHeight}mm\" viewBox=\"0 0 $bedWidth $bedHeight\">\n")

        val layerMap = layers.associateBy { it.id }

        for (s in shapes) {
            val layer = layerMap[s.layerId]
            val colorHexStr = String.format("#%06X", (layer?.colorHex ?: 0xFF00E5FF) and 0xFFFFFF)
            when (s) {
                is LaserShape.Rectangle -> {
                    svg.append("  <rect x=\"${s.x}\" y=\"${s.y}\" width=\"${s.width}\" height=\"${s.height}\" rx=\"${s.cornerRadius}\" fill=\"none\" stroke=\"$colorHexStr\" stroke-width=\"0.2\" />\n")
                }
                is LaserShape.Circle -> {
                    svg.append("  <circle cx=\"${s.centerX}\" cy=\"${s.centerY}\" r=\"${s.radius}\" fill=\"none\" stroke=\"$colorHexStr\" stroke-width=\"0.2\" />\n")
                }
                is LaserShape.Ellipse -> {
                    svg.append("  <ellipse cx=\"${s.centerX}\" cy=\"${s.centerY}\" rx=\"${s.radiusX}\" ry=\"${s.radiusY}\" fill=\"none\" stroke=\"$colorHexStr\" stroke-width=\"0.2\" />\n")
                }
                is LaserShape.Line -> {
                    svg.append("  <line x1=\"${s.x1}\" y1=\"${s.y1}\" x2=\"${s.x2}\" y2=\"${s.y2}\" stroke=\"$colorHexStr\" stroke-width=\"0.2\" />\n")
                }
                is LaserShape.Polygon -> {
                    val pts = s.points.joinToString(" ") { "${it.x},${it.y}" }
                    val tag = if (s.isClosed) "polygon" else "polyline"
                    svg.append("  <$tag points=\"$pts\" fill=\"none\" stroke=\"$colorHexStr\" stroke-width=\"0.2\" />\n")
                }
                is LaserShape.Text -> {
                    svg.append("  <text x=\"${s.x}\" y=\"${s.y + s.heightMm}\" font-size=\"${s.heightMm}\" fill=\"$colorHexStr\">${s.text}</text>\n")
                }
                else -> {}
            }
        }
        svg.append("</svg>\n")
        return svg.toString()
    }

    fun exportToDxf(shapes: List<LaserShape>): String {
        val dxf = StringBuilder()
        dxf.append("0\nSECTION\n2\nENTITIES\n")

        for (s in shapes) {
            when (s) {
                is LaserShape.Line -> {
                    dxf.append("0\nLINE\n8\nLASER_CUT\n")
                    dxf.append("10\n${s.x1}\n20\n${s.y1}\n30\n0.0\n")
                    dxf.append("11\n${s.x2}\n21\n${s.y2}\n31\n0.0\n")
                }
                is LaserShape.Circle -> {
                    dxf.append("0\nCIRCLE\n8\nLASER_CUT\n")
                    dxf.append("10\n${s.centerX}\n20\n${s.centerY}\n30\n0.0\n")
                    dxf.append("40\n${s.radius}\n")
                }
                is LaserShape.Rectangle -> {
                    dxf.append("0\nLWPOLYLINE\n8\nLASER_CUT\n90\n4\n70\n1\n")
                    dxf.append("10\n${s.x}\n20\n${s.y}\n")
                    dxf.append("10\n${s.x + s.width}\n20\n${s.y}\n")
                    dxf.append("10\n${s.x + s.width}\n20\n${s.y + s.height}\n")
                    dxf.append("10\n${s.x}\n20\n${s.y + s.height}\n")
                }
                is LaserShape.Polygon -> {
                    dxf.append("0\nLWPOLYLINE\n8\nLASER_CUT\n")
                    dxf.append("90\n${s.points.size}\n")
                    dxf.append("70\n${if (s.isClosed) 1 else 0}\n")
                    for (p in s.points) {
                        dxf.append("10\n${p.x}\n20\n${p.y}\n")
                    }
                }
                is LaserShape.Text -> {
                    dxf.append("0\nTEXT\n8\nLASER_MARK\n")
                    dxf.append("10\n${s.x}\n20\n${s.y}\n40\n${s.heightMm}\n1\n${s.text}\n")
                }
                else -> {}
            }
        }

        dxf.append("0\nENDSEC\n0\nEOF\n")
        return dxf.toString()
    }
}
