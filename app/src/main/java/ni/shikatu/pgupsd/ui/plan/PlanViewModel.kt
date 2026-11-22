package ni.shikatu.pgupsd.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ni.shikatu.pgupsd.data.plan.PlanRepository
import ni.shikatu.pgupsd.data.plan.suggestPlan
import ni.shikatu.pgupsd.domain.Curriculum
import ni.shikatu.pgupsd.domain.PlanLink
import ni.shikatu.pgupsd.domain.UserProfile
import ni.shikatu.pgupsd.domain.WebFaculty
import ni.shikatu.pgupsd.domain.WebProgram
import java.time.LocalDate
import kotlin.math.abs

sealed interface PlanUi {
    data object Loading : PlanUi
    data class Failed(val message: String) : PlanUi
    data class Faculties(val items: List<WebFaculty>) : PlanUi
    data class Programs(
        val faculty: String,
        val items: List<WebProgram>,
        val suggested: PlanLink?,
    ) : PlanUi
    data class Ready(
        val curriculum: Curriculum,
        val link: PlanLink,
        val semester: Int,
        val done: Set<String>,
        val query: String,
    ) : PlanUi
}

class PlanViewModel(
    private val profiles: Flow<UserProfile?>,
    private val plans: PlanRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow<PlanUi>(PlanUi.Loading)
    val ui = _ui.asStateFlow()

    private var programs = emptyList<WebProgram>()
    private var facultyName = ""
    private var suggested: PlanLink? = null
    private var semester = 1
    private var done = plans.loadChecks()
    private var query = ""
    private var lastFaculty = ""
    private var lastCourse: Int? = null
    private var lastGroup = ""
    private var facultyList = emptyList<WebFaculty>()

    init {
        viewModelScope.launch {
            profiles
                .map { profile ->
                    Triple(profile?.facultyFullName.orEmpty(), profile?.course, profile?.groupName.orEmpty())
                }
                .distinctUntilChanged()
                .collect { (faculty, course, group) ->
                    lastFaculty = faculty
                    lastCourse = course
                    lastGroup = group
                    loadFor(faculty, course, group)
                }
        }
    }

    fun retry() {
        val ready = _ui.value as? PlanUi.Ready
        viewModelScope.launch {
            if (ready != null) open(ready.link, force = true) else loadFor(lastFaculty, lastCourse, lastGroup)
        }
    }

    fun selectFaculty(faculty: WebFaculty) {
        viewModelScope.launch { openFaculty(faculty, lastGroup, lastCourse, auto = false) }
    }

    fun selectPlan(link: PlanLink) {
        viewModelScope.launch { open(link, force = false) }
    }

    fun showPrograms() {
        if (programs.isEmpty()) return
        _ui.value = PlanUi.Programs(facultyName, programs, suggested)
    }

    fun showFaculties() {
        if (facultyList.isNotEmpty()) {
            _ui.value = PlanUi.Faculties(facultyList)
            return
        }
        viewModelScope.launch {
            _ui.value = PlanUi.Loading
            try {
                facultyList = withContext(Dispatchers.IO) { plans.faculties() }
                _ui.value = if (facultyList.isEmpty()) {
                    PlanUi.Failed("На странице факультетов ничего не нашлось")
                } else {
                    PlanUi.Faculties(facultyList)
                }
            } catch (error: Exception) {
                _ui.value = PlanUi.Failed(failureText(error))
            }
        }
    }

    fun setSemester(value: Int) {
        semester = value
        publishReady()
    }

    fun setQuery(value: String) {
        query = value
        publishReady()
    }

    fun toggleDone(courseId: String, semester: Int) {
        val link = (_ui.value as? PlanUi.Ready)?.link ?: return
        val key = doneKey(link, courseId, semester)
        done = if (key in done) done - key else done + key
        plans.saveChecks(done)
        publishReady()
    }

    private suspend fun loadFor(faculty: String, course: Int?, group: String) {
        _ui.value = PlanUi.Loading
        try {
            val faculties = withContext(Dispatchers.IO) { plans.faculties() }
            facultyList = faculties
            if (faculties.isEmpty()) {
                _ui.value = PlanUi.Failed("На странице факультетов ничего не нашлось")
                return
            }
            val match = matchFaculty(faculties, faculty)
            if (match == null) {
                _ui.value = PlanUi.Faculties(faculties)
                return
            }
            openFaculty(match, group, course, auto = true)
        } catch (error: Exception) {
            _ui.value = PlanUi.Failed(failureText(error))
        }
    }

    private suspend fun openFaculty(faculty: WebFaculty, group: String, course: Int?, auto: Boolean) {
        _ui.value = PlanUi.Loading
        try {
            val loaded = withContext(Dispatchers.IO) { plans.programs(faculty) }
            programs = loaded
            facultyName = faculty.name
            suggested = if (course != null) {
                suggestPlan(loaded, group, course, LocalDate.now())
            } else {
                null
            }
            val pick = suggested
            if (auto && pick != null) {
                open(pick, force = false)
            } else {
                _ui.value = PlanUi.Programs(faculty.name, loaded, suggested)
            }
        } catch (error: Exception) {
            _ui.value = PlanUi.Failed(failureText(error))
        }
    }

    private suspend fun open(link: PlanLink, force: Boolean) {
        _ui.value = PlanUi.Loading
        try {
            val curriculum = withContext(Dispatchers.IO) { plans.curriculum(link, force) }
            semester = preferredSemester(curriculum.semesters, lastCourse)
            query = ""
            _ui.value = PlanUi.Ready(curriculum, link, semester, done, query)
        } catch (error: Exception) {
            _ui.value = PlanUi.Failed(failureText(error))
        }
    }

    private fun preferredSemester(semesters: List<Int>, course: Int?): Int {
        if (semesters.isEmpty()) return 1
        if (course == null || course <= 0) return semesters.first()
        val wanted = if (LocalDate.now().monthValue >= 9) course * 2 - 1 else course * 2
        return semesters.minBy { abs(it - wanted) }
    }

    private fun publishReady() {
        val ready = _ui.value as? PlanUi.Ready ?: return
        _ui.value = ready.copy(semester = semester, done = done, query = query)
    }
}

private fun matchFaculty(faculties: List<WebFaculty>, fullName: String): WebFaculty? {
    val needle = normalize(fullName)
    if (needle.isBlank()) return null
    return faculties.find { normalize(it.name) == needle }
        ?: faculties.find { name ->
            val value = normalize(name.name)
            needle.contains(value) || value.contains(needle)
        }
}

private fun doneKey(link: PlanLink, courseId: String, semester: Int) = "${link.url}|$courseId|$semester"

private fun failureText(error: Exception): String {
    val message = error.message.orEmpty()
    return when {
        message.contains("Unable to resolve", ignoreCase = true) ||
            message.contains("failed to connect", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) -> "Нет связи с pgups.ru"
        message.isNotBlank() -> message
        else -> "Не удалось загрузить план"
    }
}

private fun normalize(value: String): String =
    value.lowercase().replace('ё', 'е').replace(Regex("\\s+"), " ").trim()
