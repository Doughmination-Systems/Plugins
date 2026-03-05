/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughminationcord.commands.moderation;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import win.doughmination.api.LibMain;
import win.doughmination.doughminationcord.CordMain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BanCommandExecutor implements CommandExecutor, TabCompleter {

    private final CordMain plugin;

    public BanCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dough.ban")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /ecban <player> [reason]");
            return true;
        }

        String targetName = args[0];

        // Build reason from remaining args, or use default
        String reason = "No reason specified";
        if (args.length > 1) {
            StringBuilder reasonBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (i > 1) reasonBuilder.append(" ");
                reasonBuilder.append(args[i]);
            }
            reason = reasonBuilder.toString();
        }

        // Try to find the player (online or offline)
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = targetPlayer.getUniqueId();

        // Check if already banned
        LibMain cloveLib = LibMain.getInstance();
        if (cloveLib.isPlayerBanned(targetUUID)) {
            sender.sendMessage(ChatColor.RED + targetName + " is already banned!");
            return true;
        }

        // Get banner info
        String bannedBy;
        UUID bannedByUUID;
        if (sender instanceof Player player) {
            bannedBy = player.getName();
            bannedByUUID = player.getUniqueId();
        } else {
            bannedBy = "Console";
            bannedByUUID = null;
        }

        // Execute the ban via CloveLib
        cloveLib.banPlayer(targetUUID, targetName, reason, bannedBy, bannedByUUID);

        // Broadcast the ban
        Bukkit.broadcastMessage(ChatColor.RED + "⛔ " + ChatColor.WHITE + targetName +
                ChatColor.RED + " has been banned by " + ChatColor.WHITE + bannedBy +
                ChatColor.RED + "!");
        Bukkit.broadcastMessage(ChatColor.GRAY + "Reason: " + reason);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partialName = args[0].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partialName)) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }
}