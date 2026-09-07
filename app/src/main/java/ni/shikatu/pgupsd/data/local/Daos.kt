package ni.shikatu.pgupsd.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE groupName = :group ORDER BY dayOfWeek, startMinutes")
    fun observeByGroup(group: String): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE groupName = :group ORDER BY dayOfWeek, startMinutes")
    suspend fun listByGroup(group: String): List<LessonEntity>

    @Query("SELECT DISTINCT groupName FROM lessons ORDER BY groupName")
    suspend fun groups(): List<String>

    @Query("DELETE FROM lessons")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<LessonEntity>)

    @Transaction
    suspend fun replaceAll(items: List<LessonEntity>) {
        clear()
        if (items.isNotEmpty()) insertAll(items)
    }
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM local_notes WHERE groupName = :group")
    fun observeLocal(group: String): Flow<List<LocalNoteEntity>>

    @Query("SELECT * FROM local_notes WHERE groupName = :group")
    suspend fun listLocal(group: String): List<LocalNoteEntity>

    @Query(
        """
        SELECT * FROM local_notes
        WHERE groupName = :group AND dateEpoch = :date AND startMinutes = :start
          AND subjectKey = :subject AND subgroup = :subgroup
        LIMIT 1
        """,
    )
    suspend fun findLocal(
        group: String,
        date: Long,
        start: Int,
        subject: String,
        subgroup: Int,
    ): LocalNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocal(note: LocalNoteEntity): Long

    @Query("DELETE FROM local_notes WHERE id = :id")
    suspend fun deleteLocal(id: Long)

    @Query("SELECT * FROM global_notes")
    fun observeGlobal(): Flow<List<GlobalNoteEntity>>

    @Query("SELECT * FROM global_notes WHERE subjectKey = :subject AND teacherKey = :teacher LIMIT 1")
    suspend fun findGlobal(subject: String, teacher: String): GlobalNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGlobal(note: GlobalNoteEntity): Long

    @Query("DELETE FROM global_notes WHERE id = :id")
    suspend fun deleteGlobal(id: Long)
}

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff ORDER BY fullName")
    fun observeAll(): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff")
    suspend fun listAll(): List<StaffEntity>

    @Query("SELECT * FROM staff WHERE slug = :slug LIMIT 1")
    suspend fun find(slug: String): StaffEntity?

    @Query("SELECT COUNT(*) FROM staff")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<StaffEntity>)
}
