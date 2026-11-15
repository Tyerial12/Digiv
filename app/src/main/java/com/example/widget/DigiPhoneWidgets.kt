package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.model.CreatureRaisingProgress
import com.example.data.model.DigimonCatalog
import com.example.viewmodel.DashboardUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WidgetTelemetrySnapshot(
    val partnerName: String,
    val speciesId: String,
    val speciesName: String,
    val stage: String,
    val attribute: String,
    val health: Int,
    val stamina: Int,
    val hunger: Int,
    val evolutionProgress: Int,
    val attack: Int,
    val defense: Int,
    val speed: Int,
    val wisdom: Int,
    val bondLevel: Int,
    val careMistakes: Int,
    val isResting: Boolean,
    val combatPower: Int,
    val squadTotalCp: Int,
    val streakDays: Int,
    val moodLabel: String,
    val nextEvolutionTarget: String,
    val ageLabel: String,
    val lastSyncedLabel: String,
    val canEvolve: Boolean = false
)

object DigiWidgetSyncManager {
    private const val PREFS_NAME = "digi_phone_widgets_prefs"
    private const val AUTO_REFRESH_INTERVAL_MS = 60_000L // Auto-refresh every 60 seconds
    private const val AUTO_REFRESH_REQ_CODE = 990

    const val ACTION_AUTO_REFRESH_WIDGETS = "com.aistudio.digipulse.holovx.ACTION_AUTO_REFRESH_WIDGETS"
    const val ACTION_QUICK_HEAL = "com.aistudio.digipulse.holovx.ACTION_QUICK_HEAL"
    const val ACTION_TOGGLE_REST = "com.aistudio.digipulse.holovx.ACTION_TOGGLE_REST"
    const val ACTION_QUICK_FEED = "com.aistudio.digipulse.holovx.ACTION_QUICK_FEED"
    const val EXTRA_WIDGET_QUICK_ACTION = "extra_widget_quick_action"

    fun syncFromUiState(context: Context, uiState: DashboardUiState) {
        val active = uiState.activeCreature ?: return
        val spec = DigimonCatalog.getSpec(active.speciesId)
        val nextTarget = spec.evolutionBranches.firstOrNull()?.targetSpeciesName ?: "Apex"
        val totalSquadCp = uiState.creatures.sumOf { it.combatPower() }.coerceAtLeast(active.combatPower())
        val streak = uiState.trainingLogs.map { it.dateKey }.distinct().size.coerceAtLeast(1)
        val nowMs = System.currentTimeMillis()
        val createdMs = active.createdAt?.toDate()?.time ?: nowMs

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("partnerName", active.nickname)
            .putString("speciesId", active.speciesId)
            .putString("speciesName", active.speciesName)
            .putString("stage", active.stage)
            .putString("attribute", active.attribute)
            .putInt("health", active.health)
            .putInt("stamina", active.stamina)
            .putInt("hunger", active.hunger)
            .putInt("evolutionProgress", active.evolutionProgress)
            .putInt("attack", active.attack)
            .putInt("defense", active.defense)
            .putInt("speed", active.speed)
            .putInt("wisdom", active.wisdom)
            .putInt("bondLevel", active.bondLevel)
            .putInt("careMistakes", active.careMistakes)
            .putBoolean("isResting", active.isResting)
            .putBoolean("canEvolve", active.canEvolveAnyBranch())
            .putInt("combatPower", active.combatPower())
            .putInt("squadTotalCp", totalSquadCp)
            .putInt("streakDays", streak)
            .putString("moodLabel", active.moodLabel())
            .putString("nextEvolutionTarget", nextTarget)
            .putLong("createdAtMs", createdMs)
            .putLong("lastUpdatedMs", nowMs)
            .apply()

        scheduleAutoRefresh(context)
        refreshAllWidgets(context)
    }

    fun clearAndRefreshWidgets(context: Context) {
        val nowMs = System.currentTimeMillis()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .clear()
            .putString("partnerName", "Draft Starter")
            .putString("speciesId", "agumon")
            .putString("speciesName", "No Partner")
            .putString("stage", "Rookie")
            .putString("attribute", "Vaccine")
            .putInt("health", 100)
            .putInt("stamina", 100)
            .putInt("hunger", 100)
            .putInt("evolutionProgress", 0)
            .putBoolean("canEvolve", false)
            .putInt("combatPower", 0)
            .putInt("squadTotalCp", 0)
            .putInt("streakDays", 0)
            .putString("moodLabel", "RESET")
            .putString("nextEvolutionTarget", "Choose Starter")
            .putLong("createdAtMs", nowMs)
            .putLong("lastUpdatedMs", nowMs)
            .apply()
        refreshAllWidgets(context)
    }

