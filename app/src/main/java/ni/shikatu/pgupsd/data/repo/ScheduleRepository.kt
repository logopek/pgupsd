@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package ni.shikatu.pgupsd.data.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import ni.shikatu.pgupsd.data.local.AppDatabase
import ni.shikatu.pgupsd.data.local.GlobalNoteEntity
import ni.shikatu.pgupsd.data.local.LessonEntity
import ni.shikatu.pgupsd.data.local.LocalNoteEntity
import ni.shikatu.pgupsd.data.local.ScheduleFiles
import ni.shikatu.pgupsd.data.local.StaffEntity
import ni.shikatu.pgupsd.data.local.UserPrefs
import ni.shikatu.pgupsd.data.remote.EmployeeDirectoryParser
import ni.shikatu.pgupsd.data.remote.PgupsClient
import ni.shikatu.pgupsd.data.remote.ScheduleCatalogParser
import ni.shikatu.pgupsd.data.remote.XlsxDownload
import ni.shikatu.pgupsd.data.xlsx.ParsedWorkbook
import ni.shikatu.pgupsd.data.xlsx.ScheduleXlsxParser
import ni.shikatu.pgupsd.domain.Faculty
import ni.shikatu.pgupsd.domain.GlobalNote
import ni.shikatu.pgupsd.domain.Lesson
import ni.shikatu.pgupsd.domain.LocalNote
import ni.shikatu.pgupsd.domain.Occurrence
import ni.shikatu.pgupsd.domain.RefreshMeta
import ni.shikatu.pgupsd.domain.ScheduleCatalog
import ni.shikatu.pgupsd.domain.StaffProfile
import ni.shikatu.pgupsd.domain.Teacher
import ni.shikatu.pgupsd.domain.TeacherCard
import ni.shikatu.pgupsd.domain.UserProfile
import ni.shikatu.pgupsd.domain.personMatchKey
import java.time.LocalDate

