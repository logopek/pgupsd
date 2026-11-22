package ni.shikatu.pgupsd

import android.app.Application
import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ni.shikatu.pgupsd.data.local.AppDatabase
import ni.shikatu.pgupsd.data.local.ScheduleFiles
import ni.shikatu.pgupsd.data.local.UserPrefs
import ni.shikatu.pgupsd.data.remote.PgupsClient
import ni.shikatu.pgupsd.data.plan.PlanRepository
import ni.shikatu.pgupsd.data.repo.ScheduleRepository
import ni.shikatu.pgupsd.widget.TodayWidget

class PgupsApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val db: AppDatabase = AppDatabase.create(appContext)
    val prefs: UserPrefs = UserPrefs(appContext)
    val files: ScheduleFiles = ScheduleFiles(appContext)
    val client: PgupsClient = PgupsClient(cacheDir = appContext.cacheDir)
    val plans: PlanRepository = PlanRepository(client, appContext)
    val repo: ScheduleRepository = ScheduleRepository(
        db = db,
        prefs = prefs,
        files = files,
        client = client,
        onWidgetUpdate = { TodayWidget.updateAll(appContext) },
    )

    val viewModelFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val vm: ViewModel = when {
                modelClass.isAssignableFrom(ni.shikatu.pgupsd.ui.DiaryViewModel::class.java) ->
                    ni.shikatu.pgupsd.ui.DiaryViewModel(repo)
                modelClass.isAssignableFrom(ni.shikatu.pgupsd.ui.onboarding.OnboardingViewModel::class.java) ->
                    ni.shikatu.pgupsd.ui.onboarding.OnboardingViewModel(repo)
                modelClass.isAssignableFrom(ni.shikatu.pgupsd.ui.plan.PlanViewModel::class.java) ->
                    ni.shikatu.pgupsd.ui.plan.PlanViewModel(repo.profile, plans)
                else -> error("Unknown ViewModel ${modelClass.name}")
            }
            return vm as T
        }
    }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer is not provided")
}

fun Context.appContainer(): AppContainer = (applicationContext as PgupsApp).container
