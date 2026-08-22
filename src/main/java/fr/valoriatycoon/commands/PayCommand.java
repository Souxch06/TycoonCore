package fr.valoriatycoon.commands;

import fr.valoriatycoon.api.economy.EconomyService;
import fr.valoriatycoon.api.economy.EconomyTransactionResult;
import fr.valoriatycoon.api.economy.EconomyTransactionStatus;
import fr.valoriatycoon.config.CoreSettings;
import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.economy.CurrencyFormatter;
import fr.valoriatycoon.economy.MoneyCodec;
import fr.valoriatycoon.economy.PaymentRateLimiter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Validates and performs atomic online-player payments. */
public final class PayCommand implements CommandExecutor, TabCompleter {
    private final EconomyService economy;
    private final Supplier<CoreSettings.Economy> settings;
    private final CurrencyFormatter formatter;
    private final MessageService messages;
    private final CommandGate gate;
    private final PaymentRateLimiter rateLimiter;
    private final Executor mainThread;

    public PayCommand(
            EconomyService economy,
            Supplier<CoreSettings.Economy> settings,
            CurrencyFormatter formatter,
            MessageService messages,
            CommandGate gate,
            PaymentRateLimiter rateLimiter,
            Executor mainThread
    ) {
        this.economy = Objects.requireNonNull(economy, "economy");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.gate = Objects.requireNonNull(gate, "gate");
        this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player source)) {
            messages.send(sender, "errors.players-only");
            return true;
        }
        if (!gate.requireReady(source)) {
            return true;
        }
        if (args.length != 2) {
            messages.send(source, "errors.usage-pay");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            messages.send(source, "errors.player-not-found", Placeholder.unparsed("player", args[0]));
            return true;
        }
        if (source.getUniqueId().equals(target.getUniqueId())) {
            messages.send(source, "errors.self-payment");
            return true;
        }

        BigDecimal amount = MoneyCodec.parseUserAmount(args[1]).orElse(null);
        if (amount == null || amount.signum() <= 0) {
            messages.send(source, "errors.invalid-amount");
            return true;
        }
        long cents = MoneyCodec.toCents(amount);
        CoreSettings.Economy policy = settings.get();
        if (cents < policy.minimumPaymentCents() || cents > policy.maximumPaymentCents()) {
            messages.send(
                    source,
                    "errors.payment-limits",
                    Placeholder.unparsed("min", formatter.format(policy.minimumPaymentCents())),
                    Placeholder.unparsed("max", formatter.format(policy.maximumPaymentCents()))
            );
            return true;
        }

        long waitSeconds = rateLimiter.tryAcquire(source.getUniqueId(), policy.payCooldown());
        if (waitSeconds > 0) {
            messages.send(source, "errors.payment-cooldown", Placeholder.unparsed("seconds", Long.toString(waitSeconds)));
            return true;
        }

        economy.transfer(source.getUniqueId(), target.getUniqueId(), amount, "command:/pay")
                .whenCompleteAsync((result, error) -> {
                    if (error != null || result == null) {
                        messages.send(source, "errors.storage");
                        return;
                    }
                    handleResult(source, target, result);
                }, mainThread);
        return true;
    }

    private void handleResult(Player source, Player target, EconomyTransactionResult result) {
        if (result.status() == EconomyTransactionStatus.SUCCESS) {
            String formattedAmount = formatter.format(MoneyCodec.toCents(result.amount()));
            messages.send(
                    source,
                    "payment.sent",
                    Placeholder.unparsed("amount", formattedAmount),
                    Placeholder.unparsed("player", target.getName()),
                    Placeholder.unparsed("balance", formatter.format(MoneyCodec.toCents(result.sourceBalance())))
            );
            if (target.isOnline()) {
                messages.send(
                        target,
                        "payment.received",
                        Placeholder.unparsed("amount", formattedAmount),
                        Placeholder.unparsed("player", source.getName()),
                        Placeholder.unparsed("balance", formatter.format(MoneyCodec.toCents(result.targetBalance())))
                );
            }
            return;
        }
        if (result.status() == EconomyTransactionStatus.INSUFFICIENT_FUNDS) {
            messages.send(
                    source,
                    "errors.insufficient-funds",
                    Placeholder.unparsed("balance", formatter.format(MoneyCodec.toCents(result.sourceBalance())))
            );
            return;
        }
        if (result.status() == EconomyTransactionStatus.INVALID_AMOUNT) {
            messages.send(source, "errors.invalid-amount");
            return;
        }
        messages.send(source, "errors.storage");
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
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> !(sender instanceof Player player) || !name.equals(player.getName()))
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
