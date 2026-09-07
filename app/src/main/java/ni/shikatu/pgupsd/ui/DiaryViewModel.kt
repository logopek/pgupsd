package ni.shikatu.pgupsd.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ni.shikatu.pgupsd.data.repo.ScheduleRepository
import ni.shikatu.pgupsd.domain.GlobalNote
import ni.shikatu.pgupsd.domain.Lesson
import ni.shikatu.pgupsd.domain.LocalNote
import ni.shikatu.pgupsd.domain.Occurrence
import ni.shikatu.pgupsd.domain.RefreshMeta
import ni.shikatu.pgupsd.domain.StaffProfile
import ni.shikatu.pgupsd.domain.TeacherCard
import ni.shikatu.pgupsd.domain.UserProfile
import java.time.LocalDate

data class DiaryUiState(
    val loading: Boolean = true,
    val onboarded: Boolean = false,
    val profile: UserProfile? = null,
    val lessons: List<Lesson> = emptyList(),
    val localNotes: List<LocalNote> = emptyList(),
    val globalNotes: List<GlobalNote> = emptyList(),
    val staff: List<StaffProfile> = emptyList(),
    val refresh: RefreshMeta? = null,
    val refreshing: Boolean = false,
)

class DiaryViewModel(
    private val repo: ScheduleRepository,
) : ViewModel() {
    val state = combine(
        combine(
            repo.profile,
            repo.observeLessons(),
            repo.observeLocalNotes(),
            repo.observeGlobalNotes(),
            repo.refreshMeta,
        ) { profile, lessons, local, global, refresh ->
            DiaryUiState(
                loading = false,
                onboarded = profile != null,
                profile = profile,
                lessons = lessons,
                localNotes = local,
                globalNotes = global,
                refresh = refresh,
            )
        },
        repo.observeStaff(),
        repo.refreshing,
    ) { ui, staff, refreshing -> ui.copy(staff = staff, refreshing = refreshing) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiaryUiState())

    init {
        viewModelScope.launch {
            if (repo.profile.first() != null) {
                repo.refresh(force = false)
            }
        }
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch { repo.refresh(force) }
    }

    fun occurrences(date: LocalDate, state: DiaryUiState): List<Occurrence> =
        repo.occurrencesFor(date, state.lessons, state.localNotes, state.globalNotes)

    fun teachers(state: DiaryUiState): List<TeacherCard> = repo.teacherCards(state.lessons, state.staff)

    fun loadStaffDetails(slug: String) {
        viewModelScope.launch { repo.loadStaffDetails(slug) }
    }

    fun saveLocal(occurrence: Occurrence, text: String) {
        viewModelScope.launch { repo.saveLocalNote(occurrence, text) }
    }

    fun saveLocalDeadline(occurrence: Occurrence, enabled: Boolean) {
        viewModelScope.launch { repo.saveLocalDeadline(occurrence, enabled) }
    }

    fun deadlines(state: DiaryUiState): List<Occurrence> =
        state.localNotes
            .filter { it.deadline }
            .mapNotNull { note ->
                occurrences(note.date, state).find { occ ->
                    occ.lesson.subjectKey == note.subjectKey &&
                        occ.lesson.start.hour * 60 + occ.lesson.start.minute == note.startMinutes &&
                        occ.lesson.subgroup == note.subgroup
                }
            }
            .sortedWith(compareBy({ it.date }, { it.lesson.start }, { it.lesson.subject }))

    fun saveGlobal(
        subjectKey: String,
        teacherKey: String,
        subjectLabel: String,
        teacherLabel: String,
        text: String,
    ) {
        viewModelScope.launch {
            repo.saveGlobalNote(subjectKey, teacherKey, subjectLabel, teacherLabel, text)
        }
    }

    fun changeGroup(group: String) {
        viewModelScope.launch { repo.changeGroup(group) }
    }

    fun reset() {
        viewModelScope.launch { repo.resetOnboarding() }
    }

    suspend fun storedGroups(): List<String> = repo.storedGroups()
}
