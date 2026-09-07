package ni.shikatu.pgupsd.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ni.shikatu.pgupsd.data.repo.ScheduleRepository
import ni.shikatu.pgupsd.data.xlsx.ParsedWorkbook
import ni.shikatu.pgupsd.domain.Faculty
import ni.shikatu.pgupsd.domain.ScheduleCatalog
import ni.shikatu.pgupsd.domain.UserProfile

data class OnboardingUi(
    val step: Int = 0,
    val catalog: ScheduleCatalog? = null,
    val faculty: Faculty? = null,
    val course: Int? = null,
    val workbook: ParsedWorkbook? = null,
    val query: String = "",
    val loading: Boolean = true,
    val downloading: Boolean = false,
    val error: String? = null,
)

class OnboardingViewModel(
    private val repo: ScheduleRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(OnboardingUi())
    val ui = _ui.asStateFlow()

    init {
        reloadCatalog()
    }

    fun reloadCatalog() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            runCatching { repo.loadCatalog() }
                .onSuccess { catalog ->
                    _ui.update { it.copy(catalog = catalog, loading = false) }
                }
                .onFailure { error ->
                    _ui.update { it.copy(loading = false, error = error.message) }
                }
        }
    }

    fun selectFaculty(faculty: Faculty) {
        _ui.update { it.copy(faculty = faculty, course = null, workbook = null, step = 1, error = null) }
    }

    fun selectCourse(course: Int) {
        val faculty = _ui.value.faculty ?: return
        viewModelScope.launch {
            _ui.update { it.copy(course = course, downloading = true, error = null, step = 2) }
            runCatching { repo.previewGroups(faculty, course) }
                .onSuccess { workbook ->
                    _ui.update { it.copy(workbook = workbook, downloading = false, step = 3) }
                }
                .onFailure { error ->
                    _ui.update {
                        it.copy(
                            downloading = false,
                            error = error.message ?: "Не удалось скачать расписание",
                            step = 1,
                        )
                    }
                }
        }
    }

    fun setQuery(query: String) {
        _ui.update { it.copy(query = query) }
    }

    fun back() {
        _ui.update { current ->
            when (current.step) {
                3 -> current.copy(step = 1, workbook = null, query = "")
                1 -> current.copy(step = 0, faculty = null, course = null)
                else -> current
            }
        }
    }

    fun finish(group: String, onDone: () -> Unit) {
        val ui = _ui.value
        val faculty = ui.faculty ?: return
        val course = ui.course ?: return
        viewModelScope.launch {
            repo.completeOnboarding(
                UserProfile(
                    facultyCode = faculty.code,
                    facultyShortName = faculty.shortName,
                    facultyFullName = faculty.fullName,
                    course = course,
                    groupName = group,
                    yearFolder = faculty.yearFolder,
                    xlsxUrl = faculty.fileUrl(course),
                ),
            )
            onDone()
        }
    }
}
