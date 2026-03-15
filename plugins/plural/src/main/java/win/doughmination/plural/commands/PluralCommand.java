package win.doughmination.plural.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import win.doughmination.plural.PluralMain;

public class PluralCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendRichMessage("<red>Only players can use this command!");
            return true;
        }

        if (args.length == 0) {
            player.sendRichMessage("<gray>Usage: /plural <system|help>");
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            showHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "system", "sys" -> showSystem(player);
            case "sync" -> doSync(player);
            default -> showHelp(player);
        }
        return true;
    }

    private void showHelp(Player player) {
        String url = PluralMain.getInstance().getConfig().getString("api_url", "your-plural-server.com");
        player.sendRichMessage("<aqua>── Plural Cloud ──");
        player.sendRichMessage("<gray>All system management is done via the dashboard.");
        player.sendRichMessage("<gray>Visit: <aqua>" + url + "/dashboard");
        player.sendRichMessage("");
        player.sendRichMessage("<gray>/plural system <white>— view your system info");
        player.sendRichMessage("<gray>/plural sync   <white>— refresh your data from the server");
        player.sendRichMessage("<gray>/plural help   <white>— show this message");
    }

    private void doSync(Player player) {
        if (PluralMain.getApiClient() == null) {
            player.sendRichMessage("<red>[Plural] API not configured.");
            return;
        }
        player.sendRichMessage("<gray>[Plural] Syncing...");
        PluralMain.getInstance().getServer().getScheduler()
                .runTaskAsynchronously(PluralMain.getInstance(), () -> {
                    PluralMain.PlayerSystemData data = PluralMain.getApiClient().fetchPlayerData(player.getUniqueId());
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

    private void showSystem(Player player) {
        PluralMain.PlayerSystemData data = PluralMain.systemCache.get(player.getUniqueId());
        String url = PluralMain.getInstance().getConfig().getString("api_url", "https://plural.doughmination.win");

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