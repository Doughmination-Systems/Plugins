/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands.travel;

import win.doughmination.doughcord.CordMain;
import win.doughmination.doughcord.commands.travel.tpAskCommandExecutor.TeleportRequest;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import win.doughmination.api.LibMain;

import java.util.UUID;

public class tpAcceptCommandExecutor implements CommandExecutor, org.bukkit.command.TabCompleter {

    private final CordMain plugin;

    public tpAcceptCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player target)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        if (!LibMain.getInstance().canUseCommand(target, "tpaccept")) {
            target.sendMessage(ChatColor.RED + "You cannot use this command while jailed!");
            return true;
        }

        tpAskCommandExecutor tpAskExecutor = (tpAskCommandExecutor) plugin.getCommand("tpask").getExecutor();

        UUID targetUUID = target.getUniqueId();
        if (!tpAskExecutor.hasRequest(targetUUID)) {
            target.sendMessage(ChatColor.RED + "You have no pending teleport requests.");
            return true;
        }

        TeleportRequest request = tpAskExecutor.getRequest(targetUUID);

        Player requester = plugin.getServer().getPlayer(request.getRequesterUUID());
        if (requester == null || !requester.isOnline()) {
            target.sendMessage(ChatColor.RED + "The requester is no longer online.");
            tpAskExecutor.removeRequest(targetUUID);
            return true;
        }

        requester.teleport(target.getLocation());
        tpAskExecutor.removeRequest(targetUUID);

        requester.sendMessage(ChatColor.GREEN + "Teleport request accepted by " + ChatColor.AQUA + target.getName() + ChatColor.GREEN + "!");
        target.sendMessage(ChatColor.GREEN + "You have accepted the teleport request from " + ChatColor.AQUA + requester.getName() + ChatColor.GREEN + ".");
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
