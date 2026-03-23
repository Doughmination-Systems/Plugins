/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughutils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * JSON-backed configuration for DoughUtils.
 *
 * Replaces Bukkit's FileConfiguration / config.yml.
 * Uses Gson (bundled with Paper) — no extra dependency needed.
 *
 * Usage:
 *   DoughConfig cfg = new DoughConfig(plugin);
 *   cfg.load();
 *   boolean allFlight = cfg.isAllFlight();
 *   cfg.setSpawnWorld("world"); cfg.save();
 */
public class DoughConfig {

    private static final String FILE_NAME = "config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Main plugin;
    private final File configFile;
    private final Logger log;

    private JsonObject root = new JsonObject();

    // -------------------------------------------------------------------------
    // Construction & lifecycle
    // -------------------------------------------------------------------------

    public DoughConfig(Main plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.configFile = new File(plugin.getDataFolder(), FILE_NAME);
    }

    /**
     * Loads config.json from the plugin data folder, generating a fresh default
     * programmatically if the file is absent (no bundled resource required).
     */
    public void load() {
        if (!configFile.exists()) {
            plugin.getDataFolder().mkdirs();
            writeDefaults();
            save();
            log.info("DoughConfig: generated default config.json.");
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
            JsonElement el = JsonParser.parseReader(reader);
            root = el.isJsonObject() ? el.getAsJsonObject() : new JsonObject();
        } catch (IOException e) {
            log.severe("DoughConfig: failed to load config.json: " + e.getMessage());
            root = new JsonObject();
        }
    }

