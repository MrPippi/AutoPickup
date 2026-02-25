package com.autopickup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AutoPickupPlugin.legacyToMiniMessage() — no server needed.
 */
class AutoPickupPluginMessageTest {

    @Test
    void null_returnsEmpty() {
        assertEquals("", AutoPickupPlugin.legacyToMiniMessage(null));
    }

    @Test
    void empty_returnsEmpty() {
        assertEquals("", AutoPickupPlugin.legacyToMiniMessage(""));
    }

    @Test
    void noColorCodes_unchanged() {
        assertEquals("Hello World!", AutoPickupPlugin.legacyToMiniMessage("Hello World!"));
    }

    @Test
    void ampersandColorCode_convertedToTag() {
        assertEquals("<green>text", AutoPickupPlugin.legacyToMiniMessage("&atext"));
        assertEquals("<red>text",   AutoPickupPlugin.legacyToMiniMessage("&ctext"));
        assertEquals("<yellow>text", AutoPickupPlugin.legacyToMiniMessage("&etext"));
        assertEquals("<white>text",  AutoPickupPlugin.legacyToMiniMessage("&ftext"));
    }

    @Test
    void sectionSignColorCode_convertedToTag() {
        assertEquals("<green>text", AutoPickupPlugin.legacyToMiniMessage("§atext"));
    }

    @Test
    void uppercaseCode_convertedToTag() {
        assertEquals("<green>text", AutoPickupPlugin.legacyToMiniMessage("&Atext"));
        assertEquals("<red>text",   AutoPickupPlugin.legacyToMiniMessage("&Ctext"));
    }

    @Test
    void formatCodes_convertedToTag() {
        assertEquals("<bold>text",          AutoPickupPlugin.legacyToMiniMessage("&ltext"));
        assertEquals("<italic>text",        AutoPickupPlugin.legacyToMiniMessage("&otext"));
        assertEquals("<underlined>text",    AutoPickupPlugin.legacyToMiniMessage("&ntext"));
        assertEquals("<strikethrough>text", AutoPickupPlugin.legacyToMiniMessage("&mtext"));
        assertEquals("<obfuscated>text",    AutoPickupPlugin.legacyToMiniMessage("&ktext"));
        assertEquals("<reset>text",         AutoPickupPlugin.legacyToMiniMessage("&rtext"));
    }

    @Test
    void hexCode_convertedToTag() {
        assertEquals("<#00ff00>text", AutoPickupPlugin.legacyToMiniMessage("&#00ff00text"));
        assertEquals("<#AABBCC>text", AutoPickupPlugin.legacyToMiniMessage("&#AABBCCtext"));
    }

    @Test
    void mixedLegacyAndMiniMessage_bothPreserved() {
        String input  = "&aHello <blue>World</blue>&r!";
        String result = AutoPickupPlugin.legacyToMiniMessage(input);
        assertEquals("<green>Hello <blue>World</blue><reset>!", result);
    }

    @Test
    void multipleCodes_allConverted() {
        String input  = "&aAuto-pickup has been &fenabled&a.";
        String result = AutoPickupPlugin.legacyToMiniMessage(input);
        assertEquals("<green>Auto-pickup has been <white>enabled<green>.", result);
    }

    @Test
    void miniMessageTagsPassedThrough() {
        String input = "<yellow>Hello <#00ff00>RGB!</yellow>";
        assertEquals(input, AutoPickupPlugin.legacyToMiniMessage(input));
    }

    @Test
    void incompleteCode_notConverted() {
        // & at end of string — no following char
        assertEquals("text&", AutoPickupPlugin.legacyToMiniMessage("text&"));
    }

    @Test
    void incompleteHex_notConverted() {
        // &#12345 — only 5 hex digits, not 6
        String input = "&#12345x";
        assertEquals("&#12345x", AutoPickupPlugin.legacyToMiniMessage(input));
    }

    @Test
    void unknownCode_notConverted() {
        // &z is not a valid code
        assertEquals("&ztext", AutoPickupPlugin.legacyToMiniMessage("&ztext"));
    }
}
