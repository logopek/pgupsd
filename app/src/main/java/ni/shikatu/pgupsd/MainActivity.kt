package ni.shikatu.pgupsd

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.util.Consumer
import ni.shikatu.pgupsd.ui.AppNav
import ni.shikatu.pgupsd.ui.theme.PGUPSdTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = appContainer()
        setContent {
            var pendingLesson by remember { mutableStateOf(DeepLinks.keyFrom(intent)) }
            DisposableEffect(this) {
                val listener = Consumer<Intent> { incoming ->
                    setIntent(incoming)
                    pendingLesson = DeepLinks.keyFrom(incoming)
                }
                addOnNewIntentListener(listener)
                onDispose { removeOnNewIntentListener(listener) }
            }
            CompositionLocalProvider(LocalAppContainer provides container) {
                PGUPSdTheme {
                    AppNav(
                        factory = container.viewModelFactory,
                        pendingLessonKey = pendingLesson,
                        onPendingConsumed = { pendingLesson = null },
                    )
                }
            }
        }
    }
}
