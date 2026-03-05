/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughminationcord.commands.moderation;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import win.doughmination.doughminationcord.CordMain;

public class VersionCommandExecutor implements CommandExecutor, org.bukkit.command.TabCompleter {

    private final CordMain plugin;

    public VersionCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commonMessage = "DoughminationCord";

        String version = plugin.getDescription().getVersion();
        String authors = String.join(", ", plugin.getDescription().getAuthors());
        String website = plugin.getDescription().getWebsite();

        sender.sendMessage(ChatColor.GOLD + "======== " + commonMessage + ChatColor.GREEN + " Plugin Info" + ChatColor.GOLD + " ========");
        sender.sendMessage(ChatColor.YELLOW + "Version: " + ChatColor.WHITE + version);
        sender.sendMessage(ChatColor.YELLOW + "Authors: " + ChatColor.WHITE + authors);
        if (website != null && !website.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Website: " + ChatColor.WHITE + website);
        }
        sender.sendMessage(ChatColor.GOLD + "================================");

        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
