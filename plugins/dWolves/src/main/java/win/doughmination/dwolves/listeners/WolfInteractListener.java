package win.doughmination.dwolves.listeners;

import io.papermc.paper.event.player.PlayerNameEntityEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import win.doughmination.dwolves.DWolvesPlugin;
import win.doughmination.dwolves.trust.*;
import win.doughmination.dwolves.util.Msg;
import win.doughmination.dwolves.wolf.TempOwnershipManager;

import java.util.Optional;
import java.util.UUID;

public class WolfInteractListener implements Listener {

    private final DWolvesPlugin plugin;

    public WolfInteractListener(DWolvesPlugin plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Core helper — resolve real owner UUID regardless of temp transfer state
    // -------------------------------------------------------------------------

    /**
     * Returns the real owner UUID of a wolf.
     * If the wolf is under a temporary transfer, returns the original owner from
     * TempOwnershipManager rather than wolf.getOwnerUniqueId() (which would
     * return the trusted player's UUID while transferred).
     */
    private UUID realOwnerOf(Wolf wolf) {
        TempOwnershipManager tom = plugin.getTempOwnershipManager();
        return tom.getRealOwner(wolf).orElse(wolf.getOwnerUniqueId());
    }

    // -------------------------------------------------------------------------
    // Right-click interaction
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Wolf wolf)) return;

        Player player = event.getPlayer();

        UUID realOwner = realOwnerOf(wolf);

        // Wolf has no owner — not our concern
        if (realOwner == null) return;

        // Player is the real owner — let vanilla handle everything
        if (realOwner.equals(player.getUniqueId())) return;

        // Resolve trust entry against the real owner
        TrustManager tm = plugin.getTrustManager();
        Optional<TrustEntry> entryOpt = tm.getEntry(realOwner, player.getUniqueId());

        if (entryOpt.isEmpty()) {
            event.setCancelled(true);
            player.sendMessage(Msg.error("You are not trusted with this wolf."));
            return;
        }

        TrustEntry entry = entryOpt.get();
        Material heldItem = player.getInventory().getItemInMainHand().getType();

        // Feeding — any food item a wolf accepts
        if (isWolfFood(heldItem)) {
            // Basic trust and above can feed
            // Just allow — vanilla handles the heal/tame logic
            return;
        }

        // Name tag — requires FULL trust
        // We cancel here too as a fallback; PlayerNameEntityEvent is the primary guard
        if (heldItem == Material.NAME_TAG) {
            if (!entry.getLevel().canRename()) {
                event.setCancelled(true);
                player.sendMessage(Msg.error("You need Full trust level to rename this wolf."));
            }
            return;
        }

        // Any other right-click — sit/stand toggle
        event.setCancelled(true);

        if (wolf.isSitting()) {
            // Unsit — only if real owner is offline (wolf is currently sitting,
            // meaning it hasn't been transferred yet)
            boolean ownerOnline = plugin.getServer().getPlayer(realOwner) != null;
            if (ownerOnline) {
                player.sendMessage(Msg.warn("The owner is online — you cannot take control of their wolf."));
                return;
            }

            plugin.getTempOwnershipManager().transfer(wolf, player);
            player.sendMessage(Msg.info("Wolf is now following you. Right-click again to sit it back down."));
        } else {
            // Sit back down and revert ownership
            plugin.getTempOwnershipManager().revert(wolf);
            player.sendMessage(Msg.info("Wolf is now sitting."));
        }
    }

    // -------------------------------------------------------------------------
    // Rename guard — PlayerNameEntityEvent fires when a name tag is applied
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onNameEntity(PlayerNameEntityEvent event) {
        if (!(event.getEntity() instanceof Wolf wolf)) return;

        Player player = event.getPlayer();
        UUID realOwner = realOwnerOf(wolf);
        if (realOwner == null) return;
        if (realOwner.equals(player.getUniqueId())) return;

        TrustManager tm = plugin.getTrustManager();
        Optional<TrustEntry> entryOpt = tm.getEntry(realOwner, player.getUniqueId());

        if (entryOpt.isEmpty()) {
            event.setCancelled(true);
            player.sendMessage(Msg.error("You are not trusted with this wolf."));
            return;
        }

        if (!entryOpt.get().getLevel().canRename()) {
            event.setCancelled(true);
            player.sendMessage(Msg.error("You need Full trust level to rename this wolf."));
        }
    }

    // -------------------------------------------------------------------------
    // Damage guard
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Wolf wolf)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        UUID realOwner = realOwnerOf(wolf);
        if (realOwner == null) return;
        if (realOwner.equals(player.getUniqueId())) return;

        TrustManager tm = plugin.getTrustManager();
        if (!tm.isTrusted(realOwner, player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(Msg.error("You are not trusted with this wolf — it won't take kindly to that."));
            wolf.setTarget(player);
        } else {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (wolf.getTarget() instanceof Player target && target.equals(player)) {
                    wolf.setTarget(null);
                }
            });
        }
    }

    // -------------------------------------------------------------------------
    // Targeting guard — prevent wolves retaliating against trusted players
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWolfTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof Wolf wolf)) return;
        if (!(event.getTarget() instanceof Player player)) return;

        UUID realOwner = realOwnerOf(wolf);
        if (realOwner == null) return;
        if (realOwner.equals(player.getUniqueId())) return;

        TrustManager tm = plugin.getTrustManager();
        if (tm.isTrusted(realOwner, player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // -------------------------------------------------------------------------
    // Util
    // -------------------------------------------------------------------------

    private boolean isWolfFood(Material material) {
        return switch (material) {
            case BEEF, COOKED_BEEF,
                 CHICKEN, COOKED_CHICKEN,
                 PORKCHOP, COOKED_PORKCHOP,
                 MUTTON, COOKED_MUTTON,
                 RABBIT, COOKED_RABBIT,
                 ROTTEN_FLESH -> true;
            default -> false;
        };
    }
}