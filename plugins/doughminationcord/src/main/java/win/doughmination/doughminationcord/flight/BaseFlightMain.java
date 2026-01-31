/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughminationcord.flight;

import win.doughmination.doughminationcord.CordMain;
import org.bukkit.Bukkit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BaseFlightMain {

    private final CordMain doughPlugin;
    private final Map<UUID, Boolean> flightToggles;
    private final Map<String, FlyZone> communalFlyZones;

    public BaseFlightMain(CordMain plugin) {
        this.doughPlugin = plugin;
        this.flightToggles = new HashMap<>();
        this.communalFlyZones = new HashMap<>();
    }

    public void onEnable() {
        // Register commands and events
        Bukkit.getPluginManager().registerEvents(new FlightListener(this, doughPlugin), doughPlugin);

        doughPlugin.getCommand("basefly").setExecutor(new BaseFlyCommandExecutor(this, doughPlugin));
        doughPlugin.getCommand("flyzone").setExecutor(new FlyZoneCommandExecutor(this));
        doughPlugin.getCommand("rmflyzone").setExecutor(new RemoveFlyZoneCommandExecutor(this));

        // Start flight zone checks
        new FlightCheckTask(this, doughPlugin).runTaskTimer(doughPlugin, 20L, 20L);
    }

    public Map<UUID, Boolean> getFlightToggles() {
        return flightToggles;
    }

    public Map<String, FlyZone> getCommunalFlyZones() {
        return communalFlyZones;
    }

    public static class FlyZone {
        private final String name;
        private final org.bukkit.Location corner1;
        private final org.bukkit.Location corner2;

        public FlyZone(String name, org.bukkit.Location corner1, org.bukkit.Location corner2) {
            this.name = name;
            this.corner1 = corner1;
            this.corner2 = corner2;
        }

        public boolean isWithinZone(org.bukkit.Location location) {
            double minX = Math.min(corner1.getX(), corner2.getX());
            double maxX = Math.max(corner1.getX(), corner2.getX());
            double minY = Math.min(corner1.getY(), corner2.getY());
            double maxY = Math.max(corner1.getY(), corner2.getY());
            double minZ = Math.min(corner1.getZ(), corner2.getZ());
            double maxZ = Math.max(corner1.getZ(), corner2.getZ());

            return location.getX() >= minX && location.getX() <= maxX &&
                    location.getY() >= minY && location.getY() <= maxY &&
                    location.getZ() >= minZ && location.getZ() <= maxZ;
        }

        public String getName() {
            return name;
        }
    }
}
