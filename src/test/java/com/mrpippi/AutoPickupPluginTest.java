package com.autopickup;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for AutoPickupPlugin's static helpers: legacyToMiniMessage (characterization) and resolveMessage. */
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

    // --- resolveMessage: config.yml vs lang.yml precedence ---

    private static final String KEY = "messages.toggled-on";
    private static final String BUNDLED = "&aAuto-pickup has been &fenabled&a.";

    /** A server config.yml backed by the bundled config.yml as defaults, like JavaPlugin.getConfig(). */
    private static YamlConfiguration serverConfig(String yaml) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        var in = AutoPickupPluginTest.class.getClassLoader().getResourceAsStream("config.yml");
        config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
        return config;
    }

    private static YamlConfiguration yaml(String yaml) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return config;
    }

    @Test
    void bundledDefaultsAreWhatTheTestsAssume() throws Exception {
        assertEquals(BUNDLED, serverConfig("").getString(KEY));
    }

    @Test
    void editedConfigMessageWinsOverLang() throws Exception {
        var config = serverConfig("messages:\n  toggled-on: \"from config\"\n");
        var lang = yaml("messages:\n  toggled-on: \"from lang\"\n");
        assertEquals("from config", AutoPickupPlugin.resolveMessage(KEY, config, lang));
    }

    @Test
    void uneditedConfigMessageDoesNotShadowCustomisedLang() throws Exception {
        var config = serverConfig("messages:\n  toggled-on: \"" + BUNDLED + "\"\n");
        var lang = yaml("messages:\n  toggled-on: \"from lang\"\n");
        assertEquals("from lang", AutoPickupPlugin.resolveMessage(KEY, config, lang));
    }

    @Test
    void keyMissingFromConfigFileFallsBackToLang() throws Exception {
        var config = serverConfig("settings:\n  default-enabled: true\n");
        var lang = yaml("messages:\n  toggled-on: \"from lang\"\n");
        assertEquals("from lang", AutoPickupPlugin.resolveMessage(KEY, config, lang));
    }

    @Test
    void keyMissingFromBothFilesFallsBackToBundledDefault() throws Exception {
        assertEquals(BUNDLED, AutoPickupPlugin.resolveMessage(KEY, serverConfig(""), yaml("")));
        assertEquals(BUNDLED, AutoPickupPlugin.resolveMessage(KEY, serverConfig(""), null));
    }

    @Test
    void configKeyWithNoBundledDefaultWins() throws Exception {
        var config = serverConfig("messages:\n  actionbar: \"from config\"\n");
        var lang = yaml("messages:\n  actionbar: \"from lang\"\n");
        assertEquals("from config", AutoPickupPlugin.resolveMessage("messages.actionbar", config, lang));
    }

    @Test
    void langOnlyKeyIsUsed() throws Exception {
        var lang = yaml("messages:\n  actionbar: \"from lang\"\n");
        assertEquals("from lang", AutoPickupPlugin.resolveMessage("messages.actionbar", serverConfig(""), lang));
    }

    @Test
    void unknownKeyResolvesToNull() throws Exception {
        assertNull(AutoPickupPlugin.resolveMessage("messages.nope", serverConfig(""), yaml("")));
        assertNull(AutoPickupPlugin.resolveMessage("messages.nope", null, null));
    }
}
