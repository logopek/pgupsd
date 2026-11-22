package ni.shikatu.pgupsd.ui.plan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ni.shikatu.pgupsd.LocalAppContainer
import ni.shikatu.pgupsd.domain.PlanBlock
import ni.shikatu.pgupsd.domain.PlanCourse
import ni.shikatu.pgupsd.domain.PlanHours
import ni.shikatu.pgupsd.domain.PlanLink
import ni.shikatu.pgupsd.domain.PlanOffering
import ni.shikatu.pgupsd.domain.WebProgram
import ni.shikatu.pgupsd.ui.theme.Eyebrow
import ni.shikatu.pgupsd.ui.theme.cardSurface
import ni.shikatu.pgupsd.ui.theme.insetSurface
import ni.shikatu.pgupsd.ui.theme.barControl
import ni.shikatu.pgupsd.ui.theme.barLab
import ni.shikatu.pgupsd.ui.theme.barLecture
import ni.shikatu.pgupsd.ui.theme.barPractice
import ni.shikatu.pgupsd.ui.theme.barSelf
import ni.shikatu.pgupsd.ui.theme.barWork
import ni.shikatu.pgupsd.ui.theme.paperBorder
import ni.shikatu.pgupsd.ui.theme.paperSubtle
import ni.shikatu.pgupsd.ui.theme.toneDoneMark
import ni.shikatu.pgupsd.ui.theme.toneExam
import ni.shikatu.pgupsd.ui.theme.toneMuted
import ni.shikatu.pgupsd.ui.theme.tonePass
import ni.shikatu.pgupsd.ui.theme.toneTrack
import ni.shikatu.pgupsd.ui.theme.toneWork

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlanScreen(
    viewModel: PlanViewModel = viewModel(factory = LocalAppContainer.current.viewModelFactory),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    when (val screen = ui) {
        PlanUi.Loading -> PlanMessage("Открываю учебный план", loading = true)
        is PlanUi.Failed -> PlanMessage(screen.message, loading = false, onRetry = viewModel::retry)
        is PlanUi.Faculties -> FacultyList(screen, onSelect = viewModel::selectFaculty)
        is PlanUi.Programs -> ProgramList(
            screen,
            onSelect = viewModel::selectPlan,
            onFaculties = viewModel::showFaculties,
        )
        is PlanUi.Ready -> SemesterPlan(screen, viewModel)
    }
}

@Composable
private fun PlanMessage(text: String, loading: Boolean, onRetry: (() -> Unit)? = null) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (loading) {
                CircularWavyProgressIndicator()
            }
            Text(text, style = MaterialTheme.typography.bodyLarge, color = paperSubtle())
            if (onRetry != null) {
                TextButton(onClick = onRetry) { Text("Повторить") }
            }
        }
    }
}

