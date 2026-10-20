package com.gathin.tvhome;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LaunchStatsTest {

    @Test public void aLaunchCountsOnceAndRemembersWhen() {
        Map<String, LaunchStats.Entry> s = LaunchStats.withLaunch(new HashMap<>(), "com.netflix.ninja", 100);
        s = LaunchStats.withLaunch(s, "com.netflix.ninja", 250);
        assertEquals(2, s.get("com.netflix.ninja").count);
        assertEquals(250, s.get("com.netflix.ninja").last);
    }

    @Test public void theOriginalMapIsNotChanged() {
        Map<String, LaunchStats.Entry> before = new HashMap<>();
        LaunchStats.withLaunch(before, "com.netflix.ninja", 1);
        assertTrue(before.isEmpty());
    }

    @Test public void mostUsedThenMostRecentThenUnknownApps() {
        Map<String, LaunchStats.Entry> s = new HashMap<>();
        s.put("a.used", new LaunchStats.Entry(5, 10));
        s.put("b.used", new LaunchStats.Entry(5, 20));
        s.put("c.once", new LaunchStats.Entry(1, 99));
        assertTrue(LaunchStats.compareUsage(s, "b.used", "a.used") < 0);   // same count: the more recent first
        assertTrue(LaunchStats.compareUsage(s, "a.used", "c.once") < 0);
        assertTrue(LaunchStats.compareUsage(s, "c.once", "never.opened") < 0);
        assertEquals(0, LaunchStats.compareUsage(s, "x.never", "y.never"));
    }

    @Test public void recentOrdersByLastLaunchOnly() {
        Map<String, LaunchStats.Entry> s = new HashMap<>();
        s.put("a.many", new LaunchStats.Entry(50, 10));
        s.put("b.once", new LaunchStats.Entry(1, 20));
        assertTrue(LaunchStats.compareRecent(s, "b.once", "a.many") < 0);
        assertTrue(LaunchStats.compareRecent(s, "a.many", "z.never") < 0);
    }

    @Test public void theOldestAppsAreDroppedBeyondTheLimit() {
        Map<String, LaunchStats.Entry> s = new HashMap<>();
        for (int i = 0; i < LaunchStats.MAX_ENTRIES; i++) s.put("com.example.app" + i, new LaunchStats.Entry(1, 1000 + i));
        s = LaunchStats.withLaunch(s, "com.example.fresh", 5000);
        assertEquals(LaunchStats.MAX_ENTRIES, s.size());
        assertFalse(s.containsKey("com.example.app0"));
        assertTrue(s.containsKey("com.example.fresh"));
    }

    @Test public void theFileRoundTrips() {
        Map<String, LaunchStats.Entry> s = new HashMap<>();
        s.put("com.globo.globotv", new LaunchStats.Entry(3, 1760000000000L));
        s.put("com.netflix.ninja", new LaunchStats.Entry(1, 5));
        String text = LaunchStats.encode(s);
        assertEquals("com.globo.globotv\t3\t1760000000000\ncom.netflix.ninja\t1\t5\n", text);
        Map<String, LaunchStats.Entry> back = LaunchStats.decode(text);
        assertEquals(2, back.size());
        assertEquals(3, back.get("com.globo.globotv").count);
        assertEquals(5, back.get("com.netflix.ninja").last);
        assertTrue(LaunchStats.decode("").isEmpty());
    }

    @Test public void malformedFilesAreRejectedWhole() {
        for (String raw : new String[]{"com.example\t1\n", "../x\t1\t1\n", "com.example\tx\t1\n",
                "com.example\t0\t1\n", "com.example\t1\t-1\n", "com.example\t1\t1\textra\n"}) {
            try {
                LaunchStats.decode(raw);
                fail(raw);
            } catch (RuntimeException expected) {
            }
        }
    }
}
