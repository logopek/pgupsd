package ni.shikatu.pgupsd

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

object DeepLinks {
    const val SCHEME = "pgupsd"
    const val HOST_LESSON = "lesson"
    const val EXTRA_LESSON_KEY = "ni.shikatu.pgupsd.LESSON_KEY"
    const val QUERY_KEY = "key"

    fun lessonUri(key: String): Uri =
        Uri.Builder()
            .scheme(SCHEME)
            .authority(HOST_LESSON)
            .appendQueryParameter(QUERY_KEY, key)
            .build()

    fun lessonIntent(context: Context, key: String? = null): Intent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
        if (key != null) {
            intent.action = Intent.ACTION_VIEW
            intent.data = lessonUri(key)
            intent.putExtra(EXTRA_LESSON_KEY, key)
            if (Build.VERSION.SDK_INT >= 29) {
                intent.identifier = "lesson:$key"
            }
        }
        return intent
    }

    fun keyFrom(intent: Intent?): String? {
        if (intent == null) return null
        intent.getStringExtra(EXTRA_LESSON_KEY)?.takeIf { it.isNotBlank() }?.let { return it }
        val data = intent.data ?: return null
        if (data.scheme != SCHEME || data.host != HOST_LESSON) return null
        return data.getQueryParameter(QUERY_KEY)?.takeIf { it.isNotBlank() }
    }
}