@Composable
private fun FacultyList(screen: PlanUi.Faculties, onSelect: (ni.shikatu.pgupsd.domain.WebFaculty) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Eyebrow("ПГУПС")
            Text("Факультет", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Выбери факультет — открою направления и учебные планы с сайта.",
                style = MaterialTheme.typography.bodyMedium,
                color = paperSubtle(),
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
        }
        items(screen.items, key = { it.path }) { faculty ->
            Surface(
                onClick = { onSelect(faculty) },
                shape = MaterialTheme.shapes.large,
                color = cardSurface(),
                border = paperBorder(),
            ) {
                Text(
                    faculty.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun ProgramList(
    screen: PlanUi.Programs,
    onSelect: (PlanLink) -> Unit,
    onFaculties: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Eyebrow(screen.faculty, modifier = Modifier.weight(1f))
                TextButton(onClick = onFaculties) { Text("Факультеты") }
            }
            Text("Учебный план", style = MaterialTheme.typography.headlineMedium)
            Text(
                "PDF со страницы «Направления подготовки». Беру учебный план, не описание программы.",
                style = MaterialTheme.typography.bodyMedium,
                color = paperSubtle(),
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            )
        }
        items(screen.items, key = { it.code + it.profile }) { program ->
            ProgramCard(program, screen.suggested, onSelect)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgramCard(program: WebProgram, suggested: PlanLink?, onSelect: (PlanLink) -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = cardSurface(),
        border = paperBorder(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(program.code, style = MaterialTheme.typography.labelSmall, color = paperSubtle())
            Text(program.direction, style = MaterialTheme.typography.titleLarge)
            if (program.profile.isNotBlank()) {
                Text(program.profile, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (program.level.isNotBlank()) {
                Text(program.level, style = MaterialTheme.typography.bodySmall, color = paperSubtle())
            }
            if (program.plans.isEmpty()) {
                Text("На сайте нет файла учебного плана", style = MaterialTheme.typography.bodySmall, color = paperSubtle())
            } else {
                program.plans.groupBy { it.year }.toSortedMap(compareByDescending { it }).forEach { (year, links) ->
                    Text("$year", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        links.forEach { link ->
                            val selected = link == suggested
                            Surface(
                                onClick = { onSelect(link) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary else cardSurface(),
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                border = if (selected) null else paperBorder(),
                            ) {
                                Text(
                                    link.form,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SemesterPlan(screen: PlanUi.Ready, viewModel: PlanViewModel) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val plan = screen.curriculum
    val semester = screen.semester
    val courses = plan.courses
        .filter { it.offering(semester) != null }
        .filter { course ->
            val query = screen.query.trim()
            query.isEmpty() ||
                course.name.contains(query, true) ||
                course.id.contains(query, true) ||
                course.department.contains(query, true)
        }
    val counted = courses.filter { it.inPlan }
    val credits = counted.sumOf { it.offering(semester)?.credits ?: 0 }
    val exams = counted.count { semester in it.exams }
    val zachets = counted.count { semester in it.zachets }
    val hours = counted.mapNotNull { it.offering(semester)?.hours }.fold(PlanHours()) { acc, item -> acc + item }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Eyebrow(
                    text = listOf(plan.meta.code, "набор ${screen.link.year}", screen.link.form)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = viewModel::showPrograms) { Text("Другой") }
            }
            Text(
                if (plan.semesters.isEmpty()) "Учебный план" else "$semester семестр",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                plan.meta.profile.ifBlank { plan.meta.direction },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "$credits з.е. · $exams экз. · $zachets зач.",
                style = MaterialTheme.typography.bodyMedium,
                color = paperSubtle(),
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            )
            plan.semesters.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { value ->
                        val active = value == semester
                        Surface(
                            onClick = { viewModel.setSemester(value) },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = if (active) MaterialTheme.colorScheme.primary else cardSurface(),
                            contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            border = if (active) null else paperBorder(),
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("$value", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                shape = MaterialTheme.shapes.large,
                color = cardSurface(),
                border = paperBorder(),
            ) {
                Row(Modifier.padding(vertical = 12.dp)) {
                    Stat("Часы", hours.total.toString(), Modifier.weight(1f))
                    Stat("Контакт", hours.contact.toString(), Modifier.weight(1f))
                    Stat("Дисциплин", counted.size.toString(), Modifier.weight(1f))
                }
            }
            OutlinedTextField(
                value = screen.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                placeholder = { Text("Название, индекс или кафедра") },
            )
        }
        items(courses, key = { it.id }) { course ->
            CourseRow(
                course = course,
                semester = semester,
                done = "${screen.link.url}|${course.id}|$semester" in screen.done,
                onToggle = { viewModel.toggleDone(course.id, semester) },
                onOpen = { selectedId = course.id },
            )
        }
    }
    val selected = plan.courses.find { it.id == selectedId }
    if (selected != null) {
        CourseDetailSheet(
            course = selected,
            courses = plan.courses,
            done = screen.done,
            planUrl = screen.link.url,
            onDismiss = { selectedId = null },
            onOpen = { selectedId = it },
            onToggle = viewModel::toggleDone,
        )
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall, color = paperSubtle())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CourseRow(
    course: PlanCourse,
    semester: Int,
    done: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    val offering = course.offering(semester)
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.large,
        color = cardSurface(),
        border = paperBorder(),
        modifier = Modifier.fillMaxWidth().alpha(if (done || !course.inPlan) 0.72f else 1f),
    ) {
        Row(Modifier.padding(start = 6.dp, end = 14.dp, top = 10.dp, bottom = 10.dp)) {
            DoneMark(done = done, onToggle = onToggle)
            Column(
                Modifier
                    .weight(1f)
                    .clickable(onClickLabel = "Подробнее", onClick = onOpen),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(course.id, style = MaterialTheme.typography.labelSmall, color = paperSubtle())
                    val credits = offering?.credits ?: 0
                    Text(
                        if (credits > 0) "$credits з.е." else "вне объёма",
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Text(
                    course.name,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                )
                if (course.department.isNotBlank()) {
                    Text(
                        course.department,
                        style = MaterialTheme.typography.bodySmall,
                        color = paperSubtle(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (semester in course.exams) Badge("экзамен", exam = true)
                    if (semester in course.zachets) Badge("зачёт", pass = true)
                    if (semester in course.courseProjects) Badge("КП", work = true)
                    if (semester in course.courseWorks) Badge("КР", work = true)
                    if (!course.inPlan) Badge("альтернатива", muted = true)
                    if (course.block == PlanBlock.PRACTICE || course.block == PlanBlock.GIA || course.block == PlanBlock.EXTRA) {
                        Badge(blockLabel(course.block), muted = true)
                    }
                }
                offering?.let {
                    Text(hoursLine(it.hours), style = MaterialTheme.typography.bodySmall, color = paperSubtle())
                }
            }
        }
    }
}

@Composable
private fun DoneMark(
    done: Boolean,
    onToggle: () -> Unit,
    hit: Dp = 44.dp,
    mark: Dp = 20.dp,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onToggle,
        modifier = Modifier.size(hit),
        shape = CircleShape,
        color = Color.Transparent,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val (markColor, markIcon) = toneDoneMark()
            Surface(
                shape = CircleShape,
                color = if (done) markColor else Color.Transparent,
                border = BorderStroke(1.dp, if (done) markColor else scheme.outline),
                modifier = Modifier.size(mark),
            ) {
                if (done) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = "Сдано",
                        tint = markIcon,
                        modifier = Modifier.padding(2.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CourseDetailSheet(
    course: PlanCourse,
    courses: List<PlanCourse>,
    done: Set<String>,
    planUrl: String,
    onDismiss: () -> Unit,
    onOpen: (String) -> Unit,
    onToggle: (String, Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val hours = course.offerings.fold(PlanHours()) { acc, offering -> acc + offering.hours }
    val siblings = electiveSiblings(courses, course)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
        containerColor = scheme.surface,
        contentColor = scheme.onSurface,
        tonalElevation = 0.dp,
        contentWindowInsets = { BottomSheetDefaults.standardWindowInsets },
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(course.id, style = MaterialTheme.typography.labelSmall, color = paperSubtle())
                Text(course.name, style = MaterialTheme.typography.headlineSmall)
                if (course.department.isNotBlank()) {
                    Text(course.department, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                }
                FlowRow(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    InkBadge(blockTitle(course.block))
                    MutedBadge(if (course.credits > 0) "${course.credits} з.е." else "вне объёма")
                    if (hours.total > 0) MutedBadge("${hours.total} ч")
                    if (!course.inPlan) Badge("альтернатива", muted = true)
                }
            }
            if (hours.total > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Eyebrow("Часы")
                    HoursBar(hours)
                    Text(
                        "контактных ${hours.contact} · самостоятельно ${hours.selfStudy} · контроль ${hours.control}",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            if (course.offerings.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Eyebrow("По семестрам")
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = insetSurface(),
                        border = paperBorder(),
                    ) {
                        Column {
                            course.offerings.forEachIndexed { index, offering ->
                                if (index > 0) HorizontalDivider(color = scheme.outline)
                                SemesterLine(
                                    course = course,
                                    offering = offering,
                                    done = "$planUrl|${course.id}|${offering.semester}" in done,
                                    onToggle = { onToggle(course.id, offering.semester) },
                                )
                            }
                        }
                    }
                }
            }
            if (siblings.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Eyebrow("Выбор дисциплины")
                    siblings.forEach { option ->
                        val opened = option.id == course.id
                        Surface(
                            onClick = { onOpen(option.id) },
                            shape = MaterialTheme.shapes.medium,
                            color = if (opened) scheme.primary else insetSurface(),
                            contentColor = if (opened) scheme.onPrimary else scheme.onSurface,
                            border = if (opened) null else paperBorder(),
                        ) {
                            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                                Text(option.id, style = MaterialTheme.typography.labelSmall, color = LocalContentMuted(opened))
                                Text(option.name, style = MaterialTheme.typography.titleMedium)
                                if (option.inPlan) {
                                    Text(
                                        "в плане",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LocalContentMuted(opened),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SemesterLine(
    course: PlanCourse,
    offering: PlanOffering,
    done: Boolean,
    onToggle: () -> Unit,
) {
    val year = (offering.semester + 1) / 2
    Row(Modifier.padding(start = 6.dp, end = 14.dp, top = 8.dp, bottom = 10.dp)) {
        DoneMark(done = done, onToggle = onToggle, hit = 40.dp, mark = 22.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${offering.semester} сем. · $year курс",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    "${offering.credits} з.е. · ${offering.hours.total} ч",
                    style = MaterialTheme.typography.bodySmall,
                    color = paperSubtle(),
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (offering.semester in course.exams) Badge("экзамен", exam = true)
                if (offering.semester in course.zachets) Badge("зачёт", pass = true)
                if (offering.semester in course.courseProjects) Badge("КП", work = true)
                if (offering.semester in course.courseWorks) Badge("КР", work = true)
            }
            HoursBar(offering.hours)
        }
    }
}

@Composable
private fun LocalContentMuted(onPrimary: Boolean): Color =
    if (onPrimary) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f) else paperSubtle()

@Composable
private fun InkBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
private fun MutedBadge(text: String) {
    val (bg, fg) = toneMuted()
    Surface(
        shape = RoundedCornerShape(50),
        color = bg,
        contentColor = fg,
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HoursBar(hours: PlanHours) {
    val slices = hourSlices(hours)
    if (slices.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(toneTrack()),
        ) {
            slices.forEach { slice ->
                Box(Modifier.weight(slice.value.toFloat()).fillMaxHeight().background(slice.color))
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            slices.forEach { slice ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(slice.color))
                    Text(
                        "${slice.label} ${slice.value}",
                        style = MaterialTheme.typography.bodySmall,
                        color = paperSubtle(),
                    )
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String, exam: Boolean = false, pass: Boolean = false, work: Boolean = false, muted: Boolean = false) {
    val (bg, fg) = when {
        exam -> toneExam()
        pass -> tonePass()
        work -> toneWork()
        else -> toneMuted()
    }
    Surface(shape = RoundedCornerShape(50), color = bg, contentColor = fg) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

private fun blockLabel(block: PlanBlock): String = when (block) {
    PlanBlock.PRACTICE -> "практика"
    PlanBlock.GIA -> "ГИА"
    PlanBlock.EXTRA -> "факультатив"
    else -> ""
}

private fun blockTitle(block: PlanBlock): String = when (block) {
    PlanBlock.CORE -> "Обязательная"
    PlanBlock.VARIABLE -> "Вариативная"
    PlanBlock.ELECTIVE -> "По выбору"
    PlanBlock.PRACTICE -> "Практика"
    PlanBlock.GIA -> "ГИА"
    PlanBlock.EXTRA -> "Факультатив"
    PlanBlock.OTHER -> "Дисциплина"
}

private val electiveGroupId = Regex("""^(.*\.ДВ\.\d+)\.\d+$""")

private fun electiveSiblings(courses: List<PlanCourse>, course: PlanCourse): List<PlanCourse> {
    val group = electiveGroupId.find(course.id)?.groupValues?.get(1) ?: return emptyList()
    return courses.filter { it.id.startsWith("$group.") }
}

private data class HourSlice(val value: Int, val label: String, val color: Color)

@Composable
private fun hourSlices(hours: PlanHours): List<HourSlice> = listOf(
    HourSlice(hours.lecture, "лек", barLecture()),
    HourSlice(hours.lab, "лаб", barLab()),
    HourSlice(hours.practice, "пр", barPractice()),
    HourSlice(hours.srp, "СРП", barWork()),
    HourSlice(hours.selfStudy, "СР", barSelf()),
    HourSlice(hours.control, "контр", barControl()),
).filter { it.value > 0 }

private fun hoursLine(hours: PlanHours): String = buildList {
    if (hours.lecture > 0) add("${hours.lecture}лек")
    if (hours.lab > 0) add("${hours.lab}лаб")
    if (hours.practice > 0) add("${hours.practice}пр")
    if (hours.srp > 0) add("${hours.srp}СРП")
    if (hours.selfStudy > 0) add("${hours.selfStudy}СР")
    if (hours.control > 0) add("${hours.control}контр")
}.joinToString(" · ")
