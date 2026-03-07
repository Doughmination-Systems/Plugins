/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands.roleplay;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import win.doughmination.doughcord.CordMain;

import java.util.List;
import java.util.Random;

public class barkCommandExecutor implements CommandExecutor, org.bukkit.command.TabCompleter {
    private final CordMain plugin;
    private final Random random;

    public barkCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        // Fetch bark messages from the config file
        List<String> barkMessages = plugin.getConfig().getStringList("bark-command.messages");

        if (barkMessages.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No bark messages are set in the config!");
            return true;
        }

        // Pick a random message
        String randomMessage = barkMessages.get(random.nextInt(barkMessages.size()));

        // Construct the full formatted message
        String formattedMessage = ChatColor.YELLOW + player.getName() + " says " + randomMessage;

        // Send the message to all players online
        plugin.getServer().broadcastMessage(formattedMessage);

        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
