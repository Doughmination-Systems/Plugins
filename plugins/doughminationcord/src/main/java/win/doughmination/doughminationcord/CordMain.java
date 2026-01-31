/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughminationcord;

import win.doughmination.api.LibMain;
import win.doughmination.doughminationcord.flight.*;
import win.doughmination.doughminationcord.listeners.*;
import win.doughmination.doughminationcord.size.*;
import win.doughmination.doughminationcord.spawneggs.*;
import win.doughmination.doughminationcord.commands.*;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;

public class CordMain extends JavaPlugin {

    // Variables to store player data like playtime and bases
    private final Map<UUID, Long> playtimeMap = new HashMap<>();
    private final Map<UUID, Long> loginTimestamps = new HashMap<>();
    private final Map<UUID, Location> bases = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        String startupMessage =
                ChatColor.AQUA + "Doughmination is starting up...";
        getLogger().info(startupMessage);

        if (LibMain.getInstance() == null) {
            getLogger().severe("DoughminationAPI is not initialized! Ensure it is installed and loaded.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Load the config file
        saveDefaultConfig();

        // Register all your commands
        registerCommands();

        RecipeManager.registerRecipes(this);
        PotionRecipeManager.registerRecipes(this);

        // Load bases from config (if saved)
        loadBases();

        // Flight
        BaseFlightMain baseFlightMain = new BaseFlightMain(this);
        baseFlightMain.onEnable();
        getServer().getPluginManager().registerEvents(new FlightListener(baseFlightMain, this), this);
        new FlightCheckTask(baseFlightMain, this).runTaskTimer(this, 20L, 20L);

        // Veinminer
        veinminerCommandExecutor veinMinerExecutor = new veinminerCommandExecutor(this);
        getServer().getPluginManager().registerEvents(new blockVeinminerListener(this, veinMinerExecutor), this);

        // Potions
        getServer().getPluginManager().registerEvents(new PotionUseListener(this), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        String shutdownMessage =
                ChatColor.AQUA + "Doughminationcord is shutting down...";

        getLogger().info(shutdownMessage);

        // Save all player bases to config
        saveBases();

        // Additional cleanup if necessary
    }

    private void registerCommands() {
        // Register command executors
        getCommand("setspawn").setExecutor(new setSpawnCommandExecutor(this));
        getCommand("spawn").setExecutor(new spawnCommandExecutor(this));
        getCommand("tpask").setExecutor(new tpAskCommandExecutor(this));
        getCommand("tpaccept").setExecutor(new tpAcceptCommandExecutor(this));
        getCommand("tpdeny").setExecutor(new tpDenyCommandExecutor(this));
        getCommand("setbase").setExecutor(new setBaseCommandExecutor(this));
        getCommand("visitbase").setExecutor(new visitBaseCommandExecutor(this));
        getCommand("base").setExecutor(new baseCommandExecutor(this));
        getCommand("kitty").setExecutor(new kittyCommandExecutor(this));
        getCommand("kiss").setExecutor(new kissCommandExecutor(this));
        getCommand("playtime").setExecutor(new playtimeCommandExecutor(this));
        getCommand("veinminer").setExecutor(new veinminerCommandExecutor(this));
        getCommand("visitbase").setTabCompleter(new visitBaseCommandExecutor(this));
        getCommand("kiss").setTabCompleter(new kissCommandExecutor(this));
        getCommand("dough").setExecutor(new DoughCommandExecutor(this));
        getCommand("version").setExecutor(new VersionCommandExecutor(this));
        getCommand("doughreload").setExecutor(new ReloadCommandExecutor(this));
        getCommand("recipes").setExecutor(new RecipesCommandExecutor(this));
        getCommand("growthpotion").setExecutor(new GrowthShrinkPotionCommand(this));
        getCommand("shrinkpotion").setExecutor(new GrowthShrinkPotionCommand(this));

        // Ban commands (via CloveLib)
        BanCommandExecutor banExecutor = new BanCommandExecutor(this);
        getCommand("ecban").setExecutor(banExecutor);
        getCommand("ecban").setTabCompleter(banExecutor);

        UnbanCommandExecutor unbanExecutor = new UnbanCommandExecutor(this);
        getCommand("unban").setExecutor(unbanExecutor);
        getCommand("unban").setTabCompleter(unbanExecutor);

        getCommand("banlist").setExecutor(new BanlistCommandExecutor(this));
    }

    public Map<UUID, Long> getPlaytimeMap() {
        return playtimeMap;
    }

    public Map<UUID, Long> getLoginTimestamps() {
        return loginTimestamps;
    }

    public Map<UUID, Location> getBases() {
        return bases;
    }

    // Add and retrieve jailed status for players
    public boolean isPlayerJailed(Player player) {
        LibMain cloveLib = LibMain.getInstance();
        return cloveLib != null && cloveLib.isPlayerJailed(player.getUniqueId());
    }

    public Location getBaseLocation(UUID playerUUID) {
        return bases.get(playerUUID);
    }

    public boolean hasBase(UUID playerUUID) {
        return bases.containsKey(playerUUID);
    }

    private void loadBases() {
        if (!getConfig().contains("bases")) return;

        for (String key : getConfig().getConfigurationSection("bases").getKeys(false)) {
            try {
                UUID playerUUID = UUID.fromString(key);
                Location baseLocation = Location.deserialize(getConfig().getConfigurationSection("bases." + key).getValues(false));

                bases.put(playerUUID, baseLocation);
            } catch (Exception e) {
                getLogger().warning("Failed to load base for player UUID: " + key);
            }
        }
    }

    private void saveBases() {
        for (Map.Entry<UUID, Location> entry : bases.entrySet()) {
            UUID playerUUID = entry.getKey();
            Location location = entry.getValue();

            getConfig().set("bases." + playerUUID, location.serialize());
        }

        saveConfig();
    }
}