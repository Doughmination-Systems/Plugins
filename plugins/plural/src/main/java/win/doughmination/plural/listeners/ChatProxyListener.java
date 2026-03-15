package win.doughmination.plural.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import win.doughmination.plural.PluralMain;

import java.util.UUID;

public class ChatProxyListener implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    // Listen at MONITOR so we run AFTER LuckPerms (which runs at HIGHEST)
    // We then override the renderer if the player has an active front
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        PluralMain.PlayerSystemData data = PluralMain.systemCache.get(uuid);
        if (data == null || data.activeFrontNames.isEmpty()) return;

        String front = buildFrontString(data);
        String systemName = data.systemName != null ? data.systemName : "";
        boolean useLuckPerms = PluralMain.getInstance().getConfig()
                .getBoolean("luckperms_prefix", true);

        String lpPrefix = "";
        String lpSuffix = "";
        if (useLuckPerms) {
            try {
                RegisteredServiceProvider<LuckPerms> provider =
                        PluralMain.getInstance().getServer()
                                .getServicesManager().getRegistration(LuckPerms.class);
                if (provider != null) {
                    LuckPerms lp = provider.getProvider();
                    User lpUser = lp.getUserManager().getUser(uuid);
                    if (lpUser != null) {
                        CachedMetaData meta = lpUser.getCachedData().getMetaData();
                        lpPrefix = meta.getPrefix() != null ? meta.getPrefix() : "";
                        lpSuffix = meta.getSuffix() != null ? meta.getSuffix() : "";
                    }
                }
            } catch (NoClassDefFoundError | Exception ignored) {
                // LuckPerms not installed — silently skip
            }
        }

        // Format: [lpPrefix] <username ~ front | systemName>[lpSuffix]: message
        String legacyFormat = lpPrefix
                + "§7<§f" + player.getName()
                + "§7 ~ §f" + front
                + "§7 | §b" + systemName
                + "§7>§r" + lpSuffix;

        Component prefix = LEGACY.deserialize(legacyFormat);

        event.renderer((source, sourceDisplayName, message, viewer) ->
                prefix.append(Component.text(" ")).append(message)
        );
    }

    private String buildFrontString(PluralMain.PlayerSystemData data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.activeFrontNames.size(); i++) {
            if (i > 0) sb.append("§7 & §f");
            String name = data.activeFrontNames.get(i);
            PluralMain.MemberInfo info = data.getMember(name);
            sb.append(info != null && info.displayName != null ? info.displayName : name);
        }
        return sb.toString();
    }
}