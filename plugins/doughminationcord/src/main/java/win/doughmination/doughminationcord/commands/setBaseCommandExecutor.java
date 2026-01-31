/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughminationcord.commands;

import win.doughmination.doughminationcord.CordMain;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import win.doughmination.api.LibMain;

import java.util.UUID;

public class setBaseCommandExecutor implements CommandExecutor {

    private final CordMain plugin;

    public setBaseCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!LibMain.getInstance().canUseCommand(player, "setbase")) {
            player.sendMessage(ChatColor.RED + "You cannot use this command while jailed!");
            return true;
        }

        UUID playerUUID = player.getUniqueId();
        Location location = player.getLocation();

        plugin.getBases().put(playerUUID, location);
        plugin.getConfig().set("bases." + playerUUID, location.serialize());
        plugin.saveConfig();

        player.sendMessage(ChatColor.GREEN + "Your base location has been set!");
        return true;
    }
}
