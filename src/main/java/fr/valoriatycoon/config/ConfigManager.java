package fr.valoriatycoon.config;

import fr.valoriatycoon.compaction.CompactionConfigLoader;
import fr.valoriatycoon.compaction.CompactionSettings;
import fr.valoriatycoon.crates.CrateConfigLoader;
import fr.valoriatycoon.crates.CrateRewardConfigLoader;
import fr.valoriatycoon.crates.CrateRewardSettings;
import fr.valoriatycoon.crates.CrateSettings;
import fr.valoriatycoon.crates.CrateStationConfigLoader;
import fr.valoriatycoon.crates.CrateStationSettings;
import fr.valoriatycoon.farm.FarmConfigLoader;
import fr.valoriatycoon.farm.FarmSettings;
import fr.valoriatycoon.gui.IslandMenuConfigLoader;
import fr.valoriatycoon.gui.IslandMenuSettings;
import fr.valoriatycoon.leaderboards.LeaderboardConfigLoader;
import fr.valoriatycoon.leaderboards.LeaderboardHologramConfigLoader;
import fr.valoriatycoon.leaderboards.LeaderboardHologramSettings;
import fr.valoriatycoon.leaderboards.LeaderboardSettings;
import fr.valoriatycoon.machines.MachineConfigLoader;
import fr.valoriatycoon.machines.MachineSettings;
import fr.valoriatycoon.pets.PetConfigLoader;
import fr.valoriatycoon.pets.PetSettings;
import fr.valoriatycoon.professions.ProfessionConfigLoader;
import fr.valoriatycoon.professions.ProfessionSettings;
import fr.valoriatycoon.quests.QuestConfigLoader;
import fr.valoriatycoon.quests.QuestSettings;
import fr.valoriatycoon.ranks.RankConfigLoader;
import fr.valoriatycoon.ranks.RankSettings;
import fr.valoriatycoon.resourcepack.ResourcePackConfigLoader;
import fr.valoriatycoon.resourcepack.ResourcePackSettings;
import fr.valoriatycoon.spawn.SpawnConfigLoader;
import fr.valoriatycoon.spawn.SpawnSettings;
import fr.valoriatycoon.tools.ToolConfigLoader;
import fr.valoriatycoon.tools.ToolSettings;
import fr.valoriatycoon.tutorial.TutorialConfigLoader;
import fr.valoriatycoon.tutorial.TutorialHubConfigLoader;
import fr.valoriatycoon.tutorial.TutorialHubSettings;
import fr.valoriatycoon.tutorial.TutorialSettings;
import fr.valoriatycoon.tycoon.TycoonConfigLoader;
import fr.valoriatycoon.tycoon.TycoonSettings;
import fr.valoriatycoon.upgrades.PlotUpgradeConfigLoader;
import fr.valoriatycoon.upgrades.PlotUpgradeSettings;
import fr.valoriatycoon.warps.WarpConfigLoader;
import fr.valoriatycoon.warps.WarpSettings;
import java.io.File;
import java.util.Objects;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** Owns configuration files and publishes validated immutable settings snapshots. */
public final class ConfigManager {
    private final JavaPlugin plugin;
    private volatile CoreSettings settings;
    private volatile CompactionSettings compactionSettings;
    private volatile CrateSettings crateSettings;
    private volatile CrateRewardSettings crateRewardSettings;
    private volatile CrateStationSettings crateStationSettings;
    private volatile FarmSettings farmSettings;
    private volatile ToolSettings toolSettings;
    private volatile MachineSettings machineSettings;
    private volatile PetSettings petSettings;
    private volatile ProfessionSettings professionSettings;
    private volatile IslandMenuSettings islandMenuSettings;
    private volatile LeaderboardSettings leaderboardSettings;
    private volatile LeaderboardHologramSettings leaderboardHologramSettings;
    private volatile QuestSettings questSettings;
    private volatile RankSettings rankSettings;
    private volatile ResourcePackSettings resourcePackSettings;
    private volatile SpawnSettings spawnSettings;
    private volatile TutorialSettings tutorialSettings;
    private volatile TutorialHubSettings tutorialHubSettings;
    private volatile TycoonSettings tycoonSettings;
    private volatile PlotUpgradeSettings plotUpgradeSettings;
    private volatile WarpSettings warpSettings;
    private volatile YamlConfiguration messages;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void initialize() {
        plugin.saveDefaultConfig();
        saveResourceIfMissing("messages.yml");
        saveResourceIfMissing("compaction.yml");
        saveResourceIfMissing("crates.yml");
        saveResourceIfMissing("crate-rewards.yml");
        saveResourceIfMissing("crate-stations.yml");
        saveResourceIfMissing("farms.yml");
        saveResourceIfMissing("tools.yml");
        saveResourceIfMissing("machines.yml");
        saveResourceIfMissing("pets.yml");
        saveResourceIfMissing("professions.yml");
        saveResourceIfMissing("menus.yml");
        saveResourceIfMissing("leaderboards.yml");
        saveResourceIfMissing("leaderboard-holograms.yml");
        saveResourceIfMissing("quests.yml");
        saveResourceIfMissing("ranks.yml");
        saveResourceIfMissing("resource-pack.yml");
        saveResourceIfMissing("spawn.yml");
        saveResourceIfMissing("tutorial.yml");
        saveResourceIfMissing("tutorial-hub.yml");
        saveResourceIfMissing("tycoons.yml");
        saveResourceIfMissing("upgrades.yml");
        saveResourceIfMissing("warps.yml");
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        CoreSettings validatedSettings = CoreSettings.from(plugin.getConfig());
        YamlConfiguration loadedMessages = load("messages.yml");
        CompactionSettings validatedCompactionSettings = CompactionConfigLoader.load(
                load("compaction.yml")
        );
        CrateSettings validatedCrates = CrateConfigLoader.load(load("crates.yml"));
        CrateRewardSettings validatedCrateRewards = CrateRewardConfigLoader.load(
                load("crate-rewards.yml")
        );
        CrateStationSettings validatedCrateStations = CrateStationConfigLoader.load(
                load("crate-stations.yml")
        );
        FarmSettings validatedFarmSettings = FarmConfigLoader.load(load("farms.yml"));
        ToolSettings validatedToolSettings = ToolConfigLoader.load(load("tools.yml"));
        MachineSettings validatedMachineSettings = MachineConfigLoader.load(load("machines.yml"));
        PetSettings validatedPetSettings = PetConfigLoader.load(load("pets.yml"));
        ProfessionSettings validatedProfessionSettings = ProfessionConfigLoader.load(
                load("professions.yml")
        );
        IslandMenuSettings validatedIslandMenu = IslandMenuConfigLoader.load(load("menus.yml"));
        LeaderboardSettings validatedLeaderboards = LeaderboardConfigLoader.load(
                load("leaderboards.yml")
        );
        LeaderboardHologramSettings validatedLeaderboardHolograms = LeaderboardHologramConfigLoader.load(
                load("leaderboard-holograms.yml")
        );
        QuestSettings validatedQuests = QuestConfigLoader.load(load("quests.yml"));
        RankSettings validatedRanks = RankConfigLoader.load(
                load("ranks.yml"),
                validatedCompactionSettings
        );
        ResourcePackSettings validatedResourcePack = ResourcePackConfigLoader.load(
                load("resource-pack.yml")
        );
        SpawnSettings validatedSpawn = SpawnConfigLoader.load(load("spawn.yml"));
        TutorialSettings validatedTutorial = TutorialConfigLoader.load(load("tutorial.yml"));
        TutorialHubSettings validatedTutorialHub = TutorialHubConfigLoader.load(
                load("tutorial-hub.yml")
        );
        TycoonSettings validatedTycoonSettings = TycoonConfigLoader.load(load("tycoons.yml"));
        PlotUpgradeSettings validatedPlotUpgrades = PlotUpgradeConfigLoader.load(
                load("upgrades.yml"),
                validatedTycoonSettings
        );
        WarpSettings validatedWarps = WarpConfigLoader.load(load("warps.yml"));

        this.settings = validatedSettings;
        this.messages = loadedMessages;
        this.compactionSettings = validatedCompactionSettings;
        this.crateSettings = validatedCrates;
        this.crateRewardSettings = validatedCrateRewards;
        this.crateStationSettings = validatedCrateStations;
        this.farmSettings = validatedFarmSettings;
        this.toolSettings = validatedToolSettings;
        this.machineSettings = validatedMachineSettings;
        this.petSettings = validatedPetSettings;
        this.professionSettings = validatedProfessionSettings;
        this.islandMenuSettings = validatedIslandMenu;
        this.leaderboardSettings = validatedLeaderboards;
        this.leaderboardHologramSettings = validatedLeaderboardHolograms;
        this.questSettings = validatedQuests;
        this.rankSettings = validatedRanks;
        this.resourcePackSettings = validatedResourcePack;
        this.spawnSettings = validatedSpawn;
        this.tutorialSettings = validatedTutorial;
        this.tutorialHubSettings = validatedTutorialHub;
        this.tycoonSettings = validatedTycoonSettings;
        this.plotUpgradeSettings = validatedPlotUpgrades;
        this.warpSettings = validatedWarps;
    }

