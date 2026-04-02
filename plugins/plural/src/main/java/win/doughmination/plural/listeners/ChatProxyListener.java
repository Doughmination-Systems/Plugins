package win.doughmination.plural.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import win.doughmination.plural.PluralConfig;
import win.doughmination.plural.PluralMain;

import java.util.UUID;

public class ChatProxyListener implements Listener {

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

        Component lpPrefix = Component.empty();
        Component lpSuffix = Component.empty();

        if (PluralConfig.LUCKPERMS_PREFIX) {
            try {
                RegisteredServiceProvider<LuckPerms> provider =
                        PluralMain.getInstance().getServer()
                                .getServicesManager().getRegistration(LuckPerms.class);
                if (provider != null) {
                    LuckPerms lp = provider.getProvider();
                    User lpUser = lp.getUserManager().getUser(uuid);
                    if (lpUser != null) {
                        CachedMetaData meta = lpUser.getCachedData().getMetaData();
                        if (meta.getPrefix() != null) {
                            lpPrefix = LegacyComponentSerializer.legacyAmpersand().deserialize(meta.getPrefix());
                        }
                        if (meta.getSuffix() != null) {
                            lpSuffix = LegacyComponentSerializer.legacyAmpersand().deserialize(meta.getSuffix());
                        }
                    }
                }
            } catch (NoClassDefFoundError | Exception ignored) {
                // LuckPerms not installed — silently skip
            }
        }

        Component frontComp = Component.text(front).color(NamedTextColor.WHITE);
        Component systemComp = Component.text(systemName).color(NamedTextColor.AQUA);

        // Full formatted message: [Prefix] <player ~ front | systemName>[Suffix]: message
        Component format = lpPrefix
                .append(Component.text(" <").color(NamedTextColor.GRAY))
                .append(Component.text(player.getName()).color(NamedTextColor.WHITE))
                .append(Component.text(" ~ ").color(NamedTextColor.GRAY))
                .append(frontComp)
                .append(Component.text(" | ").color(NamedTextColor.GRAY))
                .append(systemComp)
                .append(Component.text(">").color(NamedTextColor.GRAY))
                .append(lpSuffix)
                .append(Component.text(" ").color(NamedTextColor.WHITE));

        event.renderer((source, sourceDisplayName, message, viewer) ->
                format.append(message.colorIfAbsent(NamedTextColor.WHITE))
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
