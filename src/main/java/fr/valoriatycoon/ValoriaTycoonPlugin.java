package fr.valoriatycoon;

import fr.valoriatycoon.api.economy.EconomyService;
import fr.valoriatycoon.commands.AutoSellCommand;
import fr.valoriatycoon.commands.BalanceCommand;
import fr.valoriatycoon.commands.CommandGate;
import fr.valoriatycoon.commands.CommandRegistrar;
import fr.valoriatycoon.commands.FarmCommand;
import fr.valoriatycoon.commands.PayCommand;
import fr.valoriatycoon.commands.PetsCommand;
import fr.valoriatycoon.commands.RankCommand;
import fr.valoriatycoon.commands.ShopCommand;
import fr.valoriatycoon.commands.SpawnCommand;
import fr.valoriatycoon.commands.TopCommand;
import fr.valoriatycoon.commands.TycoonAdminCommand;
import fr.valoriatycoon.commands.TycoonCommand;
import fr.valoriatycoon.commands.WarpCommand;
import fr.valoriatycoon.compaction.CompactionListener;
import fr.valoriatycoon.compaction.CompactionService;
import fr.valoriatycoon.config.ConfigManager;
import fr.valoriatycoon.config.CoreSettings;
import fr.valoriatycoon.config.LegacyInfrastructureMigrator;
import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.crates.CrateKeyItemService;
import fr.valoriatycoon.crates.CrateKeyProtectionListener;
import fr.valoriatycoon.crates.CrateKeyRepository;
import fr.valoriatycoon.crates.CrateKeyService;
import fr.valoriatycoon.crates.CrateRewardItemService;
import fr.valoriatycoon.crates.CrateRewardProtectionListener;
import fr.valoriatycoon.crates.CrateRewardRepository;
import fr.valoriatycoon.crates.CrateRewardSelector;
import fr.valoriatycoon.crates.CrateRewardService;
import fr.valoriatycoon.crates.CrateRewardSettings;
import fr.valoriatycoon.crates.CrateSettings;
import fr.valoriatycoon.crates.CrateStationService;
import fr.valoriatycoon.crates.CrateStationSettings;
import fr.valoriatycoon.crates.VotifierCrateBridge;
import fr.valoriatycoon.database.SqliteDatabase;
import fr.valoriatycoon.economy.CurrencyFormatter;
import fr.valoriatycoon.economy.InternalEconomyService;
import fr.valoriatycoon.economy.PaymentRateLimiter;
import fr.valoriatycoon.economy.PlayerAccountRepository;
import fr.valoriatycoon.farm.FarmSettings;
import fr.valoriatycoon.farm.FarmWorldService;
import fr.valoriatycoon.farm.autosell.AutoSellBatchService;
import fr.valoriatycoon.farm.autosell.AutoSellRepository;
import fr.valoriatycoon.farm.autosell.AutoSellService;
import fr.valoriatycoon.farm.regeneration.BlockRegenerationRepository;
import fr.valoriatycoon.farm.regeneration.BlockRegenerationService;
import fr.valoriatycoon.farm.regeneration.PendingBlockRegeneration;
import fr.valoriatycoon.gui.AutoSellMenuController;
import fr.valoriatycoon.gui.AutoSellPanel;
import fr.valoriatycoon.gui.FarmMenu;
import fr.valoriatycoon.gui.IslandMainMenu;
import fr.valoriatycoon.gui.IslandMenuSettings;
import fr.valoriatycoon.gui.LeaderboardPanel;
import fr.valoriatycoon.gui.MachineControlPanel;
import fr.valoriatycoon.gui.MachineShopPanel;
import fr.valoriatycoon.gui.PetPanel;
import fr.valoriatycoon.gui.PlotUpgradePanel;
import fr.valoriatycoon.gui.QuestPanel;
import fr.valoriatycoon.gui.RankPanel;
import fr.valoriatycoon.listeners.FarmPlayerListener;
import fr.valoriatycoon.listeners.FarmProtectionListener;
import fr.valoriatycoon.listeners.MachineListener;
import fr.valoriatycoon.listeners.PlayerAccountListener;
import fr.valoriatycoon.listeners.QuestPlayerListener;
import fr.valoriatycoon.listeners.SpawnProtectionListener;
import fr.valoriatycoon.listeners.ToolPlayerListener;
import fr.valoriatycoon.listeners.TycoonProtectionListener;
import fr.valoriatycoon.listeners.TycoonSessionListener;
import fr.valoriatycoon.leaderboards.LeaderboardHologramService;
import fr.valoriatycoon.leaderboards.LeaderboardHologramSettings;
import fr.valoriatycoon.leaderboards.LeaderboardRepository;
import fr.valoriatycoon.leaderboards.LeaderboardService;
import fr.valoriatycoon.leaderboards.LeaderboardSettings;
import fr.valoriatycoon.leaderboards.LeaderboardValueFormatter;
import fr.valoriatycoon.machines.MachineItemService;
import fr.valoriatycoon.machines.MachineRepository;
import fr.valoriatycoon.machines.MachineService;
import fr.valoriatycoon.machines.MachineSettings;
import fr.valoriatycoon.machines.MachineSnapshot;
import fr.valoriatycoon.pets.PetEggListener;
import fr.valoriatycoon.pets.PetEggService;
import fr.valoriatycoon.pets.PetKeyListener;
import fr.valoriatycoon.pets.PetKeyService;
import fr.valoriatycoon.pets.PetReclaimService;
import fr.valoriatycoon.pets.PetRepository;
import fr.valoriatycoon.pets.PetService;
import fr.valoriatycoon.pets.PetSettings;
import fr.valoriatycoon.pets.PetSnapshot;
import fr.valoriatycoon.professions.ProfessionRepository;
import fr.valoriatycoon.professions.ProfessionService;
import fr.valoriatycoon.professions.ProfessionSettings;
import fr.valoriatycoon.quests.QuestRepository;
import fr.valoriatycoon.quests.QuestService;
import fr.valoriatycoon.quests.QuestSettings;
import fr.valoriatycoon.ranks.RankBenefitService;
import fr.valoriatycoon.ranks.RankRepository;
import fr.valoriatycoon.ranks.RankService;
import fr.valoriatycoon.ranks.RankSettings;
import fr.valoriatycoon.resourcepack.ItemVisualService;
import fr.valoriatycoon.spawn.SpawnSettings;
import fr.valoriatycoon.spawn.SpawnWorldService;
import fr.valoriatycoon.tools.MultiToolItemService;
import fr.valoriatycoon.tools.MultiToolProtectionListener;
import fr.valoriatycoon.tools.MultiToolService;
import fr.valoriatycoon.tools.ToolAbilityListener;
import fr.valoriatycoon.tools.ToolEffectService;
import fr.valoriatycoon.tools.ToolProgressionService;
import fr.valoriatycoon.tools.ToolRepository;
import fr.valoriatycoon.tools.ToolSettings;
import fr.valoriatycoon.tutorial.TutorialHubService;
import fr.valoriatycoon.tutorial.TutorialHubSettings;
import fr.valoriatycoon.tutorial.TutorialListener;
import fr.valoriatycoon.tutorial.TutorialRepository;
import fr.valoriatycoon.tutorial.TutorialService;
import fr.valoriatycoon.gui.ToolCurrencyChoiceController;
import fr.valoriatycoon.gui.ToolUpgradeItemFactory;
import fr.valoriatycoon.gui.ToolUpgradePanel;
import fr.valoriatycoon.gui.WarpPanel;
import fr.valoriatycoon.hooks.ValoriaPlaceholderExpansion;
import fr.valoriatycoon.tycoon.ConfiguredTycoonFlightPolicy;
import fr.valoriatycoon.tycoon.PlotResetService;
import fr.valoriatycoon.tycoon.TycoonBoundaryService;
import fr.valoriatycoon.tycoon.TycoonDataSnapshot;
import fr.valoriatycoon.tycoon.TycoonFlightService;
import fr.valoriatycoon.tycoon.TycoonInviteService;
import fr.valoriatycoon.tycoon.TycoonPlaytimeService;
import fr.valoriatycoon.tycoon.TycoonRepository;
import fr.valoriatycoon.tycoon.TycoonService;
import fr.valoriatycoon.tycoon.TycoonSettings;
import fr.valoriatycoon.tycoon.TycoonWorldService;
import fr.valoriatycoon.upgrades.PlotUpgradeSettings;
import fr.valoriatycoon.utils.MainThreadExecutor;
import fr.valoriatycoon.warps.WarpService;
import fr.valoriatycoon.warps.WarpSettings;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