    public CoreSettings settings() {
        return Objects.requireNonNull(settings, "Configuration has not been initialized");
    }

    public CompactionSettings compactionSettings() {
        return Objects.requireNonNull(
                compactionSettings,
                "Compaction configuration has not been initialized"
        );
    }

    public CrateSettings crateSettings() {
        return Objects.requireNonNull(crateSettings, "Crates have not been initialized");
    }

    public CrateRewardSettings crateRewardSettings() {
        return Objects.requireNonNull(
                crateRewardSettings,
                "Crate rewards have not been initialized"
        );
    }

    public CrateStationSettings crateStationSettings() {
        return Objects.requireNonNull(
                crateStationSettings,
                "Physical crate stations have not been initialized"
        );
    }

    public FarmSettings farmSettings() {
        return Objects.requireNonNull(farmSettings, "Farm configuration has not been initialized");
    }

    public ToolSettings toolSettings() {
        return Objects.requireNonNull(toolSettings, "Tool configuration has not been initialized");
    }

    public MachineSettings machineSettings() {
        return Objects.requireNonNull(machineSettings, "Machine configuration has not been initialized");
    }

    public PetSettings petSettings() {
        return Objects.requireNonNull(petSettings, "Pet configuration has not been initialized");
    }

    public ProfessionSettings professionSettings() {
        return Objects.requireNonNull(
                professionSettings,
                "Profession configuration has not been initialized"
        );
    }

