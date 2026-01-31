package win.dougmination.plural.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class SystemCommandTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return null; // No suggestions for the console mfs
        }

        if (args.length == 1) {
            return Arrays.asList("create", "rename", "remove"); // Main subcommands
        } else if ((args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("rename")) && args.length == 2) {
            return List.of("<Name>"); // Name input Placeholder
        }

        return null;

    }

}