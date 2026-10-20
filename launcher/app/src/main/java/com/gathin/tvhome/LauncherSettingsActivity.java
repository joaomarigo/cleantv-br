package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Remote-friendly settings owned by the launcher; system settings stay one row away. Every row writes a choice
 * that Config applies over config.xml and the theme, so it survives a reboot; Restore defaults removes them all.
 * Any change sets RESULT_OK, and Home rebuilds itself when this screen closes.
 */
public final class LauncherSettingsActivity extends Activity {
    private static final int HOME_ROLE_REQUEST = 7;
    private SharedPreferences choices;
    private LinearLayout list;
    /** Redraws every row from the current Config; filled while the rows are built. */
    private final List<Runnable> refreshers = new ArrayList<>();
    private Config config;
    private View firstFocus;
    private TextView homeStatus;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        config = Config.load(this);
        list = findViewById(R.id.list);

        header(R.string.settings_section_home);
        firstFocus = choice(R.string.columns_label, new String[]{"4", "5", "6", "7"},
                () -> config.columns - 4, i -> choices.edit().putInt("columns", i + 4).apply());
        choice(R.string.rows_below_dock_label, new String[]{"0", "1", "2", "3"},
                () -> config.rowsBelowDock, i -> choices.edit().putInt("rows_below_dock", i).apply());
        toggle(R.string.dock_row_label, () -> config.dockRow, on -> choices.edit().putBoolean("dock_row", on).apply());
        toggle(R.string.dock_title_label, () -> config.dockTitle,
                on -> choices.edit().putBoolean("dock_title", on).apply());
        choice(R.string.grid_sort_label, new String[]{getString(R.string.sort_alpha), getString(R.string.sort_usage)},
                () -> Config.SORT_USAGE.equals(config.gridSort) ? 1 : 0,
                i -> choices.edit().putString("grid_sort", i == 1 ? Config.SORT_USAGE : Config.SORT_ALPHA).apply());
        hint(R.string.grid_sort_hint);
        toggle(R.string.app_library_label, () -> config.appLibrary,
                on -> choices.edit().putBoolean("app_library", on).apply());
        toggle(R.string.greeting_label, () -> config.showGreeting,
                on -> choices.edit().putBoolean("show_greeting", on).apply());
        toggle(R.string.stats_label, () -> config.showStats, on -> choices.edit().putBoolean("show_stats", on).apply());

