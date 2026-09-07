package ni.shikatu.pgupsd.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [LessonEntity::class, LocalNoteEntity::class, GlobalNoteEntity::class, StaffEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lessons(): LessonDao
    abstract fun notes(): NoteDao
    abstract fun staff(): StaffDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `staff` (
                        `slug` TEXT NOT NULL,
                        `fullName` TEXT NOT NULL,
                        `photoUrl` TEXT,
                        `degree` TEXT,
                        `rank` TEXT,
                        `posts` TEXT NOT NULL,
                        `phones` TEXT NOT NULL,
                        `emails` TEXT NOT NULL,
                        `office` TEXT,
                        `disciplines` TEXT NOT NULL,
                        `profileUrl` TEXT NOT NULL,
                        PRIMARY KEY(`slug`)
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `local_notes` ADD COLUMN `dueEpoch` INTEGER")
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "pgupsd.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
