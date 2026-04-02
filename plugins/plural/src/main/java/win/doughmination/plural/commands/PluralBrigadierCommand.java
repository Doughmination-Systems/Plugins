package win.doughmination.plural.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import win.doughmination.plural.PluralConfig;
import win.doughmination.plural.PluralMain;

/**
 * Registers /plural (alias /pl) using Paper's Brigadier API.
 * Replaces PluralCommand + PluralCommandTabCompleter.
 *
 * Subcommands: system (alias sys), sync, help
 */
@SuppressWarnings("UnstableApiUsage")
public final class PluralBrigadierCommand {

    private PluralBrigadierCommand() {}

    public static void register(Plugin plugin) {
        LifecycleEventManager<Plugin> manager = plugin.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            LiteralArgumentBuilder<CommandSourceStack> root =
                    Commands.literal("plural")
                            .requires(src -> src.getSender().hasPermission("plural.use"))
                            .executes(ctx -> {
                                ctx.getSource().getSender().sendRichMessage(
                                        "<gray>Usage: /plural <system|sync|help>");
                                return Command.SINGLE_SUCCESS;
                            })
                            .then(Commands.literal("help")
                                    .executes(ctx -> {
                                        sendHelp(ctx.getSource());
                                        return Command.SINGLE_SUCCESS;
                                    }))
                            .then(Commands.literal("system")
                                    .executes(ctx -> {
                                        showSystem(ctx.getSource());
                                        return Command.SINGLE_SUCCESS;
                                    }))
                            .then(Commands.literal("sys")   // alias kept for muscle memory
                                    .executes(ctx -> {
                                        showSystem(ctx.getSource());
                                        return Command.SINGLE_SUCCESS;
                                    }))
                            .then(Commands.literal("sync")
                                    .executes(ctx -> {
                                        doSync(ctx.getSource());
                                        return Command.SINGLE_SUCCESS;
                                    }));

            commands.register(root.build(), "Plural Cloud commands", java.util.List.of("pl"));
        });
    }

    // ---- helpers ----

    private static void sendHelp(CommandSourceStack src) {
        String url = PluralConfig.API_URL;
        src.getSender().sendRichMessage("<aqua>── Plural Cloud ──");
        src.getSender().sendRichMessage("<gray>All system management is done via the dashboard.");
        src.getSender().sendRichMessage("<gray>Visit: <aqua>" + url + "/dashboard");
        src.getSender().sendRichMessage("");
        src.getSender().sendRichMessage("<gray>/plural system <white>— view your system info");
        src.getSender().sendRichMessage("<gray>/plural sync   <white>— refresh your data from the server");
        src.getSender().sendRichMessage("<gray>/plural help   <white>— show this message");
    }

    private static void doSync(CommandSourceStack src) {
        if (!(src.getSender() instanceof Player player)) {
            src.getSender().sendRichMessage("<red>Only players can use this command!");
            return;
        }
        if (PluralMain.getApiClient() == null) {
            player.sendRichMessage("<red>[Plural] API not configured.");
            return;
        }
        player.sendRichMessage("<gray>[Plural] Syncing...");
        PluralMain.getInstance().getServer().getScheduler()
                .runTaskAsynchronously(PluralMain.getInstance(), () -> {
                    PluralMain.PlayerSystemData data =
                            PluralMain.getApiClient().fetchPlayerData(player.getUniqueId());
                    PluralMain.getInstance().getServer().getScheduler()
                            .runTask(PluralMain.getInstance(), () -> {
                                if (data != null) {
                                    PluralMain.systemCache.put(player.getUniqueId(), data);
                                    String fronting = data.activeFrontNames.isEmpty()
                                            ? "<dark_gray>none"
                                            : "<green>" + String.join("<gray> & <green>", data.activeFrontNames);
                                    player.sendRichMessage("<green>[Plural] Synced! <gray>Fronting: " + fronting);
                                } else {
                                    player.sendRichMessage("<yellow>[Plural] No account linked.");
                                }
                            });
                });
    }

    private static void showSystem(CommandSourceStack src) {
        if (!(src.getSender() instanceof Player player)) {
            src.getSender().sendRichMessage("<red>Only players can use this command!");
            return;
        }
        PluralMain.PlayerSystemData data = PluralMain.systemCache.get(player.getUniqueId());
        String url = PluralConfig.API_URL;

        if (data == null) {
            player.sendRichMessage("<yellow>You don't have a Plural Cloud account linked.");
            player.sendRichMessage("<gray>Sign in at: <aqua>" + url + "/");
            return;
        }

        player.sendRichMessage("<aqua>── Your System ──");
        player.sendRichMessage("<gray>Name: <white>" + data.systemName);
        if (data.systemTag != null) {
            player.sendRichMessage("<gray>Tag: <white>" + data.systemTag);
        }
        player.sendRichMessage("<gray>Members: <white>" + data.members.size());
        if (!data.activeFrontNames.isEmpty()) {
            String fronting = "<green>" + String.join("<gray> & <green>", data.activeFrontNames);
            player.sendRichMessage("<gray>Fronting: " + fronting);
        } else {
            player.sendRichMessage("<gray>Fronting: <dark_gray>none");
        }
        player.sendRichMessage("<gray>Manage at: <aqua>" + url + "/dashboard");
    }
}