/** Paper entry point. Composition is deliberately kept here while domain logic lives in services. */
public final class ValoriaTycoonPlugin extends JavaPlugin {
    private static final Duration DATABASE_CLOSE_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration AUTOSELL_CLOSE_TIMEOUT = Duration.ofSeconds(5);

    private final AtomicReference<LifecycleState> lifecycle = new AtomicReference<>(LifecycleState.STARTING);
    private ConfigManager configManager;
    private SqliteDatabase database;
    private InternalEconomyService economy;
    private CompactionService compaction;
    private CrateKeyService crateKeys;
    private CrateRewardService crateRewards;
    private CrateStationService crateStations;
    private AutoSellService autoSell;
    private AutoSellBatchService autoSellBatches;
    private BlockRegenerationService regeneration;
    private FarmWorldService farmWorlds;
    private SpawnWorldService spawnWorld;
    private ToolProgressionService tools;
    private ProfessionService professions;
    private ToolEffectService toolEffects;
    private TycoonService tycoonService;
    private TycoonWorldService tycoonWorlds;
    private PlotResetService plotResets;
    private TycoonPlaytimeService tycoonPlaytime;
    private TycoonFlightService tycoonFlight;
    private TycoonBoundaryService tycoonBoundaries;
    private MachineService machines;
    private LeaderboardService leaderboards;
    private LeaderboardHologramService leaderboardHolograms;
    private PetService pets;
    private PetKeyService petKeys;
    private PetEggService petEggs;
    private PetReclaimService petReclaim;
    private QuestService quests;
    private TutorialService tutorial;
    private TutorialHubService tutorialHub;
    private RankService ranks;
    private WarpService warps;
    private IslandMainMenu islandMainMenu;
    private List<ValoriaPlaceholderExpansion> placeholders = List.of();

