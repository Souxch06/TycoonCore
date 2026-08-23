package fr.valoriatycoon.commands;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.gui.RankPanel;
import java.util.List;
import java.util.Objects;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Opens the medieval-rank requirements and promotion panel through /rank. */
public final class RankCommand implements CommandExecutor, TabCompleter {
    private final RankPanel panel;
    private final MessageService messages;
    private final CommandGate gate;

    public RankCommand(RankPanel panel, MessageService messages, CommandGate gate) {
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
        if (gate.requireReady(player) && gate.requirePermission(player, "tycoon.ranks")) {
            panel.open(player);
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
        return List.of();
    }
}
