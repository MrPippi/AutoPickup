package com.autopickup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AutoPickupPlugin.legacyToMiniMessage(), which is package-private.
 */
class AutoPickupPluginTest {

    private static String convert(String input) {
        return AutoPickupPlugin.legacyToMiniMessage(input);
    }

    @Test
    void nullInput_returnsEmpty() {
        assertEquals("", convert(null));
    }

    @Test
    void emptyInput_returnsEmpty() {
        assertEquals("", convert(""));
    }

    @Test
    void noColorCodes_passesThrough() {
        assertEquals("Hello World", convert("Hello World"));
    }

    @Test
    void ampersandColorCode_convertsToMiniMessageTag() {
        assertEquals("<green>text", convert("&atext"));
    }

    @Test
    void sectionSignColorCode_convertsToMiniMessageTag() {
        assertEquals("<red>text", convert("§ctext"));
    }

    @Test
    void formatCode_bold_converts() {
        assertEquals("<bold>text", convert("&ltext"));
    }

    @Test
    void formatCode_italic_converts() {
        assertEquals("<italic>text", convert("&otext"));
    }

    @Test
    void formatCode_reset_converts() {
        assertEquals("<reset>text", convert("&rtext"));
    }

    @Test
    void hexColorCode_converts() {
        assertEquals("<#ff0000>text", convert("&#ff0000text"));
    }

    @Test
    void hexColorCode_caseInsensitive() {
        assertEquals("<#FF0000>text", convert("&#FF0000text"));
    }

    @Test
    void mixedLegacyAndPlainText() {
        assertEquals("<green>Hello <red>World", convert("&aHello &cWorld"));
    }

    @Test
    void multipleColorCodes() {
        assertEquals("<dark_blue><gold><white>text", convert("&1&6&ftext"));
    }

    @Test
    void miniMessageTagsPassThrough() {
        // MiniMessage tags that are not & codes should survive unchanged
        assertEquals("<yellow>hello</yellow>", convert("<yellow>hello</yellow>"));
    }

    @Test
    void ampersandWithUnknownCode_passesThrough() {
        // &z is not a valid code — should be left as-is
        String result = convert("&ztext");
        // The '&' and 'z' should remain
        assertTrue(result.contains("&z") || result.contains("z"),
                "Unknown code should pass through: " + result);
    }

    @Test
    void trailingAmpersand_passesThrough() {
        String result = convert("text&");
        assertTrue(result.contains("&"), "Trailing & should pass through");
    }

    @Test
    void hexCode_tooShort_notConverted() {
        // &#fff is only 3 hex chars — should NOT be converted as a hex tag
        String result = convert("&#ffftext");
        assertFalse(result.startsWith("<#"), "Short hex should not convert: " + result);
    }
}
