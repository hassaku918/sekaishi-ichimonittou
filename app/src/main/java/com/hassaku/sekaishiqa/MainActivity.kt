package com.hassaku.sekaishiqa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.hassaku.sekaishiqa.data.AppStore
import com.hassaku.sekaishiqa.ui.AccentBlue
import com.hassaku.sekaishiqa.ui.AppRoot
import com.hassaku.sekaishiqa.ui.InkOutline
import com.hassaku.sekaishiqa.ui.InkSurfaceHigh
import com.hassaku.sekaishiqa.ui.InkText
import com.hassaku.sekaishiqa.ui.InkTextSub
import com.hassaku.sekaishiqa.ui.PaperBackground

private val AppColors = lightColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    background = PaperBackground,
    onBackground = InkText,
    surface = Color.White,
    onSurface = InkText,
    surfaceVariant = InkSurfaceHigh,
    onSurfaceVariant = InkTextSub,
    outline = InkOutline
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = AppStore(this)
        setContent {
            MaterialTheme(colorScheme = AppColors) {
                Surface(color = PaperBackground) {
                    AppRoot(store)
                }
            }
        }
    }
}
