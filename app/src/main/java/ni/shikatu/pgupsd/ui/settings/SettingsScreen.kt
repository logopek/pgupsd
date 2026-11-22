package ni.shikatu.pgupsd.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ni.shikatu.pgupsd.ui.DiaryUiState
import ni.shikatu.pgupsd.ui.theme.Eyebrow
import ni.shikatu.pgupsd.ui.theme.cardSurface
import ni.shikatu.pgupsd.ui.theme.paperBorder
import ni.shikatu.pgupsd.ui.theme.paperTopBarColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: DiaryUiState,
    groups: List<String>,
    onBack: () -> Unit,
    onChangeGroup: (String) -> Unit,
    onRefresh: () -> Unit,
    onReset: () -> Unit,
) {
    val profile = state.profile
    val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault())
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        containerColor = scheme.background,
        topBar = {
            TopAppBar(
                title = {},
                colors = paperTopBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Настройки", style = MaterialTheme.typography.headlineMedium)
            }
            item {
                PaperBlock {
                    Eyebrow("Группа")
                    Text(
                        text = profile?.groupName.orEmpty(),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "${profile?.facultyShortName.orEmpty()} · ${profile?.course}-й курс",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    if (!profile?.facultyFullName.isNullOrBlank()) {
                        Text(
                            text = profile?.facultyFullName.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    if (groups.isNotEmpty()) {
                        Text(
                            text = "Сменить группу в этом файле",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            groups.forEach { group ->
                                val selected = group == profile?.groupName
                                Surface(
                                    onClick = { onChangeGroup(group) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (selected) scheme.primary else cardSurface(),
                                    contentColor = if (selected) scheme.onPrimary else scheme.onSurface,
                                    border = if (selected) null else paperBorder(),
                                ) {
                                    Text(
                                        text = group,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                PaperBlock {
                    Eyebrow("Расписание")
                    val success = state.refresh?.lastSuccessMillis
                    Text(
                        text = if (success != null) "Актуально на ${fmt.format(Instant.ofEpochMilli(success))}"
                        else "Ещё не обновлялось",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (!state.refresh?.error.isNullOrBlank()) {
                        Text(
                            text = "Последняя ошибка: ${state.refresh?.error}",
                            color = scheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Показываю последнюю сохранённую копию.",
                            color = scheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (!state.refresh?.title.isNullOrBlank()) {
                        Text(
                            text = state.refresh?.title.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = onRefresh,
                        enabled = !state.refreshing,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(if (state.refreshing) "Обновляю…" else "Обновить сейчас")
                    }
                }
            }
            item {
                PaperBlock {
                    Eyebrow("Другой факультет")
                    Text(
                        text = "Заметки к парам останутся в базе, но на экране будет новая группа.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = onReset,
                        colors = ButtonDefaults.textButtonColors(contentColor = scheme.error),
                    ) {
                        Text("Выбрать заново")
                    }
                }
            }
        }
    }
}

@Composable
private fun PaperBlock(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = cardSurface(),
        border = paperBorder(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}