    /**
     * Reads the current snapshot AND dynamically computes real-world elapsed changes
     * (age, stamina recovery only while resting, hunger decay, and passive evolution sync) so that background
     * home screen auto-refresh ticks always show live, up-to-the-minute telemetry even when
     * the app is in the background!
     */
    fun readSnapshot(context: Context): WidgetTelemetrySnapshot {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nowMs = System.currentTimeMillis()
        val lastUpdatedMs = prefs.getLong("lastUpdatedMs", nowMs)
        val createdAtMs = prefs.getLong("createdAtMs", nowMs)
        val elapsedMinutes = ((nowMs - lastUpdatedMs).coerceAtLeast(0L)) / 60_000L

        val rawIsResting = prefs.getBoolean("isResting", false)
        val rawHealth = prefs.getInt("health", 100)
        val rawStamina = prefs.getInt("stamina", 100)
        val rawHunger = prefs.getInt("hunger", 85)
        val rawEvo = prefs.getInt("evolutionProgress", 0)

        // Apply live elapsed-time simulation for home screen auto-refresh (Stamina ONLY recovers in Cryo-Rest)
        val cappedElapsed = elapsedMinutes.coerceAtMost(720L).toInt()
        val liveStamina = (rawStamina + if (rawIsResting) cappedElapsed * 3 else 0).coerceIn(0, 100)
        val isResting = if (rawIsResting && liveStamina >= 100) false else rawIsResting
        val liveHunger = (rawHunger - if (isResting) cappedElapsed / 6 else cappedElapsed / 3).coerceIn(10, 100)
        val liveHealth = (rawHealth + if (liveHunger < 20) -(cappedElapsed / 10) else 0).coerceIn(20, 100)
        val liveEvo = (rawEvo + if (liveHunger >= 50 && liveHealth >= 65) (cappedElapsed / 10).coerceAtMost(15) else 0).coerceIn(0, 100)

        val totalAgeMins = ((nowMs - createdAtMs).coerceAtLeast(0L)) / 60_000L
        val ageLabel = formatCompactMinutes(totalAgeMins)
        val timeStamp = SimpleDateFormat("HH:mm", Locale.US).format(Date(nowMs))

        return WidgetTelemetrySnapshot(
            partnerName = prefs.getString("partnerName", "Agumon") ?: "Agumon",
            speciesId = prefs.getString("speciesId", "agumon") ?: "agumon",
            speciesName = prefs.getString("speciesName", "Agumon") ?: "Agumon",
            stage = prefs.getString("stage", "Rookie") ?: "Rookie",
            attribute = prefs.getString("attribute", "Vaccine") ?: "Vaccine",
            health = liveHealth,
            stamina = liveStamina,
            hunger = liveHunger,
            evolutionProgress = liveEvo,
            attack = prefs.getInt("attack", 52),
            defense = prefs.getInt("defense", 42),
            speed = prefs.getInt("speed", 44),
            wisdom = prefs.getInt("wisdom", 36),
            bondLevel = prefs.getInt("bondLevel", 10),
            careMistakes = prefs.getInt("careMistakes", 0),
            isResting = isResting,
            combatPower = prefs.getInt("combatPower", 480),
            squadTotalCp = prefs.getInt("squadTotalCp", 1440),
            streakDays = prefs.getInt("streakDays", 1),
            moodLabel = prefs.getString("moodLabel", "STABLE") ?: "STABLE",
            nextEvolutionTarget = prefs.getString("nextEvolutionTarget", "Greymon") ?: "Greymon",
            ageLabel = ageLabel,
            lastSyncedLabel = timeStamp,
            canEvolve = prefs.getBoolean("canEvolve", false)
        )
    }

    private fun formatCompactMinutes(mins: Long): String {
        if (mins < 60L) return "${mins}m"
        val hours = mins / 60L
        val remMins = mins % 60L
        if (hours < 24L) {
            return if (remMins > 0L) "${hours}h${remMins}m" else "${hours}h"
        }
        val days = hours / 24L
        val remHours = hours % 24L
        return "${days}d${remHours}h"
    }

