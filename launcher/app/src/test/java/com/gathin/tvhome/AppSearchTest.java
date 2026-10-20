package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppSearchTest {

    @Test public void accentsCaseAndExtraSpacesDoNotMatter() {
        assertEquals("musica e video", AppSearch.normalize("  Música   e  VÍDEO "));
        assertTrue(AppSearch.matches("musica", "Música", "com.example.music"));
        assertTrue(AppSearch.matches("GLOBO", "Globoplay", "com.globo.globotv"));
        assertTrue(AppSearch.matches("conexao", "Conexão", "com.example"));
    }

    @Test public void everyWordMustAppearInTheNameOrThePackage() {
        assertTrue(AppSearch.matches("prime video", "Prime Video", "com.amazon.amazonvideo.livingroom"));
        assertTrue(AppSearch.matches("amazon video", "Prime Video", "com.amazon.amazonvideo.livingroom"));
        assertFalse(AppSearch.matches("prime netflix", "Prime Video", "com.amazon.amazonvideo.livingroom"));
    }

    @Test public void anEmptyQueryMatchesEverything() {
        assertTrue(AppSearch.matches("", "Netflix", "com.netflix.ninja"));
        assertTrue(AppSearch.matches("   ", "Netflix", "com.netflix.ninja"));
        assertTrue(AppSearch.matches(null, "Netflix", "com.netflix.ninja"));
    }

    @Test public void nullNamesAreSafe() {
        assertFalse(AppSearch.matches("x", null, null));
        assertEquals("", AppSearch.normalize(null));
    }

    @Test public void namesStartingWithTheQueryComeFirst() {
        assertTrue(AppSearch.compareRelevance("you", "YouTube", "Spotify You") < 0);
        assertTrue(AppSearch.compareRelevance("you", "Spotify You", "YouTube") > 0);
        assertEquals(0, AppSearch.compareRelevance("you", "YouTube", "YouTube Music"));
        assertEquals(0, AppSearch.compareRelevance("", "B", "A"));
    }
}
