package win.doughmination.dwolves.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import win.doughmination.dwolves.DWolvesPlugin;
import win.doughmination.dwolves.wolf.TempOwnershipManager;

import java.util.Set;
import java.util.UUID;

public class OwnerLoginListener implements Listener {

    private final DWolvesPlugin plugin;

    public OwnerLoginListener(DWolvesPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * When the real owner logs in, revert any of their wolves that are currently
     * under a temporary ownership transfer.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player owner = event.getPlayer();
        TempOwnershipManager tom = plugin.getTempOwnershipManager();

        Set<UUID> ownerWolves = tom.getWolvesByRealOwner(owner.getUniqueId());
        if (ownerWolves.isEmpty()) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        plugin.getServer().getWorlds().forEach(world ->
                                world.getEntitiesByClass(Wolf.class).forEach(wolf -> {
                                    if (!ownerWolves.contains(wolf.getUniqueId())) return;

                                    String name = wolf.customName() != null
                                            ? PlainTextComponentSerializer.plainText().serialize(wolf.customName())
                                            : "Wolf";

                                    tom.revertToPlayer(wolf, owner);
                                    owner.sendMessage(Component.text(
                                            "Your wolf \"" + name + "\" has returned to you.",
                                            NamedTextColor.GREEN
                                    ));
                                })
                        )
                , 20L);
    }

    /**
     * When a trusted player logs out while wolves are following them,
     * revert those wolves immediately.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player trusted = event.getPlayer();
        TempOwnershipManager tom = plugin.getTempOwnershipManager();

        plugin.getServer().getWorlds().forEach(world ->
                world.getEntitiesByClass(Wolf.class).forEach(wolf -> {
                    if (!trusted.getUniqueId().equals(wolf.getOwnerUniqueId())) return;
                    if (!tom.isTemporary(wolf)) return;

                    tom.revert(wolf);
                })
        );
    }
}