    fun scheduleAutoRefresh(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context.applicationContext, DigiHealthWidgetProvider::class.java).apply {
            action = ACTION_AUTO_REFRESH_WIDGETS
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context.applicationContext,
            AUTO_REFRESH_REQ_CODE,
            intent,
            flags
        )
        // Non-wakeup inexact repeating alarm every 60 seconds (requires zero special permissions)
        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + AUTO_REFRESH_INTERVAL_MS,
            AUTO_REFRESH_INTERVAL_MS,
            pendingIntent
        )
    }

    fun applyOptimisticWidgetAction(context: Context, action: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        when (action) {
            ACTION_QUICK_HEAL -> {
                val cur = prefs.getInt("health", 90)
                prefs.edit()
                    .putInt("health", (cur + 25).coerceIn(0, 100))
                    .putLong("lastUpdatedMs", System.currentTimeMillis())
                    .apply()
            }
            ACTION_TOGGLE_REST -> {
                val curRest = prefs.getBoolean("isResting", false)
                val curSta = prefs.getInt("stamina", 80)
                if (!curRest && curSta <= 50) {
                    prefs.edit()
                        .putBoolean("isResting", true)
                        .putLong("lastUpdatedMs", System.currentTimeMillis())
                        .apply()
                }
            }
            ACTION_QUICK_FEED -> {
                val curHun = prefs.getInt("hunger", 75)
                prefs.edit()
                    .putInt("hunger", (curHun + 28).coerceIn(0, 100))
                    .putLong("lastUpdatedMs", System.currentTimeMillis())
                    .apply()
            }
        }
        refreshAllWidgets(context)
    }

    fun refreshAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val snapshot = readSnapshot(context)

        val healthIds = appWidgetManager.getAppWidgetIds(ComponentName(context, DigiHealthWidgetProvider::class.java))
        healthIds.forEach { id ->
            DigiHealthWidgetProvider.updateWidget(context, appWidgetManager, id, snapshot)
        }

        val staminaIds = appWidgetManager.getAppWidgetIds(ComponentName(context, DigiStaminaWidgetProvider::class.java))
        staminaIds.forEach { id ->
            DigiStaminaWidgetProvider.updateWidget(context, appWidgetManager, id, snapshot)
        }

        val hungerIds = appWidgetManager.getAppWidgetIds(ComponentName(context, DigiHungerWidgetProvider::class.java))
        hungerIds.forEach { id ->
            DigiHungerWidgetProvider.updateWidget(context, appWidgetManager, id, snapshot)
        }

        val evoIds = appWidgetManager.getAppWidgetIds(ComponentName(context, DigiEvolutionWidgetProvider::class.java))
        evoIds.forEach { id ->
            DigiEvolutionWidgetProvider.updateWidget(context, appWidgetManager, id, snapshot)
        }
    }

    fun requestPinWidget(context: Context, providerClass: Class<out AppWidgetProvider>): Boolean {
        scheduleAutoRefresh(context)
        refreshAllWidgets(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val provider = ComponentName(context, providerClass)
                return appWidgetManager.requestPinAppWidget(provider, null, null)
            }
        }
        return false
    }

    internal fun buildLaunchAppIntent(context: Context, quickAction: String? = null, requestCode: Int = 0): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (quickAction != null) {
                putExtra(EXTRA_WIDGET_QUICK_ACTION, quickAction)
            }
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }
}

abstract class BaseAutoRefreshWidgetProvider : AppWidgetProvider() {
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        DigiWidgetSyncManager.scheduleAutoRefresh(context)
        DigiWidgetSyncManager.refreshAllWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == DigiWidgetSyncManager.ACTION_AUTO_REFRESH_WIDGETS ||
            intent.action == Intent.ACTION_USER_PRESENT ||
            intent.action == Intent.ACTION_TIME_CHANGED
        ) {
            DigiWidgetSyncManager.scheduleAutoRefresh(context)
            DigiWidgetSyncManager.refreshAllWidgets(context)
        }
    }
}

class DigiHealthWidgetProvider : BaseAutoRefreshWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        DigiWidgetSyncManager.scheduleAutoRefresh(context)
        val snapshot = DigiWidgetSyncManager.readSnapshot(context)
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, snapshot)
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            snapshot: WidgetTelemetrySnapshot
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_digi_health)
            views.setImageViewResource(
                R.id.iv_widget_monster,
                DigimonCatalog.getMonsterImageRes(snapshot.speciesId)
            )
            views.setTextViewText(R.id.tv_widget_badge, "LIVE ${snapshot.lastSyncedLabel}")
            views.setTextViewText(R.id.tv_partner_name, snapshot.partnerName)
            views.setTextViewText(R.id.tv_primary_value, "${snapshot.health}%")
            views.setProgressBar(R.id.pb_widget_meter, 100, snapshot.health, false)
            views.setTextViewText(
                R.id.tv_secondary_info,
                "CP ${snapshot.combatPower} • DEF ${snapshot.defense}"
            )
            views.setOnClickPendingIntent(
                R.id.widget_root,
                DigiWidgetSyncManager.buildLaunchAppIntent(context, null, 101)
            )
            views.setOnClickPendingIntent(
                R.id.btn_widget_action,
                DigiWidgetSyncManager.buildLaunchAppIntent(context, DigiWidgetSyncManager.ACTION_QUICK_HEAL, 102)
            )
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

