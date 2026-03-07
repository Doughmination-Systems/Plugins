/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 * WingSync
 */

package win.doughmination.wingsync.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import win.doughmination.wingsync.Main;

/**
 * Listens for player bans via vanilla /ban command
 * and synchronizes them to Discord
 */
public class BanListener implements Listener {

    private final Main plugin;

    public BanListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        // Check if this is a ban (Bukkit doesn't have a dedicated ban event)
        // When a player is banned, they are kicked with a specific reason
        String kickReason = event.getReason();

        // Check if player is now banned after the kick
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String playerName = event.getPlayer().getName();

            if (Bukkit.getBannedPlayers().stream()
                    .anyMatch(ban -> ban.getName() != null && ban.getName().equalsIgnoreCase(playerName))) {

                plugin.getLogger().info("Detected ban for " + playerName + " - processing Discord ban sync...");

                // Ban from Discord FIRST (before removing data, so we can still look up their Discord ID)
                try {
                    plugin.banUserFromDiscord(playerName);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to ban " + playerName + " from Discord: " + e.getMessage());
                }

                // Remove from whitelist
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "whitelist remove " + playerName);

                // Remove from our database/storage AFTER Discord ban
                try {
                    plugin.removePlayerDataByName(playerName);
                    plugin.getLogger().info("Successfully removed " + playerName + " from WingSync whitelist data.");
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to remove " + playerName + " from WingSync data: " + e.getMessage());
                }
            }
        }, 5L); // Wait 5 ticks (0.25 seconds) to ensure ban is registered
    }
}