package ni.shikatu.pgupsd.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun paperBorder(): BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

@Composable
fun cardSurface(): Color = MaterialTheme.colorScheme.surface

@Composable
fun insetSurface(): Color = MaterialTheme.colorScheme.background

@Composable
fun paperSubtle(): Color = if (isSystemInDarkTheme()) Ink.subtleDark else Ink.subtle

@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = paperSubtle(),
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun paperTopBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.background,
    scrolledContainerColor = MaterialTheme.colorScheme.background,
    titleContentColor = MaterialTheme.colorScheme.onBackground,
    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
)
