/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughminationcord.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import win.doughmination.doughminationcord.CordMain;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EChestCommandExecutor implements CommandExecutor {

    // Stores persistent VIP inventories per player (survives command re-use, cleared on server restart)
    private final Map<UUID, Inventory> vipInventories = new HashMap<>();
    private final CordMain plugin;

    public EChestCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        if (!player.hasPermission("dough.echest")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            return true;
        }

        // VIP: open a persistent double-chest sized inventory
        if (player.hasPermission("dough.echest.vip")) {
            UUID uuid = player.getUniqueId();
            Inventory vipChest = vipInventories.computeIfAbsent(uuid, k ->
                    Bukkit.createInventory(player, 54,
                            ChatColor.LIGHT_PURPLE + "✦ " + player.getName() + "'s VIP Chest"));
            player.openInventory(vipChest);
            return true;
        }

        // Regular: open the player's real vanilla ender chest
        player.openInventory(player.getEnderChest());
        return true;
    }
}