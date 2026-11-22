package ni.shikatu.pgupsd.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ni.shikatu.pgupsd.domain.Faculty
import ni.shikatu.pgupsd.ui.theme.Eyebrow
import ni.shikatu.pgupsd.ui.theme.cardSurface
import ni.shikatu.pgupsd.ui.theme.paperBorder
import ni.shikatu.pgupsd.ui.theme.paperSubtle
import ni.shikatu.pgupsd.ui.theme.paperTopBarColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                colors = paperTopBarColors(),
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
            Text(label, style = MaterialTheme.typography.bodyLarge, color = paperSubtle())
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Eyebrow("ПГУПС")
            Text("Дневник", style = MaterialTheme.typography.displaySmall)
            Text(
                "Выбери факультет — скачаю официальное расписание с rasp.pgups.ru",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
            )
        }
        if (error != null) {
            item {
                Text(error, color = MaterialTheme.colorScheme.error)
                Button(
                    onClick = onRetry,
                    modifier = Modifier.padding(top = 8.dp),
                    shape = MaterialTheme.shapes.medium,
                ) { Text("Повторить") }
            }
        }
        items(faculties, key = { it.code }) { faculty ->
            Surface(
                onClick = { onSelect(faculty) },
                shape = MaterialTheme.shapes.large,
                color = cardSurface(),
                border = paperBorder(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(faculty.shortName, style = MaterialTheme.typography.titleLarge)
                    Text(
                        faculty.fullName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
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
    val courses = faculty?.courses.orEmpty()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Eyebrow(faculty?.shortName ?: "Факультет")
        Text("Какой курс?", style = MaterialTheme.typography.headlineMedium)
        if (!faculty?.fullName.isNullOrBlank()) {
            Text(
                faculty?.fullName.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        courses.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { course ->
                    Surface(
                        onClick = { onSelect(course) },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = cardSurface(),
                        border = paperBorder(),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("$course", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
                repeat(4 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        Text(
            "После выбора скачаю xlsx этого курса и покажу список групп.",
            style = MaterialTheme.typography.bodyMedium,
            color = paperSubtle(),
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Eyebrow(ui.faculty?.shortName?.let { "$it · ${ui.course}-й курс" } ?: "Группа")
            Text("Группа", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Найдено ${ui.workbook?.groups.orEmpty().size}",
                style = MaterialTheme.typography.bodyMedium,
                color = paperSubtle(),
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            )
            OutlinedTextField(
                value = ui.query,
                onValueChange = onQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text("Поиск группы") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )
        }
        items(groups, key = { it }) { group ->
            Surface(
                onClick = { onSelect(group) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = cardSurface(),
                border = paperBorder(),
            ) {
                Text(
                    group,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }
        }
    }
}
