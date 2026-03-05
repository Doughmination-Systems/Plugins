/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughminationcord.listeners.veinminer;

import win.doughmination.doughminationcord.CordMain;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.*;

public class handleTreeVeinminer {

    private final CordMain plugin;

    public handleTreeVeinminer(CordMain plugin) {
        this.plugin = plugin;
    }

    public void handleTreeBreak(Player player, Block block) {
        Material blockType = block.getType();
        if (!isLog(blockType)) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        boolean usingAxe = isAxe(tool.getType());

        Location start = block.getLocation();

        if (!usingAxe) {
            // Fists (or any non-axe): break just the one block, drop 1 log, no veinmine
            block.setType(Material.AIR);
            block.getWorld().dropItemNaturally(start, new ItemStack(blockType, 1));
            return;
        }

        // Axe: full veinmine
        boolean silkTouch = tool.getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0;
        Set<Location> treeBlocks = new HashSet<>();
        findTreeBlocks(start, blockType, treeBlocks);

        for (Location loc : treeBlocks) {
            loc.getBlock().setType(Material.AIR);
            loc.getWorld().dropItemNaturally(loc, new ItemStack(blockType));
        }

        player.playSound(player.getLocation(), Sound.BLOCK_WOOD_BREAK, 1.0f, 1.0f);
        player.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, start, 20, 1.0, 1.0, 1.0, 0.1);
    }

    private boolean isAxe(Material m) {
        return m == Material.WOODEN_AXE || m == Material.STONE_AXE
                || m == Material.COPPER_AXE || m == Material.IRON_AXE
                || m == Material.GOLDEN_AXE || m == Material.DIAMOND_AXE
                || m == Material.NETHERITE_AXE;
    }

    private boolean isLog(Material material) {
        return material.name().endsWith("_LOG");
    }

    private void findTreeBlocks(Location start, Material blockType, Set<Location> foundBlocks) {
        int maxBlocks = plugin.getConfig().getInt("tree-remover.max-blocks", 100);

        Queue<Location> toCheck = new LinkedList<>();
        toCheck.add(start);

        while (!toCheck.isEmpty() && foundBlocks.size() < maxBlocks) {
            Location current = toCheck.poll();
            if (foundBlocks.contains(current)) continue;
            if (current.getBlock().getType() != blockType) continue;

            foundBlocks.add(current);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        toCheck.add(current.clone().add(dx, dy, dz));
                    }
                }
            }
        }
    }
}