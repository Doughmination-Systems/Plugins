/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package win.doughmination.doughcord.commands.moderation;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import win.doughmination.api.LibMain;
import win.doughmination.api.BanData;
import win.doughmination.doughcord.CordMain;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class BanlistCommandExecutor implements CommandExecutor, org.bukkit.command.TabCompleter {

    private final CordMain plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public BanlistCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dough.banlist")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            return true;
        }

        LibMain cloveLib = LibMain.getInstance();
        Map<UUID, BanData> bans = cloveLib.getAllBans();

        if (bans.isEmpty()) {
            sender.sendMessage(ChatColor.GREEN + "There are no banned players.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "======== " + ChatColor.RED + "Banned Players" +
                ChatColor.GOLD + " (" + bans.size() + ") ========");

        for (BanData banData : bans.values()) {
            String banDate = dateFormat.format(new Date(banData.getBannedAt()));

            sender.sendMessage(ChatColor.RED + "• " + ChatColor.WHITE + banData.getPlayerName());
            sender.sendMessage(ChatColor.GRAY + "  Reason: " + banData.getReason());
            sender.sendMessage(ChatColor.GRAY + "  Banned by: " + banData.getBannedBy() + " on " + banDate);
        }

        sender.sendMessage(ChatColor.GOLD + "================================");

        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
