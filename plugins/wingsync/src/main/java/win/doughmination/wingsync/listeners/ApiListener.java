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
import win.doughmination.api.events.PlayerBannedEvent;
import win.doughmination.wingsync.Main;

public class ApiListener implements Listener {

    private final Main plugin;

    public ApiListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerBanned(PlayerBannedEvent event) {
        String playerName = event.getPlayerName();

        plugin.getLogger().info("Received ban event for " + playerName + " - removing from whitelist...");

        // Ban from Discord FIRST (before removing data, so we can still look up their Discord ID)
        try {
            plugin.banUserFromDiscord(playerName);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to ban " + playerName + " from Discord: " + e.getMessage());
        }

        // Remove from whitelist via console command
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "whitelist remove " + playerName);
        });

        // Remove from our database/storage AFTER Discord ban
        try {
            plugin.removePlayerDataByName(playerName);
            plugin.getLogger().info("Successfully removed " + playerName + " from WingSync whitelist data.");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to remove " + playerName + " from WingSync data: " + e.getMessage());
        }
    }
}