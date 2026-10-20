package com.gathin.tvhome;

import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.provider.Settings;

/**
 * The TV's own settings screens the launcher links to, through public Settings actions only. Each is offered
 * only when something on this TV answers it: Android TV builds differ (the reference Caixun/MT5867 TV has no
 * Display screen, but has Display &amp; sound under the Sound action).
 */
final class SystemShortcuts {
    private SystemShortcuts() { }

    static boolean resolves(Context ctx, Intent intent) {
        return intent.resolveActivity(ctx.getPackageManager()) != null;
    }

    /** Picture settings: the Display screen, or Display &amp; sound where that is all there is; null if neither. */
    static Intent picture(Context ctx) {
        Intent display = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
        if (resolves(ctx, display)) return display;
        Intent sound = new Intent(Settings.ACTION_SOUND_SETTINGS);
        return resolves(ctx, sound) ? sound : null;
    }

    /** "Display" for the Display screen, "Picture &amp; sound" when the Sound action stands in for it. */
    static int pictureTitle(Intent picture) {
        return Settings.ACTION_DISPLAY_SETTINGS.equals(picture.getAction())
                ? R.string.quick_picture : R.string.quick_picture_sound;
    }

    /** The app Android sends the Home button to now, as a label; null when it cannot tell (a chooser, say). */
    static CharSequence currentHomeLabel(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo info = pm.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
        if (info == null || info.activityInfo == null || "android".equals(info.activityInfo.packageName)) return null;
        return info.loadLabel(pm);
    }

    /** Whether Home currently resolves to this launcher. */
    static boolean isCurrentHome(Context ctx) {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo info = ctx.getPackageManager().resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
        return info != null && info.activityInfo != null && ctx.getPackageName().equals(info.activityInfo.packageName);
    }

    /**
     * The best official way to ask for this launcher to become Home: Android 10+'s role request (a system
     * dialog), else the system's Home picker; null when this TV offers neither. Whether the choice beats a
     * preinstalled launcher with a higher priority is up to the firmware; see docs/INSTALACAO.md.
     */
    static Intent homeRequest(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roles = ctx.getSystemService(RoleManager.class);
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
                Intent request = roles.createRequestRoleIntent(RoleManager.ROLE_HOME);
                if (resolves(ctx, request)) return request;
            }
        }
        Intent picker = new Intent(Settings.ACTION_HOME_SETTINGS);
        return resolves(ctx, picker) ? picker : null;
    }
}
