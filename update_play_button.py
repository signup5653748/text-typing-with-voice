import re

with open("app/src/main/java/com/example/ui/components/PlayButton.kt", "r") as f:
    content = f.read()

# Replace import
content = content.replace("import androidx.compose.material.icons.filled.Stop", "import androidx.compose.material.icons.filled.Close\nimport androidx.compose.ui.graphics.vector.ImageVector\nimport androidx.compose.ui.graphics.vector.path\nimport androidx.compose.ui.graphics.SolidColor")

# Add custom StopIcon
stop_icon_code = """
val StopIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Stop",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(6f, 6f)
            horizontalLineToRelative(12f)
            verticalLineToRelative(12f)
            horizontalLineTo(6f)
            close()
        }
    }.build()

@Composable
"""
content = content.replace("@Composable", stop_icon_code, 1)

# Use StopIcon
content = content.replace("Icons.Default.Stop", "StopIcon")

with open("app/src/main/java/com/example/ui/components/PlayButton.kt", "w") as f:
    f.write(content)
