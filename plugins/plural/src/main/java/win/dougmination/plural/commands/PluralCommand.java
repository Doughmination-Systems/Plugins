package win.dougmination.plural.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import win.dougmination.plural.PluralMain;

public class PluralCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.GRAY + "Usage: " + ChatColor.WHITE + "/plural <system|help>");
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
        player.sendMessage(ChatColor.AQUA + "── Plural Cloud ──");
        player.sendMessage(ChatColor.GRAY + "All system management is done via the dashboard.");
        player.sendMessage(ChatColor.GRAY + "Visit: " + ChatColor.AQUA + url + "/dashboard");
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "/plural system " + ChatColor.WHITE + "— view your system info");
        player.sendMessage(ChatColor.GRAY + "/plural sync   " + ChatColor.WHITE + "— refresh your data from the server");
        player.sendMessage(ChatColor.GRAY + "/plural help   " + ChatColor.WHITE + "— show this message");
    }

    private void doSync(Player player) {
        if (PluralMain.getApiClient() == null) {
            player.sendMessage(ChatColor.RED + "[Plural] API not configured.");
            return;
        }
        player.sendMessage(ChatColor.GRAY + "[Plural] Syncing...");
        PluralMain.getInstance().getServer().getScheduler()
                .runTaskAsynchronously(PluralMain.getInstance(), () -> {
                    PluralMain.PlayerSystemData data = PluralMain.getApiClient().fetchPlayerData(player.getUniqueId());
                    PluralMain.getInstance().getServer().getScheduler()
                            .runTask(PluralMain.getInstance(), () -> {
                                if (data != null) {
                                    PluralMain.systemCache.put(player.getUniqueId(), data);
                                    player.sendMessage(ChatColor.GREEN + "[Plural] Synced! "
                                            + ChatColor.GRAY + "Fronting: "
                                            + (data.activeFrontNames.isEmpty()
                                            ? ChatColor.DARK_GRAY + "none"
                                            : ChatColor.GREEN + String.join(ChatColor.GRAY + " & " + ChatColor.GREEN, data.activeFrontNames)));
                                } else {
                                    player.sendMessage(ChatColor.YELLOW + "[Plural] No account linked.");
                                }
                            });
                });
    }

    private void showSystem(Player player) {
        PluralMain.PlayerSystemData data = PluralMain.systemCache.get(player.getUniqueId());

        if (data == null) {
            player.sendMessage(ChatColor.YELLOW + "You don't have a Plural Cloud account linked.");
            player.sendMessage(ChatColor.GRAY + "Sign in at: " + ChatColor.AQUA
                    + PluralMain.getInstance().getConfig().getString("api_url", "your-plural-server.com") + "/");
            return;
        }

        player.sendMessage(ChatColor.AQUA + "── Your System ──");
        player.sendMessage(ChatColor.GRAY + "Name: " + ChatColor.WHITE + data.systemName);
        if (data.systemTag != null) {
            player.sendMessage(ChatColor.GRAY + "Tag: " + ChatColor.WHITE + data.systemTag);
        }
        player.sendMessage(ChatColor.GRAY + "Members: " + ChatColor.WHITE + data.members.size());
        if (!data.activeFrontNames.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Fronting: " + ChatColor.GREEN
                    + String.join(ChatColor.GRAY + " & " + ChatColor.GREEN, data.activeFrontNames));
        } else {
            player.sendMessage(ChatColor.GRAY + "Fronting: " + ChatColor.DARK_GRAY + "none");
        }
        player.sendMessage(ChatColor.GRAY + "Manage at: " + ChatColor.AQUA
                + PluralMain.getInstance().getConfig().getString("api_url", "") + "/dashboard");
    }
}