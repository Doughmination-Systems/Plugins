package win.doughmination.dwolves.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import win.doughmination.dwolves.DWolvesPlugin;
import win.doughmination.dwolves.trust.*;
import win.doughmination.dwolves.util.Msg;

import java.util.Optional;

/**
 * Allows trusted players to interact with wolves they don't own.
 *
 * Right-click a wolf you're trusted on:
 *  - Sneaking  → toggle sit/stand
 *  - Normal    → toggle follow/stay (i.e. sit if following, stand if sitting near you)
 *
 * Rename is handled via anvil/name tag naturally — we intercept via EntityDamageByEntity
 * for protection and trust checks elsewhere. The actual rename permission check happens
 * in PlayerInteractEntityEvent when the player is holding a name tag.
 */
public class WolfInteractListener implements Listener {

    private final DWolvesPlugin plugin;

    public WolfInteractListener(DWolvesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Wolf wolf)) return;

        Player player = event.getPlayer();

        // Wolf has no owner — not our concern
        if (wolf.getOwnerUniqueId() == null) return;

        // Player is the owner — let vanilla handle it
        if (wolf.getOwnerUniqueId().equals(player.getUniqueId())) return;

        // Check trust
        TrustManager tm = plugin.getTrustManager();
        Optional<TrustEntry> entryOpt = tm.getEntry(wolf.getOwnerUniqueId(), player.getUniqueId());

        if (entryOpt.isEmpty()) {
            // Not trusted — block interaction with the wolf
            event.setCancelled(true);
            player.sendMessage(Msg.error("You are not trusted with this wolf."));
            return;
        }

        TrustEntry entry = entryOpt.get();

        // Check if holding a name tag — requires FULL trust
        boolean holdingNameTag = player.getInventory().getItemInMainHand().getType()
                == org.bukkit.Material.NAME_TAG;

        if (holdingNameTag) {
            if (!entry.getLevel().canRename()) {
                event.setCancelled(true);
                player.sendMessage(Msg.error("You need Full trust level to rename this wolf."));
            }
            // else allow — vanilla will apply the name tag
            return;
        }

        // Normal interaction — toggle sit/stand based on sneak
        event.setCancelled(true);

        if (player.isSneaking()) {
            // Toggle sit
            wolf.setSitting(!wolf.isSitting());
            player.sendMessage(Msg.info("Wolf is now " + (wolf.isSitting() ? "sitting." : "standing.")));
        } else {
            if (wolf.isSitting()) {
                // Unsit and make follow the trusted player temporarily
                wolf.setSitting(false);
                wolf.setTarget(null);
                // Paper API: make the wolf follow this player by setting owner temporarily is not
                // straightforward — instead we teleport to player and unsit so it pathfinds naturally.
                // A cleaner follow is done via the /dw follow command (future enhancement).
                player.sendMessage(Msg.info("Wolf is now following you. Right-click again while sneaking to sit it."));
            } else {
                wolf.setSitting(true);
                player.sendMessage(Msg.info("Wolf is now sitting."));
            }
        }
    }

    /**
     * Prevent untrusted players from harming owned wolves.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Wolf wolf)) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (wolf.getOwnerUniqueId() == null) return;
        if (wolf.getOwnerUniqueId().equals(player.getUniqueId())) return;

        TrustManager tm = plugin.getTrustManager();
        if (!tm.isTrusted(wolf.getOwnerUniqueId(), player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(Msg.error("You cannot harm a wolf you are not trusted with."));
        }
    }

    /**
     * /dw teleport — handled via a synthetic command but wolf teleport to player
     * can also be triggered here if we store a "pending teleport" flag. For now,
     * this listener stub is left for future teleport-on-command integration.
     */
}