    /** Writes the current in-memory config back to config.json. */
    public void save() {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            log.severe("DoughConfig: failed to save config.json: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Default generation
    // -------------------------------------------------------------------------

    /** Populates root with all default values. Called only when no config.json exists yet. */
    private void writeDefaults() {
        root = new JsonObject();

        set("version", new com.google.gson.JsonPrimitive("1.0.0"));

        // Flight
        set("flight.allflight",               new com.google.gson.JsonPrimitive(false));
        set("flight.base-radius",             new com.google.gson.JsonPrimitive(100));
        set("flight.communal-zones.enabled",  new com.google.gson.JsonPrimitive(true));

        // Veinminer
        set("veinminer.enabled",                        new com.google.gson.JsonPrimitive(true));
        set("veinminer.tree-remover.max-blocks",        new com.google.gson.JsonPrimitive(100));
        set("veinminer.tree-remover.max-height",        new com.google.gson.JsonPrimitive(30));
        set("veinminer.tree-remover.max-leaf-blocks",   new com.google.gson.JsonPrimitive(200));
        set("veinminer.ore-remover.max-blocks",         new com.google.gson.JsonPrimitive(100));
        set("veinminer.ore-remover.max-height",         new com.google.gson.JsonPrimitive(30));

        // Spawn
        set("spawn.world", new com.google.gson.JsonPrimitive("world"));
        set("spawn.x",     new com.google.gson.JsonPrimitive(0.0));
        set("spawn.y",     new com.google.gson.JsonPrimitive(65.0));
        set("spawn.z",     new com.google.gson.JsonPrimitive(0.0));
        set("spawn.yaw",   new com.google.gson.JsonPrimitive(0.0));
        set("spawn.pitch", new com.google.gson.JsonPrimitive(0.0));

        // Base teleport
        set("base-teleport.cooldown-hours", new com.google.gson.JsonPrimitive(1));

        // Sounds
        set("sounds.base", new com.google.gson.JsonPrimitive("ENTITY_ENDERMAN_TELEPORT"));

        // Playtime tracking
        set("playtime-tracking.enabled", new com.google.gson.JsonPrimitive(true));

        // Meow messages
        JsonArray meow = new JsonArray();
        for (String s : new String[]{"mrrp!", "bwaa!", "uwu X3 rwar *pounces on you*", "nya!", "meow", ":3", "rwar", "X3"})
            meow.add(s);
        set("meow-command.messages", meow);

        // Bark messages
        JsonArray bark = new JsonArray();
        for (String s : new String[]{"woof!", "arf!", "bork!", "awoo!", "*wags tail*", "ruff!", "*runs in circles*", "henlo fren!"})
            bark.add(s);
        set("bark-command.messages", bark);

        // Jail
        set("jail.world", new com.google.gson.JsonPrimitive("world"));
        set("jail.x",     new com.google.gson.JsonPrimitive(0.0));
        set("jail.y",     new com.google.gson.JsonPrimitive(64.0));
        set("jail.z",     new com.google.gson.JsonPrimitive(0.0));
        set("jail.yaw",   new com.google.gson.JsonPrimitive(0.0));
        set("jail.pitch", new com.google.gson.JsonPrimitive(0.0));

        // WingSync — disabled by default
        set("wingsync.enabled",                  new com.google.gson.JsonPrimitive(false));
        set("wingsync.discord.token",            new com.google.gson.JsonPrimitive("YOUR_DISCORD_BOT_TOKEN"));
        set("wingsync.discord.guild_id",         new com.google.gson.JsonPrimitive("YOUR_DISCORD_GUILD_ID"));
        set("wingsync.discord.admin_id",         new com.google.gson.JsonPrimitive("YOUR_DISCORD_ADMIN_ID"));
        set("wingsync.discord.sync_bans",        new com.google.gson.JsonPrimitive(true));
        set("wingsync.storage.type",             new com.google.gson.JsonPrimitive("json"));
        set("wingsync.storage.host",             new com.google.gson.JsonPrimitive("localhost"));
        set("wingsync.storage.port",             new com.google.gson.JsonPrimitive(3306));
        set("wingsync.storage.database",         new com.google.gson.JsonPrimitive("minecraft"));
        set("wingsync.storage.username",         new com.google.gson.JsonPrimitive("root"));
        set("wingsync.storage.password",         new com.google.gson.JsonPrimitive(""));
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /** Navigates dot-separated path, returning null if any segment is missing. */
    private JsonObject nav(String path) {
        String[] parts = path.split("\\.");
        JsonObject cur = root;
        for (int i = 0; i < parts.length - 1; i++) {
            if (!cur.has(parts[i]) || !cur.get(parts[i]).isJsonObject()) return null;
            cur = cur.getAsJsonObject(parts[i]);
        }
        return cur;
    }

    /** Returns the leaf element at dot-path, or null. */
    private JsonElement get(String path) {
        String[] parts = path.split("\\.");
        JsonObject parent = nav(path);
        if (parent == null) return null;
        String leaf = parts[parts.length - 1];
        return parent.has(leaf) ? parent.get(leaf) : null;
    }

    /** Navigates (creating objects as needed) and sets the leaf. */
    private void set(String path, JsonElement value) {
        String[] parts = path.split("\\.");
        JsonObject cur = root;
        for (int i = 0; i < parts.length - 1; i++) {
            if (!cur.has(parts[i]) || !cur.get(parts[i]).isJsonObject()) {
                cur.add(parts[i], new JsonObject());
            }
            cur = cur.getAsJsonObject(parts[i]);
        }
        cur.add(parts[parts.length - 1], value);
    }

    private String getString(String path, String def) {
        JsonElement el = get(path);
        return (el != null && el.isJsonPrimitive()) ? el.getAsString() : def;
    }

    private boolean getBoolean(String path, boolean def) {
        JsonElement el = get(path);
        return (el != null && el.isJsonPrimitive()) ? el.getAsBoolean() : def;
    }

    private double getDouble(String path, double def) {
        JsonElement el = get(path);
        return (el != null && el.isJsonPrimitive()) ? el.getAsDouble() : def;
    }

    private int getInt(String path, int def) {
        JsonElement el = get(path);
        return (el != null && el.isJsonPrimitive()) ? el.getAsInt() : def;
    }

    private List<String> getStringList(String path) {
        JsonElement el = get(path);
        List<String> result = new ArrayList<>();
        if (el != null && el.isJsonArray()) {
            for (JsonElement item : el.getAsJsonArray()) {
                if (item.isJsonPrimitive()) result.add(item.getAsString());
            }
        }
        return result;
    }

    private void setString(String path, String value) {
        set(path, new com.google.gson.JsonPrimitive(value));
    }

    private void setDouble(String path, double value) {
        set(path, new com.google.gson.JsonPrimitive(value));
    }

    private void setFloat(String path, float value) {
        set(path, new com.google.gson.JsonPrimitive(value));
    }

    // =========================================================================
    // Public typed API
    // =========================================================================

    // ── Meta ─────────────────────────────────────────────────────────────────

    public String getVersion() {
        return getString("version", "unknown");
    }

    // ── Flight ───────────────────────────────────────────────────────────────

    /** Global all-flight toggle (flight.allflight). */
    public boolean isAllFlight() {
        return getBoolean("flight.allflight", false);
    }

    /** Radius around a player's base where flight is permitted. */
    public double getBaseFlightRadius() {
        return getDouble("flight.base-radius", 100.0);
    }

    /** Whether communal fly-zones are enabled. */
    public boolean isCommunalZonesEnabled() {
        return getBoolean("flight.communal-zones.enabled", true);
    }

    // ── Veinminer ────────────────────────────────────────────────────────────

    public boolean isVeinminerEnabled() {
        return getBoolean("veinminer.enabled", true);
    }

    public int getTreeRemoverMaxBlocks() {
        return getInt("veinminer.tree-remover.max-blocks", 100);
    }

    public int getTreeRemoverMaxLeafBlocks() {
        return getInt("veinminer.tree-remover.max-leaf-blocks", 200);
    }

    public int getOreRemoverMaxBlocks() {
        return getInt("veinminer.ore-remover.max-blocks", 100);
    }

    // ── Spawn ────────────────────────────────────────────────────────────────

    public boolean hasSpawnLocation() {
        JsonElement el = get("spawn.world");
        return el != null && el.isJsonPrimitive() && !el.getAsString().isEmpty();
    }

    public String getSpawnWorld() {
        return getString("spawn.world", "world");
    }

    public double getSpawnX() { return getDouble("spawn.x", 0.0); }
    public double getSpawnY() { return getDouble("spawn.y", 65.0); }
    public double getSpawnZ() { return getDouble("spawn.z", 0.0); }
    public float  getSpawnYaw()   { return (float) getDouble("spawn.yaw",   0.0); }
    public float  getSpawnPitch() { return (float) getDouble("spawn.pitch", 0.0); }

    public void setSpawnWorld(String world) { setString("spawn.world", world); }
    public void setSpawnX(double v)  { setDouble("spawn.x", v); }
    public void setSpawnY(double v)  { setDouble("spawn.y", v); }
    public void setSpawnZ(double v)  { setDouble("spawn.z", v); }
    public void setSpawnYaw(float v)   { setFloat("spawn.yaw", v); }
    public void setSpawnPitch(float v) { setFloat("spawn.pitch", v); }

    // ── Base teleport ─────────────────────────────────────────────────────────

    public int getBaseTeleportCooldownHours() {
        return getInt("base-teleport.cooldown-hours", 1);
    }

    // ── Sounds ───────────────────────────────────────────────────────────────

    public String getBaseSound() {
        return getString("sounds.base", "ENTITY_ENDERMAN_TELEPORT");
    }

    // ── Playtime tracking ────────────────────────────────────────────────────

    public boolean isPlaytimeTrackingEnabled() {
        return getBoolean("playtime-tracking.enabled", true);
    }

    // ── Roleplay messages ────────────────────────────────────────────────────

    public List<String> getMeowMessages() {
        return getStringList("meow-command.messages");
    }

    public List<String> getBarkMessages() {
        return getStringList("bark-command.messages");
    }

    // ── Jail ─────────────────────────────────────────────────────────────────

    public boolean hasJailLocation() {
        JsonElement el = get("jail.world");
        return el != null && el.isJsonPrimitive() && !el.getAsString().isEmpty();
    }

    public String getJailWorld() { return getString("jail.world", "world"); }
    public double getJailX()     { return getDouble("jail.x", 0.0); }
    public double getJailY()     { return getDouble("jail.y", 64.0); }
    public double getJailZ()     { return getDouble("jail.z", 0.0); }
    public float  getJailYaw()   { return (float) getDouble("jail.yaw",   0.0); }
    public float  getJailPitch() { return (float) getDouble("jail.pitch", 0.0); }

    public void setJailWorld(String world) { setString("jail.world", world); }
    public void setJailX(double v)  { setDouble("jail.x", v); }
    public void setJailY(double v)  { setDouble("jail.y", v); }
    public void setJailZ(double v)  { setDouble("jail.z", v); }
    public void setJailYaw(float v)   { setFloat("jail.yaw", v); }
    public void setJailPitch(float v) { setFloat("jail.pitch", v); }

    // ── WingSync ─────────────────────────────────────────────────────────────

    public boolean isWingSyncEnabled() {
        return getBoolean("wingsync.enabled", false);
    }

    public String getWingSyncToken() {
        return getString("wingsync.discord.token", "");
    }

    public String getWingSyncGuildId() {
        return getString("wingsync.discord.guild_id", "");
    }

    public String getWingSyncAdminId() {
        return getString("wingsync.discord.admin_id", "");
    }

    public boolean isWingSyncBanSyncEnabled() {
        return getBoolean("wingsync.discord.sync_bans", true);
    }

    public String getWingSyncStorageType() {
        return getString("wingsync.storage.type", "json").toLowerCase();
    }

    public String getWingSyncStorageHost() {
        return getString("wingsync.storage.host", "localhost");
    }

    public int getWingSyncStoragePort(int defaultPort) {
        return getInt("wingsync.storage.port", defaultPort);
    }

    public String getWingSyncStorageDatabase() {
        return getString("wingsync.storage.database", "minecraft");
    }

    public String getWingSyncStorageUsername(String defaultUser) {
        return getString("wingsync.storage.username", defaultUser);
    }

    public String getWingSyncStoragePassword() {
        return getString("wingsync.storage.password", "");
    }
}
