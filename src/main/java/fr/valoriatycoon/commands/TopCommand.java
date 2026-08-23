package fr.valoriatycoon.commands;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.gui.LeaderboardPanel;
import java.util.List;
import java.util.Objects;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Opens cached asynchronous server leaderboards through /top. */
public final class TopCommand implements CommandExecutor, TabCompleter {
    private final LeaderboardPanel panel;
    private final MessageService messages;
    private final CommandGate gate;

    public TopCommand(LeaderboardPanel panel, MessageService messages, CommandGate gate) {
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
        if (gate.requireReady(player) && gate.requirePermission(player, "tycoon.leaderboards")) {
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
