import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import java.io.File

@UntrackedTask(because = "ставит APK на живой телефон")
abstract class InstallPhoneTask : DefaultTask() {
    @get:Input
    abstract val adb: Property<String>

    @get:Optional
    @get:InputFile
    abstract val apk: RegularFileProperty

    @get:Input
    abstract val applicationId: Property<String>

    @get:Input
    abstract val activity: Property<String>

    @get:Input
    abstract val signed: Property<Boolean>

    @TaskAction
    fun install() {
        if (!signed.get()) {
            error(
                """
                Нет пароля от sign.jks — release нельзя подписать и поставить.
                Добавь в local.properties (файл уже в .gitignore):
                  pgupsd.storePassword=ПАРОЛЬ
                  pgupsd.keyPassword=ПАРОЛЬ
                  pgupsd.keyAlias=key0
                Или те же значения в переменных PGUPSD_STORE_PASSWORD / PGUPSD_KEY_PASSWORD.
                """.trimIndent(),
            )
        }
        val apkFile = apk.get().asFile
        check(apkFile.isFile) { "Release APK не собран: ${apkFile.absolutePath}" }
        val adbBin = adb.get()
        check(adbBin.isNotBlank() && File(adbBin).canExecute()) {
            "Не найден adb. Проверь sdk.dir в local.properties."
        }
        val devices = devices(adbBin)
        check(devices.isNotEmpty()) {
            "Телефон не виден. Включи USB debugging и проверь: adb devices"
        }
        devices.forEach { serial ->
            logger.lifecycle("Ставлю ${apkFile.name} на $serial")
            val install = run(adbBin, "-s", serial, "install", "-r", "-d", "--no-incremental", apkFile.absolutePath)
            check(install.code == 0) { "adb install не удался ($serial):\n${install.output}" }
            run(adbBin, "-s", serial, "shell", "am", "start", "-n", "${applicationId.get()}/${activity.get()}")
            logger.lifecycle("Запущено на $serial")
        }
    }

    private fun devices(adbBin: String): List<String> {
        val serial = System.getenv("ANDROID_SERIAL")?.trim().orEmpty()
        val listed = run(adbBin, "devices").output.lineSequence()
            .drop(1)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.split(Regex("\\s+")) }
            .filter { it.size >= 2 && it[1] == "device" }
            .map { it[0] }
            .toList()
        if (serial.isNotEmpty()) {
            check(serial in listed) {
                "ANDROID_SERIAL=$serial нет среди ${listed.ifEmpty { listOf("нет устройств") }}"
            }
            return listOf(serial)
        }
        return listed
    }

    private fun run(vararg command: String): Result {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        return Result(process.waitFor(), output)
    }

    private data class Result(val code: Int, val output: String)
}
