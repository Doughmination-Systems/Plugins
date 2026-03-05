/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughminationcord.commands.travel;

import win.doughmination.doughminationcord.CordMain;
import win.doughmination.doughminationcord.commands.travel.tpAskCommandExecutor.TeleportRequest;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import win.doughmination.api.LibMain;

import java.util.UUID;

public class tpDenyCommandExecutor implements CommandExecutor, org.bukkit.command.TabCompleter {

    private final CordMain plugin;

    public tpDenyCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player target)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        if (!LibMain.getInstance().canUseCommand(target, "tpdeny")) {
            target.sendMessage(ChatColor.RED + "You cannot use this command while jailed!");
            return true;
        }

        tpAskCommandExecutor tpAskExecutor = (tpAskCommandExecutor) plugin.getCommand("tpask").getExecutor();

        UUID targetUUID = target.getUniqueId();
        if (!tpAskExecutor.hasRequest(targetUUID)) {
            target.sendMessage(ChatColor.RED + "You have no pending teleport requests to deny.");
            return true;
        }

        TeleportRequest request = tpAskExecutor.getRequest(targetUUID);
        Player requester = plugin.getServer().getPlayer(request.getRequesterUUID());

        if (requester != null && requester.isOnline()) {
            requester.sendMessage(ChatColor.RED + "Your teleport request to " + ChatColor.AQUA + target.getName() + ChatColor.RED + " was denied.");
        }

        target.sendMessage(ChatColor.YELLOW + "You have denied the teleport request.");
        tpAskExecutor.removeRequest(targetUUID);
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
