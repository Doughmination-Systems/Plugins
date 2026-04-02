package win.doughmination.dwolves.wolf;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import win.doughmination.dwolves.DWolvesPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;

/**
 * Manages temporary wolf ownership transfers.
 *
 * wolfUUID -> real owner UUID
 *
 * Persisted to temp_ownership.json so a crash or restart doesn't leave wolves
 * permanently owned by the wrong player. On startup, any wolves found in loaded
 * chunks are reverted immediately. Wolves in unloaded chunks are caught via
 * ChunkLoadEvent as they come in.
 */
public class TempOwnershipManager implements Listener {

    private final DWolvesPlugin plugin;
    private final Path dataFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final Map<UUID, UUID> tempMap = new HashMap<>();

    public TempOwnershipManager(DWolvesPlugin plugin) {
        this.plugin   = plugin;
        this.dataFile = plugin.getDataFolder().toPath().resolve("temp_ownership.json");
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Transfer a wolf to a trusted player temporarily.
     * Persists immediately so a crash doesn't lose the real owner.
     */
    public void transfer(Wolf wolf, org.bukkit.entity.Player trustedPlayer) {
        tempMap.put(wolf.getUniqueId(), wolf.getOwnerUniqueId());
        wolf.setOwner(trustedPlayer);
        save();
    }

    /**
     * Revert a wolf back to its real owner. Pass the real owner as an OfflinePlayer.
     */
    public void revert(Wolf wolf) {
        UUID realOwnerUuid = tempMap.remove(wolf.getUniqueId());
        if (realOwnerUuid == null) return;
        wolf.setOwner(plugin.getServer().getOfflinePlayer(realOwnerUuid));
        wolf.setSitting(true);
        wolf.setTarget(null);
        save();
    }

    /**
     * Revert a wolf back to a specific known online Player (used on owner login).
     */
    public void revertToPlayer(Wolf wolf, org.bukkit.entity.Player owner) {
        tempMap.remove(wolf.getUniqueId());
        wolf.setOwner(owner);
        wolf.setSitting(true);
        wolf.setTarget(null);
        save();
    }

    public boolean isTemporary(Wolf wolf) {
        return tempMap.containsKey(wolf.getUniqueId());
    }

    /**
     * Returns the real owner UUID for a temporarily transferred wolf, if present.
     */
    public Optional<UUID> getRealOwner(Wolf wolf) {
        return Optional.ofNullable(tempMap.get(wolf.getUniqueId()));
    }

    /**
     * Returns all wolf UUIDs currently under a temporary transfer.
     */
    public Set<UUID> getAllTemporary() {
        return Collections.unmodifiableSet(tempMap.keySet());
    }

    /**
     * Returns all wolf UUIDs whose real owner is the given UUID.
     */
    public Set<UUID> getWolvesByRealOwner(UUID realOwnerUuid) {
        Set<UUID> result = new HashSet<>();
        tempMap.forEach((wolfId, ownerId) -> {
            if (ownerId.equals(realOwnerUuid)) result.add(wolfId);
        });
        return result;
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    public void load() {
        if (!Files.exists(dataFile)) return;
        try (Reader reader = Files.newBufferedReader(dataFile)) {
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> raw = gson.fromJson(reader, type);
            if (raw == null) return;
            raw.forEach((wolfId, ownerId) ->
                    tempMap.put(UUID.fromString(wolfId), UUID.fromString(ownerId)));
            plugin.getLogger().info("Loaded " + tempMap.size() + " temporary ownership entries.");
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to load temp_ownership.json: " + ex.getMessage());
        }
    }

    public void save() {
        try {
            Files.createDirectories(dataFile.getParent());
            Map<String, String> raw = new LinkedHashMap<>();
            tempMap.forEach((wolfId, ownerId) -> raw.put(wolfId.toString(), ownerId.toString()));
            try (Writer writer = Files.newBufferedWriter(dataFile)) {
                gson.toJson(raw, writer);
            }
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save temp_ownership.json: " + ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Startup revert — wolves already in loaded chunks
    // -------------------------------------------------------------------------

    /**
     * Called once on enable after load(). Scans all currently loaded worlds/chunks
     * and reverts any wolves found in the temp map.
     */
    public void revertLoadedWolves() {
        if (tempMap.isEmpty()) return;
        plugin.getServer().getWorlds().forEach(world ->
                world.getEntitiesByClass(Wolf.class).forEach(wolf -> {
                    if (tempMap.containsKey(wolf.getUniqueId())) {
                        plugin.getLogger().info("Reverting temporarily owned wolf " + wolf.getUniqueId() + " on startup.");
                        revert(wolf);
                    }
                })
        );
    }

    // -------------------------------------------------------------------------
    // ChunkLoadEvent — catch wolves in chunks that load after startup
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (tempMap.isEmpty()) return;

        for (org.bukkit.entity.Entity entity : event.getChunk().getEntities()) {
            if (!(entity instanceof Wolf wolf)) continue;
            if (!tempMap.containsKey(wolf.getUniqueId())) continue;

            plugin.getLogger().info("Reverting temporarily owned wolf " + wolf.getUniqueId() + " on chunk load.");
            revert(wolf);
        }
    }
}