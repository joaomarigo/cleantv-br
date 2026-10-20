package com.gathin.tvhome;

import android.content.Context;
import android.util.AtomicFile;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * How often, and when last, each app was opened from this launcher (Home tiles and All apps). It feeds the
 * "Most used" and "Recent" orders. Only this launcher's own launches are counted: reading other apps' usage
 * would need the special PACKAGE_USAGE_STATS access, which a Home screen should not ask for.
 *
 * One small text file in the app's private storage, a line per package: {@code pkg<TAB>count<TAB>lastMillis}.
 * Writes go to one background thread; a corrupt file reads as empty and is replaced by the next launch.
 */
final class LaunchStats {
    private static final String TAG = "TvHome.Stats";
    private static final String FILE = "launch_stats.txt";
    /** Packages kept; the least recently opened go first when there are more. */
    static final int MAX_ENTRIES = 200;
    private static final int MAX_BYTES = MAX_ENTRIES * 300;
    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "launch-stats");
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });
    /** The last state read or written in this process, so Home does not re-read the file on every rebuild. */
    private static volatile Map<String, Entry> cache;

    static final class Entry {
        final int count;
        final long last;

        Entry(int count, long last) {
            this.count = count;
            this.last = last;
        }
    }

    private LaunchStats() { }

    private static AtomicFile store(Context ctx) {
        return new AtomicFile(new File(ctx.getFilesDir(), FILE));
    }

    /** Current counts; empty when nothing was recorded yet or the file cannot be read. */
    static Map<String, Entry> load(Context ctx) {
        Map<String, Entry> c = cache;
        if (c != null) return c;
        c = Collections.unmodifiableMap(read(store(ctx.getApplicationContext())));
        cache = c;
        return c;
    }

    /** Counts one launch of {@code pkg} now. Returns at once; the file is written in the background. */
    static void record(Context ctx, String pkg) {
        if (!LibraryApps.validPackage(pkg)) return;
        final Context app = ctx.getApplicationContext();
        final long now = System.currentTimeMillis();
        Map<String, Entry> next = withLaunch(load(app), pkg, now);
        cache = Collections.unmodifiableMap(next);
        WRITER.execute(() -> write(store(app), next));
    }

    /** A copy of {@code stats} with one more launch of {@code pkg} at {@code now}, trimmed to MAX_ENTRIES. */
    static Map<String, Entry> withLaunch(Map<String, Entry> stats, String pkg, long now) {
        Map<String, Entry> next = new HashMap<>(stats);
        Entry old = next.get(pkg);
        int count = old == null ? 1 : (old.count >= Integer.MAX_VALUE - 1 ? old.count : old.count + 1);
        next.put(pkg, new Entry(count, now));
        if (next.size() > MAX_ENTRIES) {
            List<Map.Entry<String, Entry>> byAge = new ArrayList<>(next.entrySet());
            byAge.sort((a, b) -> Long.compare(a.getValue().last, b.getValue().last));
            for (int i = 0; i < byAge.size() - MAX_ENTRIES; i++) next.remove(byAge.get(i).getKey());
        }
        return next;
    }

    /** Most used first: more launches, then the more recent; apps never opened here keep their order (0). */
    static int compareUsage(Map<String, Entry> stats, String a, String b) {
        Entry ea = stats.get(a), eb = stats.get(b);
        int ca = ea == null ? 0 : ea.count, cb = eb == null ? 0 : eb.count;
        if (ca != cb) return Integer.compare(cb, ca);
        long la = ea == null ? 0 : ea.last, lb = eb == null ? 0 : eb.last;
        return Long.compare(lb, la);
    }

    /** Most recently opened first; apps never opened here after them, in their order (0). */
    static int compareRecent(Map<String, Entry> stats, String a, String b) {
        Entry ea = stats.get(a), eb = stats.get(b);
        long la = ea == null ? 0 : ea.last, lb = eb == null ? 0 : eb.last;
        return Long.compare(lb, la);
    }

    static String encode(Map<String, Entry> stats) {
        StringBuilder text = new StringBuilder();
        List<String> keys = new ArrayList<>(stats.keySet());
        Collections.sort(keys);
        for (String pkg : keys) {
            Entry e = stats.get(pkg);
            text.append(pkg).append('\t').append(e.count).append('\t').append(e.last).append('\n');
        }
        return text.toString();
    }

    /** Parses the file; any malformed line makes the whole file count as empty rather than half-read. */
    static Map<String, Entry> decode(String text) {
        Map<String, Entry> out = new HashMap<>();
        if (text.length() > MAX_BYTES) throw new IllegalArgumentException("stats file too large");
        for (String line : text.split("\n")) {
            if (line.isEmpty()) continue;
            String[] parts = line.split("\t");
            if (parts.length != 3 || !LibraryApps.validPackage(parts[0])) {
                throw new IllegalArgumentException("bad stats line");
            }
            int count = Integer.parseInt(parts[1]);
            long last = Long.parseLong(parts[2]);
            if (count < 1 || last < 0) throw new IllegalArgumentException("bad stats values");
            out.put(parts[0], new Entry(count, last));
            if (out.size() > MAX_ENTRIES) throw new IllegalArgumentException("too many stats entries");
        }
        return out;
    }

    private static Map<String, Entry> read(AtomicFile file) {
        if (!file.getBaseFile().exists() && !new File(file.getBaseFile() + ".bak").exists()) return new HashMap<>();
        try (FileInputStream input = file.openRead()) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            int n;
            while ((n = input.read(buffer)) != -1) {
                if (bytes.size() + n > MAX_BYTES) throw new IOException("stats file too large");
                bytes.write(buffer, 0, n);
            }
            return decode(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            Log.w(TAG, "Ignoring unreadable launch stats", e);
            return new HashMap<>();
        }
    }

    private static void write(AtomicFile file, Map<String, Entry> stats) {
        FileOutputStream out = null;
        try {
            out = file.startWrite();
            out.write(encode(stats).getBytes(StandardCharsets.UTF_8));
            file.finishWrite(out);
        } catch (IOException | RuntimeException e) {
            if (out != null) file.failWrite(out);
            Log.w(TAG, "Could not save launch stats", e);
        }
    }
}
