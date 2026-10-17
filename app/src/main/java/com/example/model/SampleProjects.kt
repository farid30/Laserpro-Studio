package com.example.model

object SampleProjects {

    data class SampleProject(
        val title: String,
        val subtitle: String,
        val shapes: List<LaserShape>,
        val layers: List<LaserLayer>,
        val category: String
    )

    fun getSampleProjects(): List<SampleProject> {
        return listOf(
            SampleProject(
                title = "LaserPro Calibration Matrix",
                subtitle = "Speed vs Power cutting and engraving test grid (80x80 mm)",
                category = "Calibration",
                layers = LaserLayer.DEFAULT_LAYERS,
                shapes = listOf(
                    // Outer border cut (C00 - Cut)
                    LaserShape.Rectangle(
                        layerId = "C00",
                        name = "Outer Border Cut",
                        x = 10f,
                        y = 10f,
                        width = 80f,
                        height = 80f,
                        cornerRadius = 4f
                    ),
                    // Test engraving patch 1 (C01 - Engrave)
                    LaserShape.Rectangle(
                        layerId = "C01",
                        name = "Engrave Patch 20%",
                        x = 18f,
                        y = 20f,
                        width = 15f,
                        height = 15f
                    ),
                    // Test engraving patch 2 (C01 - Engrave)
                    LaserShape.Rectangle(
                        layerId = "C01",
                        name = "Engrave Patch 50%",
                        x = 38f,
                        y = 20f,
                        width = 15f,
                        height = 15f
                    ),
                    // Test engraving patch 3 (C01 - Engrave)
                    LaserShape.Rectangle(
                        layerId = "C01",
                        name = "Engrave Patch 80%",
                        x = 58f,
                        y = 20f,
                        width = 15f,
                        height = 15f
                    ),
                    // Fine line test (C02 - Mark)
                    LaserShape.Line(layerId = "C02", name = "Test Line 1", x1 = 18f, y1 = 45f, x2 = 73f, y2 = 45f),
                    LaserShape.Line(layerId = "C02", name = "Test Line 2", x1 = 18f, y1 = 50f, x2 = 73f, y2 = 50f),
                    LaserShape.Line(layerId = "C02", name = "Test Line 3", x1 = 18f, y1 = 55f, x2 = 73f, y2 = 55f),
                    // Text marking (C02 - Mark)
                    LaserShape.Text(layerId = "C02", name = "Label Text", text = "LASERPRO", x = 18f, y = 65f, heightMm = 8f),
                    LaserShape.Circle(layerId = "C00", name = "Hanging Hole", centerX = 50f, centerY = 16f, radius = 2.5f)
                )
            ),
            SampleProject(
                title = "Living Hinge Flexible Wood Joint",
                subtitle = "Parametric lattice alternating slit pattern for bending 3mm plywood (100x60 mm)",
                category = "Woodworking",
                layers = LaserLayer.DEFAULT_LAYERS,
                shapes = buildList {
                    // Outer cut rectangle
                    add(LaserShape.Rectangle(layerId = "C00", name = "Hinge Outer Cut", x = 10f, y = 10f, width = 100f, height = 60f))
                    // Alternating flex slits
                    val slitSpacing = 4f
                    val slitLength = 24f
                    var colX = 18f
                    var index = 1
                    while (colX < 100f) {
                        if (index % 2 == 1) {
                            add(LaserShape.Line(layerId = "C00", name = "Slit Top", x1 = colX, y1 = 14f, x2 = colX, y2 = 34f))
                            add(LaserShape.Line(layerId = "C00", name = "Slit Bottom", x1 = colX, y1 = 38f, x2 = colX, y2 = 58f))
                        } else {
                            add(LaserShape.Line(layerId = "C00", name = "Slit Center", x1 = colX, y1 = 24f, x2 = colX, y2 = 48f))
                        }
                        colX += slitSpacing
                        index++
                    }
                }
            ),
            SampleProject(
                title = "Precision Workshop Badge & Emblem",
                subtitle = "Multi-layer keychain with vector cut, marking, and circular engraving (60 mm)",
                category = "Keychains",
                layers = LaserLayer.DEFAULT_LAYERS,
                shapes = listOf(
                    // Outer cut disc
                    LaserShape.Circle(layerId = "C00", name = "Outer Disc Cut", centerX = 50f, centerY = 50f, radius = 30f),
                    // Keyring hole
                    LaserShape.Circle(layerId = "C00", name = "Keyring Hole", centerX = 50f, centerY = 26f, radius = 2.5f),
                    // Inner decorative circle mark
                    LaserShape.Circle(layerId = "C02", name = "Inner Ring Mark", centerX = 50f, centerY = 50f, radius = 23f),
                    // Center Star Polygon
                    LaserShape.Polygon(
                        layerId = "C02",
                        name = "Geometric Star",
                        points = listOf(
                            Point2D(50f, 36f),
                            Point2D(53f, 44f),
                            Point2D(62f, 44f),
                            Point2D(55f, 50f),
                            Point2D(58f, 58f),
                            Point2D(50f, 53f),
                            Point2D(42f, 58f),
                            Point2D(45f, 50f),
                            Point2D(38f, 44f),
                            Point2D(47f, 44f)
                        ),
                        isClosed = true
                    ),
                    // Text
                    LaserShape.Text(layerId = "C02", name = "Badge Text", text = "STUDIO", x = 32f, y = 62f, heightMm = 6f)
                )
            )
        )
    }
}
