package fr.valoriatycoon.commands;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.economy.CurrencyFormatter;
import fr.valoriatycoon.economy.MoneyCodec;
import fr.valoriatycoon.gui.IslandMainMenu;
import fr.valoriatycoon.gui.PlotUpgradePanel;
import fr.valoriatycoon.gui.QuestPanel;
import fr.valoriatycoon.ranks.RankSettings;
import fr.valoriatycoon.tycoon.MemberOperationStatus;
import fr.valoriatycoon.tycoon.PlotResetService;
import fr.valoriatycoon.tycoon.Tycoon;
import fr.valoriatycoon.tycoon.TycoonAllocationStatus;
import fr.valoriatycoon.tycoon.TycoonInviteService;
import fr.valoriatycoon.tycoon.TycoonService;
import fr.valoriatycoon.tycoon.TycoonSettings;
import fr.valoriatycoon.tycoon.TycoonStatus;
import fr.valoriatycoon.tycoon.TycoonWorldService;
import fr.valoriatycoon.upgrades.PlotUpgradeType;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Player command router for Tycoon ownership, home, statistics, reset and trusted members. */
public final class TycoonCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of(
            "menu", "create", "go", "settings", "visit", "stats", "upgrades",
            "quests", "reset", "invite", "accept", "kick", "members"
    );

    private final BalanceCommand balanceCommand;
    private final TycoonService tycoons;
    private final TycoonWorldService worlds;
    private final PlotResetService resets;
    private final TycoonInviteService invites;
    private final TycoonSettings settings;
    private final PlotUpgradePanel upgradePanel;
    private final IslandMainMenu mainMenu;
    private final QuestPanel questPanel;
    private final RankSettings rankSettings;
    private final CurrencyFormatter currency;
    private final MessageService messages;
    private final CommandGate gate;
    private final Executor mainThread;
    private final Map<UUID, Long> resetConfirmations = new HashMap<>();

    public TycoonCommand(
            BalanceCommand balanceCommand,
            TycoonService tycoons,
            TycoonWorldService worlds,
            PlotResetService resets,
            TycoonInviteService invites,
            TycoonSettings settings,
            PlotUpgradePanel upgradePanel,
            IslandMainMenu mainMenu,
            QuestPanel questPanel,
            RankSettings rankSettings,
            CurrencyFormatter currency,
            MessageService messages,
            CommandGate gate,
            Executor mainThread
    ) {
        this.balanceCommand = Objects.requireNonNull(balanceCommand, "balanceCommand");
        this.tycoons = Objects.requireNonNull(tycoons, "tycoons");
        this.worlds = Objects.requireNonNull(worlds, "worlds");
        this.resets = Objects.requireNonNull(resets, "resets");
        this.invites = Objects.requireNonNull(invites, "invites");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.upgradePanel = Objects.requireNonNull(upgradePanel, "upgradePanel");
        this.mainMenu = Objects.requireNonNull(mainMenu, "mainMenu");
        this.questPanel = Objects.requireNonNull(questPanel, "questPanel");
        this.rankSettings = Objects.requireNonNull(rankSettings, "rankSettings");
        this.currency = Objects.requireNonNull(currency, "currency");
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
        if (!gate.requireReady(player)) {
            return true;
        }
        if (args.length == 0) {
            mainMenu.open(player);
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "menu" -> menu(player);
            case "create" -> create(player);
            case "go", "home" -> home(player);
            case "settings" -> settings(player);
            case "visit" -> visit(player, args);
            case "stats" -> stats(player);
            case "reset" -> reset(player, args);
            case "invite" -> invite(player, args);
            case "accept" -> accept(player, args);
            case "kick" -> kick(player, args);
            case "members" -> members(player);
            case "upgrades" -> upgrades(player);
            case "quests" -> quests(player);
            default -> {
                sendHelp(player);
                yield true;
            }
        };
    }

    private boolean menu(Player player) {
        mainMenu.open(player);
        return true;
    }

    private boolean create(Player player) {
        if (!gate.requirePermission(player, "tycoon.create")) {
            return true;
        }
        messages.send(player, "tycoon.create-started");
        tycoons.reserve(player.getUniqueId(), settings.defaultGroup().id()).whenCompleteAsync((result, error) -> {
            if (error != null || result == null) {
                messages.send(player, "errors.storage");
            } else if (result.status() == TycoonAllocationStatus.SUCCESS) {
                resets.schedulePreparation(result.tycoon());
            } else if (result.status() == TycoonAllocationStatus.ALREADY_OWNS) {
                messages.send(player, "tycoon.already-owns");
            } else {
                messages.send(player, "tycoon.group-full");
            }
        }, mainThread);
        return true;
    }

    private boolean home(Player player) {
        Tycoon tycoon = requireOwned(player);
        if (tycoon == null) {
            return true;
        }
        if (tycoon.status() != TycoonStatus.ACTIVE) {
            messages.send(player, "tycoon.not-ready");
            return true;
        }
        player.teleportAsync(tycoon.home(worlds.world(tycoon.worldName()))).thenAcceptAsync(success -> {
            if (!success) {
                messages.send(player, "tycoon.teleport-failed");
            }
        }, mainThread);
        return true;
    }

    private boolean visit(Player player, String[] args) {
        if (!gate.requirePermission(player, "tycoon.members")) {
            return true;
        }
        if (args.length != 2) {
            messages.send(player, "tycoon.visit-usage");
            return true;
        }
        Player owner = Bukkit.getPlayerExact(args[1]);
        Tycoon tycoon = owner == null ? null : tycoons.ownedBy(owner.getUniqueId()).orElse(null);
        if (tycoon == null || tycoon.status() != TycoonStatus.ACTIVE) {
            messages.send(player, "tycoon.none");
            return true;
        }
        if (!tycoon.ownerId().equals(player.getUniqueId())
                && !tycoons.members(tycoon.id()).contains(player.getUniqueId())) {
            messages.send(player, "tycoon.not-member");
            return true;
        }
        player.teleportAsync(tycoon.home(worlds.world(tycoon.worldName())));
        return true;
    }

    private boolean settings(Player player) {
        Tycoon tycoon = requireOwned(player);
        if (tycoon == null) {
            return true;
        }
        messages.send(
                player,
                "tycoon.settings",
                Placeholder.unparsed(
                        "plot_size",
                        Integer.toString(tycoons.upgradeValue(tycoon, PlotUpgradeType.PLOT_SIZE))
                ),
                Placeholder.unparsed(
                        "hopper_limit",
                        Integer.toString(tycoons.upgradeValue(tycoon, PlotUpgradeType.HOPPER_LIMIT))
                ),
                Placeholder.unparsed(
                        "member_limit",
                        Integer.toString(tycoons.upgradeValue(tycoon, PlotUpgradeType.MEMBER_LIMIT))
                ),
                Placeholder.unparsed("fly", settings.flight().enabled() ? "ON" : "OFF")
        );
        return true;
    }

    private boolean stats(Player player) {
        Tycoon tycoon = requireOwned(player);
        if (tycoon == null) {
            return true;
        }
        balanceCommand.balance(player).whenCompleteAsync((balance, error) -> {
            if (error != null) {
                messages.send(player, "errors.storage");
                return;
            }
            messages.send(
                    player,
                    "tycoon.stats",
                    Placeholder.unparsed("level", Integer.toString(tycoon.level())),
                    Placeholder.unparsed("rank", Integer.toString(tycoon.prestige())),
                    Placeholder.unparsed("rank_name", rankSettings.name(tycoon.prestige())),
                    Placeholder.unparsed("progress", Long.toString(tycoon.progressPoints())),
                    Placeholder.unparsed("production", Long.toString(tycoon.totalProduction())),
                    Placeholder.unparsed("playtime", Long.toString(tycoon.playtimeSeconds())),
                    Placeholder.unparsed("members", Integer.toString(tycoons.members(tycoon.id()).size())),
                    Placeholder.unparsed(
                            "plot_size",
                            Integer.toString(tycoons.upgradeValue(tycoon, PlotUpgradeType.PLOT_SIZE))
                    ),
                    Placeholder.unparsed(
                            "hopper_limit",
                            Integer.toString(tycoons.upgradeValue(tycoon, PlotUpgradeType.HOPPER_LIMIT))
                    ),
                    Placeholder.unparsed(
                            "member_limit",
                            Integer.toString(tycoons.upgradeValue(tycoon, PlotUpgradeType.MEMBER_LIMIT))
                    ),
                    Placeholder.unparsed("money", currency.format(MoneyCodec.toCents(balance)))
            );
        }, mainThread);
        return true;
    }

    private boolean reset(Player player, String[] args) {
        if (!gate.requirePermission(player, "tycoon.reset")) {
            return true;
        }
        Tycoon tycoon = requireOwned(player);
        if (tycoon == null) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            resetConfirmations.put(
                    player.getUniqueId(),
                    now + settings.resetConfirmationSeconds() * 1000L
            );
            messages.send(
                    player,
                    "tycoon.reset-confirm",
                    Placeholder.unparsed("seconds", Integer.toString(settings.resetConfirmationSeconds()))
            );
            return true;
        }
        Long deadline = resetConfirmations.remove(player.getUniqueId());
        if (deadline == null || deadline < now) {
            messages.send(player, "tycoon.reset-expired");
            return true;
        }
        tycoons.beginDeletion(player.getUniqueId()).whenCompleteAsync((optional, error) -> {
            if (error != null) {
                messages.send(player, "errors.storage");
            } else if (optional.isEmpty()) {
                messages.send(player, "tycoon.not-ready");
            } else {
                messages.send(player, "tycoon.reset-started");
                resets.scheduleDeletion(optional.get());
            }
        }, mainThread);
        return true;
    }

    private boolean invite(Player owner, String[] args) {
        if (!gate.requirePermission(owner, "tycoon.members")) {
            return true;
        }
        if (args.length != 2) {
            messages.send(owner, "tycoon.invite-usage");
            return true;
        }
        Tycoon tycoon = requireActiveOwned(owner);
        if (tycoon == null) {
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            messages.send(owner, "errors.player-not-found", Placeholder.unparsed("player", args[1]));
            return true;
        }
        if (target.getUniqueId().equals(owner.getUniqueId())) {
            messages.send(owner, "tycoon.cannot-invite-self");
            return true;
        }
        invites.invite(owner.getUniqueId(), target.getUniqueId());
        messages.send(owner, "tycoon.invite-sent", Placeholder.unparsed("player", target.getName()));
        messages.send(
                target,
                "tycoon.invite-received",
                Placeholder.unparsed("player", owner.getName()),
                Placeholder.unparsed("seconds", Integer.toString(settings.inviteExpirationSeconds()))
        );
        return true;
    }

    private boolean accept(Player player, String[] args) {
        if (!gate.requirePermission(player, "tycoon.members")) {
            return true;
        }
        if (args.length != 2) {
            messages.send(player, "tycoon.accept-usage");
            return true;
        }
        Player owner = Bukkit.getPlayerExact(args[1]);
        if (owner == null || !invites.consume(owner.getUniqueId(), player.getUniqueId())) {
            messages.send(player, "tycoon.invite-missing");
            return true;
        }
        tycoons.addMember(owner.getUniqueId(), player.getUniqueId()).whenCompleteAsync((status, error) -> {
            if (error != null) {
                messages.send(player, "errors.storage");
            } else {
                sendMemberResult(owner, player, status, true);
            }
        }, mainThread);
        return true;
    }

    private boolean kick(Player owner, String[] args) {
        if (!gate.requirePermission(owner, "tycoon.members")) {
            return true;
        }
        if (args.length != 2) {
            messages.send(owner, "tycoon.kick-usage");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            messages.send(owner, "errors.player-not-found", Placeholder.unparsed("player", args[1]));
            return true;
        }
        tycoons.removeMember(owner.getUniqueId(), target.getUniqueId()).whenCompleteAsync((status, error) -> {
            if (error != null) {
                messages.send(owner, "errors.storage");
            } else {
                sendMemberResult(owner, target, status, false);
            }
        }, mainThread);
        return true;
    }

    private boolean members(Player player) {
        if (!gate.requirePermission(player, "tycoon.members")) {
            return true;
        }
        Tycoon tycoon = requireOwned(player);
        if (tycoon == null) {
            return true;
        }
        String memberList = tycoons.members(tycoon.id()).stream()
                .map(id -> {
                    Player online = Bukkit.getPlayer(id);
                    return online == null ? id.toString() : online.getName();
                })
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(java.util.stream.Collectors.joining(", "));
        messages.send(
                player,
                "tycoon.members",
                Placeholder.unparsed("members", memberList.isEmpty() ? "-" : memberList)
        );
        return true;
    }

    private boolean upgrades(Player player) {
        if (!gate.requirePermission(player, "tycoon.upgrades")) {
            return true;
        }
        upgradePanel.open(player);
        return true;
    }

    private boolean quests(Player player) {
        if (!gate.requirePermission(player, "tycoon.quests")) return true;
        questPanel.open(player);
        return true;
    }

    private void sendMemberResult(Player owner, Player target, MemberOperationStatus status, boolean adding) {
        if (status == MemberOperationStatus.SUCCESS) {
            messages.send(
                    owner,
                    adding ? "tycoon.member-added-owner" : "tycoon.member-removed-owner",
                    Placeholder.unparsed("player", target.getName())
            );
            messages.send(
                    target,
                    adding ? "tycoon.member-added-target" : "tycoon.member-removed-target",
                    Placeholder.unparsed("player", owner.getName())
            );
        } else if (status == MemberOperationStatus.MEMBER_LIMIT) {
            messages.send(owner, "tycoon.member-limit");
        } else if (status == MemberOperationStatus.ALREADY_MEMBER) {
            messages.send(owner, "tycoon.already-member");
        } else if (status == MemberOperationStatus.NOT_MEMBER) {
            messages.send(owner, "tycoon.not-member");
        } else {
            messages.send(owner, "tycoon.member-failed");
        }
    }

    private Tycoon requireOwned(Player player) {
        Tycoon tycoon = tycoons.ownedBy(player.getUniqueId()).orElse(null);
        if (tycoon == null) {
            messages.send(player, "tycoon.none");
        }
        return tycoon;
    }

    private Tycoon requireActiveOwned(Player player) {
        Tycoon tycoon = requireOwned(player);
        if (tycoon != null && tycoon.status() != TycoonStatus.ACTIVE) {
            messages.send(player, "tycoon.not-ready");
            return null;
        }
        return tycoon;
    }

    private void sendHelp(Player player) {
        messages.send(player, "command.tycoon-header");
        messages.send(player, "tycoon.help");
    }

    public void releasePlayer(UUID playerId) {
        resetConfirmations.remove(playerId);
        invites.clearPlayer(playerId);
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
        if (args.length == 2 && List.of("invite", "accept", "kick", "visit").contains(args[0].toLowerCase(Locale.ROOT))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            return "confirm".startsWith(args[1].toLowerCase(Locale.ROOT)) ? List.of("confirm") : List.of();
        }
        return List.of();
    }
}