    public IslandMenuSettings islandMenuSettings() {
        return Objects.requireNonNull(islandMenuSettings, "Island menu has not been initialized");
    }

    public LeaderboardSettings leaderboardSettings() {
        return Objects.requireNonNull(
                leaderboardSettings,
                "Leaderboards have not been initialized"
        );
    }

    public LeaderboardHologramSettings leaderboardHologramSettings() {
        return Objects.requireNonNull(
                leaderboardHologramSettings,
                "Leaderboard holograms have not been initialized"
        );
    }

    public QuestSettings questSettings() {
        return Objects.requireNonNull(questSettings, "Quests have not been initialized");
    }

    public RankSettings rankSettings() {
        return Objects.requireNonNull(rankSettings, "Ranks have not been initialized");
    }

    public ResourcePackSettings resourcePackSettings() {
        return Objects.requireNonNull(
                resourcePackSettings,
                "Resource-pack configuration has not been initialized"
        );
    }

    public SpawnSettings spawnSettings() {
        return Objects.requireNonNull(spawnSettings, "Spawn configuration has not been initialized");
    }

    public TutorialSettings tutorialSettings() {
        return Objects.requireNonNull(
                tutorialSettings,
                "Tutorial configuration has not been initialized"
        );
    }

    public TutorialHubSettings tutorialHubSettings() {
        return Objects.requireNonNull(
                tutorialHubSettings,
                "Tutorial hub configuration has not been initialized"
        );
    }

    public TycoonSettings tycoonSettings() {
        return Objects.requireNonNull(tycoonSettings, "Tycoon configuration has not been initialized");
    }

    public PlotUpgradeSettings plotUpgradeSettings() {
        return Objects.requireNonNull(plotUpgradeSettings, "Plot upgrades have not been initialized");
    }

