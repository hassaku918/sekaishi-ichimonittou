package com.hassaku.sekaishiqa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.hassaku.sekaishiqa.data.AppStore
import com.hassaku.sekaishiqa.ui.AppRoot
import com.hassaku.sekaishiqa.ui.PaperBackground

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E4BFF),
    onPrimary = Color.White,
    background = Color(0xFFF5F5FA),
    onBackground = Color(0xFF1C1C30),
    surface = Color.White,
    onSurface = Color(0xFF1C1C30),
    surfaceVariant = Color(0xFFEFF0F6),
    onSurfaceVariant = Color(0xFF8A8CA3),
    outline = Color(0xFFE3E3EF)
)

// Dark mode with dark blue as the main color.
private val DarkColors = darkColorScheme(
    primary = Color(0xFF4C6FFF),
    onPrimary = Color.White,
    background = Color(0xFF0A1128),
    onBackground = Color(0xFFEDF1FA),
    surface = Color(0xFF14213D),
    onSurface = Color(0xFFEDF1FA),
    surfaceVariant = Color(0xFF1E2C4F),
    onSurfaceVariant = Color(0xFF97A5C4),
    outline = Color(0xFF2C3D66)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = AppStore(this)
        setContent {
            val data by store.data.collectAsState()
            MaterialTheme(colorScheme = if (data.darkMode) DarkColors else LightColors) {
                Surface(color = PaperBackground) {
                    AppRoot(store)
                }
            }
        }
    }
}
