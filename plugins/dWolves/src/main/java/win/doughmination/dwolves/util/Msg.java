package win.doughmination.dwolves.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class Msg {

    private static final Component PREFIX = Component.text()
            .append(Component.text("[", NamedTextColor.DARK_GRAY))
            .append(Component.text("dWolves", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text("] ", NamedTextColor.DARK_GRAY))
            .build();

    private Msg() {}

    public static Component info(String message) {
        return PREFIX.append(Component.text(message, NamedTextColor.GRAY));
    }

    public static Component success(String message) {
        return PREFIX.append(Component.text(message, NamedTextColor.GREEN));
    }

    public static Component error(String message) {
        return PREFIX.append(Component.text(message, NamedTextColor.RED));
    }

    public static Component warn(String message) {
        return PREFIX.append(Component.text(message, NamedTextColor.YELLOW));
    }

    public static Component highlight(String label, String value) {
        return PREFIX
                .append(Component.text(label + ": ", NamedTextColor.GRAY))
                .append(Component.text(value, NamedTextColor.AQUA));
    }
}