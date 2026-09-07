package ni.shikatu.pgupsd.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class StackPosition { Single, First, Middle, Last }

fun stackPosition(index: Int, size: Int): StackPosition = when {
    size <= 1 -> StackPosition.Single
    index == 0 -> StackPosition.First
    index == size - 1 -> StackPosition.Last
    else -> StackPosition.Middle
}

fun stackShape(
    position: StackPosition,
    large: Dp = 28.dp,
    small: Dp = 12.dp,
    middle: Dp = 16.dp,
): RoundedCornerShape = when (position) {
    StackPosition.Single -> RoundedCornerShape(large)
    StackPosition.First -> RoundedCornerShape(
        topStart = large,
        topEnd = large,
        bottomStart = small,
        bottomEnd = small,
    )
    StackPosition.Middle -> RoundedCornerShape(middle)
    StackPosition.Last -> RoundedCornerShape(
        topStart = small,
        topEnd = small,
        bottomStart = large,
        bottomEnd = large,
    )
}