class ScheduleRepository(
    private val db: AppDatabase,
    private val prefs: UserPrefs,
    private val files: ScheduleFiles,
    private val client: PgupsClient,
    private val onWidgetUpdate: suspend () -> Unit = {},
) {
    val profile: Flow<UserProfile?> = prefs.profile
    val refreshMeta: Flow<RefreshMeta> = prefs.refreshMeta
    val refreshing = MutableStateFlow(false)
    private val refreshMutex = Mutex()
    private val localNoteMutex = Mutex()
    private val globalNoteMutex = Mutex()

    fun observeLessons(): Flow<List<Lesson>> = profile.flatMapLatest { user ->
        if (user == null) flowOf(emptyList())
        else db.lessons().observeByGroup(user.groupName).map { list -> list.map { it.toDomain() } }
    }

    fun observeLocalNotes(): Flow<List<LocalNote>> = profile.flatMapLatest { user ->
        if (user == null) flowOf(emptyList())
        else db.notes().observeLocal(user.groupName).map { list -> list.map { it.toDomain() } }
    }

    fun observeGlobalNotes(): Flow<List<GlobalNote>> =
        db.notes().observeGlobal().map { list -> list.map { it.toDomain() } }

    fun observeStaff(): Flow<List<StaffProfile>> =
        db.staff().observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun storedGroups(): List<String> = db.lessons().groups()

    suspend fun loadCatalog(): ScheduleCatalog = withContext(Dispatchers.IO) {
        runCatching { client.fetchCatalog() }.getOrElse { ScheduleCatalogParser.fallback() }
    }

    suspend fun previewGroups(faculty: Faculty, course: Int): ParsedWorkbook = withContext(Dispatchers.IO) {
        val url = faculty.fileUrl(course)
        when (val download = client.downloadXlsx(url, etag = null, lastModified = null)) {
            is XlsxDownload.NotModified -> error("Неожиданный 304 при первой загрузке")
            is XlsxDownload.Updated -> {
                val incoming = files.writeIncoming(download.bytes)
                val parsed = incoming.inputStream().use { ScheduleXlsxParser.parse(it) }
                if (parsed.groups.isEmpty()) error("В файле нет групп")
                files.promoteIncoming()
                db.lessons().replaceAll(parsed.lessons.map(LessonEntity::from))
                prefs.saveRefresh(
                    success = true,
                    etag = download.etag,
                    lastModified = download.lastModified,
                    fileName = url.substringAfterLast('/'),
                    title = parsed.title,
                    error = null,
                )
                parsed
            }
        }
    }

    suspend fun completeOnboarding(profile: UserProfile) {
        prefs.saveProfile(profile)
        onWidgetUpdate()
    }

    suspend fun changeGroup(groupName: String) {
        val current = prefs.profile.first() ?: return
        prefs.saveProfile(current.copy(groupName = groupName))
        onWidgetUpdate()
    }

    suspend fun resetOnboarding() {
        prefs.clearProfile()
    }

    suspend fun refresh(force: Boolean = false): Result<Unit> {
        if (!refreshMutex.tryLock()) return Result.success(Unit)
        refreshing.value = true
        return try {
            withContext(Dispatchers.IO) { performRefresh(force) }
        } finally {
            refreshing.value = false
            refreshMutex.unlock()
            runCatching { onWidgetUpdate() }
            runCatching { withContext(Dispatchers.IO) { refreshStaff(force) } }
        }
    }

    private suspend fun performRefresh(force: Boolean): Result<Unit> {
        val user = prefs.profile.first()
            ?: return Result.failure(IllegalStateException("Группа не выбрана"))
        reparseLocalIfStale(user.groupName)
        val meta = prefs.refreshMeta.first()
        if (!force) {
            val last = meta.lastSuccessMillis
            if (last != null && System.currentTimeMillis() - last < 15_000) {
                return Result.success(Unit)
            }
        }
        return runCatching {
            val download = client.downloadXlsx(
                url = user.xlsxUrl,
                etag = if (force) null else meta.etag,
                lastModified = if (force) null else meta.lastModifiedHeader,
                forceNetwork = force,
            )
            when (download) {
                is XlsxDownload.NotModified -> {
                    reparseLocalIfStale(user.groupName)
                    prefs.saveRefresh(
                        success = true,
                        etag = download.etag,
                        lastModified = download.lastModified,
                        fileName = meta.sourceFileName,
                        title = meta.title,
                        error = null,
                    )
                }
                is XlsxDownload.Updated -> {
                    val sameEtag = !download.etag.isNullOrBlank() && download.etag == meta.etag
                    val sameBytes = !force && files.hasCurrent() &&
                        files.current.length() == download.bytes.size.toLong() &&
                        files.current.readBytes().contentEquals(download.bytes)
                    if (sameEtag || sameBytes) {
                        reparseLocalIfStale(user.groupName)
                        prefs.saveRefresh(
                            success = true,
                            etag = download.etag ?: meta.etag,
                            lastModified = download.lastModified ?: meta.lastModifiedHeader,
                            fileName = meta.sourceFileName,
                            title = meta.title,
                            error = null,
                        )
                        return@runCatching
                    }
                    val incoming = files.writeIncoming(download.bytes)
                    val parsed = runCatching {
                        incoming.inputStream().use { ScheduleXlsxParser.parse(it) }
                    }.getOrElse { error ->
                        incoming.delete()
                        throw error
                    }
                    if (parsed.lessons.none { it.groupName == user.groupName }) {
                        throw IllegalStateException("В обновлённом файле нет группы ${user.groupName}")
                    }
                    files.promoteIncoming()
                    db.lessons().replaceAll(parsed.lessons.map(LessonEntity::from))
                    prefs.setParserRevision(ScheduleXlsxParser.REVISION)
                    prefs.saveRefresh(
                        success = true,
                        etag = download.etag,
                        lastModified = download.lastModified,
                        fileName = user.xlsxUrl.substringAfterLast('/'),
                        title = parsed.title,
                        error = null,
                    )
                }
            }
        }.onFailure { error ->
            prefs.saveRefresh(
                success = false,
                etag = meta.etag,
                lastModified = meta.lastModifiedHeader,
                fileName = meta.sourceFileName,
                title = meta.title,
                error = error.message ?: error.toString(),
            )
        }
    }

    private suspend fun reparseLocalIfStale(groupName: String, force: Boolean = false) {
        if (!force && prefs.parserRevision() == ScheduleXlsxParser.REVISION) return
        if (!files.hasCurrent()) return
        val parsed = runCatching {
            files.current.inputStream().use { ScheduleXlsxParser.parse(it) }
        }.getOrNull() ?: return
        if (parsed.lessons.none { it.groupName == groupName }) return
        db.lessons().replaceAll(parsed.lessons.map(LessonEntity::from))
        prefs.setParserRevision(ScheduleXlsxParser.REVISION)
    }

    suspend fun saveLocalNote(occurrence: Occurrence, text: String) = localNoteMutex.withLock {
        val existing = findLocalEntity(occurrence)
        upsertLocal(
            occurrence = occurrence,
            existing = existing,
            text = text,
            deadline = existing?.dueEpoch != null,
        )
    }

    suspend fun saveLocalDeadline(occurrence: Occurrence, enabled: Boolean) = localNoteMutex.withLock {
        val existing = findLocalEntity(occurrence)
        upsertLocal(
            occurrence = occurrence,
            existing = existing,
            text = existing?.text.orEmpty(),
            deadline = enabled,
        )
    }

    private suspend fun findLocalEntity(occurrence: Occurrence): LocalNoteEntity? {
        val lesson = occurrence.lesson
        return db.notes().findLocal(
            group = lesson.groupName,
            date = occurrence.date.toEpochDay(),
            start = lesson.start.hour * 60 + lesson.start.minute,
            subject = lesson.subjectKey,
            subgroup = lesson.subgroup ?: 0,
        )
    }

    private suspend fun upsertLocal(
        occurrence: Occurrence,
        existing: LocalNoteEntity?,
        text: String,
        deadline: Boolean,
    ) {
        val lesson = occurrence.lesson
        val trimmed = text.trim()
        if (trimmed.isEmpty() && !deadline) {
            if (existing != null) db.notes().deleteLocal(existing.id)
            onWidgetUpdate()
            return
        }
        val entity = existing ?: LocalNoteEntity(
            groupName = lesson.groupName,
            dateEpoch = occurrence.date.toEpochDay(),
            startMinutes = lesson.start.hour * 60 + lesson.start.minute,
            subjectKey = lesson.subjectKey,
            subgroup = lesson.subgroup ?: 0,
            text = trimmed,
            dueEpoch = if (deadline) occurrence.date.toEpochDay() else null,
            updatedAt = System.currentTimeMillis(),
        )
        db.notes().upsertLocal(
            entity.copy(
                text = trimmed,
                dueEpoch = if (deadline) occurrence.date.toEpochDay() else null,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        onWidgetUpdate()
    }

    suspend fun saveGlobalNote(
        subjectKey: String,
        teacherKey: String,
        subjectLabel: String,
        teacherLabel: String,
        text: String,
    ) = globalNoteMutex.withLock {
        val existing = db.notes().findGlobal(subjectKey, teacherKey)
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            if (existing != null) db.notes().deleteGlobal(existing.id)
        } else {
            val entity = existing ?: GlobalNoteEntity(
                subjectKey = subjectKey,
                teacherKey = teacherKey,
                subjectLabel = subjectLabel,
                teacherLabel = teacherLabel,
                text = trimmed,
                updatedAt = System.currentTimeMillis(),
            )
            db.notes().upsertGlobal(
                entity.copy(
                    text = trimmed,
                    subjectLabel = subjectLabel,
                    teacherLabel = teacherLabel,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun occurrencesFor(
        date: LocalDate,
        lessons: List<Lesson>,
        notes: List<LocalNote>,
        globalNotes: List<GlobalNote> = emptyList(),
    ): List<Occurrence> {
        val dayLessons = lessons.filter { it.visibleOn(date) }
            .distinctBy { lesson ->
                listOf(
                    lesson.start.toString(),
                    lesson.end.toString(),
                    lesson.subjectKey,
                    lesson.kind.name,
                    (lesson.subgroup ?: 0).toString(),
                    lesson.teachers.joinToString { it.key },
                    lesson.rooms.joinToString(),
                ).joinToString("|")
            }
            .sortedWith(compareBy({ it.start }, { it.subgroup ?: 0 }, { it.subject }))
        return dayLessons.map { lesson ->
            val note = notes.find {
                it.date == date &&
                    it.startMinutes == lesson.start.hour * 60 + lesson.start.minute &&
                    it.subjectKey == lesson.subjectKey &&
                    it.subgroup == lesson.subgroup
            }
            val teacherKeys = lesson.teachers.map { it.key }.toSet()
            val globals = globalNotes.filter { global ->
                global.text.isNotBlank() &&
                    global.subjectKey == lesson.subjectKey &&
                    global.teacherKey in teacherKeys
            }
            Occurrence(date, lesson, note, globals)
        }
    }

    fun teacherCards(lessons: List<Lesson>, staff: List<StaffProfile> = emptyList()): List<TeacherCard> {
        val map = linkedMapOf<String, MutableTeacher>()
        for (lesson in lessons) {
            val teachers = lesson.teachers.filter { it.name.isNotBlank() }
            for (teacher in teachers) {
                val bucket = map.getOrPut(teacher.key) { MutableTeacher(teacher) }
                bucket.subjects += lesson.subject
                bucket.rooms += lesson.rooms
            }
        }
        val byMatch = staff.associateBy { personMatchKey(it.fullName) }
        return map.values
            .map { bucket ->
                TeacherCard(
                    teacher = bucket.teacher,
                    subjects = bucket.subjects.toList().sorted(),
                    rooms = bucket.rooms.toList().sorted(),
                    staff = byMatch[personMatchKey(bucket.teacher.name)],
                )
            }
            .sortedBy { it.displayName }
    }

    suspend fun loadStaffDetails(slug: String): StaffProfile? = withContext(Dispatchers.IO) {
        val existing = db.staff().find(slug)?.toDomain() ?: return@withContext null
        if (existing.disciplines.isNotEmpty()) return@withContext existing
        runCatching {
            val html = client.fetchHtml(existing.profileUrl)
            val updated = EmployeeDirectoryParser.parseProfile(html, existing) ?: existing
            db.staff().upsertAll(listOf(StaffEntity.from(updated)))
            updated
        }.getOrDefault(existing)
    }

    private suspend fun refreshStaff(force: Boolean) {
        val user = prefs.profile.first() ?: return
        val lessons = db.lessons().listByGroup(user.groupName).map { it.toDomain() }
        val teachers = lessons.flatMap { it.teachers }.distinctBy { it.key }
            .filter { it.name.isNotBlank() && !it.name.contains("не указан", ignoreCase = true) }
        if (teachers.isEmpty()) return
        val have = db.staff().listAll().map { personMatchKey(it.fullName) }.toSet()
        val missing = if (force) teachers else teachers.filter { personMatchKey(it.name) !in have }
        if (missing.isEmpty()) return
        val limit = Semaphore(4)
        val found = supervisorScope {
            missing.map { teacher ->
                async {
                    limit.withPermit {
                        val lastName = teacher.name.substringBefore(' ').trim()
                        if (lastName.length < 3) return@withPermit null
                        val html = runCatching { client.fetchHtml(client.employeeSearchUrl(lastName)) }
                            .getOrNull()
                            ?: return@withPermit null
                        val candidates = EmployeeDirectoryParser.parseList(html)
                        val key = personMatchKey(teacher.name)
                        candidates.firstOrNull { personMatchKey(it.fullName) == key }
                            ?: candidates.filter { personMatchKey(it.fullName).startsWith(key.substringBefore('|') + "|") }
                                .singleOrNull()
                    }
                }
            }.awaitAll().filterNotNull()
        }
        if (found.isNotEmpty()) db.staff().upsertAll(found.map(StaffEntity::from))
    }

    private class MutableTeacher(val teacher: Teacher) {
        val subjects = sortedSetOf<String>()
        val rooms = sortedSetOf<String>()
    }
}
