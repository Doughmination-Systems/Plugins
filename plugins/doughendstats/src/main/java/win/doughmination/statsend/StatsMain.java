package win.doughmination.statsend;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bstats.bukkit.Metrics;
import win.doughmination.statsend.utils.EventListener;
import win.doughmination.statsend.utils.EventManager;

public class StatsMain extends JavaPlugin {

    private EventManager eventManager;

    @Override
    public void onEnable() {
        int pluginId = 29921;
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
                sender.sendRichMessage("<red>You don't have permission to use this command!</red>");
                return true;
            }

            eventManager.startEvent();
            sender.sendRichMessage("<green>Dragon event started!</green>");
            getServer().broadcast(MiniMessage.miniMessage().deserialize("<yellow>Dragon Event has begun! Jump into the End to participate!</yellow>"));
            return true;
        }

        if (command.getName().equalsIgnoreCase("estop")) {
            if (!sender.hasPermission("dragonevent.admin")) {
                sender.sendRichMessage("<red>You don't have permission to use this command!</red>");
                return true;
            }

            eventManager.stopEvent();
            sender.sendRichMessage("<red>Dragon event stopped and reset!</red>");
            return true;
        }

        return false;
    }
}