    @Override
    public void onEnable() {
        lifecycle.set(LifecycleState.STARTING);
        try {
            bootstrap();
        } catch (RuntimeException exception) {
            lifecycle.set(LifecycleState.FAILED);
            getLogger().log(Level.SEVERE, "ValoriaTycoon bootstrap failed", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void bootstrap() {
        LegacyInfrastructureMigrator.migrate(this);
        configManager = new ConfigManager(this);
        configManager.initialize();
        CoreSettings initialSettings = configManager.settings();
        CrateSettings crateSettings = configManager.crateSettings();
        CrateRewardSettings crateRewardSettings = configManager.crateRewardSettings();
        CrateStationSettings crateStationSettings = configManager.crateStationSettings();
        FarmSettings farmSettings = configManager.farmSettings();
        ToolSettings toolSettings = configManager.toolSettings();
        ProfessionSettings professionSettings = configManager.professionSettings();
        MachineSettings machineSettings = configManager.machineSettings();
        PetSettings petSettings = configManager.petSettings();
        IslandMenuSettings islandMenuSettings = configManager.islandMenuSettings();
        LeaderboardSettings leaderboardSettings = configManager.leaderboardSettings();
        LeaderboardHologramSettings leaderboardHologramSettings = configManager.leaderboardHologramSettings();
        QuestSettings questSettings = configManager.questSettings();
        RankSettings rankSettings = configManager.rankSettings();
        SpawnSettings spawnSettings = configManager.spawnSettings();
        TutorialHubSettings tutorialHubSettings = configManager.tutorialHubSettings();
        TycoonSettings tycoonSettings = configManager.tycoonSettings();
        PlotUpgradeSettings plotUpgradeSettings = configManager.plotUpgradeSettings();
        WarpSettings warpSettings = configManager.warpSettings();
        boolean sharedResourceWorld = farmSettings.farms().values().stream()
                .anyMatch(farm -> tycoonSettings.groups().values().stream()
                        .anyMatch(group -> group.worldName().equals(farm.worldName())));
        if (sharedResourceWorld) {
            throw new IllegalArgumentException("A private Tycoon world cannot also be a public farm world");
        }
        boolean duplicateSpawnWorld = farmSettings.farms().values().stream()
                .anyMatch(farm -> farm.worldName().equals(spawnSettings.worldName()))
                || tycoonSettings.groups().values().stream()
                .anyMatch(group -> group.worldName().equals(spawnSettings.worldName()));
        if (duplicateSpawnWorld) {
            throw new IllegalArgumentException("Spawn world must be separate from farms and Skyblocks");
        }
        for (SpawnSettings.PortalDefinition portal : spawnSettings.portals()) {
            if (farmSettings.farm(portal.farmId()).filter(fr.valoriatycoon.farm.FarmDefinition::enabled).isEmpty()) {
                throw new IllegalArgumentException(
                        "Spawn portal references unknown farm " + portal.farmId()
                );
            }
        }

        Path dataDirectory = getDataFolder().toPath().toAbsolutePath().normalize();
        Path databaseFile = dataDirectory.resolve(initialSettings.database().sqliteFile()).normalize();
        if (!databaseFile.startsWith(dataDirectory)) {
            throw new IllegalArgumentException("database.sqlite.file must remain inside the plugin data directory");
        }

        spawnWorld = new SpawnWorldService(spawnSettings, getLogger());
        spawnWorld.initialize();
        warps = new WarpService(warpSettings, spawnWorld);

        database = new SqliteDatabase(
                databaseFile,
                initialSettings.database().busyTimeoutMillis(),
                getLogger()
        );
        PlayerAccountRepository accountRepository = new PlayerAccountRepository(
                database,
                initialSettings.economy().startingBalanceCents()
        );
        economy = new InternalEconomyService(accountRepository, database::isInitialized, getLogger());
        autoSell = new AutoSellService(
                new AutoSellRepository(database),
                economy,
                farmSettings.autoSell()
        );
        BlockRegenerationRepository regenerationRepository = new BlockRegenerationRepository(database);
        CrateKeyRepository crateKeyRepository = new CrateKeyRepository(database);
        CrateRewardRepository crateRewardRepository = new CrateRewardRepository(database);
        TycoonRepository tycoonRepository = new TycoonRepository(database);
        MachineRepository machineRepository = new MachineRepository(database);
        LeaderboardRepository leaderboardRepository = new LeaderboardRepository(database);
        PetRepository petRepository = new PetRepository(database, petSettings);
        QuestRepository questRepository = new QuestRepository(database, questSettings);
        RankRepository rankRepository = new RankRepository(database, questSettings);
        RankBenefitService rankBenefits = new RankBenefitService(
                rankSettings,
                playerId -> tycoonService == null
                        ? 0
                        : tycoonService.ownedBy(playerId).map(tycoon -> tycoon.prestige()).orElse(0)
        );
        MessageService messages = new MessageService(configManager::messages);
        ItemVisualService itemVisuals = new ItemVisualService(configManager.resourcePackSettings());
        leaderboards = new LeaderboardService(
                this,
                leaderboardSettings,
                leaderboardRepository,
                getLogger()
        );
        compaction = new CompactionService(
                this,
                configManager.compactionSettings(),
                itemVisuals,
                messages,
                getLogger()
        );
        CurrencyFormatter formatter = new CurrencyFormatter(() -> configManager.settings().economy());
        LeaderboardValueFormatter leaderboardValues = new LeaderboardValueFormatter(
                formatter,
                rankSettings
        );
        leaderboardHolograms = new LeaderboardHologramService(
                this,
                leaderboardHologramSettings,
                leaderboards,
                leaderboardValues,
                spawnWorld,
                messages,
                getLogger()
        );
        tutorialHub = new TutorialHubService(
                this,
                tutorialHubSettings,
                spawnWorld,
                messages,
                getLogger()
        );
        MainThreadExecutor mainThread = new MainThreadExecutor(this);
        CrateKeyItemService crateKeyItems = new CrateKeyItemService(
                this,
                crateSettings,
                itemVisuals,
                messages
        );
        crateKeys = new CrateKeyService(
                crateSettings,
                crateKeyRepository,
                crateKeyItems,
                messages,
                mainThread,
                database::isInitialized,
                getLogger()
        );
        WarpPanel warpPanel = new WarpPanel(warps, itemVisuals, messages, mainThread);
        CommandGate commandGate = new CommandGate(lifecycle::get, messages);
        PaymentRateLimiter paymentRateLimiter = new PaymentRateLimiter();
        tycoonService = new TycoonService(
                tycoonSettings,
                plotUpgradeSettings,
                tycoonRepository,
                economy,
                mainThread
        );
        petKeys = new PetKeyService(this, petSettings, itemVisuals, messages);
        petEggs = new PetEggService(this, petSettings, itemVisuals, messages);
        pets = new PetService(
                this,
                petSettings,
                petRepository,
                petEggs,
                economy,
                messages,
                mainThread,
                getLogger()
        );
        petReclaim = new PetReclaimService(
                this,
                pets,
                petEggs,
                itemVisuals,
                petSettings,
                formatter,
                messages,
                mainThread
        );
        professions = new ProfessionService(
                this,
                professionSettings,
                new ProfessionRepository(database, professionSettings),
                rankBenefits,
                pets,
                getLogger()
        );
        tools = new ToolProgressionService(
                this,
                toolSettings,
                new ToolRepository(database, toolSettings),
                economy,
                professions,
                rankBenefits,
                pets,
                crateKeys,
                getLogger()
        );
        quests = new QuestService(
                this,
                questSettings,
                questRepository,
                economy,
                crateKeys,
                getLogger()
        );
        tutorial = new TutorialService(
                this,
                configManager.tutorialSettings(),
                new TutorialRepository(database),
                economy,
                quests,
                tycoonService,
                formatter,
                messages,
                getLogger()
        );
        ranks = new RankService(
                rankSettings,
                rankBenefits,
                rankRepository,
                quests,
                tools,
                professions,
                compaction,
                tutorial,
                tycoonService,
                economy,
                mainThread
        );
        tycoonWorlds = new TycoonWorldService(
                tycoonSettings,
                spawnWorld::spawn,
                getLogger()
        );
        tycoonWorlds.initialize();
        tycoonBoundaries = new TycoonBoundaryService(this, tycoonService, tycoonWorlds);
        tycoonPlaytime = new TycoonPlaytimeService(this, tycoonService, getLogger());
        tycoonFlight = new TycoonFlightService(
                this,
                tycoonSettings.flight(),
                tycoonService,
                tycoonWorlds,
                new ConfiguredTycoonFlightPolicy(tycoonSettings.flight()),
                messages
        );
        TycoonInviteService tycoonInvites = new TycoonInviteService(
                Duration.ofSeconds(tycoonSettings.inviteExpirationSeconds())
        );
        plotResets = new PlotResetService(
                this,
                tycoonSettings,
                tycoonService,
                tycoonWorlds,
                messages,
                mainThread,
                getLogger()
        );
        machines = new MachineService(
                this,
                machineSettings,
                machineRepository,
                economy,
                ranks,
                pets,
                mainThread,
                getLogger()
        );

        farmWorlds = new FarmWorldService(farmSettings, ranks::currentRank, getLogger());
        farmWorlds.initialize();
        regeneration = new BlockRegenerationService(
                this,
                farmSettings.regeneration(),
                regenerationRepository,
                getLogger()
        );
        autoSellBatches = new AutoSellBatchService(
                this,
                economy,
                formatter,
                messages,
                mainThread,
                getLogger(),
                farmSettings.autoSell().flushIntervalTicks()
        );
        AutoSellMenuController autoSellMenu = new AutoSellMenuController(
                farmSettings.autoSellMenu(),
                autoSell,
                itemVisuals,
                messages,
                mainThread
        );
        AutoSellPanel autoSellPanel = new AutoSellPanel(
                farmSettings.autoSellMenu(),
                autoSellMenu,
                messages
        );
        FarmMenu farmMenu = new FarmMenu(
                farmSettings,
                farmWorlds,
                itemVisuals,
                messages,
                mainThread
        );
        MultiToolItemService multiToolItems = new MultiToolItemService(
                this,
                toolSettings.multiTool(),
                itemVisuals,
                messages
        );
        toolEffects = new ToolEffectService(
                this,
                tools,
                multiToolItems,
                ranks::currentRank,
                () -> lifecycle.get() == LifecycleState.READY
        );
        MultiToolService multiTool = new MultiToolService(
                toolSettings.multiTool(),
                tools,
                farmWorlds,
                quests,
                toolEffects,
                multiToolItems,
                ranks::currentRank,
                () -> lifecycle.get() == LifecycleState.READY,
                messages
        );
        MultiToolProtectionListener multiToolProtection = new MultiToolProtectionListener(
                multiToolItems
        );
        ToolAbilityListener toolAbilities = new ToolAbilityListener(
                this,
                tools,
                multiToolItems,
                farmWorlds
        );
        ToolUpgradeItemFactory toolItems = new ToolUpgradeItemFactory(
                toolSettings,
                tools,
                professions,
                autoSell,
                formatter,
                itemVisuals,
                messages
        );
        ToolCurrencyChoiceController toolCurrencyChoice = new ToolCurrencyChoiceController(
                toolSettings,
                tools,
                toolEffects,
                economy,
                formatter,
                itemVisuals,
                messages,
                mainThread
        );
        ToolUpgradePanel toolUpgradePanel = new ToolUpgradePanel(
                toolSettings,
                autoSell,
                multiToolItems,
                toolItems,
                toolCurrencyChoice,
                formatter,
                messages,
                mainThread,
                () -> lifecycle.get() == LifecycleState.READY
        );
        PlotUpgradePanel plotUpgradePanel = new PlotUpgradePanel(
                plotUpgradeSettings,
                tycoonService,
                tycoonBoundaries,
                formatter,
                itemVisuals,
                messages,
                mainThread
        );
        QuestPanel questPanel = new QuestPanel(quests, itemVisuals, messages);
        LeaderboardPanel leaderboardPanel = new LeaderboardPanel(
                leaderboards,
                leaderboardValues,
                itemVisuals,
                messages
        );
        RankPanel rankPanel = new RankPanel(
                ranks,
                compaction,
                tycoonService,
                formatter,
                itemVisuals,
                multiToolItems,
                messages,
                mainThread
        );
        PetPanel petPanel = new PetPanel(
                pets,
                tycoonService,
                rankSettings,
                petKeys,
                itemVisuals,
                messages,
                mainThread
        );
        MachineItemService machineItems = new MachineItemService(this, itemVisuals, messages);
        CrateRewardItemService crateRewardItems = new CrateRewardItemService(
                this,
                toolSettings,
                machineSettings,
                itemVisuals,
                formatter,
                messages
        );
        crateRewards = new CrateRewardService(
                crateSettings,
                crateRewardRepository,
                new CrateRewardSelector(
                        crateRewardSettings,
                        ranks::currentRank,
                        List.copyOf(machineSettings.machines().keySet())
                ),
                crateRewardItems,
                crateKeyItems,
                crateKeys,
                petKeys,
                machineItems,
                machineSettings,
                economy,
                tools,
                messages,
                mainThread,
                database::isInitialized,
                getLogger()
        );
        crateStations = new CrateStationService(
                this,
                crateStationSettings,
                crateRewards,
                petPanel,
                spawnWorld,
                itemVisuals,
                messages,
                getLogger()
        );
        crateRewards.setOpeningEffect(crateStations::playOpening);
        MachineControlPanel machineControls = new MachineControlPanel(
                machines,
                machineSettings,
                formatter,
                itemVisuals,
                messages,
                mainThread
        );
        MachineShopPanel machineShop = new MachineShopPanel(
                machineSettings,
                machineItems,
                economy,
                tools,
                tycoonService,
                formatter,
                itemVisuals,
                messages,
                mainThread
        );
        MachineListener machineListener = new MachineListener(
                this,
                machines,
                machineItems,
                machineControls,
                tycoonService,
                messages,
                mainThread
        );

        TycoonCommand tycoonCommand = registerCommands(
                messages,
                formatter,
                mainThread,
                commandGate,
                paymentRateLimiter,
                farmMenu,
                autoSellPanel,
                plotUpgradePanel,
                machineShop,
                questPanel,
                leaderboardPanel,
                rankPanel,
                petPanel,
                warpPanel,
                itemVisuals,
                rankSettings,
                islandMenuSettings,
                tycoonInvites,
                tycoonSettings
        );
        registerListeners(
                messages,
                farmSettings,
                paymentRateLimiter,
                farmMenu,
                autoSellPanel,
                toolUpgradePanel,
                plotUpgradePanel,
                machineShop,
                machineControls,
                machineListener,
                questPanel,
                leaderboardPanel,
                rankPanel,
                petPanel,
                crateKeyItems,
                crateRewardItems,
                warpPanel,
                multiToolItems,
                multiTool,
                multiToolProtection,
                toolAbilities,
                tycoonCommand,
                tycoonPlaytime,
                mainThread
        );

        List<PlayerIdentity> alreadyOnline = getServer().getOnlinePlayers().stream()
                .map(player -> new PlayerIdentity(player.getUniqueId(), player.getName()))
                .toList();

        database.start()
                .thenCompose(ignored -> activateAccounts(alreadyOnline))
                .thenCompose(ignored -> activateFarmPreferences(alreadyOnline))
                .thenCompose(ignored -> activateProfessionProfiles(alreadyOnline))
                .thenCompose(ignored -> activateToolProfiles(alreadyOnline))
                .thenCompose(ignored -> activateQuestProfiles(alreadyOnline))
                .thenCompose(ignored -> tycoonRepository.loadAll())
                .thenCompose(tycoonSnapshot -> regenerationRepository.loadAll()
                        .thenCombine(
                                machineRepository.loadAll(),
                                ResourceSnapshots::new
                        )
                        .thenCombine(
                                petRepository.loadAll(),
                                (resources, petSnapshot) -> new StartupData(
                                        resources.pendingRegenerations(),
                                        tycoonSnapshot,
                                        resources.machines(),
                                        petSnapshot
                                )
                        ))
                .whenCompleteAsync(this::completeStartup, mainThread);
    }

    private TycoonCommand registerCommands(
            MessageService messages,
            CurrencyFormatter formatter,
            MainThreadExecutor mainThread,
            CommandGate commandGate,
            PaymentRateLimiter paymentRateLimiter,
            FarmMenu farmMenu,
            AutoSellPanel autoSellPanel,
            PlotUpgradePanel plotUpgradePanel,
            MachineShopPanel machineShop,
            QuestPanel questPanel,
            LeaderboardPanel leaderboardPanel,
            RankPanel rankPanel,
            PetPanel petPanel,
            WarpPanel warpPanel,
            ItemVisualService itemVisuals,
            RankSettings rankSettings,
            IslandMenuSettings islandMenuSettings,
            TycoonInviteService tycoonInvites,
            TycoonSettings tycoonSettings
    ) {
        BalanceCommand balance = new BalanceCommand(economy, formatter, messages, commandGate, mainThread);
        PayCommand pay = new PayCommand(
                economy,
                () -> configManager.settings().economy(),
                formatter,
                messages,
                commandGate,
                paymentRateLimiter,
                mainThread
        );
        TycoonAdminCommand admin = new TycoonAdminCommand(
                configManager,
                economy,
                economy,
                database,
                tycoonService,
                plotResets,
                tycoonSettings,
                petKeys,
                crateKeys,
                formatter,
                messages,
                commandGate,
                lifecycle::get,
                mainThread,
                getLogger()
        );

        islandMainMenu = new IslandMainMenu(
                islandMenuSettings,
                balance,
                farmMenu,
                machineShop,
                plotUpgradePanel,
                autoSellPanel,
                questPanel,
                leaderboardPanel,
                rankPanel,
                petPanel,
                itemVisuals,
                messages
        );
        TycoonCommand tycoonCommand = new TycoonCommand(
                balance,
                tycoonService,
                tycoonWorlds,
                plotResets,
                tycoonInvites,
                tycoonSettings,
                plotUpgradePanel,
                islandMainMenu,
                questPanel,
                rankSettings,
                formatter,
                messages,
                commandGate,
                mainThread
        );
        CommandRegistrar commands = new CommandRegistrar(this);
        commands.register("balance", balance);
        commands.register("pay", pay);
        commands.register("farm", new FarmCommand(farmMenu, messages, commandGate));
        commands.register("autosell", new AutoSellCommand(autoSellPanel, messages, commandGate));
        commands.register("shop", new ShopCommand(machineShop, messages, commandGate));
        commands.register("rank", new RankCommand(rankPanel, messages, commandGate));
        commands.register("pets", new PetsCommand(petPanel, messages, commandGate));
        commands.register("top", new TopCommand(leaderboardPanel, messages, commandGate));
        commands.register("warp", new WarpCommand(warps, warpPanel, messages, commandGate));
        commands.register("spawn", new SpawnCommand(spawnWorld, messages, commandGate, mainThread));
        commands.register("is", tycoonCommand);
        commands.register("isadmin", admin);
        return tycoonCommand;
    }

    private void registerListeners(
            MessageService messages,
            FarmSettings farmSettings,
            PaymentRateLimiter paymentRateLimiter,
            FarmMenu farmMenu,
            AutoSellPanel autoSellPanel,
            ToolUpgradePanel toolUpgradePanel,
            PlotUpgradePanel plotUpgradePanel,
            MachineShopPanel machineShop,
            MachineControlPanel machineControls,
            MachineListener machineListener,
            QuestPanel questPanel,
            LeaderboardPanel leaderboardPanel,
            RankPanel rankPanel,
            PetPanel petPanel,
            CrateKeyItemService crateKeyItems,
            CrateRewardItemService crateRewardItems,
            WarpPanel warpPanel,
            MultiToolItemService multiToolItems,
            MultiToolService multiTool,
            MultiToolProtectionListener multiToolProtection,
            ToolAbilityListener toolAbilities,
            TycoonCommand tycoonCommand,
            TycoonPlaytimeService tycoonPlaytime,
            MainThreadExecutor mainThread
    ) {
        getServer().getPluginManager().registerEvents(
                new PlayerAccountListener(
                        economy,
                        paymentRateLimiter,
                        lifecycle::get,
                        messages,
                        getLogger()
                ),
                this
        );
        getServer().getPluginManager().registerEvents(islandMainMenu, this);
        getServer().getPluginManager().registerEvents(farmMenu, this);
        getServer().getPluginManager().registerEvents(
                new SpawnProtectionListener(this, spawnWorld, farmMenu, messages),
                this
        );
        getServer().getPluginManager().registerEvents(autoSellPanel, this);
        getServer().getPluginManager().registerEvents(toolUpgradePanel, this);
        getServer().getPluginManager().registerEvents(plotUpgradePanel, this);
        getServer().getPluginManager().registerEvents(machineShop, this);
        getServer().getPluginManager().registerEvents(machineControls, this);
        getServer().getPluginManager().registerEvents(machineListener, this);
        getServer().getPluginManager().registerEvents(questPanel, this);
        getServer().getPluginManager().registerEvents(leaderboardPanel, this);
        getServer().getPluginManager().registerEvents(rankPanel, this);
        getServer().getPluginManager().registerEvents(petPanel, this);
        getServer().getPluginManager().registerEvents(warpPanel, this);
        getServer().getPluginManager().registerEvents(warps, this);
        getServer().getPluginManager().registerEvents(tutorialHub, this);
        getServer().getPluginManager().registerEvents(crateKeys, this);
        getServer().getPluginManager().registerEvents(crateRewards, this);
        getServer().getPluginManager().registerEvents(crateStations, this);
        getServer().getPluginManager().registerEvents(
                new CrateKeyProtectionListener(crateKeyItems),
                this
        );
        getServer().getPluginManager().registerEvents(
                new CrateRewardProtectionListener(crateRewardItems),
                this
        );
        getServer().getPluginManager().registerEvents(pets, this);
        getServer().getPluginManager().registerEvents(new PetKeyListener(petKeys, messages), this);
        getServer().getPluginManager().registerEvents(
                new PetEggListener(pets, petEggs, configManager.rankSettings(), messages, mainThread),
                this
        );
        getServer().getPluginManager().registerEvents(petReclaim, this);
        getServer().getPluginManager().registerEvents(new CompactionListener(compaction), this);
        getServer().getPluginManager().registerEvents(multiTool, this);
        getServer().getPluginManager().registerEvents(multiToolProtection, this);
        getServer().getPluginManager().registerEvents(toolAbilities, this);
        getServer().getPluginManager().registerEvents(toolEffects, this);
        getServer().getPluginManager().registerEvents(new QuestPlayerListener(quests, getLogger()), this);
        getServer().getPluginManager().registerEvents(
                new TutorialListener(tutorial, farmWorlds),
                this
        );
        getServer().getPluginManager().registerEvents(
                new ToolPlayerListener(
                        tools,
                        professions,
                        toolEffects,
                        mainThread,
                        getLogger()
                ),
                this
        );
        FarmProtectionListener protection = new FarmProtectionListener(
                farmWorlds,
                farmSettings.rankBarrier(),
                regeneration,
                autoSell,
                autoSellBatches,
                tools,
                multiToolItems,
                quests,
                ranks,
                pets,
                messages
        );
        getServer().getPluginManager().registerEvents(
                new FarmPlayerListener(autoSell, autoSellBatches, farmWorlds, protection, getLogger()),
                this
        );
        getServer().getPluginManager().registerEvents(protection, this);
        TycoonProtectionListener tycoonProtection = new TycoonProtectionListener(
                tycoonService,
                tycoonWorlds,
                messages
        );
        getServer().getPluginManager().registerEvents(tycoonProtection, this);
        getServer().getPluginManager().registerEvents(tycoonFlight, this);
        getServer().getPluginManager().registerEvents(tycoonBoundaries, this);
        getServer().getPluginManager().registerEvents(
                new TycoonSessionListener(tycoonCommand, tycoonProtection, tycoonPlaytime),
                this
        );
    }

    private CompletableFuture<Void> activateAccounts(List<PlayerIdentity> identities) {
        CompletableFuture<?>[] loads = identities.stream()
                .map(identity -> economy.activateAccount(identity.playerId(), identity.name()))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(loads);
    }

    private CompletableFuture<Void> activateFarmPreferences(List<PlayerIdentity> identities) {
        CompletableFuture<?>[] loads = identities.stream()
                .map(identity -> autoSell.activate(identity.playerId()))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(loads);
    }

    private CompletableFuture<Void> activateToolProfiles(List<PlayerIdentity> identities) {
        CompletableFuture<?>[] loads = identities.stream()
                .map(identity -> tools.activate(identity.playerId()))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(loads);
    }

    private CompletableFuture<Void> activateProfessionProfiles(List<PlayerIdentity> identities) {
        CompletableFuture<?>[] loads = identities.stream()
                .map(identity -> professions.activate(identity.playerId()))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(loads);
    }

    private CompletableFuture<Void> activateQuestProfiles(List<PlayerIdentity> identities) {
        CompletableFuture<?>[] loads = identities.stream()
                .map(identity -> quests.activate(identity.playerId()))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(loads);
    }

    private void completeStartup(StartupData startup, Throwable error) {
        if (lifecycle.get() == LifecycleState.STOPPING) {
            return;
        }
        if (error != null) {
            lifecycle.set(LifecycleState.FAILED);
            getLogger().log(Level.SEVERE, "ValoriaTycoon storage initialization failed", unwrap(error));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        tycoonService.initialize(startup.tycoons());
        machines.initialize(startup.machines());
        pets.initialize(startup.pets());
        compaction.start();
        plotResets.start();
        plotResets.resume(tycoonService.all());
        tycoonPlaytime.start();
        getServer().getOnlinePlayers().forEach(tycoonFlight::refresh);
        getServer().getOnlinePlayers().forEach(tycoonBoundaries::refresh);
        regeneration.start(startup.pendingRegenerations());
        autoSellBatches.start();
        pets.start();
        petReclaim.start();
        professions.start();
        tools.start();
        quests.start();
        crateKeys.start();
        crateRewards.start();
        crateStations.start();
        VotifierCrateBridge.register(this, crateKeys, getLogger());
        leaderboards.start();
        leaderboardHolograms.start();
        tutorialHub.start();
        tutorial.start();
        machines.start();
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholders = List.of(
                    new ValoriaPlaceholderExpansion(
                            this,
                            "valoriatycoon",
                            economy,
                            tycoonService,
                            machines,
                            pets,
                            petKeys,
                            configManager.rankSettings()
                    ),
                    new ValoriaPlaceholderExpansion(
                            this,
                            "tycoon",
                            economy,
                            tycoonService,
                            machines,
                            pets,
                            petKeys,
                            configManager.rankSettings()
                    )
            );
            placeholders.forEach(ValoriaPlaceholderExpansion::register);
            getLogger().info("PlaceholderAPI expansions registered (valoriatycoon + legacy tycoon).");
        }
        lifecycle.set(LifecycleState.READY);
        getServer().getOnlinePlayers().forEach(toolEffects::refresh);
        getLogger().info(
                "ValoriaTycoon is ready (economy, " + farmWorlds.farms().size()
                        + " public farm worlds, medieval spawn, tutorial academy, Skyblock Tycoons, onboarding, compaction, generic crates, machines, pets, leaderboards/holograms, tools and professions)."
        );
    }

    @Override
    public void onDisable() {
        lifecycle.set(LifecycleState.STOPPING);
        placeholders.forEach(ValoriaPlaceholderExpansion::unregister);
        placeholders = List.of();
        if (compaction != null) {
            compaction.stop();
        }
        if (machines != null) {
            machines.stop();
        }
        if (tutorialHub != null) {
            tutorialHub.stop();
        }
        if (leaderboardHolograms != null) {
            leaderboardHolograms.stop();
        }
        if (leaderboards != null) {
            leaderboards.stop();
        }
        if (crateStations != null) {
            crateStations.stop();
        }
        if (crateRewards != null) {
            crateRewards.stop();
        }
        if (petReclaim != null) {
            petReclaim.stop();
        }
        if (pets != null && database != null && database.isInitialized()) {
            pets.stop(AUTOSELL_CLOSE_TIMEOUT);
        }
        if (tutorial != null && database != null && database.isInitialized()) {
            tutorial.stop(AUTOSELL_CLOSE_TIMEOUT);
        }
        if (quests != null && database != null && database.isInitialized()) {
            quests.stop(AUTOSELL_CLOSE_TIMEOUT);
        }
        if (autoSellBatches != null && database != null && database.isInitialized()) {
            autoSellBatches.stopAndFlush(AUTOSELL_CLOSE_TIMEOUT);
        }
        if (toolEffects != null) {
            toolEffects.stop();
        }
        if (tools != null && database != null && database.isInitialized()) {
            tools.stopAndFlush(AUTOSELL_CLOSE_TIMEOUT);
        }
        if (professions != null && database != null && database.isInitialized()) {
            professions.stopAndFlush(AUTOSELL_CLOSE_TIMEOUT);
        }
        if (tycoonBoundaries != null) {
            tycoonBoundaries.stop();
        }
        if (tycoonFlight != null) {
            tycoonFlight.stop();
        }
        if (tycoonPlaytime != null) {
            tycoonPlaytime.stop();
        }
        if (plotResets != null) {
            plotResets.stop();
        }
        if (regeneration != null) {
            regeneration.stop();
        }
        if (database != null) {
            database.close(DATABASE_CLOSE_TIMEOUT);
        }
    }

    /** Returns the currently installed economy implementation for server-side integrations. */
    public EconomyService getEconomyService() {
        return Objects.requireNonNull(economy, "ValoriaTycoon has not been bootstrapped");
    }

    /** Returns idempotent generic crate-key issuance for vote and server integrations. */
    public CrateKeyService getCrateKeyService() {
        return Objects.requireNonNull(crateKeys, "ValoriaTycoon crate keys have not been bootstrapped");
    }

    /** Returns authenticated compact-item creation and validation services. */
    public CompactionService getCompactionService() {
        return Objects.requireNonNull(compaction, "ValoriaTycoon compaction has not been bootstrapped");
    }

    /** Returns first-rank onboarding progress and reward services. */
    public TutorialService getTutorialService() {
        return Objects.requireNonNull(tutorial, "ValoriaTycoon tutorial has not been bootstrapped");
    }

    /** Returns configurable menu/direct warp resolution and teleport services. */
    public WarpService getWarpService() {
        return Objects.requireNonNull(warps, "ValoriaTycoon warps have not been bootstrapped");
    }

    /** Returns the generated medieval hub and spawn teleport service. */
    public SpawnWorldService getSpawnWorldService() {
        return Objects.requireNonNull(spawnWorld, "ValoriaTycoon spawn has not been bootstrapped");
    }

    /** Returns the generated public-farm world service for server-side integrations. */
    public FarmWorldService getFarmWorldService() {
        return Objects.requireNonNull(farmWorlds, "ValoriaTycoon farm worlds have not been bootstrapped");
    }

    /** Returns per-player tool progression and capability services. */
    public ToolProgressionService getToolProgressionService() {
        return Objects.requireNonNull(tools, "ValoriaTycoon tools have not been bootstrapped");
    }

    /** Returns permanent profession progression linked to the four tool types. */
    public ProfessionService getProfessionService() {
        return Objects.requireNonNull(
                professions,
                "ValoriaTycoon professions have not been bootstrapped"
        );
    }

    /** Returns the placed-machine production and storage service. */
    public MachineService getMachineService() {
        return Objects.requireNonNull(machines, "ValoriaTycoon machines have not been bootstrapped");
    }

    /** Returns the non-blocking cached server leaderboard service. */
    public LeaderboardService getLeaderboardService() {
        return Objects.requireNonNull(
                leaderboards,
                "ValoriaTycoon leaderboards have not been bootstrapped"
        );
    }

    /** Returns pet ownership, progression, effects and follower visuals. */
    public PetService getPetService() {
        return Objects.requireNonNull(pets, "ValoriaTycoon pets have not been bootstrapped");
    }

    /** Returns authenticated physical-key creation and validation services. */
    public PetKeyService getPetKeyService() {
        return Objects.requireNonNull(petKeys, "ValoriaTycoon pet keys have not been bootstrapped");
    }

    /** Returns authenticated pet-egg creation and validation services. */
    public PetEggService getPetEggService() {
        return Objects.requireNonNull(petEggs, "ValoriaTycoon pet eggs have not been bootstrapped");
    }

    /** Returns the private Tycoon ownership and plot service. */
    public TycoonService getTycoonService() {
        return Objects.requireNonNull(tycoonService, "ValoriaTycoon plots have not been bootstrapped");
    }

    /** Returns the fail-closed plugin lifecycle state. */
    public LifecycleState getLifecycleState() {
        return lifecycle.get();
    }

    private Throwable unwrap(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null
                && (current instanceof java.util.concurrent.CompletionException
                || current instanceof java.util.concurrent.ExecutionException)) {
            current = current.getCause();
        }
        return current;
    }

    private record ResourceSnapshots(
            List<PendingBlockRegeneration> pendingRegenerations,
            MachineSnapshot machines
    ) {
    }

    private record StartupData(
            List<PendingBlockRegeneration> pendingRegenerations,
            TycoonDataSnapshot tycoons,
            MachineSnapshot machines,
            PetSnapshot pets
    ) {
    }

    private record PlayerIdentity(java.util.UUID playerId, String name) {
    }
}
