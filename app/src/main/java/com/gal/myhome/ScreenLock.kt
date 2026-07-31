package com.gal.myhome

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Device-admin receiver whose only purpose is the `force-lock` policy — see
 * res/xml/device_admin.xml. Nothing here reacts to admin callbacks; the app
 * never manages the device, it just needs permission to turn the screen off.
 */
class AdminReceiver : DeviceAdminReceiver()

/**
 * Turning the panel's screen off at night.
 *
 * Blanking the UI (night mode) still leaves the backlight on, which is what a
 * wall-mounted tablet in a hallway should not do all night. `lockNow()` is the
 * only way an ordinary app can actually cut the screen, and it needs the user
 * to activate the app as a device admin once — hence [grantIntent].
 *
 * Locking also means the tablet needs unlocking in the morning, which is the
 * intent: the panel shouldn't be operable from a dark hallway at 3am.
 */
object ScreenLock {

    private fun component(ctx: Context) = ComponentName(ctx, AdminReceiver::class.java)

    private fun dpm(ctx: Context) =
        ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    /** True once the user has activated the app in Settings > Device admin apps. */
    fun canLock(ctx: Context): Boolean = dpm(ctx).isAdminActive(component(ctx))

    /** No-op unless [canLock]; never throws if the permission was revoked. */
    fun lockNow(ctx: Context) {
        val manager = dpm(ctx)
        if (manager.isAdminActive(component(ctx))) manager.lockNow()
    }

    /** System prompt that asks the user to activate the admin. */
    fun grantIntent(ctx: Context): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component(ctx))
            .putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Lets My Home turn the tablet's screen off on a schedule. " +
                    "It is used for nothing else — no wipe, no password control.",
            )

    /** Lets the user revoke it from inside the app. */
    fun revoke(ctx: Context) {
        val manager = dpm(ctx)
        if (manager.isAdminActive(component(ctx))) manager.removeActiveAdmin(component(ctx))
    }
}
