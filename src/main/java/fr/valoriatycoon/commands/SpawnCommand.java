package fr.valoriatycoon.commands;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.spawn.SpawnWorldService;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Teleports players to the generated medieval server hub. */
public final class SpawnCommand implements CommandExecutor, TabCompleter {
    private final SpawnWorldService spawn;
    private final MessageService messages;
    private final CommandGate gate;
    private final Executor mainThread;

    public SpawnCommand(
            SpawnWorldService spawn,
            MessageService messages,
            CommandGate gate,
            Executor mainThread
    ) {
        this.spawn = Objects.requireNonNull(spawn, "spawn");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.gate = Objects.requireNonNull(gate, "gate");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
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
        if (!gate.requireReady(player) || !gate.requirePermission(player, "tycoon.spawn")) {
            return true;
        }
        spawn.teleport(player).whenCompleteAsync((success, error) -> {
            if (error != null || !Boolean.TRUE.equals(success)) {
                messages.send(player, "spawn.teleport-failed");
            } else {
                messages.send(player, "spawn.teleported");
            }
        }, mainThread);
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
