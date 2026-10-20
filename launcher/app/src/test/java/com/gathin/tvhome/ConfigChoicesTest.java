package com.gathin.tvhome;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConfigChoicesTest {

    @Test public void knownBackgroundsMapToTheirColoursAndAnythingElseKeepsTheTheme() {
        assertEquals(0xFF000000, Config.backgroundColor("black", 0xFF123456));
        assertEquals(0xFF14171C, Config.backgroundColor("graphite", 0xFF123456));
        assertEquals(0xFF0B1424, Config.backgroundColor("navy", 0xFF123456));
        assertEquals(0xFF123456, Config.backgroundColor(null, 0xFF123456));
        assertEquals(0xFF123456, Config.backgroundColor("purple", 0xFF123456));
    }

    @Test public void everyBackgroundNameHasAColour() {
        assertEquals(Config.BACKGROUND_NAMES.length, Config.BACKGROUND_COLORS.length);
    }

    /** Restore defaults and a theme push clear these; a key missing here would survive both. */
    @Test public void everySettingTheScreenWritesIsAResettableChoice() {
        List<String> keys = Arrays.asList(Config.CHOICE_KEYS);
        assertEquals(keys.size(), new HashSet<>(keys).size());
        for (String key : new String[]{"columns", "animations", "advanced_effects", "reduced_motion",
                "rows_below_dock", "dock_row", "dock_title", "grid_sort", "app_library", "show_greeting",
                "show_stats", "background", "sky_intensity"}) {
            assertTrue(key, keys.contains(key));
        }
    }
}