    public WarpSettings warpSettings() {
        return Objects.requireNonNull(warpSettings, "Warps have not been initialized");
    }

    public YamlConfiguration messages() {
        return Objects.requireNonNull(messages, "Messages have not been initialized");
    }

    private YamlConfiguration load(String resourceName) {
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), resourceName)
        );
        migratePremiumTitles(resourceName, loaded);
        return loaded;
    }

    /** Applies the v0.38 visual title upgrade only when an administrator kept the exact old default. */
    private void migratePremiumTitles(String resourceName, YamlConfiguration config) {
        switch (resourceName) {
            case "farms.yml" -> {
                replaceDefault(config, "menu.title", "<dark_gray>Choisir un monde de farm</dark_gray>",
                        "<gold><bold>FARMS DE VALORIA</bold></gold>");
                replaceDefault(config, "zone-menu.title", "<dark_gray><farm> — choisir une zone</dark_gray>",
                        "<gold><bold><farm></bold></gold>");
                replaceDefault(config, "autosell-menu.title", "<dark_gray>Gestion de la vente automatique</dark_gray>",
                        "<gold><bold>VENTE AUTOMATIQUE</bold></gold>");
            }
            case "leaderboards.yml" -> {
                replaceDefault(config, "menu.title", "<dark_gray>Classements de Valoria</dark_gray>",
                        "<gold><bold>CLASSEMENTS</bold></gold>");
                replaceDefault(config, "menu.detail-title", "<dark_gray>Classement — <category></dark_gray>",
                        "<gold><bold>TOP • <category></bold></gold>");
            }
            case "machines.yml" -> {
                replaceDefault(config, "shop.title", "<dark_gray>Boutique des générateurs</dark_gray>",
                        "<gold><bold>GÉNÉRATEURS</bold></gold>");
                replaceDefault(config, "control.title", "<dark_gray>Générateur — <machine></dark_gray>",
                        "<gold><bold><machine></bold></gold>");
            }
            case "menus.yml" -> replaceDefault(config, "menu.title", "<dark_gray>Mon Skyblock</dark_gray>",
                    "<gold><bold>MON SKYBLOCK</bold></gold>");
            case "pets.yml" -> {
                replaceDefault(config, "menu.title", "<dark_gray>Mes Pets</dark_gray>",
                        "<gold><bold>MES PETS</bold></gold>");
                replaceDefault(config, "reclaim.menu-title", "<dark_gray>Remettre un pet en œuf</dark_gray>",
                        "<gold><bold>GARDIEN DES ŒUFS</bold></gold>");
            }
            case "tools.yml" -> {
                replaceDefault(config, "menu.title", "<dark_gray>Améliorations — <tool></dark_gray>",
                        "<gold><bold>OUTIL • <tool></bold></gold>");
                replaceDefault(config, "purchase-menu.title", "<dark_gray>Choisir la monnaie — <tool></dark_gray>",
                        "<gold><bold>PAIEMENT • <tool></bold></gold>");
            }
            case "upgrades.yml" -> replaceDefault(config, "menu.title",
                    "<dark_gray>Améliorations du Skyblock</dark_gray>", "<gold><bold>AMÉLIORATIONS</bold></gold>");
            case "warps.yml" -> replaceDefault(config, "menu.title", "<dark_gray>Warps de Valoria</dark_gray>",
                    "<gold><bold>WARPS DE VALORIA</bold></gold>");
            case "messages.yml" -> replaceDefault(config, "machines.purchase-title",
                    "<dark_gray>Acheter <machine></dark_gray>", "<gold><bold>ACHAT • <machine></bold></gold>");
            default -> {
                // This migration intentionally leaves every unrelated setting untouched.
            }
        }
    }

    private void replaceDefault(
            YamlConfiguration config,
            String path,
            String oldDefault,
            String premiumDefault
    ) {
        if (oldDefault.equals(config.getString(path))) {
            config.set(path, premiumDefault);
        }
    }

    private void saveResourceIfMissing(String resourceName) {
        File destination = new File(plugin.getDataFolder(), resourceName);
        if (!destination.isFile()) {
            plugin.saveResource(resourceName, false);
        }
    }
}
