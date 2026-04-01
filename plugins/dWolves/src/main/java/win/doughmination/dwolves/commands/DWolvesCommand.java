package win.doughmination.dwolves.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import win.doughmination.dwolves.DWolvesPlugin;
import win.doughmination.dwolves.trust.*;
import win.doughmination.dwolves.util.Msg;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class DWolvesCommand {

    private final DWolvesPlugin plugin;

    public DWolvesCommand(DWolvesPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            var root = Commands.literal("dwolves")
                    .then(trustBranch())
                    .then(untrustBranch())
                    .then(setlevelBranch())
                    .then(acceptBranch())
                    .then(denyBranch())
                    .then(listBranch())
                    .then(teleportBranch())
                    .then(helpBranch())
                    .executes(ctx -> {
                        sendHelp(ctx.getSource());
                        return Command.SINGLE_SUCCESS;
                    })
                    .build();

            commands.register(root, "dWolves root command", List.of("dw"));
        });
    }

    // -------------------------------------------------------------------------
    // /dwolves trust <player> [basic|full]
    // -------------------------------------------------------------------------

    private com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> trustBranch() {
        return Commands.literal("trust")
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("level", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("basic");
                                    builder.suggest("full");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> executeTrust(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player"),
                                        StringArgumentType.getString(ctx, "level")))
                        )
                        .executes(ctx -> executeTrust(ctx.getSource(),
                                StringArgumentType.getString(ctx, "player"),
                                "basic"))
                )
                .build();
    }

    private int executeTrust(CommandSourceStack source, String targetName, String levelStr) {
        if (!(source.getSender() instanceof Player owner)) {
            source.getSender().sendMessage(Msg.error("Only players can use this command."));
            return 0;
        }

        if (owner.getName().equalsIgnoreCase(targetName)) {
            owner.sendMessage(Msg.error("You cannot trust yourself."));
            return 0;
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            owner.sendMessage(Msg.error("Player '" + targetName + "' is not online."));
            return 0;
        }

        TrustLevel level = TrustLevel.fromString(levelStr);
        TrustManager tm = plugin.getTrustManager();

        // If already trusted, just update level
        if (tm.isTrusted(owner.getUniqueId(), target.getUniqueId())) {
            tm.setLevel(owner.getUniqueId(), target.getUniqueId(), level);
            owner.sendMessage(Msg.success("Updated " + target.getName() + "'s trust level to " + level.displayName() + "."));
            target.sendMessage(Msg.info(owner.getName() + " updated your wolf trust level to " + level.displayName() + "."));
            return Command.SINGLE_SUCCESS;
        }

        // Send a pending request
        PendingRequest request = new PendingRequest(owner.getUniqueId(), owner.getName(), target.getUniqueId(), level);
        tm.addPendingRequest(request);

        owner.sendMessage(Msg.info("Trust request sent to " + target.getName() + " (" + level.displayName() + "). Waiting for confirmation..."));
        target.sendMessage(Msg.warn(owner.getName() + " wants to trust you with their wolves at level " +
                Component.text(level.displayName(), NamedTextColor.AQUA).content() +
                ". Use /dw accept or /dw deny within 60 seconds."));

        return Command.SINGLE_SUCCESS;
    }

    // -------------------------------------------------------------------------
    // /dwolves untrust <player>
    // -------------------------------------------------------------------------

    private com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> untrustBranch() {
        return Commands.literal("untrust")
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            if (ctx.getSource().getSender() instanceof Player owner) {
                                plugin.getTrustManager().getEntriesForOwner(owner.getUniqueId())
                                        .forEach(e -> {
                                            OfflinePlayer op = Bukkit.getOfflinePlayer(e.getTrustedUuid());
                                            if (op.getName() != null) builder.suggest(op.getName());
                                        });
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> executeUntrust(ctx.getSource(),
                                StringArgumentType.getString(ctx, "player")))
                )
                .build();
    }

    private int executeUntrust(CommandSourceStack source, String targetName) {
        if (!(source.getSender() instanceof Player owner)) {
            source.getSender().sendMessage(Msg.error("Only players can use this command."));
            return 0;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(targetName);
        if (target == null) {
            owner.sendMessage(Msg.error("Unknown player '" + targetName + "'."));
            return 0;
        }

        boolean removed = plugin.getTrustManager().removeTrust(owner.getUniqueId(), target.getUniqueId());
        if (!removed) {
            owner.sendMessage(Msg.error(targetName + " is not trusted."));
            return 0;
        }

        owner.sendMessage(Msg.success("Removed trust for " + targetName + "."));
        Player online = target.getPlayer();
        if (online != null) {
            online.sendMessage(Msg.warn(owner.getName() + " has revoked your wolf trust."));
        }
        return Command.SINGLE_SUCCESS;
    }

    // -------------------------------------------------------------------------
    // /dwolves setlevel <player> <basic|full>
    // -------------------------------------------------------------------------

    private com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> setlevelBranch() {
        return Commands.literal("setlevel")
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            if (ctx.getSource().getSender() instanceof Player owner) {
                                plugin.getTrustManager().getEntriesForOwner(owner.getUniqueId())
                                        .forEach(e -> {
                                            OfflinePlayer op = Bukkit.getOfflinePlayer(e.getTrustedUuid());
                                            if (op.getName() != null) builder.suggest(op.getName());
                                        });
                            }
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("level", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("basic");
                                    builder.suggest("full");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> executeSetLevel(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player"),
                                        StringArgumentType.getString(ctx, "level")))
                        )
                )
                .build();
    }

    private int executeSetLevel(CommandSourceStack source, String targetName, String levelStr) {
        if (!(source.getSender() instanceof Player owner)) {
            source.getSender().sendMessage(Msg.error("Only players can use this command."));
            return 0;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(targetName);
        if (target == null) {
            owner.sendMessage(Msg.error("Unknown player '" + targetName + "'."));
            return 0;
        }

        TrustManager tm = plugin.getTrustManager();
        if (!tm.isTrusted(owner.getUniqueId(), target.getUniqueId())) {
            owner.sendMessage(Msg.error(targetName + " is not trusted. Trust them first with /dw trust."));
            return 0;
        }

        TrustLevel level = TrustLevel.fromString(levelStr);
        tm.setLevel(owner.getUniqueId(), target.getUniqueId(), level);
        owner.sendMessage(Msg.success("Set " + targetName + "'s trust level to " + level.displayName() + "."));

        Player online = target.getPlayer();
        if (online != null) {
            online.sendMessage(Msg.info(owner.getName() + " updated your wolf trust level to " + level.displayName() + "."));
        }
        return Command.SINGLE_SUCCESS;
    }

    // -------------------------------------------------------------------------
    // /dwolves accept
    // -------------------------------------------------------------------------

    private com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> acceptBranch() {
        return Commands.literal("accept")
                .executes(ctx -> executeAccept(ctx.getSource()))
                .build();
    }

    private int executeAccept(CommandSourceStack source) {
        if (!(source.getSender() instanceof Player target)) {
            source.getSender().sendMessage(Msg.error("Only players can use this command."));
            return 0;
        }

        TrustManager tm = plugin.getTrustManager();
        Optional<PendingRequest> opt = tm.getPendingRequest(target.getUniqueId());
        if (opt.isEmpty()) {
            target.sendMessage(Msg.error("You have no pending trust request."));
            return 0;
        }

        PendingRequest req = opt.get();
        tm.removePendingRequest(target.getUniqueId());
        tm.addTrust(req.getOwnerUuid(), target.getUniqueId(), req.getLevel());

        target.sendMessage(Msg.success("Accepted! You can now manage " + req.getOwnerName() + "'s wolves (" + req.getLevel().displayName() + " level)."));

        Player owner = Bukkit.getPlayer(req.getOwnerUuid());
        if (owner != null) {
            owner.sendMessage(Msg.success(target.getName() + " accepted your wolf trust request (" + req.getLevel().displayName() + ")."));
        }
        return Command.SINGLE_SUCCESS;
    }

    // -------------------------------------------------------------------------
    // /dwolves deny
    // -------------------------------------------------------------------------

    private com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> denyBranch() {
        return Commands.literal("deny")
                .executes(ctx -> executeDeny(ctx.getSource()))
                .build();
    }

    private int executeDeny(CommandSourceStack source) {
        if (!(source.getSender() instanceof Player target)) {
            source.getSender().sendMessage(Msg.error("Only players can use this command."));
            return 0;
        }

        TrustManager tm = plugin.getTrustManager();
        Optional<PendingRequest> opt = tm.getPendingRequest(target.getUniqueId());
        if (opt.isEmpty()) {
            target.sendMessage(Msg.error("You have no pending trust request."));
            return 0;
        }

        PendingRequest req = opt.get();
        tm.removePendingRequest(target.getUniqueId());

        target.sendMessage(Msg.info("Denied trust request from " + req.getOwnerName() + "."));
        Player owner = Bukkit.getPlayer(req.getOwnerUuid());
        if (owner != null) {
            owner.sendMessage(Msg.warn(target.getName() + " denied your wolf trust request."));
        }
        return Command.SINGLE_SUCCESS;
    }

    // -------------------------------------------------------------------------
    // /dwolves list
    // -------------------------------------------------------------------------

    private com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> listBranch() {
        return Commands.literal("list")
                .executes(ctx -> executeList(ctx.getSource()))
                .build();
    }

    private int executeList(CommandSourceStack source) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(Msg.error("Only players can use this command."));
            return 0;
        }

        TrustManager tm = plugin.getTrustManager();
        List<TrustEntry> owned   = tm.getEntriesForOwner(player.getUniqueId());
        List<TrustEntry> trusted = tm.getEntriesForTrusted(player.getUniqueId());

        player.sendMessage(Component.text("─── dWolves Trust List ───", NamedTextColor.GOLD));

        player.sendMessage(Component.text("Players trusted with your wolves:", NamedTextColor.YELLOW));
        if (owned.isEmpty()) {
            player.sendMessage(Component.text("  None.", NamedTextColor.GRAY));
        } else {
            owned.forEach(e -> {
                OfflinePlayer op = Bukkit.getOfflinePlayer(e.getTrustedUuid());
                String name = op.getName() != null ? op.getName() : e.getTrustedUuid().toString();
                player.sendMessage(Component.text("  " + name + " — " + e.getLevel().displayName(), NamedTextColor.AQUA));
            });
        }

        player.sendMessage(Component.text("Owners who trust you:", NamedTextColor.YELLOW));
        if (trusted.isEmpty()) {
            player.sendMessage(Component.text("  None.", NamedTextColor.GRAY));
        } else {
            trusted.forEach(e -> {
                OfflinePlayer op = Bukkit.getOfflinePlayer(e.getOwnerUuid());
                String name = op.getName() != null ? op.getName() : e.getOwnerUuid().toString();
                player.sendMessage(Component.text("  " + name + " — " + e.getLevel().displayName(), NamedTextColor.AQUA));
            });
        }

        return Command.SINGLE_SUCCESS;
    }

    // -------------------------------------------------------------------------
    // /dwolves teleport <ownerName>
    // Teleports all of that owner's wolves to you (requires Basic+ trust)
    // -------------------------------------------------------------------------

    private com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> teleportBranch() {
        return Commands.literal("teleport")
                .then(Commands.argument("owner", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            if (ctx.getSource().getSender() instanceof Player p) {
                                plugin.getTrustManager().getEntriesForTrusted(p.getUniqueId())
                                        .forEach(e -> {
                                            OfflinePlayer op = Bukkit.getOfflinePlayer(e.getOwnerUuid());
                                            if (op.getName() != null) builder.suggest(op.getName());
                                        });
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> executeTeleport(ctx.getSource(),
                                StringArgumentType.getString(ctx, "owner")))
                )
                .build();
    }

    private int executeTeleport(CommandSourceStack source, String ownerName) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(Msg.error("Only players can use this command."));
            return 0;
        }

        OfflinePlayer owner = Bukkit.getOfflinePlayerIfCached(ownerName);
        if (owner == null) {
            player.sendMessage(Msg.error("Unknown player '" + ownerName + "'."));
            return 0;
        }

        TrustManager tm = plugin.getTrustManager();
        if (!tm.isTrusted(owner.getUniqueId(), player.getUniqueId())) {
            player.sendMessage(Msg.error("You are not trusted by " + ownerName + "."));
            return 0;
        }

        // Find all wolves in the same world owned by this player
        long count = player.getWorld().getEntitiesByClass(org.bukkit.entity.Wolf.class).stream()
                .filter(w -> owner.getUniqueId().equals(w.getOwnerUniqueId()))
                .peek(w -> {
                    w.setSitting(false);
                    w.teleport(player.getLocation());
                })
                .count();

        if (count == 0) {
            player.sendMessage(Msg.warn("No wolves owned by " + ownerName + " found in this world."));
        } else {
            player.sendMessage(Msg.success("Teleported " + count + " wolf(ves) to you."));
        }
        return Command.SINGLE_SUCCESS;
    }

    // -------------------------------------------------------------------------
    // /dwolves help
    // -------------------------------------------------------------------------

    private com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> helpBranch() {
        return Commands.literal("help")
                .executes(ctx -> {
                    sendHelp(ctx.getSource());
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }

    private void sendHelp(CommandSourceStack source) {
        source.getSender().sendMessage(Component.text("─── dWolves Help ───", NamedTextColor.GOLD));
        source.getSender().sendMessage(Component.text("/dw trust <player> [basic|full]", NamedTextColor.AQUA)
                .append(Component.text(" — Trust a player with your wolves", NamedTextColor.GRAY)));
        source.getSender().sendMessage(Component.text("/dw untrust <player>", NamedTextColor.AQUA)
                .append(Component.text(" — Revoke a player's trust", NamedTextColor.GRAY)));
        source.getSender().sendMessage(Component.text("/dw setlevel <player> <basic|full>", NamedTextColor.AQUA)
                .append(Component.text(" — Change a trusted player's level", NamedTextColor.GRAY)));
        source.getSender().sendMessage(Component.text("/dw accept", NamedTextColor.AQUA)
                .append(Component.text(" — Accept a pending trust request", NamedTextColor.GRAY)));
        source.getSender().sendMessage(Component.text("/dw deny", NamedTextColor.AQUA)
                .append(Component.text(" — Deny a pending trust request", NamedTextColor.GRAY)));
        source.getSender().sendMessage(Component.text("/dw teleport <owner>", NamedTextColor.AQUA)
                .append(Component.text(" — Teleport that owner's wolves to you", NamedTextColor.GRAY)));
        source.getSender().sendMessage(Component.text("/dw list", NamedTextColor.AQUA)
                .append(Component.text(" — View your trust relationships", NamedTextColor.GRAY)));
    }
}