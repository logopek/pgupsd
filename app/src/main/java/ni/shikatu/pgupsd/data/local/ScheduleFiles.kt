package ni.shikatu.pgupsd.data.local

import android.content.Context
import java.io.File

class ScheduleFiles(context: Context) {
    private val dir = File(context.filesDir, "schedule").apply { mkdirs() }
    val current: File = File(dir, "current.xlsx")
    private val backup: File = File(dir, "current.xlsx.bak")
    private val incoming: File = File(dir, "incoming.xlsx")

    fun writeIncoming(bytes: ByteArray): File {
        incoming.writeBytes(bytes)
        return incoming
    }

    fun promoteIncoming() {
        if (current.exists()) {
            current.copyTo(backup, overwrite = true)
        }
        incoming.copyTo(current, overwrite = true)
        incoming.delete()
    }

    fun restoreBackup(): Boolean {
        if (!backup.exists()) return false
        backup.copyTo(current, overwrite = true)
        return true
    }

    fun hasCurrent(): Boolean = current.exists() && current.length() > 64
}
