package win.doughmination.dwolves;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.java.JavaPlugin;
import win.doughmination.dwolves.commands.DWolvesCommand;
import win.doughmination.dwolves.listeners.OwnerLoginListener;
import win.doughmination.dwolves.listeners.WolfInteractListener;
import win.doughmination.dwolves.trust.TrustManager;
import win.doughmination.dwolves.wolf.TempOwnershipManager;

public class DWolvesPlugin extends JavaPlugin {

    private TrustManager trustManager;
    private TempOwnershipManager tempOwnershipManager;

    @Override
    public void onEnable() {
        this.trustManager = new TrustManager(this);
        trustManager.load();

        this.tempOwnershipManager = new TempOwnershipManager(this);
        tempOwnershipManager.load();
        tempOwnershipManager.revertLoadedWolves();

        new DWolvesCommand(this).register();
        getServer().getPluginManager().registerEvents(new WolfInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new OwnerLoginListener(this), this);
        // TempOwnershipManager listens for ChunkLoadEvent to catch late-loading wolves
        getServer().getPluginManager().registerEvents(tempOwnershipManager, this);

        getComponentLogger().info(Component.text("dWolves enabled!", NamedTextColor.GREEN));
    }

    @Override
    public void onDisable() {
        if (trustManager != null) {
            trustManager.save();
        }
        if (tempOwnershipManager != null) {
            tempOwnershipManager.save();
        }
        getComponentLogger().info(Component.text("dWolves disabled.", NamedTextColor.RED));
    }

    public TrustManager getTrustManager() {
        return trustManager;
    }

    public TempOwnershipManager getTempOwnershipManager() {
        return tempOwnershipManager;
    }
}