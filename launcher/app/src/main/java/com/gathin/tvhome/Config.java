package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The launcher's settings: the values in res/values/config.xml, with an optional overlay read from
 * {@code config.json} in the app's external files directory
 * ({@code /sdcard/Android/data/com.gathin.tvhome/files/}). Every key in the JSON is optional and an
 * unknown or badly typed key is ignored, so a theme can change the dock and the look without a rebuild:
 * {@code tools/theme.sh themes/<name>} pushes a theme folder there and restarts Home.
 *
 * Read once in {@link MainActivity#onCreate}; nothing here touches the network.
 */
final class Config {
    private static final String TAG = "TvHome.Config";
    static final String FILE_NAME = "config.json";
    static final String WALLPAPER_NAME = "wallpaper.jpg";
    static final String SETTINGS_NAME = "launcher_settings";
    private static final String THEME_REVISION_NAME = "theme_revision";
    private static final String THEME_SIGNATURE_KEY = "theme_signature";

    /**
     * Every choice LauncherSettingsActivity can store on the TV. A theme push or reset, and "Restore defaults",
     * clear all of them, so the theme or config.xml applies again.
     */
    static final String[] CHOICE_KEYS = {
            "columns", "animations", "advanced_effects", "reduced_motion", "rows_below_dock", "dock_row",
            "dock_title", "grid_sort", "app_library", "show_greeting", "show_stats", "background", "sky_intensity",
    };
    /** grid_sort values: alphabetical (the original order) or by how often apps were opened from this launcher. */
    static final String SORT_ALPHA = "alpha", SORT_USAGE = "usage";
    /** background values and the colour each stands for. */
    static final String[] BACKGROUND_NAMES = {"black", "graphite", "navy"};
    static final int[] BACKGROUND_COLORS = {0xFF000000, 0xFF14171C, 0xFF0B1424};

    final List<String> dockApps;
    /** Grid apps fill the dock slots dock_apps leaves free (config.xml, fill_dock); on by default, false leaves the slots empty. */
    final boolean fillDock;
    final List<String> hiddenApps;
    final String liveTvPackage;
    final String liveTvAction;
    final String sourcePanelService;
    final String sourcePanelAction;
    final String inputAction;
    final int columns;
    final boolean animations;
    final boolean advancedEffects;
    final int rowsBelowDock;
    final boolean shamsiDate;
    /** Name after the greeting ("Good morning, Mr. Trịnh"); empty for the greeting alone. */
    final String greetingName;
    final boolean moduleCards;
    /** Calmer motion for weak GPUs (config.xml, reduced_motion): stiff spring, short fades, no replay. */
    final boolean reducedMotion;
    /** The focused dock app's name in large type above the dock (config.xml, dock_title). */
    final boolean dockTitle;
    /** The dock as its own row: band, no names, parked low (config.xml dock_row). false: one plain grid. */
    final boolean dockRow;
    /** Reorder button in the top bar (config.xml reorder_button); the Settings long-press menu offers it either way. */
    final boolean reorderButton;
    final String vpnApp;
    final boolean showHomeSettingsShortcut;
    /** One line under the clock: CPU, free memory, free storage. Off unless config or a theme turns it on. */
    final boolean showStats;
    /** App library pill after the last Home row (config.xml app_library). */
    final boolean appLibrary;
    /** Background colour behind everything, #RRGGBB; black unless a theme says otherwise. */
    final int backgroundColor;
    /** Time-of-day backdrop, 0 to 100 (config.xml, sky_intensity); 0 keeps the plain background. */
    final int skyIntensity;
    /** Remote keycodes that open Settings through HomeKeyService (config.xml, settings_key_keycodes). */
    final int[] settingsKeycodes;
    /** The Settings button opens the quick settings panel instead of the TV's Settings (settings_key_panel). */
    final boolean settingsKeyPanel;
    /** Full-screen picture behind the launcher, or null for the plain colour. */
    final File wallpaper;
    /** Whether the overlay file was present and parsed. */
    final boolean overlayApplied;
    /** Festive skin from a theme (festival, festival_until, greeting, accent_color); {@link Festival#NONE} if none. */
    final Festival festival;
    /** The greeting above the clock (config.xml show_greeting); false hides the line. */
    final boolean showGreeting;
    /** Grid apps after the dock: {@link #SORT_ALPHA} or {@link #SORT_USAGE} (config.xml grid_sort). */
    final String gridSort;

    private Config(List<String> dockApps, boolean fillDock, List<String> hiddenApps, String liveTvPackage,
                   String liveTvAction, String sourcePanelService, String sourcePanelAction, String inputAction,
                   int columns, int rowsBelowDock, boolean animations, boolean advancedEffects,
                   boolean shamsiDate, String greetingName, boolean moduleCards, boolean reducedMotion, boolean dockTitle, boolean dockRow, boolean reorderButton, String vpnApp,
                   boolean showHomeSettingsShortcut, boolean showStats, boolean appLibrary, int backgroundColor, int skyIntensity,
                   int[] settingsKeycodes, boolean settingsKeyPanel, File wallpaper, boolean overlayApplied,
                   Festival festival, boolean showGreeting, String gridSort) {
        this.showGreeting = showGreeting;
        this.gridSort = gridSort;
        this.dockApps = Collections.unmodifiableList(dockApps);
        this.fillDock = fillDock;
        this.hiddenApps = Collections.unmodifiableList(hiddenApps);
        this.liveTvPackage = liveTvPackage;
        this.liveTvAction = liveTvAction;
        this.sourcePanelService = sourcePanelService;
        this.sourcePanelAction = sourcePanelAction;
        this.inputAction = inputAction;
        this.columns = columns;
        this.rowsBelowDock = rowsBelowDock;
        this.animations = animations;
        this.advancedEffects = advancedEffects;
        this.shamsiDate = shamsiDate;
        this.greetingName = greetingName;
        this.moduleCards = moduleCards;
        this.reducedMotion = reducedMotion;
        this.dockTitle = dockTitle;
        this.dockRow = dockRow;
        this.reorderButton = reorderButton;
        this.vpnApp = vpnApp;
        this.showHomeSettingsShortcut = showHomeSettingsShortcut;
        this.showStats = showStats;
        this.appLibrary = appLibrary;
        this.backgroundColor = backgroundColor;
        this.skyIntensity = skyIntensity;
        this.settingsKeycodes = settingsKeycodes;
        this.settingsKeyPanel = settingsKeyPanel;
        this.wallpaper = wallpaper;
        this.overlayApplied = overlayApplied;
        this.festival = festival;
    }

    /** The folder the theme tools push into; null when external storage is unavailable. */
    static File dir(Context ctx) {
        return ctx.getExternalFilesDir(null);
    }

    static Config load(Context ctx) {
        Resources res = ctx.getResources();
        List<String> dock = new ArrayList<>(Arrays.asList(res.getStringArray(R.array.dock_apps)));
        boolean fillDock = res.getBoolean(R.bool.fill_dock);
        List<String> hidden = new ArrayList<>(Arrays.asList(res.getStringArray(R.array.hidden_apps)));
        String liveTv = res.getString(R.string.live_tv_package).trim();
        String liveTvAction = res.getString(R.string.live_tv_action).trim();
        String panelService = res.getString(R.string.source_panel_service).trim();
        String panelAction = res.getString(R.string.source_panel_action).trim();
        String inputAction = res.getString(R.string.input_action).trim();
        int columns = res.getInteger(R.integer.columns);
        int rowsBelow = res.getInteger(R.integer.rows_below_dock);
        boolean animations = res.getBoolean(R.bool.animations);
        boolean advancedEffects = res.getBoolean(R.bool.advanced_effects);
        boolean shamsi = res.getBoolean(R.bool.shamsi_date);
        String greetingName = res.getString(R.string.greeting_name).trim();
        boolean moduleCards = res.getBoolean(R.bool.module_cards);
        boolean reducedMotion = res.getBoolean(R.bool.reduced_motion);
        boolean dockTitle = res.getBoolean(R.bool.dock_title);
        boolean dockRow = res.getBoolean(R.bool.dock_row);
        boolean reorderButton = res.getBoolean(R.bool.reorder_button);
        String vpn = res.getString(R.string.vpn_app).trim();
        boolean homeSettings = res.getBoolean(R.bool.show_home_settings_shortcut);
        boolean showStats = res.getBoolean(R.bool.show_stats);
        boolean library = res.getBoolean(R.bool.app_library);
        int background = 0xFF000000;
        int skyIntensity = res.getInteger(R.integer.sky_intensity);
        int[] settingsKeycodes = res.getIntArray(R.array.settings_key_keycodes);
        boolean settingsKeyPanel = res.getBoolean(R.bool.settings_key_panel);
        boolean showGreeting = res.getBoolean(R.bool.show_greeting);
        String gridSort = res.getString(R.string.grid_sort).trim();
        boolean applied = false;

        File dir = dir(ctx);
        File file = dir != null ? new File(dir, FILE_NAME) : null;
        JSONObject json = read(file);
        if (json != null) {
            applied = true;
            dock = stringList(json, "dock_apps", dock);
            fillDock = bool(json, "fill_dock", fillDock);
            hidden = stringList(json, "hidden_apps", hidden);
            liveTv = string(json, "live_tv_package", liveTv);
            liveTvAction = string(json, "live_tv_action", liveTvAction);
            panelService = string(json, "source_panel_service", panelService);
            panelAction = string(json, "source_panel_action", panelAction);
            inputAction = string(json, "input_action", inputAction);
            columns = integer(json, "columns", columns, 1, 8);
            rowsBelow = integer(json, "rows_below_dock", rowsBelow, 0, 3);
            animations = bool(json, "animations", animations);
            advancedEffects = bool(json, "advanced_effects", advancedEffects);
            shamsi = bool(json, "shamsi_date", shamsi);
            greetingName = string(json, "greeting_name", greetingName).trim();
            moduleCards = bool(json, "module_cards", moduleCards);
            reducedMotion = bool(json, "reduced_motion", reducedMotion);
            dockTitle = bool(json, "dock_title", dockTitle);
            dockRow = bool(json, "dock_row", dockRow);
            reorderButton = bool(json, "reorder_button", reorderButton);
            vpn = string(json, "vpn_app", vpn);
            homeSettings = bool(json, "show_home_settings_shortcut", homeSettings);
            showStats = bool(json, "show_stats", showStats);
            library = bool(json, "app_library", library);
            background = color(json, "background_color", background);
            skyIntensity = integer(json, "sky_intensity", skyIntensity, 0, 100);
            settingsKeycodes = intArray(json, "settings_key_keycodes", settingsKeycodes);
            settingsKeyPanel = bool(json, "settings_key_panel", settingsKeyPanel);
            showGreeting = bool(json, "show_greeting", showGreeting);
            gridSort = string(json, "grid_sort", gridSort);
        }
        // A theme push or reset invalidates choices made on the TV. On first use, remember the
        // current theme without clearing choices from an older launcher version.
        SharedPreferences choices = ctx.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE);
        syncThemeChoices(choices, dir, file);
        int chosenColumns = choices.getInt("columns", columns);
        if (chosenColumns >= 4 && chosenColumns <= 7) columns = chosenColumns;
        animations = choices.getBoolean("animations", animations);
        advancedEffects = choices.getBoolean("advanced_effects", advancedEffects);
        reducedMotion = choices.getBoolean("reduced_motion", reducedMotion);
        int chosenRows = choices.getInt("rows_below_dock", rowsBelow);
        if (chosenRows >= 0 && chosenRows <= 3) rowsBelow = chosenRows;
        dockRow = choices.getBoolean("dock_row", dockRow);
        dockTitle = choices.getBoolean("dock_title", dockTitle);
        gridSort = choices.getString("grid_sort", gridSort);
        library = choices.getBoolean("app_library", library);
        showGreeting = choices.getBoolean("show_greeting", showGreeting);
        showStats = choices.getBoolean("show_stats", showStats);
        background = backgroundColor(choices.getString("background", null), background);
        int chosenSky = choices.getInt("sky_intensity", skyIntensity);
        if (chosenSky >= 0 && chosenSky <= 100) skyIntensity = chosenSky;
        if (!SORT_USAGE.equals(gridSort)) gridSort = SORT_ALPHA;
        File wallpaper = dir != null ? new File(dir, WALLPAPER_NAME) : null;
        if (wallpaper != null && !(wallpaper.isFile() && wallpaper.length() > 0)) wallpaper = null;
        // A theme can also switch the wallpaper off while the file is still there.
        if (json != null && json.has("wallpaper") && !bool(json, "wallpaper", true)) wallpaper = null;
        // A festive skin comes only from a theme; config.xml has no festival.
        Festival festival = json == null ? Festival.NONE : Festival.parse(string(json, "festival", ""),
                string(json, "festival_until", ""), string(json, "greeting", ""), string(json, "accent_color", ""));

        return new Config(dock, fillDock, hidden, liveTv, liveTvAction, panelService, panelAction, inputAction,
                Math.max(1, columns), Math.max(0, rowsBelow), animations, advancedEffects,
                shamsi, greetingName, moduleCards, reducedMotion, dockTitle, dockRow, reorderButton, vpn, homeSettings, showStats, library, background,
                Math.max(0, Math.min(100, skyIntensity)), settingsKeycodes, settingsKeyPanel, wallpaper, applied,
                festival, showGreeting, gridSort);
    }

    /** The colour for a stored background name; any other value keeps the theme's or config.xml's colour. */
    static int backgroundColor(String name, int fallback) {
        for (int i = 0; i < BACKGROUND_NAMES.length; i++) {
            if (BACKGROUND_NAMES[i].equals(name)) return BACKGROUND_COLORS[i];
        }
        return fallback;
    }

    /** Whether any choice made on the TV is stored. */
    static boolean hasChoices(SharedPreferences choices) {
        for (String key : CHOICE_KEYS) if (choices.contains(key)) return true;
        return false;
    }

    /** Removes every choice made on the TV (Restore defaults). Synchronous: Home is rebuilt right after. */
    static void clearChoices(SharedPreferences choices) {
        clearChoices(choices.edit()).commit();
    }

    private static SharedPreferences.Editor clearChoices(SharedPreferences.Editor editor) {
        for (String key : CHOICE_KEYS) editor.remove(key);
        return editor;
    }

    private static void syncThemeChoices(SharedPreferences choices, File dir, File config) {
        File revision = dir == null ? null : new File(dir, THEME_REVISION_NAME);
        String token = revisionToken(revision);
        String signature = fileStamp(config) + ":" + token;
        String seen = choices.getString(THEME_SIGNATURE_KEY, null);
        if (seen == null) {
            // Existing preferences survive an app upgrade, unless the first launch follows an
            // explicit theme push or reset by the updated tools.
            SharedPreferences.Editor editor = choices.edit();
            if (!"absent".equals(token)) clearChoices(editor);
            editor.putString(THEME_SIGNATURE_KEY, signature).commit();
        } else if (!seen.equals(signature)) {
            clearChoices(choices.edit()).putString(THEME_SIGNATURE_KEY, signature).commit();
        }
    }

    private static String fileStamp(File file) {
        return file != null && file.isFile() ? file.length() + "@" + file.lastModified() : "absent";
    }

    private static String revisionToken(File file) {
        if (file == null || !file.isFile() || file.length() > 64) return "absent";
        try (InputStream in = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            int n = in.read(bytes);
            return n > 0 ? new String(bytes, 0, n, StandardCharsets.US_ASCII) : "absent";
        } catch (Exception ignored) {
            return "absent";
        }
    }

    /** Reads and parses the overlay; any problem means "no overlay", never a crash on the home screen. */
    private static JSONObject read(File file) {
        if (file == null || !file.isFile()) return null;
        if (file.length() > 64 * 1024) {
            Log.w(TAG, "ignoring " + file + ": larger than 64 KB");
            return null;
        }
        try (InputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[(int) file.length()];
            int n = 0, r;
            while (n < buf.length && (r = in.read(buf, n, buf.length - n)) > 0) n += r;
            String text = new String(buf, 0, n, StandardCharsets.UTF_8);
            if (text.startsWith("\uFEFF")) text = text.substring(1);   // Windows editors add a BOM
            return new JSONObject(text);
        } catch (Exception e) {
            Log.w(TAG, "ignoring " + file + ": " + e.getMessage());
            return null;
        }
    }

    private static String string(JSONObject o, String key, String fallback) {
        Object v = o.opt(key);
        return v instanceof String ? ((String) v).trim() : fallback;
    }

    private static boolean bool(JSONObject o, String key, boolean fallback) {
        Object v = o.opt(key);
        return v instanceof Boolean ? (Boolean) v : fallback;
    }

    private static int integer(JSONObject o, String key, int fallback, int min, int max) {
        Object v = o.opt(key);
        if (!(v instanceof Number)) return fallback;
        int i = ((Number) v).intValue();
        return i < min || i > max ? fallback : i;
    }

    private static int color(JSONObject o, String key, int fallback) {
        Object v = o.opt(key);
        if (!(v instanceof String)) return fallback;
        try {
            int c = Color.parseColor(((String) v).trim());
            return c | 0xFF000000;   // always opaque: the window behind is black anyway
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /** Positive keycodes only (vendor remotes use codes in the thousands); a list with anything else in it is ignored as a whole. */
    private static int[] intArray(JSONObject o, String key, int[] fallback) {
        Object v = o.opt(key);
        if (!(v instanceof JSONArray)) return fallback;
        JSONArray a = (JSONArray) v;
        int[] out = new int[a.length()];
        for (int i = 0; i < a.length(); i++) {
            Object item = a.opt(i);
            if (!(item instanceof Number)) return fallback;
            out[i] = ((Number) item).intValue();
            if (out[i] < 1) return fallback;
        }
        return out;
    }

    private static List<String> stringList(JSONObject o, String key, List<String> fallback) {
        Object v = o.opt(key);
        if (!(v instanceof JSONArray)) return fallback;
        JSONArray a = (JSONArray) v;
        List<String> out = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) {
            Object item = a.opt(i);
            if (item instanceof String && !((String) item).trim().isEmpty()) out.add(((String) item).trim());
        }
        return out;
    }
}
