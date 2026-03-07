package win.dougmination.plural.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import win.dougmination.plural.PluralMain;

import java.util.UUID;

public class PlayerConnectionListener implements Listener {

    // 30s in ticks (20 ticks/sec)
    private static final long POLL_INTERVAL_TICKS = 20L * 30;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (PluralMain.getApiClient() == null) return;

        fetchAndCache(player, uuid, true);

        PluralMain.getInstance().getServer().getScheduler()
                .runTaskTimerAsynchronously(
                        PluralMain.getInstance(),
                        () -> {
                            if (!player.isOnline()) return;
                            fetchAndCache(player, uuid, false);
                        },
                        POLL_INTERVAL_TICKS,
                        POLL_INTERVAL_TICKS
                );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        PluralMain.systemCache.remove(event.getPlayer().getUniqueId());
    }

    private void fetchAndCache(Player player, UUID uuid, boolean announceOnJoin) {
        PluralMain.getInstance().getServer().getScheduler()
                .runTaskAsynchronously(PluralMain.getInstance(), () -> {
                    PluralMain.PlayerSystemData data = PluralMain.getApiClient().fetchPlayerData(uuid);

                    PluralMain.getInstance().getServer().getScheduler()
                            .runTask(PluralMain.getInstance(), () -> {
                                if (data != null) {
                                    PluralMain.PlayerSystemData prev = PluralMain.systemCache.get(uuid);
                                    PluralMain.systemCache.put(uuid, data);

                                    if (announceOnJoin && !data.activeFrontNames.isEmpty()) {
                                        player.sendMessage(ChatColor.GRAY + "[Plural] Fronting as: "
                                                + ChatColor.WHITE + String.join(" & ", data.activeFrontNames));
                                    }

                                    if (!announceOnJoin && prev != null
                                            && !prev.activeFrontNames.equals(data.activeFrontNames)
                                            && !data.activeFrontNames.isEmpty()) {
                                        player.sendMessage(ChatColor.GRAY + "[Plural] Front updated: "
                                                + ChatColor.WHITE + String.join(" & ", data.activeFrontNames));
                                    }
                                } else {
                                    PluralMain.systemCache.remove(uuid);
                                }
                            });
                });
    }
}