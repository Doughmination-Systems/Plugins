package win.doughmination.dwolves;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.java.JavaPlugin;
import win.doughmination.dwolves.commands.DWolvesCommand;
import win.doughmination.dwolves.listeners.WolfInteractListener;
import win.doughmination.dwolves.trust.TrustManager;

public class DWolvesPlugin extends JavaPlugin {

    private TrustManager trustManager;

    @Override
    public void onEnable() {
        this.trustManager = new TrustManager(this);
        trustManager.load();

        new DWolvesCommand(this).register();
        getServer().getPluginManager().registerEvents(new WolfInteractListener(this), this);

        getComponentLogger().info(Component.text("dWolves enabled!", NamedTextColor.GREEN));
    }

    @Override
    public void onDisable() {
        if (trustManager != null) {
            trustManager.save();
        }
        getComponentLogger().info(Component.text("dWolves disabled.", NamedTextColor.RED));
    }

    public TrustManager getTrustManager() {
        return trustManager;
    }
}