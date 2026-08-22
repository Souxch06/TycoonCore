package fr.valoriatycoon.commands;

import fr.valoriatycoon.api.economy.EconomyService;
import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.economy.CurrencyFormatter;
import fr.valoriatycoon.economy.MoneyCodec;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Handles /balance and exposes the same asynchronous balance view to other commands. */
public final class BalanceCommand implements CommandExecutor, TabCompleter {
    private final EconomyService economy;
    private final CurrencyFormatter formatter;
    private final MessageService messages;
    private final CommandGate gate;
    private final Executor mainThread;

    public BalanceCommand(
            EconomyService economy,
            CurrencyFormatter formatter,
            MessageService messages,
            CommandGate gate,
            Executor mainThread
    ) {
        this.economy = Objects.requireNonNull(economy, "economy");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
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
        showBalance(player);
        return true;
    }

    public java.util.concurrent.CompletionStage<java.math.BigDecimal> balance(Player player) {
        return economy.getBalance(player.getUniqueId());
    }

    public void showBalance(Player player) {
        if (!gate.requireReady(player)) {
            return;
        }
        balance(player).whenCompleteAsync((balance, error) -> {
            if (error != null) {
                messages.send(player, "errors.storage");
                return;
            }
            messages.send(
                    player,
                    "balance.self",
                    Placeholder.unparsed("balance", formatter.format(MoneyCodec.toCents(balance)))
            );
        }, mainThread);
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
