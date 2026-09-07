package ni.shikatu.pgupsd.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ni.shikatu.pgupsd.ui.DiaryUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
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
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text("Группа", style = MaterialTheme.typography.titleLarge)
                Text(
                    "${profile?.facultyShortName.orEmpty()} · ${profile?.course}-й курс · ${profile?.groupName.orEmpty()}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    profile?.facultyFullName.orEmpty(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (groups.isNotEmpty()) {
                item { Text("Сменить группу в этом файле", style = MaterialTheme.typography.titleMedium) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        groups.chunked(3).forEach { row ->
                            androidx.compose.foundation.layout.Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                row.forEach { group ->
                                    FilterChip(
                                        selected = group == profile?.groupName,
                                        onClick = { onChangeGroup(group) },
                                        label = { Text(group) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                Text("Расписание", style = MaterialTheme.typography.titleLarge)
                val success = state.refresh?.lastSuccessMillis
                Text(
                    if (success != null) "Актуально на ${fmt.format(Instant.ofEpochMilli(success))}"
                    else "Ещё не обновлялось",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!state.refresh?.error.isNullOrBlank()) {
                    Text(
                        "Последняя ошибка: ${state.refresh?.error}",
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        "Показываю последнюю сохранённую копию.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!state.refresh?.title.isNullOrBlank()) {
                    Text(
                        state.refresh?.title.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Button(
                    onClick = onRefresh,
                    enabled = !state.refreshing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Text(if (state.refreshing) "Обновляю…" else "Обновить сейчас")
                }
            }
            item {
                Text("Другой факультет или курс", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Заметки к парам останутся в базе, но на экране будет новая группа.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onReset) { Text("Выбрать заново") }
            }
        }
    }
}
