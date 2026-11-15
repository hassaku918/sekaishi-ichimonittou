package com.hassaku.sekaishiqa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.hassaku.sekaishiqa.data.AppStore
import com.hassaku.sekaishiqa.ui.AppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = AppStore(this)
        setContent {
            MaterialTheme {
                Surface {
                    AppRoot(store)
                }
            }
        }
    }
}
