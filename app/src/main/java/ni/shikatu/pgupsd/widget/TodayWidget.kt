package ni.shikatu.pgupsd.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.color.ColorProvider as DayNightColor
import androidx.glance.material3.ColorProviders
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.unit.ColorProvider
import ni.shikatu.pgupsd.R
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import ni.shikatu.pgupsd.DeepLinks
import ni.shikatu.pgupsd.PgupsApp
import ni.shikatu.pgupsd.ui.theme.DarkColors
import ni.shikatu.pgupsd.ui.theme.Ink
import ni.shikatu.pgupsd.ui.theme.LightColors
import ni.shikatu.pgupsd.domain.Occurrence
import ni.shikatu.pgupsd.domain.isoWeekParity
import ni.shikatu.pgupsd.ui.ru
import ni.shikatu.pgupsd.ui.ruShort
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

internal const val ACTION_WIDGET_REFRESH = "ni.shikatu.pgupsd.widget.REFRESH"

private val WidgetPaper = DayNightColor(day = Ink.bg, night = Ink.bgDark)

class TodayWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as PgupsApp
        val profile = app.container.prefs.profile.first()
        val today = LocalDate.now()
        val now = LocalTime.now()
        val (date, items, tomorrow) = if (profile == null) {
            Triple(today, emptyList(), false)
        } else {
            val lessons = app.container.db.lessons().listByGroup(profile.groupName).map { it.toDomain() }
            val notes = app.container.db.notes().listLocal(profile.groupName).map { it.toDomain() }
            val todayItems = app.container.repo.occurrencesFor(today, lessons, notes)
            val remaining = todayItems.filter { !now.isAfter(it.lesson.end) }
            if (remaining.isEmpty() && todayItems.isNotEmpty()) {
                val next = today.plusDays(1)
                Triple(next, app.container.repo.occurrencesFor(next, lessons, notes), true)
            } else {
                Triple(today, remaining, false)
            }
        }
        scheduleNextRefresh(context, date, items, tomorrow)
        val group = profile?.groupName ?: "ПГУПС"
        val parity = isoWeekParity(date).ru()
        val open = DeepLinks.lessonIntent(context)
        val badge = if (tomorrow) "Завтра" else date.dayOfWeek.ruShort()
        val lines = items.map { occ ->
            WidgetLine(
                key = occ.key,
                time = formatClock(occ.lesson.start.hour, occ.lesson.start.minute),
                subject = occ.lesson.subject,
                room = occ.lesson.rooms.firstOrNull().orEmpty(),
                note = noteSnippet(occ.localNote?.text),
                tracked = occ.localNote?.deadline == true,
            )
        }
        provideContent {
            GlanceTheme(colors = ColorProviders(light = LightColors, dark = DarkColors)) {
                WidgetContent(
                    open = open,
                    badge = badge,
                    heading = "${date.dayOfMonth} ${monthShort(date.monthValue)} · $group",
                    subtitle = buildString {
                        append(parity)
                        if (lines.isNotEmpty()) {
                            append(" · ")
                            append(lines.size)
                            append(" ")
                            append(pairsWord(lines.size))
                        }
                    },
                    lines = lines,
                    empty = when {
                        profile == null -> "Выбери группу в приложении"
                        tomorrow -> "Завтра пар нет"
                        else -> "Сегодня пар нет"
                    },
                )
            }
        }
    }

    companion object {
        suspend fun updateAll(context: Context) {
            TodayWidget().updateAll(context)
        }
    }
}

private data class WidgetLine(
    val key: String,
    val time: String,
    val subject: String,
    val room: String,
    val note: String,
    val tracked: Boolean,
)