class DigiStaminaWidgetProvider : BaseAutoRefreshWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        DigiWidgetSyncManager.scheduleAutoRefresh(context)
        val snapshot = DigiWidgetSyncManager.readSnapshot(context)
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, snapshot)
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            snapshot: WidgetTelemetrySnapshot
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_digi_stamina)
            views.setImageViewResource(
                R.id.iv_widget_monster,
                DigimonCatalog.getMonsterImageRes(snapshot.speciesId)
            )
            views.setTextViewText(
                R.id.tv_widget_badge,
                if (snapshot.isResting) "CRYO 5X" else "LIVE ${snapshot.lastSyncedLabel}"
            )
            views.setTextViewText(R.id.tv_partner_name, snapshot.partnerName)
            views.setTextViewText(R.id.tv_primary_value, "${snapshot.stamina}%")
            views.setProgressBar(R.id.pb_widget_meter, 100, snapshot.stamina, false)
            views.setTextViewText(
                R.id.tv_secondary_info,
                "ATK ${snapshot.attack} • SPD ${snapshot.speed}"
            )
            views.setTextViewText(
                R.id.btn_widget_action,
                if (snapshot.isResting) "WAKE" else "REST"
            )
            views.setOnClickPendingIntent(
                R.id.widget_root,
                DigiWidgetSyncManager.buildLaunchAppIntent(context, null, 201)
            )
            views.setOnClickPendingIntent(
                R.id.btn_widget_action,
                DigiWidgetSyncManager.buildLaunchAppIntent(context, DigiWidgetSyncManager.ACTION_TOGGLE_REST, 202)
            )
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

class DigiHungerWidgetProvider : BaseAutoRefreshWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        DigiWidgetSyncManager.scheduleAutoRefresh(context)
        val snapshot = DigiWidgetSyncManager.readSnapshot(context)
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, snapshot)
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            snapshot: WidgetTelemetrySnapshot
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_digi_hunger)
            views.setImageViewResource(
                R.id.iv_widget_monster,
                DigimonCatalog.getMonsterImageRes(snapshot.speciesId)
            )
            val statusTag = when {
                snapshot.hunger <= 25 -> "LOW!"
                snapshot.hunger >= 75 -> "FULL"
                else -> "LIVE ${snapshot.lastSyncedLabel}"
            }
            views.setTextViewText(R.id.tv_widget_badge, statusTag)
            views.setTextViewText(R.id.tv_partner_name, snapshot.partnerName)
            views.setTextViewText(R.id.tv_primary_value, "${snapshot.hunger}%")
            views.setProgressBar(R.id.pb_widget_meter, 100, snapshot.hunger, false)
            views.setTextViewText(
                R.id.tv_secondary_info,
                "Bond ${snapshot.bondLevel}% • WIS ${snapshot.wisdom}"
            )
            views.setOnClickPendingIntent(
                R.id.widget_root,
                DigiWidgetSyncManager.buildLaunchAppIntent(context, null, 301)
            )
            views.setOnClickPendingIntent(
                R.id.btn_widget_action,
                DigiWidgetSyncManager.buildLaunchAppIntent(context, DigiWidgetSyncManager.ACTION_QUICK_FEED, 302)
            )
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

class DigiEvolutionWidgetProvider : BaseAutoRefreshWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        DigiWidgetSyncManager.scheduleAutoRefresh(context)
        val snapshot = DigiWidgetSyncManager.readSnapshot(context)
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, snapshot)
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            snapshot: WidgetTelemetrySnapshot
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_digi_evolution)
            views.setImageViewResource(
                R.id.iv_widget_monster,
                DigimonCatalog.getMonsterImageRes(snapshot.speciesId)
            )
            views.setTextViewText(
                R.id.tv_widget_badge,
                "AGE ${snapshot.ageLabel}"
            )
            views.setTextViewText(
                R.id.tv_partner_name,
                "${snapshot.partnerName}➔${snapshot.nextEvolutionTarget}"
            )
            views.setTextViewText(R.id.tv_primary_value, "${snapshot.evolutionProgress}%")
            views.setProgressBar(R.id.pb_widget_meter, 100, snapshot.evolutionProgress, false)
            views.setTextViewText(
                R.id.tv_secondary_info,
                "Squad ${snapshot.squadTotalCp} CP • ${snapshot.lastSyncedLabel}"
            )
            views.setTextViewText(
                R.id.btn_widget_action,
                if (snapshot.canEvolve) "EVO!" else "DECK"
            )
            views.setOnClickPendingIntent(
                R.id.widget_root,
                DigiWidgetSyncManager.buildLaunchAppIntent(context, null, 401)
            )
            views.setOnClickPendingIntent(
                R.id.btn_widget_action,
                DigiWidgetSyncManager.buildLaunchAppIntent(context, null, 402)
            )
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
