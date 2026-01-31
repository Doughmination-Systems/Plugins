/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughminationcord.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import win.doughmination.api.LibMain;
import win.doughmination.api.BanData;
import win.doughmination.doughminationcord.CordMain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UnbanCommandExecutor implements CommandExecutor, TabCompleter {

    private final CordMain plugin;

    public UnbanCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dough.unban")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /unban <player>");
            return true;
        }

        String targetName = args[0];
        LibMain doughminationAPI = LibMain.getInstance();

        // Find the banned player by name
        UUID targetUUID = null;
        for (Map.Entry<UUID, BanData> entry : doughminationAPI.getAllBans().entrySet()) {
            if (entry.getValue().getPlayerName().equalsIgnoreCase(targetName)) {
                targetUUID = entry.getKey();
                break;
            }
        }

        // If not found by name, try as UUID or offline player
        if (targetUUID == null) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);
            if (doughminationAPI.isPlayerBanned(offlinePlayer.getUniqueId())) {
                targetUUID = offlinePlayer.getUniqueId();
            }
        }

        if (targetUUID == null || !doughminationAPI.isPlayerBanned(targetUUID)) {
            sender.sendMessage(ChatColor.RED + targetName + " is not banned!");
            return true;
        }

        // Get unbanner name
        String unbannedBy;
        if (sender instanceof Player player) {
            unbannedBy = player.getName();
        } else {
            unbannedBy = "Console";
        }

        // Execute the unban via DoughminationAPI
        doughminationAPI.unbanPlayer(targetUUID, unbannedBy);

        // Broadcast the unban
        Bukkit.broadcastMessage(ChatColor.GREEN + "✓ " + ChatColor.WHITE + targetName +
                ChatColor.GREEN + " has been unbanned by " + ChatColor.WHITE + unbannedBy +
                ChatColor.GREEN + "!");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partialName = args[0].toLowerCase();
            LibMain doughminationAPI = LibMain.getInstance();

            // Suggest banned players
            for (BanData banData : doughminationAPI.getAllBans().values()) {
                if (banData.getPlayerName().toLowerCase().startsWith(partialName)) {
                    completions.add(banData.getPlayerName());
                }
            }
        }

        return completions;
    }
}