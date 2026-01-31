package win.doughmination.statsend;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;
import org.bstats.bukkit.Metrics;

public class StatsMain extends JavaPlugin {

    private EventManager eventManager;

    @Override
    public void onEnable() {
        int pluginId = 27179; // Replace with your actual plugin id
        Metrics metrics = new Metrics(this, pluginId);

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        eventManager = new EventManager(this);
        getServer().getPluginManager().registerEvents(new EventListener(eventManager), this);

        getLogger().info("End Stats Paper plugin enabled!");
    }

    @Override
    public void onDisable() {
        if (eventManager != null) {
            eventManager.saveData();
        }
        getLogger().info("End Stats Paper plugin disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("estart")) {
            if (!sender.hasPermission("dragonevent.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                return true;
            }

            eventManager.startEvent();
            sender.sendMessage(ChatColor.GREEN + "Dragon event started!");
            getServer().broadcastMessage(ChatColor.YELLOW + "Dragon Event has begun! Jump into the End to participate!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("estop")) {
            if (!sender.hasPermission("dragonevent.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                return true;
            }

            eventManager.stopEvent();
            sender.sendMessage(ChatColor.RED + "Dragon event stopped and reset!");
            return true;
        }

        return false;
    }
}