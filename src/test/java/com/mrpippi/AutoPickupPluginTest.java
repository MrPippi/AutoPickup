package com.autopickup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Characterization tests: records the current behaviour of AutoPickupPlugin.legacyToMiniMessage. */
class AutoPickupPluginTest {

    private static String convert(String s) {
        return AutoPickupPlugin.legacyToMiniMessage(s);
    }

    @Test
    void nullAndEmptyBecomeEmpty() {
        assertEquals("", convert(null));
        assertEquals("", convert(""));
    }

    @Test
    void ampersandAndSectionCodesBecomeTags() {
        assertEquals("<green>Auto-pickup has been <white>enabled<green>.",
                convert("&aAuto-pickup has been &fenabled&a."));
        assertEquals("<red>X", convert("§cX"));
        assertEquals("<bold><italic><reset>", convert("&l&o&r"));
    }

    @Test
    void codesAreCaseInsensitive() {
        assertEquals("<green>Hi", convert("&AHi"));
    }

    @Test
    void hexCodesBecomeHexTags() {
        assertEquals("<#00ff00>x", convert("&#00ff00x"));
        assertEquals("<#ABCDEF>", convert("&#ABCDEF"));
    }

    @Test
    void invalidOrTruncatedHexIsLeftAsIs() {
        assertEquals("&#zzzzzz", convert("&#zzzzzz"));
        assertEquals("&#00ff0", convert("&#00ff0"));
    }

    @Test
    void unknownCodesAndTrailingAmpersandAreLeftAsIs() {
        assertEquals("&x &", convert("&x &"));
        assertEquals("Tom & Jerry", convert("Tom & Jerry"));
    }

    @Test
    void miniMessageTagsPassThrough() {
        assertEquals("<yellow>Hi</yellow> <green>x", convert("<yellow>Hi</yellow> &ax"));
    }
}
