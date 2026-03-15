/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 */

package win.doughmination.jaileddough.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import win.doughmination.api.JailData;
import win.doughmination.api.LibMain;
import win.doughmination.jaileddough.JailMain;

public class JailCommand implements CommandExecutor {

    private final JailMain plugin;

    public JailCommand(JailMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (plugin.getJailLocation() == null) {
            sender.sendRichMessage("<red>Jail location has not been set! Use /setjail first.</red>");
            return true;
        }

        if (args.length < 1 || args.length > 2) {
            sender.sendRichMessage("<red>Usage: /jail <player> [time]</red>");
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            sender.sendRichMessage("<red>Player not found!</red>");
            return true;
        }

        long jailDurationMs = 0;
        if (args.length == 2) {
            try {
                jailDurationMs = Long.parseLong(args[1]) * 1000L;
            } catch (NumberFormatException e) {
                sender.sendRichMessage("<red>Invalid time! Use a whole number of seconds.</red>");
                return true;
            }
        }

        // Persist where the player was BEFORE teleporting them.
        plugin.getLocationStorage().store(target.getUniqueId(), target.getLocation());

        // Persist the release timestamp so it survives restarts.
        if (jailDurationMs > 0) {
            plugin.getTimerStorage().store(target.getUniqueId(), System.currentTimeMillis() + jailDurationMs);
        }

        target.teleport(plugin.getJailLocation());
        target.sendRichMessage("<red>You have been jailed!</red>");
        sender.sendRichMessage("<green>Player " + target.getName() + " has been jailed!</green>");

        LibMain doughApi = LibMain.getInstance();
        if (doughApi != null) {
            doughApi.setPlayerJailData(
                    target.getUniqueId(),
                    new JailData(true, System.currentTimeMillis() + jailDurationMs, plugin.getJailLocation(), null)
            );
        } else {
            plugin.getLogger().severe("DoughAPI instance is null while jailing a player!");
        }

        return true;
    }
}