package ni.shikatu.pgupsd.data.remote

import ni.shikatu.pgupsd.domain.ScheduleCatalog
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.net.Inet4Address
import java.util.concurrent.TimeUnit

class PgupsClient(
    cacheDir: File? = null,
) {
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .dns { hostname ->
            Dns.SYSTEM.lookup(hostname).sortedByDescending { it is Inet4Address }
        }
        .apply {
            if (cacheDir != null) {
                cache(Cache(File(cacheDir, "http"), 8L * 1024 * 1024))
            }
        }
        .build()

    fun fetchCatalog(): ScheduleCatalog {
        val request = Request.Builder()
            .url("https://rasp.pgups.ru/")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html")
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Каталог расписания: HTTP ${response.code}")
            }
            val html = response.body?.string().orEmpty()
            return ScheduleCatalogParser.parse(html)
        }
    }

    fun fetchHtml(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html")
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Страница: HTTP ${response.code}")
            }
            return response.body?.string().orEmpty()
        }
    }

    fun employeeSearchUrl(name: String): String =
        "https://www.pgups.ru/employee/".toHttpUrl().newBuilder()
            .addQueryParameter("arrFilter_ff[NAME]", name)
            .addQueryParameter("set_filter", "Найти")
            .build()
            .toString()

    fun downloadBytes(url: String): ByteArray {
        val slow = http.newBuilder()
            .callTimeout(45, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/pdf,*/*")
            .build()
        slow.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Не удалось скачать файл: HTTP ${response.code}")
            }
            val bytes = response.body?.bytes() ?: throw IOException("Пустой файл")
            if (bytes.size < 64) throw IOException("Файл слишком маленький")
            return bytes
        }
    }

    fun downloadXlsx(
        url: String,
        etag: String?,
        lastModified: String?,
        forceNetwork: Boolean = false,
    ): XlsxDownload {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,*/*")
        if (forceNetwork) {
            builder.cacheControl(CacheControl.FORCE_NETWORK)
        } else {
            if (!etag.isNullOrBlank()) builder.header("If-None-Match", etag)
            if (!lastModified.isNullOrBlank()) builder.header("If-Modified-Since", lastModified)
        }
        http.newCall(builder.build()).execute().use { response ->
            if (response.code == 304) {
                return XlsxDownload.NotModified(
                    etag = header(response, "ETag") ?: etag,
                    lastModified = header(response, "Last-Modified") ?: lastModified,
                )
            }
            if (!response.isSuccessful) {
                throw IOException("Не удалось скачать расписание: HTTP ${response.code}")
            }
            val bytes = response.body?.bytes() ?: throw IOException("Пустой файл расписания")
            if (bytes.size < 64) throw IOException("Файл расписания слишком маленький")
            return XlsxDownload.Updated(
                bytes = bytes,
                etag = header(response, "ETag"),
                lastModified = header(response, "Last-Modified"),
            )
        }
    }

    private fun header(response: Response, name: String): String? =
        response.header(name)?.trim()?.ifBlank { null }

    companion object {
        const val USER_AGENT = "PGUPSd/1.0 (Android; student diary)"
    }
}

sealed interface XlsxDownload {
    data class Updated(
        val bytes: ByteArray,
        val etag: String?,
        val lastModified: String?,
    ) : XlsxDownload

    data class NotModified(
        val etag: String?,
        val lastModified: String?,
    ) : XlsxDownload
}
