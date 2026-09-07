package ni.shikatu.pgupsd.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ni.shikatu.pgupsd.domain.Faculty

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (ui.step) {
                            0 -> "Факультет"
                            1 -> "Курс"
                            2 -> "Расписание"
                            else -> "Группа"
                        },
                    )
                },
                navigationIcon = {
                    if (ui.step > 0 && ui.step != 2) {
                        IconButton(onClick = viewModel::back) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            ui.loading -> CenterLoading(padding, "Загружаю список факультетов")
            ui.downloading -> CenterLoading(padding, "Скачиваю и разбираю xlsx")
            ui.step == 0 -> FacultyStep(
                padding = padding,
                faculties = ui.catalog?.faculties.orEmpty(),
                error = ui.error,
                onRetry = viewModel::reloadCatalog,
                onSelect = viewModel::selectFaculty,
            )
            ui.step == 1 -> CourseStep(
                padding = padding,
                faculty = ui.faculty,
                error = ui.error,
                onSelect = viewModel::selectCourse,
            )
            else -> GroupStep(
                padding = padding,
                ui = ui,
                onQuery = viewModel::setQuery,
                onSelect = { viewModel.finish(it) {} },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CenterLoading(padding: PaddingValues, label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularWavyProgressIndicator()
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun FacultyStep(
    padding: PaddingValues,
    faculties: List<Faculty>,
    error: String?,
    onRetry: () -> Unit,
    onSelect: (Faculty) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Дневник ПГУПС", style = MaterialTheme.typography.displaySmall)
            Text(
                "Выбери факультет — скачаю официальное расписание с rasp.pgups.ru",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            )
        }
        if (error != null) {
            item {
                Text(error, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) { Text("Повторить") }
            }
        }
        items(faculties, key = { it.code }) { faculty ->
            ElevatedCard(
                onClick = { onSelect(faculty) },
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.School,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(faculty.shortName, style = MaterialTheme.typography.titleLarge)
                        Text(
                            faculty.fullName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseStep(
    padding: PaddingValues,
    faculty: Faculty?,
    error: String?,
    onSelect: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(faculty?.fullName.orEmpty(), style = MaterialTheme.typography.headlineSmall)
        Text("Какой у тебя курс?", style = MaterialTheme.typography.bodyLarge)
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            faculty?.courses.orEmpty().forEach { course ->
                FilterChip(
                    selected = false,
                    onClick = { onSelect(course) },
                    label = { Text("$course") },
                )
            }
        }
        Text(
            "После выбора скачаю xlsx этого курса и покажу список групп.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GroupStep(
    padding: PaddingValues,
    ui: OnboardingUi,
    onQuery: (String) -> Unit,
    onSelect: (String) -> Unit,
) {
    val groups = ui.workbook?.groups.orEmpty()
        .filter { it.contains(ui.query.trim(), ignoreCase = true) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Найдено групп: ${ui.workbook?.groups.orEmpty().size}", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = ui.query,
                onValueChange = onQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text("Поиск группы") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
            )
        }
        items(groups, key = { it }) { group ->
            FilledTonalButton(
                onClick = { onSelect(group) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(group, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}
