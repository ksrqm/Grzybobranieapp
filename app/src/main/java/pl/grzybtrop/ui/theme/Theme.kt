package pl.grzybtrop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF0E1411)
val Surface = Color(0xFF161D19)
val Border = Color(0xFF263029)
val Lime = Color(0xFFB7D87A)
val OnLime = Color(0xFF0E1411)
val Muted = Color(0xFF9AA79F)
val Amber = Color(0xFFE8A33D)
val Danger = Color(0xFFE5675B)

val Blue = Color(0xFF7FA8E8)

private val GrzybColors = darkColorScheme(
    primary = Lime,
    onPrimary = OnLime,
    background = Bg,
    onBackground = Color.White,
    surface = Surface,
    onSurface = Color.White,
)

@Composable
fun GrzybTropTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GrzybColors, content = content)
}
