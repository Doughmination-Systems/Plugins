package win.doughmination.plural;

import org.bukkit.plugin.java.JavaPlugin;
import win.doughmination.plural.commands.PluralBrigadierCommand;
import win.doughmination.plural.listeners.ChatProxyListener;
import win.doughmination.plural.listeners.PlayerConnectionListener;
import win.doughmination.plural.api.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PluralMain extends JavaPlugin {

    public static final String MOD_NAME = "Plural";
    public static final Map<UUID, PlayerSystemData> systemCache = new ConcurrentHashMap<>();
    private static PluralMain instance;
    private static CloudApiClient apiClient;

    private PlayerConnectionListener connectionListener;

    @Override
    public void onEnable() {
        instance = this;

        String apiUrl = PluralConfig.API_URL;

        if (apiUrl.isEmpty()) {
            getLogger().warning("[Plural] API_URL not set — cloud sync disabled.");
            apiClient = null;
        } else {
            apiClient = new CloudApiClient(apiUrl);
            getLogger().info("[Plural] Cloud API connected: " + apiUrl);
        }

        // Register Brigadier command (replaces legacy CommandExecutor + TabCompleter)
        PluralBrigadierCommand.register(this);

        connectionListener = new PlayerConnectionListener();
        getServer().getPluginManager().registerEvents(new ChatProxyListener(), this);
        getServer().getPluginManager().registerEvents(connectionListener, this);

        ModrinthApi.check(this, "C6SDmIS1", "plural", getPluginMeta().getVersion());

        getLogger().info("[Plural] " + MOD_NAME + " loaded!");
    }

    @Override
    public void onDisable() {
        if (connectionListener != null) {
            connectionListener.shutdown();
        }
        systemCache.clear();
        if (apiClient != null) {
            apiClient.shutdown();
        }
        apiClient = null;
        instance = null;
    }

    public static PluralMain getInstance() { return instance; }
    public static CloudApiClient getApiClient() { return apiClient; }

    // ---- In-memory data model ----

    public static class PlayerSystemData {
        public final UUID minecraftUuid;
        public String systemName;
        public String systemTag;
        // memberName (lowercased key) -> MemberInfo
        public final Map<String, MemberInfo> members = new LinkedHashMap<>();
        // Names of currently fronting members (support multi-word, co-fronting)
        public final List<String> activeFrontNames = new ArrayList<>();

        public PlayerSystemData(UUID minecraftUuid, String systemName) {
            this.minecraftUuid = minecraftUuid;
            this.systemName = systemName;
        }

        /** Returns true if this UUID has a registered cloud system */
        public boolean hasSystem() { return systemName != null && !systemName.isEmpty(); }

        /** Case-insensitive member lookup */
        public MemberInfo getMember(String name) {
            return members.get(name.toLowerCase());
        }

        /** Returns the chat display prefix for the current front, or null if not fronting */
        public String buildChatPrefix() {
            if (activeFrontNames.isEmpty()) return null;
            StringBuilder sb = new StringBuilder();
            sb.append("§7<");
            for (int i = 0; i < activeFrontNames.size(); i++) {
                if (i > 0) sb.append("§7 & ");
                String name = activeFrontNames.get(i);
                MemberInfo info = getMember(name);
                sb.append("§f");
                sb.append(info != null && info.displayName != null ? info.displayName : name);
            }
            sb.append("§7 (§b").append(systemName).append("§7)>§r ");
            return sb.toString();
        }
    }

    public static class MemberInfo {
        public String name;         // canonical name (may contain spaces)
        public String displayName;
        public String pronouns;
        public String color;        // hex without #
        public String avatarUrl;
        public String description;
        public String pkMemberId;
    }
}
