package ni.shikatu.pgupsd.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ni.shikatu.pgupsd.domain.RefreshMeta
import ni.shikatu.pgupsd.domain.UserProfile

private val Context.dataStore by preferencesDataStore("user")

class UserPrefs(private val context: Context) {
    val profile: Flow<UserProfile?> = context.dataStore.data.map { p ->
        val group = p[GROUP] ?: return@map null
        val faculty = p[FACULTY_CODE] ?: return@map null
        val course = p[COURSE] ?: return@map null
        UserProfile(
            facultyCode = faculty,
            facultyShortName = p[FACULTY_SHORT].orEmpty(),
            facultyFullName = p[FACULTY_FULL].orEmpty(),
            course = course,
            groupName = group,
            yearFolder = p[YEAR].orEmpty(),
            xlsxUrl = p[XLSX_URL].orEmpty(),
        )
    }

    val refreshMeta: Flow<RefreshMeta> = context.dataStore.data.map { p ->
        RefreshMeta(
            lastSuccessMillis = p[LAST_SUCCESS],
            lastAttemptMillis = p[LAST_ATTEMPT],
            etag = p[ETAG],
            lastModifiedHeader = p[LAST_MODIFIED],
            sourceFileName = p[FILE_NAME],
            title = p[TITLE],
            error = p[ERROR],
        )
    }

    val widgetPayload: Flow<String?> = context.dataStore.data.map { it[WIDGET] }

    suspend fun saveProfile(profile: UserProfile) {
        context.dataStore.edit { p ->
            p[FACULTY_CODE] = profile.facultyCode
            p[FACULTY_SHORT] = profile.facultyShortName
            p[FACULTY_FULL] = profile.facultyFullName
            p[COURSE] = profile.course
            p[GROUP] = profile.groupName
            p[YEAR] = profile.yearFolder
            p[XLSX_URL] = profile.xlsxUrl
        }
    }

    suspend fun clearProfile() {
        context.dataStore.edit { p ->
            listOf(
                FACULTY_CODE, FACULTY_SHORT, FACULTY_FULL, COURSE, GROUP, YEAR, XLSX_URL,
            ).forEach { p.remove(it) }
        }
    }

    suspend fun saveRefresh(
        success: Boolean,
        etag: String?,
        lastModified: String?,
        fileName: String?,
        title: String?,
        error: String?,
        now: Long = System.currentTimeMillis(),
    ) {
        context.dataStore.edit { p ->
            p[LAST_ATTEMPT] = now
            if (success) {
                p[LAST_SUCCESS] = now
                p.remove(ERROR)
            } else if (error != null) {
                p[ERROR] = error
            }
            if (etag != null) p[ETAG] = etag
            if (lastModified != null) p[LAST_MODIFIED] = lastModified
            if (fileName != null) p[FILE_NAME] = fileName
            if (title != null) p[TITLE] = title
        }
    }

    suspend fun saveWidgetPayload(json: String) {
        context.dataStore.edit { it[WIDGET] = json }
    }

    suspend fun parserRevision(): Int = context.dataStore.data.map { it[PARSER_REV] ?: 0 }.first()

    suspend fun setParserRevision(value: Int) {
        context.dataStore.edit { it[PARSER_REV] = value }
    }

    private companion object {
        val FACULTY_CODE = stringPreferencesKey("faculty_code")
        val FACULTY_SHORT = stringPreferencesKey("faculty_short")
        val FACULTY_FULL = stringPreferencesKey("faculty_full")
        val COURSE = intPreferencesKey("course")
        val GROUP = stringPreferencesKey("group")
        val YEAR = stringPreferencesKey("year")
        val XLSX_URL = stringPreferencesKey("xlsx_url")
        val LAST_SUCCESS = longPreferencesKey("last_success")
        val LAST_ATTEMPT = longPreferencesKey("last_attempt")
        val ETAG = stringPreferencesKey("etag")
        val LAST_MODIFIED = stringPreferencesKey("last_modified")
        val FILE_NAME = stringPreferencesKey("file_name")
        val TITLE = stringPreferencesKey("title")
        val ERROR = stringPreferencesKey("error")
        val WIDGET = stringPreferencesKey("widget")
        val PARSER_REV = intPreferencesKey("parser_revision")
    }
}
