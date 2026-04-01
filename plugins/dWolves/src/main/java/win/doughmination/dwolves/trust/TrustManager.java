package win.doughmination.dwolves.trust;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import win.doughmination.dwolves.DWolvesPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;

public class TrustManager {

    private final DWolvesPlugin plugin;
    private final Path dataFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /** ownerUuid -> list of trust entries */
    private final Map<UUID, List<TrustEntry>> trustMap = new HashMap<>();

    /** targetUuid -> pending request (one at a time per target) */
    private final Map<UUID, PendingRequest> pendingRequests = new HashMap<>();

    public TrustManager(DWolvesPlugin plugin) {
        this.plugin   = plugin;
        this.dataFile = plugin.getDataFolder().toPath().resolve("trust.json");
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    public void load() {
        if (!Files.exists(dataFile)) return;
        try (Reader reader = Files.newBufferedReader(dataFile)) {
            Type listType = new TypeToken<List<SerializedEntry>>() {}.getType();
            List<SerializedEntry> raw = gson.fromJson(reader, listType);
            if (raw == null) return;
            for (SerializedEntry e : raw) {
                UUID owner   = UUID.fromString(e.ownerUuid);
                UUID trusted = UUID.fromString(e.trustedUuid);
                TrustLevel level = TrustLevel.fromString(e.level);
                trustMap.computeIfAbsent(owner, k -> new ArrayList<>())
                        .add(new TrustEntry(owner, trusted, level));
            }
            plugin.getLogger().info("Loaded " + raw.size() + " trust entries.");
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to load trust.json: " + ex.getMessage());
        }
    }

    public void save() {
        try {
            Files.createDirectories(dataFile.getParent());
            List<SerializedEntry> raw = new ArrayList<>();
            for (List<TrustEntry> entries : trustMap.values()) {
                for (TrustEntry e : entries) {
                    raw.add(new SerializedEntry(
                            e.getOwnerUuid().toString(),
                            e.getTrustedUuid().toString(),
                            e.getLevel().name()
                    ));
                }
            }
            try (Writer writer = Files.newBufferedWriter(dataFile)) {
                gson.toJson(raw, writer);
            }
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save trust.json: " + ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Trust queries
    // -------------------------------------------------------------------------

    /** Returns the trust entry for trustedUuid under ownerUuid, or empty. */
    public Optional<TrustEntry> getEntry(UUID ownerUuid, UUID trustedUuid) {
        List<TrustEntry> entries = trustMap.getOrDefault(ownerUuid, Collections.emptyList());
        return entries.stream()
                .filter(e -> e.getTrustedUuid().equals(trustedUuid))
                .findFirst();
    }

    /** Returns all owners who have trusted the given player. */
    public List<TrustEntry> getEntriesForTrusted(UUID trustedUuid) {
        List<TrustEntry> result = new ArrayList<>();
        for (List<TrustEntry> entries : trustMap.values()) {
            entries.stream()
                    .filter(e -> e.getTrustedUuid().equals(trustedUuid))
                    .forEach(result::add);
        }
        return result;
    }

    /** Returns all players trusted by the given owner. */
    public List<TrustEntry> getEntriesForOwner(UUID ownerUuid) {
        return Collections.unmodifiableList(trustMap.getOrDefault(ownerUuid, Collections.emptyList()));
    }

    public boolean isTrusted(UUID ownerUuid, UUID trustedUuid) {
        return getEntry(ownerUuid, trustedUuid).isPresent();
    }

    // -------------------------------------------------------------------------
    // Trust mutations
    // -------------------------------------------------------------------------

    public void addTrust(UUID ownerUuid, UUID trustedUuid, TrustLevel level) {
        List<TrustEntry> entries = trustMap.computeIfAbsent(ownerUuid, k -> new ArrayList<>());
        // Update if already exists
        entries.stream()
                .filter(e -> e.getTrustedUuid().equals(trustedUuid))
                .findFirst()
                .ifPresentOrElse(
                        e -> e.setLevel(level),
                        () -> entries.add(new TrustEntry(ownerUuid, trustedUuid, level))
                );
        save();
    }

    public boolean removeTrust(UUID ownerUuid, UUID trustedUuid) {
        List<TrustEntry> entries = trustMap.get(ownerUuid);
        if (entries == null) return false;
        boolean removed = entries.removeIf(e -> e.getTrustedUuid().equals(trustedUuid));
        if (removed) save();
        return removed;
    }

    public void setLevel(UUID ownerUuid, UUID trustedUuid, TrustLevel level) {
        getEntry(ownerUuid, trustedUuid).ifPresent(e -> {
            e.setLevel(level);
            save();
        });
    }

    // -------------------------------------------------------------------------
    // Pending requests
    // -------------------------------------------------------------------------

    public void addPendingRequest(PendingRequest request) {
        pendingRequests.put(request.getTargetUuid(), request);
    }

    public Optional<PendingRequest> getPendingRequest(UUID targetUuid) {
        PendingRequest req = pendingRequests.get(targetUuid);
        if (req == null) return Optional.empty();
        if (req.isExpired()) {
            pendingRequests.remove(targetUuid);
            return Optional.empty();
        }
        return Optional.of(req);
    }

    public void removePendingRequest(UUID targetUuid) {
        pendingRequests.remove(targetUuid);
    }

    // -------------------------------------------------------------------------
    // Internal serialization record
    // -------------------------------------------------------------------------

    private static class SerializedEntry {
        String ownerUuid;
        String trustedUuid;
        String level;

        SerializedEntry(String ownerUuid, String trustedUuid, String level) {
            this.ownerUuid   = ownerUuid;
            this.trustedUuid = trustedUuid;
            this.level       = level;
        }
    }
}