package fr.valoriatycoon.commands;

import fr.valoriatycoon.LifecycleState;
import fr.valoriatycoon.api.economy.EconomyService;
import fr.valoriatycoon.api.economy.EconomyTransactionResult;
import fr.valoriatycoon.api.economy.EconomyTransactionStatus;
import fr.valoriatycoon.config.ConfigManager;
import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.crates.CrateKeyService;
import fr.valoriatycoon.crates.CrateType;
import fr.valoriatycoon.database.SqliteDatabase;
import fr.valoriatycoon.economy.CurrencyFormatter;
import fr.valoriatycoon.economy.InternalEconomyService;
import fr.valoriatycoon.economy.MoneyCodec;
import fr.valoriatycoon.pets.PetKeyService;
import fr.valoriatycoon.tycoon.PlotResetService;
import fr.valoriatycoon.tycoon.TycoonAllocationStatus;
import fr.valoriatycoon.tycoon.TycoonService;
import fr.valoriatycoon.tycoon.TycoonSettings;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Administration entry point for implemented stage-one operations. */
public final class TycoonAdminCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of(
            "reload", "give", "setmoney", "petkey", "cratekey", "create", "reset", "delete", "debug"
    );

    private final ConfigManager configManager;
    private final EconomyService economy;
    private final InternalEconomyService internalEconomy;
    private final SqliteDatabase database;
    private final TycoonService tycoons;
    private final PlotResetService plotResets;
    private final TycoonSettings tycoonSettings;
    private final PetKeyService petKeys;
    private final CrateKeyService crateKeys;
    private final CurrencyFormatter formatter;
    private final MessageService messages;
    private final CommandGate gate;
    private final Supplier<LifecycleState> lifecycle;
    private final Executor mainThread;
    private final Logger logger;

    public TycoonAdminCommand(
            ConfigManager configManager,
            EconomyService economy,
            InternalEconomyService internalEconomy,
            SqliteDatabase database,
            TycoonService tycoons,
            PlotResetService plotResets,
            TycoonSettings tycoonSettings,
            PetKeyService petKeys,
            CrateKeyService crateKeys,
            CurrencyFormatter formatter,
            MessageService messages,
            CommandGate gate,
            Supplier<LifecycleState> lifecycle,
            Executor mainThread,
            Logger logger
    ) {
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.economy = Objects.requireNonNull(economy, "economy");
        this.internalEconomy = Objects.requireNonNull(internalEconomy, "internalEconomy");
        this.database = Objects.requireNonNull(database, "database");
        this.tycoons = Objects.requireNonNull(tycoons, "tycoons");
        this.plotResets = Objects.requireNonNull(plotResets, "plotResets");
        this.tycoonSettings = Objects.requireNonNull(tycoonSettings, "tycoonSettings");
        this.petKeys = Objects.requireNonNull(petKeys, "petKeys");
        this.crateKeys = Objects.requireNonNull(crateKeys, "crateKeys");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.gate = Objects.requireNonNull(gate, "gate");
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0) {
            messages.send(sender, "errors.usage-admin");
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> reload(sender);
            case "give" -> mutateBalance(sender, args, true);
            case "setmoney" -> mutateBalance(sender, args, false);
            case "petkey" -> givePetKeys(sender, args);
            case "cratekey" -> giveCrateKeys(sender, args);
            case "create" -> createPlot(sender, args);
            case "reset" -> resetPlot(sender, args, false);
            case "delete" -> resetPlot(sender, args, true);
            case "debug" -> debug(sender);
            default -> {
                messages.send(sender, "errors.usage-admin");
                yield true;
            }
        };
    }

    private boolean reload(CommandSender sender) {
        if (!gate.requirePermission(sender, "tycoon.admin.reload")) {
            return true;
        }
        try {
            configManager.reload();
            messages.send(sender, "admin.reloaded");
        } catch (RuntimeException exception) {
            logger.log(Level.SEVERE, "Configuration reload rejected; previous validated settings remain active", exception);
            messages.send(sender, "errors.unavailable");
        }
        return true;
    }

    private boolean mutateBalance(CommandSender sender, String[] args, boolean add) {
        if (!gate.requirePermission(sender, "tycoon.admin.economy") || !gate.requireReady(sender)) {
            return true;
        }
        if (args.length != 3) {
            messages.send(sender, "errors.usage-admin");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            messages.send(sender, "errors.player-not-found", Placeholder.unparsed("player", args[1]));
            return true;
        }
        BigDecimal amount = MoneyCodec.parseUserAmount(args[2]).orElse(null);
        if (amount == null || (add && amount.signum() <= 0)) {
            messages.send(sender, "errors.invalid-amount");
            return true;
        }

        var operation = add
                ? economy.addMoney(target.getUniqueId(), amount, "admin:give:" + sender.getName())
                : economy.setBalance(target.getUniqueId(), amount, "admin:setmoney:" + sender.getName());
        operation.whenCompleteAsync((result, error) -> {
            if (error != null || result == null || result.status() != EconomyTransactionStatus.SUCCESS) {
                messages.send(sender, "errors.storage");
                return;
            }
            sendEconomyUpdated(sender, target, result);
        }, mainThread);
        return true;
    }

    private void sendEconomyUpdated(CommandSender sender, Player target, EconomyTransactionResult result) {
        messages.send(
                sender,
                "admin.economy-updated",
                Placeholder.unparsed("player", target.getName()),
                Placeholder.unparsed("balance", formatter.format(MoneyCodec.toCents(result.sourceBalance())))
        );
    }

    private boolean givePetKeys(CommandSender sender, String[] args) {
        if (!gate.requirePermission(sender, "tycoon.admin.pets") || !gate.requireReady(sender)) {
            return true;
        }
        if (args.length != 3) {
            messages.send(sender, "errors.usage-admin");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            messages.send(sender, "errors.player-not-found", Placeholder.unparsed("player", args[1]));
            return true;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException exception) {
            amount = -1;
        }
        if (amount < 1 || amount > 512) {
            messages.send(sender, "errors.invalid-amount");
            return true;
        }
        petKeys.give(target, amount);
        logger.info(sender.getName() + " gave " + amount + " physical pet keys to " + target.getName());
        messages.send(
                sender,
                "admin.pet-keys-given",
                Placeholder.unparsed("player", target.getName()),
                Placeholder.unparsed("amount", Integer.toString(amount))
        );
        return true;
    }

    private boolean giveCrateKeys(CommandSender sender, String[] args) {
        if (!gate.requirePermission(sender, "tycoon.admin.crates") || !gate.requireReady(sender)) {
            return true;
        }
        if (args.length != 4) {
            messages.send(sender, "errors.usage-admin");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            messages.send(sender, "errors.player-not-found", Placeholder.unparsed("player", args[1]));
            return true;
        }
        CrateType type;
        int amount;
        try {
            type = CrateType.valueOf(args[2].toUpperCase(Locale.ROOT));
            amount = Integer.parseInt(args[3]);
        } catch (IllegalArgumentException exception) {
            messages.send(sender, "errors.invalid-amount");
            return true;
        }
        if (amount < 1 || amount > 512) {
            messages.send(sender, "errors.invalid-amount");
            return true;
        }
        crateKeys.issue(target.getUniqueId(), type, amount, "ADMIN")
                .whenCompleteAsync((ignored, error) -> {
                    if (error != null) {
                        messages.send(sender, "errors.storage");
                    } else {
                        sender.sendMessage(messages.render(
                                "<green><amount> clé(s) <type> attribuée(s) à <player>.</green>",
                                Placeholder.unparsed("amount", Integer.toString(amount)),
                                Placeholder.unparsed("type", type.displayName()),
                                Placeholder.unparsed("player", target.getName())
                        ));
                    }
                }, mainThread);
        return true;
    }

    private boolean createPlot(CommandSender sender, String[] args) {
        if (!gate.requirePermission(sender, "tycoon.admin.plot") || !gate.requireReady(sender)) {
            return true;
        }
        Player target = onlineTarget(sender, args);
        if (target == null) {
            return true;
        }
        tycoons.reserve(target.getUniqueId(), tycoonSettings.defaultGroup().id())
                .whenCompleteAsync((result, error) -> {
                    if (error != null || result == null) {
                        messages.send(sender, "errors.storage");
                    } else if (result.status() == TycoonAllocationStatus.SUCCESS) {
                        plotResets.schedulePreparation(result.tycoon());
                        messages.send(sender, "admin.plot-create-started", Placeholder.unparsed("player", target.getName()));
                    } else if (result.status() == TycoonAllocationStatus.ALREADY_OWNS) {
                        messages.send(sender, "tycoon.already-owns");
                    } else {
                        messages.send(sender, "tycoon.group-full");
                    }
                }, mainThread);
        return true;
    }

    private boolean resetPlot(CommandSender sender, String[] args, boolean delete) {
        if (!gate.requirePermission(sender, "tycoon.admin.plot") || !gate.requireReady(sender)) {
            return true;
        }
        Player target = onlineTarget(sender, args);
        if (target == null) {
            return true;
        }
        var operation = delete
                ? tycoons.beginDeletion(target.getUniqueId())
                : tycoons.beginPreparation(target.getUniqueId());
        operation.whenCompleteAsync((optional, error) -> {
            if (error != null) {
                messages.send(sender, "errors.storage");
            } else if (optional.isEmpty()) {
                messages.send(sender, "tycoon.not-ready");
            } else {
                if (delete) {
                    plotResets.scheduleDeletion(optional.get());
                } else {
                    plotResets.schedulePreparation(optional.get());
                }
                messages.send(
                        sender,
                        delete ? "admin.plot-delete-started" : "admin.plot-reset-started",
                        Placeholder.unparsed("player", target.getName())
                );
            }
        }, mainThread);
        return true;
    }

    private Player onlineTarget(CommandSender sender, String[] args) {
        if (args.length != 2) {
            messages.send(sender, "errors.usage-admin");
            return null;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            messages.send(sender, "errors.player-not-found", Placeholder.unparsed("player", args[1]));
        }
        return target;
    }

    private boolean debug(CommandSender sender) {
        if (!gate.requirePermission(sender, "tycoon.admin.debug")) {
            return true;
        }
        messages.send(
                sender,
                "admin.debug",
                Placeholder.unparsed("state", lifecycle.get().name()),
                Placeholder.unparsed("database", database.isInitialized() ? "READY" : "OFFLINE"),
                Placeholder.unparsed("cached", Integer.toString(internalEconomy.cachedAccountCount())),
                Placeholder.unparsed("plots", Integer.toString(tycoons.all().size())),
                Placeholder.unparsed("reset_jobs", Integer.toString(plotResets.queuedJobs()))
        );
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream().filter(value -> value.startsWith(prefix)).toList();
        }
        if (args.length == 2 && List.of("give", "setmoney", "petkey", "cratekey", "create", "reset", "delete")
                .contains(args[0].toLowerCase(Locale.ROOT))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("cratekey")) {
            String prefix = args[2].toUpperCase(Locale.ROOT);
            return java.util.Arrays.stream(CrateType.values())
                    .map(type -> type.name().toLowerCase(Locale.ROOT))
                    .filter(name -> name.toUpperCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
