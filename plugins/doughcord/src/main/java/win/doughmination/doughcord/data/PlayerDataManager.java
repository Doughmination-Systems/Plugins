/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

/**
 * Manages per-player settings files at:
 *   plugins/Doughminationcord/data/settings/<UUID>.json
 *
 * Schema per file:
 * {
 *   "playtime": 12345678,
 *   "veinminer": { "ores": true, "trees": true },
 *   "flight": true,
 *   "base": { "world": "world", "x": 0.0, "y": 64.0, "z": 0.0, "yaw": 0.0, "pitch": 0.0 }
 * }
 */
public class PlayerDataManager {

    private final File settingsDir;
    private final JavaPlugin plugin;

    public PlayerDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.settingsDir = new File(plugin.getDataFolder(), "data/settings");
        if (!settingsDir.exists()) settingsDir.mkdirs();
    }

    // -----------------------------------------------------------------------
    // Load
    // -----------------------------------------------------------------------

    /** Loads the full JSON object for a player, or returns an empty object if none exists. */
    @SuppressWarnings("unchecked")
    public JSONObject load(UUID uuid) {
        File file = fileFor(uuid);
        if (!file.exists()) return new JSONObject();
        try (FileReader reader = new FileReader(file)) {
            Object parsed = new JSONParser().parse(reader);
            return parsed instanceof JSONObject ? (JSONObject) parsed : new JSONObject();
        } catch (IOException | ParseException e) {
            plugin.getLogger().warning("Failed to load settings for " + uuid + ": " + e.getMessage());
            return new JSONObject();
        }
    }

    public long loadPlaytime(UUID uuid) {
        JSONObject data = load(uuid);
        Object val = data.get("playtime");
        if (val instanceof Number) return ((Number) val).longValue();
        return 0L;
    }

    public boolean loadVeinminerOres(UUID uuid) {
        return loadVeinminerFlag(uuid, "ores");
    }

    public boolean loadVeinminerTrees(UUID uuid) {
        return loadVeinminerFlag(uuid, "trees");
    }

    public boolean loadFlightToggle(UUID uuid) {
        JSONObject data = load(uuid);
        Object val = data.get("flight");
        if (val instanceof Boolean) return (Boolean) val;
        return false;
    }

    /** Returns null if no base has been saved yet. */
    public Location loadBase(UUID uuid) {
        JSONObject data = load(uuid);
        Object baseObj = data.get("base");
        if (!(baseObj instanceof JSONObject)) return null;
        JSONObject base = (JSONObject) baseObj;
        try {
            String worldName = (String) base.get("world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("Unknown world '" + worldName + "' for base of " + uuid);
                return null;
            }
            double x     = ((Number) base.get("x")).doubleValue();
            double y     = ((Number) base.get("y")).doubleValue();
            double z     = ((Number) base.get("z")).doubleValue();
            float  yaw   = ((Number) base.get("yaw")).floatValue();
            float  pitch = ((Number) base.get("pitch")).floatValue();
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            plugin.getLogger().warning("Corrupt base data for " + uuid + ": " + e.getMessage());
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Save helpers — each saves only the field it owns, preserving the rest
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public void savePlaytime(UUID uuid, long playtimeMs) {
        JSONObject data = load(uuid);
        data.put("playtime", playtimeMs);
        save(uuid, data);
    }

    @SuppressWarnings("unchecked")
    public void saveVeinminer(UUID uuid, boolean ores, boolean trees) {
        JSONObject data = load(uuid);
        JSONObject vm = new JSONObject();
        vm.put("ores", ores);
        vm.put("trees", trees);
        data.put("veinminer", vm);
        save(uuid, data);
    }

    @SuppressWarnings("unchecked")
    public void saveFlightToggle(UUID uuid, boolean enabled) {
        JSONObject data = load(uuid);
        data.put("flight", enabled);
        save(uuid, data);
    }

    @SuppressWarnings("unchecked")
    public void saveBase(UUID uuid, Location loc) {
        JSONObject data = load(uuid);
        if (loc == null) {
            data.remove("base");
        } else {
            JSONObject base = new JSONObject();
            base.put("world", loc.getWorld().getName());
            base.put("x",     loc.getX());
            base.put("y",     loc.getY());
            base.put("z",     loc.getZ());
            base.put("yaw",   (double) loc.getYaw());
            base.put("pitch", (double) loc.getPitch());
            data.put("base", base);
        }
        save(uuid, data);
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private boolean loadVeinminerFlag(UUID uuid, String key) {
        JSONObject data = load(uuid);
        Object vmObj = data.get("veinminer");
        if (!(vmObj instanceof JSONObject)) return true; // default on
        Object val = ((JSONObject) vmObj).get(key);
        if (val instanceof Boolean) return (Boolean) val;
        return true;
    }

    private void save(UUID uuid, JSONObject data) {
        File file = fileFor(uuid);
        try (FileWriter writer = new FileWriter(file)) {
            // Pretty-print manually for readability
            writer.write(prettyPrint(data));
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save settings for " + uuid + ": " + e.getMessage());
        }
    }

    private File fileFor(UUID uuid) {
        return new File(settingsDir, uuid.toString() + ".json");
    }

    /**
     * Simple pretty-printer — keeps things readable without pulling in Gson/Jackson.
     * Handles one level of nesting (which is all we need here).
     */
    @SuppressWarnings("unchecked")
    private String prettyPrint(JSONObject obj) {
        StringBuilder sb = new StringBuilder("{\n");
        int i = 0;
        for (Object key : obj.keySet()) {
            Object val = obj.get(key);
            sb.append("  \"").append(key).append("\": ");
            if (val instanceof JSONObject) {
                sb.append("{\n");
                JSONObject inner = (JSONObject) val;
                int j = 0;
                for (Object ik : inner.keySet()) {
                    Object iv = inner.get(ik);
                    sb.append("    \"").append(ik).append("\": ").append(jsonValue(iv));
                    if (++j < inner.size()) sb.append(",");
                    sb.append("\n");
                }
                sb.append("  }");
            } else {
                sb.append(jsonValue(val));
            }
            if (++i < obj.size()) sb.append(",");
            sb.append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    private String jsonValue(Object v) {
        if (v instanceof String)  return "\"" + v + "\"";
        if (v instanceof Boolean) return v.toString();
        if (v instanceof Number)  return v.toString();
        return "null";
    }
}
