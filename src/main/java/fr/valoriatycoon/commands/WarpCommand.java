package fr.valoriatycoon.commands;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.gui.WarpPanel;
import fr.valoriatycoon.warps.WarpDefinition;
import fr.valoriatycoon.warps.WarpService;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Prefix command: /warp opens the selector and /warp <id|alias> teleports directly. */
public final class WarpCommand implements CommandExecutor, TabCompleter {
    private final WarpService warps;
    private final WarpPanel panel;
    private final MessageService messages;
    private final CommandGate gate;

    public WarpCommand(
            WarpService warps,
            WarpPanel panel,
            MessageService messages,
            CommandGate gate
    ) {
        this.warps = Objects.requireNonNull(warps, "warps");
        this.panel = Objects.requireNonNull(panel, "panel");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.gate = Objects.requireNonNull(gate, "gate");
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "errors.players-only");
            return true;
        }
        if (!gate.requireReady(player) || !gate.requirePermission(player, "tycoon.warp")) {
            return true;
        }
        if (args.length == 0) {
            panel.open(player);
            return true;
        }
        if (args.length != 1) {
            messages.send(player, "warps.usage");
            return true;
        }
        WarpDefinition warp = warps.resolve(args[0]);
        if (warp == null) {
            messages.send(player, "warps.unknown");
        } else {
            panel.teleport(player, warp);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return warps.settings().aliases().keySet().stream()
                .filter(value -> value.startsWith(prefix))
                .sorted()
                .toList();
    }
}
