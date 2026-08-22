package fr.valoriatycoon.commands;

import fr.valoriatycoon.LifecycleState;
import fr.valoriatycoon.config.MessageService;
import java.util.Objects;
import java.util.function.Supplier;
import org.bukkit.command.CommandSender;

/** Shared fail-closed lifecycle and permission checks for commands. */
public final class CommandGate {
    private final Supplier<LifecycleState> lifecycle;
    private final MessageService messages;

    public CommandGate(Supplier<LifecycleState> lifecycle, MessageService messages) {
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    public boolean requireReady(CommandSender sender) {
        LifecycleState state = lifecycle.get();
        if (state == LifecycleState.READY) {
            return true;
        }
        messages.send(sender, state == LifecycleState.STARTING ? "errors.initializing" : "errors.unavailable");
        return false;
    }

    public boolean requirePermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        messages.send(sender, "errors.no-permission");
        return false;
    }
}
