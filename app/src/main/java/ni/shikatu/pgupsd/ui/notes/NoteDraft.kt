package ni.shikatu.pgupsd.ui.notes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop

/**
 * Text the user is editing. The saved copy is only a starting point: echoing it
 * back into the field (or waiting out a debounce after the screen is gone)
 * drops the tail of what was typed.
 */
@OptIn(FlowPreview::class)
@Composable
fun rememberNoteDraft(
    identity: Any?,
    saved: String,
    onSave: (String) -> Unit,
): NoteDraft {
    var text by remember(identity) { mutableStateOf(saved) }
    var dirty by remember(identity) { mutableStateOf(false) }
    val latest = remember(identity) { mutableStateOf(saved) }
    val lastSent = remember(identity) { mutableStateOf(saved) }
    val save = remember(identity) { mutableStateOf(onSave) }
    save.value = onSave

    LaunchedEffect(saved) {
        if (saved == text) {
            dirty = false
        } else if (!dirty) {
            text = saved
            latest.value = saved
            lastSent.value = saved
        }
    }

    fun publish(value: String) {
        if (value == lastSent.value) return
        lastSent.value = value
        save.value(value)
    }

    LaunchedEffect(identity) {
        snapshotFlow { latest.value }
            .drop(1)
            .debounce(400)
            .collect { publish(it) }
    }

    DisposableEffect(identity) {
        onDispose { publish(latest.value) }
    }

    return NoteDraft(
        text = text,
        onChange = { value ->
            dirty = true
            text = value
            latest.value = value
        },
    )
}

class NoteDraft(
    val text: String,
    val onChange: (String) -> Unit,
)