@Composable
private fun WidgetContent(
    badge: String,
    heading: String,
    subtitle: String,
    lines: List<WidgetLine>,
    empty: String,
    open: Intent,
) {
    val size = LocalSize.current
    val compact = size.height < 140.dp
    val inset = if (compact) 10.dp else 14.dp

    val openApp = actionStartActivity(open)
    val gap = if (compact) 6.dp else 8.dp
    val headerBlock = inset + 32.dp + 16.dp + gap + inset
    val rowBlock = if (compact) 34.dp else 42.dp
    val leftover = (size.height - headerBlock - rowBlock * lines.size).coerceAtLeast(0.dp)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .roundedFill(R.drawable.widget_background, WidgetPaper)
            .widgetSystemRadius()
            .clickable(openApp),
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(inset),
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(openApp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Pill(text = badge, container = true)
                Spacer(GlanceModifier.padding(6.dp))
                Text(
                    text = heading,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    ),
                    maxLines = 1,
                )
            }
            Text(
                text = subtitle,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp,
                ),
                maxLines = 1,
                modifier = GlanceModifier.clickable(openApp),
            )
            Spacer(GlanceModifier.height(gap))
            if (lines.isEmpty()) {
                Text(
                    text = empty,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 13.sp,
                    ),
                    modifier = GlanceModifier.clickable(openApp),
                )
                OpenAppFiller(openApp, GlanceModifier.defaultWeight().fillMaxWidth())
            } else {
                LazyColumn(
                    modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
                ) {
                    itemsIndexed(
                        items = lines,
                        itemId = { index, line ->
                            31L * index + line.key.hashCode()
                        },
                    ) { _, line ->
                        PairRow(line = line, compact = compact)
                    }
                    if (leftover > 0.dp) {
                        item(itemId = Long.MAX_VALUE) {
                            OpenAppFiller(openApp, GlanceModifier.fillMaxWidth().height(leftover))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OpenAppFiller(onOpen: androidx.glance.action.Action, modifier: GlanceModifier) {
    Box(
        modifier = modifier
            .background(WidgetPaper)
            .clickable(onOpen),
    ) {}
}

@Composable
private fun PairRow(
    line: WidgetLine,
    compact: Boolean,
) {
    val context = LocalContext.current
    val onOpen = actionStartActivity(DeepLinks.lessonIntent(context, line.key))
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(onOpen)
            .padding(vertical = if (compact) 2.dp else 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .roundedFill(R.drawable.widget_chip, GlanceTheme.colors.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = line.time,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                ),
                maxLines = 1,
            )
        }
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .padding(start = 8.dp),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = line.subject,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                if (line.tracked) {
                    FlagChip()
                }
            }
            if (line.room.isNotEmpty() || line.note.isNotEmpty()) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (line.room.isNotEmpty()) {
                        Text(
                            text = line.room,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 11.sp,
                            ),
                            maxLines = 1,
                            modifier = GlanceModifier.defaultWeight(),
                        )
                    } else {
                        Spacer(GlanceModifier.defaultWeight())
                    }
                    if (line.note.isNotEmpty()) {
                        Text(
                            text = line.note,
                            style = TextStyle(
                                color = GlanceTheme.colors.tertiary,
                                fontSize = 11.sp,
                            ),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FlagChip() {
    Box(
        modifier = GlanceModifier
            .padding(start = 6.dp)
            .roundedFill(R.drawable.widget_chip, GlanceTheme.colors.tertiaryContainer)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_widget_flag),
            contentDescription = "Отслеживание",
            modifier = GlanceModifier.size(12.dp),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onTertiaryContainer),
        )
    }
}

@Composable
private fun Pill(text: String, container: Boolean) {
    Box(
        modifier = GlanceModifier
            .roundedFill(
                R.drawable.widget_pill,
                if (container) GlanceTheme.colors.primary
                else GlanceTheme.colors.secondaryContainer,
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = if (container) GlanceTheme.colors.onPrimary
                else GlanceTheme.colors.onSecondaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            ),
            maxLines = 1,
        )
    }
}

private fun GlanceModifier.roundedFill(drawable: Int, color: ColorProvider): GlanceModifier =
    background(
        imageProvider = ImageProvider(drawable),
        colorFilter = ColorFilter.tint(color),
    )

private fun GlanceModifier.widgetSystemRadius(): GlanceModifier {
    if (Build.VERSION.SDK_INT < 31) return this
    return runCatching { cornerRadius(android.R.dimen.system_app_widget_background_radius) }
        .getOrDefault(this)
}

private fun noteSnippet(raw: String?): String {
    val text = raw.orEmpty().trim().replace(Regex("\\s+"), " ")
    if (text.isEmpty()) return ""
    return if (text.length <= 24) text else text.take(23).trimEnd() + "…"
}

private fun formatClock(hour: Int, minute: Int): String =
    String.format("%02d:%02d", hour, minute)

private fun monthShort(month: Int): String = listOf(
    "янв", "фев", "мар", "апр", "мая", "июн",
    "июл", "авг", "сен", "окт", "ноя", "дек",
)[month - 1]

private fun pairsWord(n: Int): String {
    val mod100 = n % 100
    val mod10 = n % 10
    return when {
        mod100 in 11..14 -> "пар"
        mod10 == 1 -> "пара"
        mod10 in 2..4 -> "пары"
        else -> "пар"
    }
}

private fun scheduleNextRefresh(
    context: Context,
    date: LocalDate,
    items: List<Occurrence>,
    tomorrow: Boolean,
) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pending = PendingIntent.getBroadcast(
        context,
        17,
        Intent(context, TodayWidgetReceiver::class.java).setAction(ACTION_WIDGET_REFRESH),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    am.cancel(pending)
    val zone = ZoneId.systemDefault()
    val nowMs = System.currentTimeMillis()
    val candidates = mutableListOf<Long>()
    if (!tomorrow) {
        for (occ in items) {
            val at = date.atTime(occ.lesson.end).atZone(zone).toInstant().toEpochMilli() + 2_000
            if (at > nowMs) candidates += at
        }
    }
    val midnight = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() + 2_000
    if (midnight > nowMs) candidates += midnight
    val next = candidates.minOrNull() ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending)
    } else {
        am.set(AlarmManager.RTC_WAKEUP, next, pending)
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_WIDGET_REFRESH) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                TodayWidget.updateAll(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }
}
