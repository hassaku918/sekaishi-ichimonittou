package com.hassaku.sekaishiqa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.hassaku.sekaishiqa.data.AppStore
import com.hassaku.sekaishiqa.ui.AccentPurple
import com.hassaku.sekaishiqa.ui.AppRoot
import com.hassaku.sekaishiqa.ui.InkBackground
import com.hassaku.sekaishiqa.ui.InkOutline
import com.hassaku.sekaishiqa.ui.InkSurface
import com.hassaku.sekaishiqa.ui.InkSurfaceHigh
import com.hassaku.sekaishiqa.ui.InkTextSub

private val AppColors = darkColorScheme(
    primary = AccentPurple,
    onPrimary = Color.White,
    background = InkBackground,
    onBackground = Color(0xFFF2F2F5),
    surface = InkSurface,
    onSurface = Color(0xFFF2F2F5),
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
                Surface(color = InkBackground) {
                    AppRoot(store)
                }
            }
        }
    }
}
