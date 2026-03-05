/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughminationcord.commands.travel;

import win.doughmination.doughminationcord.CordMain;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import win.doughmination.api.LibMain;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class setBaseCommandExecutor implements CommandExecutor, org.bukkit.command.TabCompleter {

    private static final long COOLDOWN_MILLIS = TimeUnit.MINUTES.toMillis(30);
    private final CordMain plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

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

        // Cooldown check (bypass for ops)
        if (!player.isOp()) {
            long now = System.currentTimeMillis();
            if (cooldowns.containsKey(playerUUID)) {
                long elapsed = now - cooldowns.get(playerUUID);
                if (elapsed < COOLDOWN_MILLIS) {
                    long remaining = COOLDOWN_MILLIS - elapsed;
                    long mins = TimeUnit.MILLISECONDS.toMinutes(remaining);
                    long secs = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60;
                    player.sendMessage(ChatColor.RED + "You must wait " +
                            ChatColor.YELLOW + mins + "m " + secs + "s" +
                            ChatColor.RED + " before setting your base again.");
                    return true;
                }
            }
            cooldowns.put(playerUUID, now);
        }

        Location location = player.getLocation();

        plugin.getBases().put(playerUUID, location);
        plugin.getPlayerDataManager().saveBase(playerUUID, location);

        player.sendMessage(ChatColor.GREEN + "Your base location has been set!");
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