        header(R.string.settings_section_look);
        choice(R.string.background_label, new String[]{getString(R.string.background_black),
                        getString(R.string.background_graphite), getString(R.string.background_navy)},
                this::backgroundIndex,
                i -> choices.edit().putString("background", Config.BACKGROUND_NAMES[i]).apply());
        final int[] sky = {0, 35, 70};
        choice(R.string.sky_label, new String[]{getString(R.string.off), getString(R.string.sky_soft),
                        getString(R.string.sky_strong)},
                () -> config.skyIntensity <= 0 ? 0 : config.skyIntensity < 50 ? 1 : 2,
                i -> choices.edit().putInt("sky_intensity", sky[i]).apply());
        toggleRow(() -> getString(R.string.animations_state, onOff(config.animations)), () -> {
            boolean on = !config.animations;
            SharedPreferences.Editor e = choices.edit().putBoolean("animations", on);
            if (!on) e.putBoolean("advanced_effects", false);
            e.apply();
            changed();
        }, null);
        toggle(R.string.reduced_motion_label, () -> config.reducedMotion,
                on -> choices.edit().putBoolean("reduced_motion", on).apply());
        hint(R.string.reduced_motion_hint);
        toggleRow(() -> getString(R.string.advanced_effects_state,
                onOff(config.animations && config.advancedEffects)), () -> {
            if (!config.animations) return;
            if (config.advancedEffects) {
                choices.edit().putBoolean("advanced_effects", false).apply();
                changed();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle(R.string.advanced_effects)
                    .setMessage(R.string.advanced_effects_warning)
                    .setPositiveButton(R.string.enable, (d, w) -> {
                        choices.edit().putBoolean("advanced_effects", true).apply();
                        changed();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }, row -> {
            row.setEnabled(config.animations);
            row.setAlpha(config.animations ? 1f : 0.5f);
        });
        hint(R.string.advanced_effects_warning);

        header(R.string.settings_section_default_home);
        homeStatus = hint(0);
        row(R.string.make_default_home, this::requestDefaultHome);
        hint(R.string.home_settings_help);

        header(R.string.settings_section_system);
        row(R.string.tv_settings, () -> open(new Intent(Settings.ACTION_SETTINGS)));
        Intent picture = SystemShortcuts.picture(this);
        if (picture != null) row(SystemShortcuts.pictureTitle(picture), () -> open(picture));
        systemRow(R.string.quick_network, new Intent(Settings.ACTION_WIFI_SETTINGS));
        systemRow(R.string.quick_apps, new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
        systemRow(R.string.device_info, new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS));

        header(R.string.settings_section_reset);
        row(R.string.reset_defaults, () -> {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.reset_defaults)
                    .setMessage(R.string.reset_defaults_confirm)
                    .setPositiveButton(R.string.reset_defaults_ok, (d, w) -> {
                        Config.clearChoices(choices);
                        changed();
                        Toast.makeText(this, R.string.reset_defaults_done, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            // Start on Cancel: an OK pressed once too often must not wipe the choices.
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
        });
        hint(R.string.reset_defaults_hint);

        refresh();
        firstFocus.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHomeStatus();   // the Home choice may have changed in the system screen
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == HOME_ROLE_REQUEST) updateHomeStatus();
    }

    // ---- building blocks -------------------------------------------------------------------------

    interface IntChoice { void save(int index); }
    interface BoolChoice { void save(boolean on); }
    interface IntValue { int get(); }
    interface BoolValue { boolean get(); }
    interface TextValue { String get(); }
    interface RowState { void apply(TextView row); }

    private void header(int title) {
        TextView v = (TextView) getLayoutInflater().inflate(R.layout.settings_header, list, false);
        v.setText(title);
        list.addView(v);
    }

    private TextView hint(int text) {
        TextView v = (TextView) getLayoutInflater().inflate(R.layout.settings_hint, list, false);
        if (text != 0) v.setText(text);
        list.addView(v);
        return v;
    }

    private TextView row(int title, Runnable action) {
        TextView v = (TextView) getLayoutInflater().inflate(R.layout.settings_row, list, false);
        v.setText(title);
        v.setOnClickListener(x -> action.run());
        list.addView(v);
        return v;
    }

    private void systemRow(int title, Intent intent) {
        if (SystemShortcuts.resolves(this, intent)) row(title, () -> open(intent));
    }

    /** A row whose text shows its state ("Greeting: On"); OK flips it. */
    private void toggle(int label, BoolValue value, BoolChoice save) {
        toggleRow(() -> getString(R.string.setting_state, getString(label), onOff(value.get())), () -> {
            save.save(!value.get());
            changed();
        }, null);
    }

    private void toggleRow(TextValue text, Runnable action, RowState state) {
        TextView v = (TextView) getLayoutInflater().inflate(R.layout.settings_row, list, false);
        v.setOnClickListener(x -> action.run());
        list.addView(v);
        refreshers.add(() -> {
            v.setText(text.get());
            if (state != null) state.apply(v);
        });
    }

    /** A label and a row of options side by side; the current one is marked. Returns the first option. */
    private View choice(int label, String[] options, IntValue current, IntChoice save) {
        TextView title = (TextView) getLayoutInflater().inflate(R.layout.settings_label, list, false);
        title.setText(label);
        list.addView(title);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Math.round(10 * getResources().getDisplayMetrics().density);
        list.addView(row, lp);
        TextView[] views = new TextView[options.length];
        for (int i = 0; i < options.length; i++) {
            final int index = i;
            TextView v = (TextView) getLayoutInflater().inflate(R.layout.settings_choice, row, false);
            v.setText(options[i]);
            v.setContentDescription(getString(R.string.setting_state, getString(label), options[i]));
            v.setOnClickListener(x -> {
                if (current.get() == index) return;
                save.save(index);
                changed();
            });
            views[i] = v;
        }
        // Hebrew reads right to left: put the options in that order so focus follows the reading direction.
        boolean rtl = getResources().getBoolean(R.bool.rtl_app_grid);
        for (int i = 0; i < views.length; i++) row.addView(views[rtl ? views.length - 1 - i : i]);
        refreshers.add(() -> {
            int now = current.get();
            for (int i = 0; i < views.length; i++) views[i].setSelected(i == now);
        });
        return views[0];
    }

    // ---- state -----------------------------------------------------------------------------------

    /** A choice was saved: reread the effective settings, redraw, and have Home rebuild when this closes. */
    private void changed() {
        config = Config.load(this);
        setResult(RESULT_OK);
        refresh();
    }

    private void refresh() {
        for (Runnable r : refreshers) r.run();
        updateHomeStatus();
    }

    private String onOff(boolean on) {
        return getString(on ? R.string.on : R.string.off);
    }

    private int backgroundIndex() {
        for (int i = 0; i < Config.BACKGROUND_COLORS.length; i++) {
            if (Config.BACKGROUND_COLORS[i] == config.backgroundColor) return i;
        }
        return -1;   // a theme's own colour: none of the three is marked
    }

    private void updateHomeStatus() {
        if (homeStatus == null) return;
        if (SystemShortcuts.isCurrentHome(this)) {
            homeStatus.setText(R.string.home_is_this);
        } else {
            CharSequence current = SystemShortcuts.currentHomeLabel(this);
            homeStatus.setText(current == null ? getString(R.string.home_is_chooser)
                    : getString(R.string.home_is_other, current));
        }
    }

    /**
     * Asks Android, through public APIs only, to make this launcher Home. Firmware with a preinstalled launcher of
     * higher priority may keep sending the Home button there anyway; the dialog then explains the manual route.
     */
    private void requestDefaultHome() {
        if (SystemShortcuts.isCurrentHome(this)) {
            Toast.makeText(this, R.string.home_is_this, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent request = SystemShortcuts.homeRequest(this);
        if (request != null) {
            try {
                if (Settings.ACTION_HOME_SETTINGS.equals(request.getAction())) {
                    Toast.makeText(this, R.string.home_picker_hint, Toast.LENGTH_LONG).show();
                    startActivity(request);
                } else {
                    startActivityForResult(request, HOME_ROLE_REQUEST);
                }
                return;
            } catch (ActivityNotFoundException | SecurityException ignored) {
                // fall through to the explanation
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.make_default_home)
                .setMessage(R.string.home_manual_body)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }
}
