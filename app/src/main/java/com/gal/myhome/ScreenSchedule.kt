package com.gal.myhome

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import java.util.Calendar

/**
 * Nightly screen off / morning screen on.
 *
 * This deliberately does NOT live in the Compose tree. Once the screen is off
 * the window stops being visible, recomposition halts, and a `LaunchedEffect`
 * tick never runs again — so a morning wake-up scheduled that way could never
 * fire. AlarmManager keeps running with the screen off, which is the entire
 * point of the feature.
 *
 * The hours are mirrored into SharedPreferences because the receiver has to
 * read them synchronously, and DataStore is a suspending API.
 */
object ScreenSchedule {

    const val ACTION_LOCK = "com.gal.myhome.action.LOCK_SCREEN"
    const val ACTION_WAKE = "com.gal.myhome.action.WAKE_SCREEN"
    private const val STORE = "screen_schedule"

    /** Mirror the current settings and (re)arm both alarms. */
    fun apply(ctx: Context, enabled: Boolean, offHour: Int, wakeEnabled: Boolean, onHour: Int) {
        ctx.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", enabled)
            .putInt("off", offHour)
            .putBoolean("wake", wakeEnabled)
            .putInt("on", onHour)
            .apply()
        reschedule(ctx)
    }

    /** Alarms are one-shot, so this runs again after every firing and on boot. */
    fun reschedule(ctx: Context) {
        val p = ctx.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        val on = p.getBoolean("enabled", false)
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        arm(ctx, am, ACTION_LOCK, on, p.getInt("off", 1))
        arm(ctx, am, ACTION_WAKE, on && p.getBoolean("wake", true), p.getInt("on", 7))
    }

    private fun arm(ctx: Context, am: AlarmManager, action: String, enabled: Boolean, hour: Int) {
        val pi = PendingIntent.getBroadcast(
            ctx,
            action.hashCode(),
            Intent(action).setClass(ctx, ScheduleReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (!enabled) {
            am.cancel(pi)
            return
        }
        val at = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // an hour already past today means tomorrow
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (_: SecurityException) {
            // exact alarms not permitted — a few minutes' drift beats not firing
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    /**
     * Turn the display back on. This wakes the screen only — the tablet stays
     * locked, so the panel still needs unlocking, which is what was asked for.
     */
    @Suppress("DEPRECATION")
    fun wakeScreen(ctx: Context) {
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        val lock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "myhome:morning",
        )
        // the timeout is a backstop; the release below is the normal path
        lock.acquire(60_000L)
        Handler(Looper.getMainLooper()).postDelayed({
            if (lock.isHeld) lock.release()
        }, 20_000L)
    }
}

class ScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ScreenSchedule.ACTION_LOCK -> ScreenLock.lockNow(context)
            ScreenSchedule.ACTION_WAKE -> ScreenSchedule.wakeScreen(context)
        }
        // one-shot alarms: book the next one as soon as this one lands, and
        // re-arm both after a reboot clears them
        ScreenSchedule.reschedule(context)
    }
}
