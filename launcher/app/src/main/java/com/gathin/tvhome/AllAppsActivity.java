package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Every launchable app on the TV, TV apps and ordinary Android apps alike: search by name, three orders
 * (A–Z, Most used, Recent), and for each app Open, Add to / Remove from Home and App info.
 *
 * Opened on demand from Home and finished when an app is launched, so it holds no memory while unused. The app
 * list is read on a background thread; icons are loaded as their tiles come on screen.
 */
public final class AllAppsActivity extends Activity {

    static final class App {
        final String pkg;
        final ComponentName component;
        final String label;
        final boolean tv;
        final long installed;

        App(String pkg, ComponentName component, String label, boolean tv, long installed) {
            this.pkg = pkg;
            this.component = component;
            this.label = label;
            this.tv = tv;
            this.installed = installed;
        }
    }

    private static final int SORT_ALPHA = 0, SORT_USAGE = 1, SORT_RECENT = 2;
    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, Drawable> icons = new HashMap<>();
    private List<App> all = Collections.emptyList();
    private final List<App> shown = new ArrayList<>();
    private Set<String> hidden = Collections.emptySet();
    private Set<String> added = Collections.emptySet();
    private Set<String> removed = Collections.emptySet();
    private int sort = SORT_ALPHA;
    private int loadGeneration;
    private boolean firstLoad = true;
    private EditText search;
    private TextView count;
    private TextView empty;
    private RecyclerView grid;
    private final TextView[] sortChoices = new TextView[3];
    private final Adapter adapter = new Adapter();
    private PopupWindow menu;
    private boolean receiverOn;
    private final BroadcastReceiver packages = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())
                    && intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return;
            load();
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_all_apps);
        hidden = new HashSet<>(Config.load(this).hiddenApps);
        search = findViewById(R.id.search);
        count = findViewById(R.id.count);
        empty = findViewById(R.id.empty);
        grid = findViewById(R.id.appGrid);
        sortChoices[SORT_ALPHA] = findViewById(R.id.sortAlpha);
        sortChoices[SORT_USAGE] = findViewById(R.id.sortUsage);
        sortChoices[SORT_RECENT] = findViewById(R.id.sortRecent);
        for (int i = 0; i < sortChoices.length; i++) {
            final int which = i;
            sortChoices[i].setOnClickListener(v -> {
                sort = which;
                showSortChoice();
                applyFilter();
            });
        }
        showSortChoice();

        float density = getResources().getDisplayMetrics().density;
        int widthDp = (int) (getResources().getDisplayMetrics().widthPixels / density) - 112;
        int columns = Math.max(3, Math.min(8, widthDp / 156));
        grid.setLayoutManager(new GridLayoutManager(this, columns));
        grid.setItemAnimator(null);
        grid.setHasFixedSize(true);
        grid.setAdapter(adapter);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int c, int a) { }
            @Override public void onTextChanged(CharSequence s, int start, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { applyFilter(); }
        });
        // The keyboard's Search (or Done) closes it and moves to the first result.
        search.setOnEditorActionListener((v, actionId, event) -> {
            boolean done = actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER);
            if (!done) return false;
            hideKeyboard();
            focusFirstResult();
            return true;
        });
        load();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(packages, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(packages, filter);
        }
        receiverOn = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (receiverOn) unregisterReceiver(packages);
        receiverOn = false;
        if (menu != null) menu.dismiss();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        loader.shutdownNow();
        main.removeCallbacksAndMessages(null);
    }

    /**
     * A letter typed on a remote with a keyboard, while focus is on the apps, goes to the search field. Menu on
     * a tile opens its actions, like holding OK.
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        View focus = getCurrentFocus();
        if (event.getAction() == KeyEvent.ACTION_DOWN && focus != search) {
            int ch = event.getUnicodeChar();
            if (ch > ' ' && !event.isCtrlPressed() && !event.isAltPressed()) {
                search.requestFocus();
                search.append(String.valueOf((char) ch));
                return true;
            }
        }
        if (event.getKeyCode() == KeyEvent.KEYCODE_MENU && focus != null && focus.getParent() == grid) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                int pos = grid.getChildAdapterPosition(focus);
                if (pos != RecyclerView.NO_POSITION && pos < shown.size()) showActions(shown.get(pos), focus);
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    /** Back with text in the search field clears it first; Back again returns to Home. */
    @Override
    public void onBackPressed() {
        if (search.length() > 0) {
            search.setText("");
            focusFirstResult();
            return;
        }
        super.onBackPressed();
    }

    private void load() {
        final int generation = ++loadGeneration;
        final Context app = getApplicationContext();
        final String self = getPackageName();
        loader.execute(() -> {
            List<App> apps = readApps(app, self);
            Set<String> add = LibraryApps.load(app);
            Set<String> rem = LibraryApps.loadRemoved(app);
            LaunchStats.load(app);
            main.post(() -> {
                if (generation != loadGeneration || isDestroyed()) return;
                all = apps;
                added = add;
                removed = rem;
                count.setText(getResources().getQuantityString(R.plurals.library_app_count, apps.size(), apps.size()));
                applyFilter();
                if (firstLoad) {
                    firstLoad = false;
                    focusFirstResult();
                }
            });
        });
    }

    /** Launchable apps, one per package, the TV entry preferred; runs on the loader thread. */
    private static List<App> readApps(Context ctx, String self) {
        PackageManager pm = ctx.getPackageManager();
        Map<String, App> unique = new LinkedHashMap<>();
        for (String category : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            boolean tv = Intent.CATEGORY_LEANBACK_LAUNCHER.equals(category);
            List<ResolveInfo> found;
            try {
                found = pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(category), 0);
            } catch (RuntimeException e) {
                continue;
            }
            for (ResolveInfo info : found) {
                if (info.activityInfo == null || !info.activityInfo.exported) continue;
                String pkg = info.activityInfo.packageName;
                if (pkg.equals(self) || unique.containsKey(pkg)) continue;
                String label;
                try {
                    label = Cards.cleanLabel(String.valueOf(info.loadLabel(pm)), pkg);
                } catch (RuntimeException e) {
                    label = pkg;
                }
                long installed;
                try {
                    installed = pm.getPackageInfo(pkg, 0).firstInstallTime;
                } catch (PackageManager.NameNotFoundException | RuntimeException e) {
                    installed = 0;
                }
                unique.put(pkg, new App(pkg, new ComponentName(pkg, info.activityInfo.name), label, tv, installed));
            }
        }
        return new ArrayList<>(unique.values());
    }

    /** Whether the app has a tile on Home: the same rule AppAdapter applies. */
    private boolean onHome(App app) {
        return LibraryApps.visible(app.tv, hidden.contains(app.pkg), added.contains(app.pkg), removed.contains(app.pkg));
    }

    private void applyFilter() {
        String query = search.getText().toString();
        Map<String, LaunchStats.Entry> stats = LaunchStats.load(this);
        Collator collator = Collator.getInstance();
        shown.clear();
        for (App app : all) if (AppSearch.matches(query, app.label, app.pkg)) shown.add(app);
        shown.sort((a, b) -> {
            int c = AppSearch.compareRelevance(query, a.label, b.label);
            if (c == 0 && sort == SORT_USAGE) c = LaunchStats.compareUsage(stats, a.pkg, b.pkg);
            if (c == 0 && sort == SORT_RECENT) c = LaunchStats.compareRecent(stats, a.pkg, b.pkg);
            if (c == 0) c = collator.compare(a.label, b.label);
            return c != 0 ? c : a.pkg.compareTo(b.pkg);
        });
        adapter.notifyDataSetChanged();
        boolean none = shown.isEmpty();
        empty.setVisibility(none ? View.VISIBLE : View.GONE);
        if (none) {
            empty.setText(all.isEmpty() ? getString(R.string.library_empty)
                    : getString(R.string.search_no_results, query.trim()));
        }
    }

    private void showSortChoice() {
        for (int i = 0; i < sortChoices.length; i++) sortChoices[i].setActivated(i == sort);
    }

    private void focusFirstResult() {
        grid.scrollToPosition(0);
        grid.post(() -> {
            RecyclerView.ViewHolder first = grid.findViewHolderForAdapterPosition(0);
            if (first != null) first.itemView.requestFocus();
            else sortChoices[sort].requestFocus();
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = getSystemService(InputMethodManager.class);
        if (imm != null) imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
    }

    private void open(App app) {
        Intent intent = new Intent(Intent.ACTION_MAIN).setComponent(app.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        // TV apps keep the leanback launch intent, so an app already running resumes instead of restarting.
        if (app.tv) intent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        try {
            startActivity(intent);
            LaunchStats.record(this, app.pkg);
            finish();   // Back and Home from the app return to Home, and this screen frees its memory
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, getString(R.string.cant_open, app.label), Toast.LENGTH_SHORT).show();
        }
    }

    private void showActions(App app, View anchor) {
        if (menu != null) menu.dismiss();
        boolean home = onHome(app);
        menu = new ActionMenu(this)
                .add(R.string.library_open, () -> open(app))
                .add(home ? R.string.library_remove_shortcut : R.string.library_add_home, () -> {
                    if (!LibraryApps.save(this, app.pkg, !home)) {
                        Toast.makeText(this, R.string.library_save_failed, Toast.LENGTH_LONG).show();
                        return;
                    }
                    Toast.makeText(this, getString(home ? R.string.removed_from_home : R.string.added_to_home,
                            app.label), Toast.LENGTH_SHORT).show();
                    load();
                })
                .add(R.string.library_app_info, () -> openAppInfo(this, app.pkg, app.label))
                .show(anchor);
        menu.setOnDismissListener(() -> {
            menu = null;
            if (anchor.isAttachedToWindow()) anchor.requestFocus();
        });
    }

    /** The system's page for one app (stop, clear data, uninstall, permissions). */
    static void openAppInfo(Context ctx, String pkg, String label) {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            ctx.startActivity(i);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(ctx, ctx.getString(R.string.cant_open, label), Toast.LENGTH_SHORT).show();
        }
    }

    private Drawable icon(App app) {
        Drawable d = icons.get(app.pkg);
        if (d != null) return d;
        try {
            d = getPackageManager().getActivityIcon(app.component);
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            d = getDrawable(R.drawable.ic_apps);
        }
        icons.put(app.pkg, d);
        return d;
    }

    private final class Adapter extends RecyclerView.Adapter<Tile> {
        Adapter() {
            setHasStableIds(true);
        }

        @Override public long getItemId(int position) { return shown.get(position).pkg.hashCode(); }

        @Override public int getItemCount() { return shown.size(); }

        @NonNull
        @Override
        public Tile onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_all_app, parent, false);
            return new Tile(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Tile h, int position) {
            App app = shown.get(position);
            h.icon.setImageDrawable(icon(app));
            h.name.setText(app.label);
            boolean home = onHome(app);
            boolean fresh = LibraryApps.recent(app.installed, System.currentTimeMillis());
            if (home) h.status.setText(R.string.library_on_home);
            else if (fresh) h.status.setText(R.string.badge_new);
            else h.status.setText("");
            h.itemView.setContentDescription(home ? getString(R.string.a11y_app_on_home, app.label) : app.label);
            h.itemView.setOnClickListener(v -> open(app));
            h.itemView.setOnLongClickListener(v -> {
                showActions(app, v);
                return true;
            });
        }
    }

    private static final class Tile extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name;
        final TextView status;

        Tile(View v) {
            super(v);
            icon = v.findViewById(R.id.icon);
            name = v.findViewById(R.id.title);
            status = v.findViewById(R.id.status);
            v.setOnFocusChangeListener((view, focused) -> view.animate().scaleX(focused ? 1.06f : 1f)
                    .scaleY(focused ? 1.06f : 1f).setDuration(120).start());
        }
    }
}
