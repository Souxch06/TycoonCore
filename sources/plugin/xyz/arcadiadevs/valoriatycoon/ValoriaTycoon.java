package xyz.arcadiadevs.valoriatycoon;

import com.awaitquality.api.spigot.chat.ChatUtil;
import com.cryptomorin.xseries.XMaterial;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.bananapuncher714.nbteditor.NBTEditor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import marcono1234.gson.recordadapter.RecordTypeAdapterFactory;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.holoeasy.HoloEasy;
import org.holoeasy.hologram.Hologram;
import org.holoeasy.pool.IHologramPool;
import xyz.arcadiadevs.valoriatycoon.commands.Commands;
import xyz.arcadiadevs.valoriatycoon.commands.CommandsTabCompletion;
import xyz.arcadiadevs.valoriatycoon.commands.SellCommandListener;
import xyz.arcadiadevs.valoriatycoon.events.BlockBreak;
import xyz.arcadiadevs.valoriatycoon.events.BlockInteraction;
import xyz.arcadiadevs.valoriatycoon.events.BlockPlace;
import xyz.arcadiadevs.valoriatycoon.events.CraftItem;
import xyz.arcadiadevs.valoriatycoon.events.EnchantItem;
import xyz.arcadiadevs.valoriatycoon.events.EntityExplode;
import xyz.arcadiadevs.valoriatycoon.events.InstantBreak;
import xyz.arcadiadevs.valoriatycoon.events.OnInventoryClose;
import xyz.arcadiadevs.valoriatycoon.events.OnInventoryOpen;
import xyz.arcadiadevs.valoriatycoon.events.OnJoin;
import xyz.arcadiadevs.valoriatycoon.events.OnWandUse;
import xyz.arcadiadevs.valoriatycoon.events.PistonEvent;
import xyz.arcadiadevs.valoriatycoon.events.SmeltItem;
import xyz.arcadiadevs.valoriatycoon.events.skyblock.Bentobox;
import xyz.arcadiadevs.valoriatycoon.events.skyblock.IridiumSkyblock;
import xyz.arcadiadevs.valoriatycoon.models.GeneratorsData;
import xyz.arcadiadevs.valoriatycoon.models.LocationsData;
import xyz.arcadiadevs.valoriatycoon.models.PlayerData;
import xyz.arcadiadevs.valoriatycoon.models.WandData;
import xyz.arcadiadevs.valoriatycoon.models.events.DropEvent;
import xyz.arcadiadevs.valoriatycoon.models.events.Event;
import xyz.arcadiadevs.valoriatycoon.models.events.SellEvent;
import xyz.arcadiadevs.valoriatycoon.models.events.SpeedEvent;
import xyz.arcadiadevs.valoriatycoon.placeholders.PapiHandler;
import xyz.arcadiadevs.valoriatycoon.tasks.CleanupTask;
import xyz.arcadiadevs.valoriatycoon.tasks.DataSaveTask;
import xyz.arcadiadevs.valoriatycoon.tasks.EventLoop;
import xyz.arcadiadevs.valoriatycoon.tasks.SpawnerTask;
import xyz.arcadiadevs.valoriatycoon.utils.HologramsUtil;
import xyz.arcadiadevs.valoriatycoon.utils.ItemUtil;
import xyz.arcadiadevs.valoriatycoon.utils.Metrics;
import xyz.arcadiadevs.valoriatycoon.utils.config.Config;
import xyz.arcadiadevs.valoriatycoon.utils.config.message.Messages;

public final class ValoriaTycoon
extends JavaPlugin {
    public static ValoriaTycoon instance;
    private IHologramPool hologramPool;
    private Gson gson;
    private LocationsData locationsData;
    private WandData wandData;
    private PlayerData playerData;
    private GeneratorsData generatorsData;
    private Economy econ = null;
    private List<Event> events;
    private DataSaveTask dataSaveTask;
    private PapiHandler papiHandler;
    private Metrics metrics;

    public void onEnable() {
        ValoriaTycoon.loadConfig0();
        instance = this;
        new BukkitRunnable(){

            public void run() {
                ValoriaTycoon.this.saveDefaultConfig();
                ValoriaTycoon.this.moveBlockData();
                ValoriaTycoon.this.saveResourceIfNotExists("data/block_data.json", false);
                ValoriaTycoon.this.saveResourceIfNotExists("data/wands_data.json", false);
                ValoriaTycoon.this.saveResourceIfNotExists("data/player_data.json", false);
                ValoriaTycoon.this.saveResourceIfNotExists("messages.yml", false);
                ValoriaTycoon.this.setupEconomy();
                Messages.init();
                ValoriaTycoon.this.gson = new GsonBuilder().registerTypeAdapterFactory(RecordTypeAdapterFactory.DEFAULT).setPrettyPrinting().create();
                ValoriaTycoon.this.generatorsData = ValoriaTycoon.this.loadGeneratorsData();
                ValoriaTycoon.this.locationsData = new LocationsData(ValoriaTycoon.this.loadBlockDataFromJson());
                ValoriaTycoon.this.playerData = new PlayerData(ValoriaTycoon.this.loadPlayerDataFromJson());
                ValoriaTycoon.this.wandData = new WandData(ValoriaTycoon.this.loadWandsDataFromJson());
                ValoriaTycoon.this.events = ValoriaTycoon.this.loadValoriaTycoonEvents();
                ValoriaTycoon.this.metrics = new Metrics(instance, 19293);
                if (ValoriaTycoon.this.getServer().getPluginManager().getPlugin("PlaceHolderAPI") != null) {
                    ValoriaTycoon.instance.papiHandler = new PapiHandler((Plugin)instance, ValoriaTycoon.this.locationsData, ValoriaTycoon.this.playerData);
                    ValoriaTycoon.instance.papiHandler.register();
                }
                ValoriaTycoon.this.registerTasks();
                ValoriaTycoon.this.loadPlayers();
                ValoriaTycoon.this.loadBukkitEvents();
                ValoriaTycoon.this.registerCommands();
                ValoriaTycoon.this.registerTabCompletion();
                ValoriaTycoon.this.loadHolograms();
                ValoriaTycoon.this.getLogger().info("ValoriaTycoon a été activé.");
            }
        }.runTaskLater((Plugin)this, 20L);
    }

    public void onDisable() {
        this.dataSaveTask.saveBlockDataToJson();
        this.dataSaveTask.saveWandDataToJson();
        this.dataSaveTask.savePlayerDataToJson();
        if (this.papiHandler != null) {
            this.papiHandler.unregister();
        }
        HandlerList.unregisterAll((Plugin)this);
        Bukkit.getScheduler().cancelTasks((Plugin)this);
        this.reloadConfig();
        for (LocationsData.GeneratorLocation generatorLocation : this.locationsData.locations()) {
            if (generatorLocation.getHologram() == null) continue;
            HologramsUtil.removeHologram(generatorLocation.getHologram());
        }
        this.locationsData.locations().clear();
        this.locationsData = null;
        this.generatorsData.generators().clear();
        this.generatorsData = null;
        this.playerData.data().clear();
        this.playerData = null;
        this.wandData.wands().clear();
        this.wandData = null;
        this.events.clear();
        this.events = null;
        this.hologramPool = null;
        this.econ = null;
        this.papiHandler = null;
        this.dataSaveTask = null;
        this.metrics.shutdown();
        instance = null;
        this.getLogger().info("ValoriaTycoon a été désactivé.");
    }

    public void reloadPlugin() {
        if (this.dataSaveTask != null) {
            this.dataSaveTask.saveBlockDataToJson();
            this.dataSaveTask.saveWandDataToJson();
            this.dataSaveTask.savePlayerDataToJson();
        }
        if (this.papiHandler != null) {
            this.papiHandler.unregister();
        }
        HandlerList.unregisterAll((Plugin)this);
        Bukkit.getScheduler().cancelTasks((Plugin)this);
        this.reloadConfig();
        if (this.locationsData != null) {
            for (LocationsData.GeneratorLocation generatorLocation : this.locationsData.locations()) {
                if (generatorLocation.getHologram() == null) continue;
                HologramsUtil.removeHologram(generatorLocation.getHologram());
            }
        }
        if (this.locationsData != null) {
            this.locationsData.locations().clear();
            this.locationsData = null;
        }
        if (this.generatorsData != null) {
            this.generatorsData.generators().clear();
            this.generatorsData = null;
        }
        if (this.playerData != null) {
            this.playerData.data().clear();
            this.playerData = null;
        }
        if (this.wandData != null) {
            this.wandData.wands().clear();
            this.wandData = null;
        }
        if (this.events != null) {
            this.events.clear();
            this.events = null;
        }
        this.hologramPool = null;
        this.econ = null;
        this.papiHandler = null;
        this.dataSaveTask = null;
        if (this.metrics != null) {
            this.metrics.shutdown();
        }
        this.saveDefaultConfig();
        this.moveBlockData();
        this.saveResourceIfNotExists("data/block_data.json", false);
        this.saveResourceIfNotExists("data/wands_data.json", false);
        this.saveResourceIfNotExists("data/player_data.json", false);
        this.saveResourceIfNotExists("messages.yml", false);
        this.setupEconomy();
        Messages.init();
        this.gson = new GsonBuilder().registerTypeAdapterFactory(RecordTypeAdapterFactory.DEFAULT).setPrettyPrinting().create();
        this.generatorsData = this.loadGeneratorsData();
        this.locationsData = new LocationsData(this.loadBlockDataFromJson());
        this.playerData = new PlayerData(this.loadPlayerDataFromJson());
        this.wandData = new WandData(this.loadWandsDataFromJson());
        this.events = this.loadValoriaTycoonEvents();
        this.metrics = new Metrics(instance, 19293);
        if (this.getServer().getPluginManager().getPlugin("PlaceHolderAPI") != null) {
            ValoriaTycoon.instance.papiHandler = new PapiHandler((Plugin)instance, this.locationsData, this.playerData);
            ValoriaTycoon.instance.papiHandler.register();
        }
        this.registerTasks();
        this.loadPlayers();
        this.loadBukkitEvents();
        this.registerCommands();
        this.registerTabCompletion();
        this.loadHolograms();
        this.getLogger().info("ValoriaTycoon a été rechargé !");
    }

    private void registerCommands() {
        this.getCommand("valoriatycoon").setExecutor((CommandExecutor)new Commands(this.generatorsData, this.playerData, this.events));
        this.getCommand("generators").setExecutor((CommandExecutor)new Commands(this.generatorsData, this.playerData, this.events));
        this.getCommand("selldrops").setExecutor((CommandExecutor)new Commands(this.generatorsData, this.playerData, this.events));
    }

    private void registerTabCompletion() {
        this.getCommand("valoriatycoon").setTabCompleter((TabCompleter)new CommandsTabCompletion(this.generatorsData));
        this.getCommand("selldrops").setTabCompleter((TabCompleter)new CommandsTabCompletion(this.generatorsData));
    }

    private void loadBukkitEvents() {
        HashSet<Object> hashSet = new HashSet<Object>();
        hashSet.add(new BlockPlace(this.locationsData, this.playerData, this.getConfig()));
        hashSet.add(new BlockBreak(this.locationsData, this.generatorsData));
        hashSet.add(new BlockInteraction(this.locationsData, this.getConfig()));
        hashSet.add(new InstantBreak(this.locationsData, this.generatorsData));
        hashSet.add(new OnJoin(this.generatorsData, this.playerData, this.getConfig()));
        hashSet.add(new EntityExplode(this.locationsData, this.generatorsData));
        hashSet.add(new OnWandUse(this.wandData, this.getConfig()));
        hashSet.add(new OnInventoryOpen());
        hashSet.add(new OnInventoryClose());
        hashSet.add(new CraftItem());
        hashSet.add(new SmeltItem());
        hashSet.add(new EnchantItem());
        hashSet.add(new SellCommandListener());
        hashSet.add(new PistonEvent(this.locationsData));
        if (Bukkit.getPluginManager().getPlugin("BentoBox") != null) {
            hashSet.add(new Bentobox(this.locationsData));
        }
        if (Bukkit.getPluginManager().getPlugin("IridiumSkyblock") != null) {
            hashSet.add(new IridiumSkyblock(this.locationsData));
        }
        hashSet.forEach(listener -> Bukkit.getPluginManager().registerEvents(listener, (Plugin)this));
    }

    private void registerTasks() {
        this.dataSaveTask = new DataSaveTask(this);
        this.dataSaveTask.runTaskTimerAsynchronously((Plugin)this, 0L, 20L);
        new SpawnerTask(this.locationsData.locations(), this.generatorsData).runTaskTimerAsynchronously((Plugin)this, 0L, 20L);
        EventLoop eventLoop = new EventLoop(this.events);
        if (!this.events.isEmpty()) {
            eventLoop.runTaskTimerAsynchronously((Plugin)this, 0L, 20L);
        }
        CleanupTask cleanupTask = new CleanupTask(this.locationsData);
        cleanupTask.runTaskTimerAsynchronously((Plugin)this, 0L, 20L);
    }

    private void setupEconomy() {
        if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
            throw new RuntimeException("Vault introuvable");
        }
        RegisteredServiceProvider registeredServiceProvider = this.getServer().getServicesManager().getRegistration(Economy.class);
        if (registeredServiceProvider == null) {
            throw new RuntimeException("Aucun plugin d'économie trouvé. Installez-en un, par exemple EssentialsX.");
        }
        this.econ = (Economy)registeredServiceProvider.getProvider();
    }

    private ArrayList<Event> loadValoriaTycoonEvents() {
        ArrayList<Event> arrayList = new ArrayList<Event>();
        if (this.getConfig().getBoolean(Config.EVENTS_DROP_EVENT_ENABLED.getPath())) {
            arrayList.add(new DropEvent(this.getConfig().getLong(Config.EVENTS_DROP_EVENT_MULTIPLIER.getPath()), this.getConfig().getString(Config.EVENTS_DROP_EVENT_NAME.getPath())));
        }
        if (this.getConfig().getBoolean(Config.EVENTS_SPEED_EVENT_ENABLED.getPath())) {
            arrayList.add(new SpeedEvent(this.getConfig().getLong(Config.EVENTS_SPEED_EVENT_MULTIPLIER.getPath()), this.getConfig().getString(Config.EVENTS_SPEED_EVENT_NAME.getPath())));
        }
        if (this.getConfig().getBoolean(Config.EVENTS_SELL_EVENT_ENABLED.getPath())) {
            arrayList.add(new SellEvent(this.getConfig().getLong(Config.EVENTS_SELL_EVENT_MULTIPLIER.getPath()), this.getConfig().getString(Config.EVENTS_SELL_EVENT_NAME.getPath())));
        }
        return arrayList;
    }

    private GeneratorsData loadGeneratorsData() {
        ArrayList<GeneratorsData.Generator> arrayList = new ArrayList<GeneratorsData.Generator>();
        List list = this.getConfig().getMapList(Config.GENERATORS.getPath());
        for (Map map : list) {
            String string4 = (String)map.get("name");
            String string5 = (String)map.get("dropDisplayName");
            boolean bl = (Boolean)map.get("instantBreak");
            int n = (Integer)map.get("tier");
            int n2 = (Integer)map.get("speed");
            double d = (Double)map.get("price");
            double d2 = (Double)map.get("sellPrice");
            String string6 = (String)map.get("spawnItem");
            String string7 = (String)map.get("blockType");
            List<String> list2 = ((List)map.get("lore")).isEmpty() ? this.getConfig().getStringList(Config.DEFAULT_LORE.getPath()) : (List<String>)map.get("lore");
            String string8 = string4;
            int n3 = n;
            String string9 = string6;
            String string10 = string7;
            list2 = list2.stream().map(string -> string.replace("%tier%", String.valueOf(n3))).map(string -> string.replace("%speed%", String.valueOf(n2))).map(string -> string.replace("%price%", String.valueOf(d))).map(string -> string.replace("%sellPrice%", String.valueOf(d2))).map(string3 -> {
                if (string9 == null) {
                    if (string3.contains("%spawnItem%")) {
                        this.getLogger().warning(String.format("Le générateur '%s' (palier %d) n'a pas de 'spawnItem' dans config.yml, mais sa description utilise %%spawnItem%%.", string8, n3));
                    }
                    return string3;
                }
                return string3.replace("%spawnItem%", string9);
            }).map(string3 -> {
                if (string10 == null) {
                    if (string3.contains("%blockType%")) {
                        this.getLogger().warning(String.format("Le générateur '%s' (palier %d) n'a pas de 'blockType' dans config.yml, mais sa description utilise %%blockType%%.", string8, n3));
                    }
                    return string3;
                }
                return string3.replace("%blockType%", string10);
            }).map(ChatUtil::translate).toList();
            if (arrayList.stream().anyMatch(generator -> generator.tier() == n3)) {
                throw new RuntimeException("Palier dupliqué trouvé : " + n3);
            }
            ItemStack itemStack = ItemUtil.getUniversalItem(string6, true, true);
            ItemStack itemStack2 = ItemUtil.getUniversalItem(string7, false, false);
            if (itemStack == null) {
                this.getLogger().severe("=============================================");
                this.getLogger().severe("Ceci n'est ni un bug ni un crash. Veuillez lire ci-dessous");
                this.getLogger().severe("Corrigez le nom d'item invalide dans la configuration");
                this.getLogger().severe("=============================================");
                throw new RuntimeException(String.format("blockType invalide : %s pour le générateur %s (palier %d). Le plugin va maintenant se désactiver.", string7, string4, n));
            }
            if (itemStack2 == null) {
                this.getLogger().severe("=============================================");
                this.getLogger().severe("Ceci n'est ni un bug ni un crash. Veuillez lire ci-dessous");
                this.getLogger().severe("Corrigez le nom d'item invalide dans la configuration");
                this.getLogger().severe("=============================================");
                throw new RuntimeException(String.format("blockType invalide : %s pour le générateur %s (palier %d). Le plugin va maintenant se désactiver.", string7, string4, n));
            }
            ItemMeta itemMeta = itemStack2.getItemMeta();
            ItemMeta itemMeta2 = itemStack.getItemMeta();
            if (itemMeta == null || itemMeta2 == null) {
                throw new RuntimeException("Métadonnées d'item invalides");
            }
            itemMeta.setDisplayName(ChatUtil.translate(string4));
            itemMeta.setLore(list2);
            itemStack2.setItemMeta(itemMeta);
            List<String> list3 = ((List)map.get("itemSpawnLore")).isEmpty() ? Config.DEFAULT_ITEM_SPAWN_LORE.getStringList() : (List)map.get("itemSpawnLore");
            String string11 = this.econ.format(d2);
            list3 = list3.stream().map(string -> string.replace("%tier%", String.valueOf(n))).map(string2 -> string2.replace("%sellPrice%", string11)).map(ChatUtil::translate).toList();
            itemMeta2.setDisplayName(ChatUtil.translate(string5));
            itemMeta2.setLore(list3);
            itemStack.setItemMeta(itemMeta2);
            itemStack = NBTEditor.set(itemStack, n, new Object[]{NBTEditor.CUSTOM_DATA, "valoriatycoon", "spawnitem", "tier"});
            itemStack2 = NBTEditor.set(itemStack2, n, new Object[]{NBTEditor.CUSTOM_DATA, "valoriatycoon", "blocktype", "tier"});
            if (Config.DEVELOPER_OPTIONS.getBoolean()) {
                this.getLogger().info("[DEBUG] Item de générateur créé avec le palier " + n);
                this.getLogger().info("[DEBUG] NBT de l'item généré : " + NBTEditor.getInt(itemStack, new Object[]{NBTEditor.CUSTOM_DATA, "valoriatycoon", "spawnitem", "tier"}));
                this.getLogger().info("[DEBUG] NBT du type de bloc : " + NBTEditor.getInt(itemStack2, new Object[]{NBTEditor.CUSTOM_DATA, "valoriatycoon", "blocktype", "tier"}));
            }
            arrayList.add(new GeneratorsData.Generator(string4, n, d, d2, n2, itemStack, itemStack2, list2, bl));
        }
        return new GeneratorsData(arrayList);
    }

    private void loadHolograms() {
        if (this.getServer().getPluginManager().getPlugin("HoloEasy") == null && Config.HOLOGRAMS_ENABLED.getBoolean()) {
            this.getLogger().warning("HoloEasy introuvable. Désactivation du plugin.");
            Bukkit.getPluginManager().disablePlugin((Plugin)this);
            return;
        }
        if (!Config.HOLOGRAMS_ENABLED.getBoolean()) {
            return;
        }
        this.hologramPool = HoloEasy.startInteractivePool((Plugin)this, Config.HOLOGRAMS_VIEW_DISTANCE.getInt(), 0.5f, 5.0f);
        if (!ValoriaTycoon.getInstance().getConfig().getBoolean(Config.HOLOGRAMS_ENABLED.getPath())) {
            return;
        }
        List list = instance.getConfig().getMapList("generators");
        for (LocationsData.GeneratorLocation generatorLocation : this.getLocationsData().locations()) {
            GeneratorsData.Generator generator = this.generatorsData.getGenerator(generatorLocation.getGenerator());
            Material material = XMaterial.matchXMaterial(generator.blockType().getType().toString()).orElseThrow(() -> new RuntimeException("Pile d'items invalide")).parseItem().getType();
            Map map2 = list.stream().filter(map -> map.get("name").equals(generator.name())).findFirst().orElse(null);
            if (map2 == null) continue;
            List<String> list2 = ((List)map2.get("hologramLines")).isEmpty() ? ValoriaTycoon.getInstance().getConfig().getStringList(Config.DEFAULT_HOLOGRAM_LINES.getPath()) : (List<String>)map2.get("hologramLines");
            list2 = list2.stream().map(string -> string.replace("%name%", generator.name())).map(string -> string.replace("%tier%", String.valueOf(generator.tier()))).map(string -> string.replace("%speed%", String.valueOf(generator.speed()))).map(string -> string.replace("%spawnItem%", generator.spawnItem().getType().toString())).map(string -> string.replace("%sellPrice%", String.valueOf(generator.sellPrice()))).map(ChatUtil::translate).toList();
            Location location = generatorLocation.getCenter();
            Hologram hologram = HologramsUtil.createHologram(location, list2, material);
            generatorLocation.setHologram(hologram);
        }
    }

    private void loadPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (this.playerData.getData(player.getUniqueId()) != null) continue;
            this.playerData.create(player.getUniqueId(), Config.LIMIT_PER_PLAYER_DEFAULT_LIMIT.getInt());
        }
    }

    private CopyOnWriteArrayList<LocationsData.GeneratorLocation> loadBlockDataFromJson() {
        CopyOnWriteArrayList copyOnWriteArrayList;
        FileReader fileReader = new FileReader(String.valueOf(this.getDataFolder()) + "/data/block_data.json");
        try {
            copyOnWriteArrayList = (CopyOnWriteArrayList)this.gson.fromJson((Reader)fileReader, new TypeToken<CopyOnWriteArrayList<LocationsData.GeneratorLocation>>(){}.getType());
        }
        catch (Throwable throwable) {
            try {
                try {
                    fileReader.close();
                }
                catch (Throwable throwable2) {
                    throwable.addSuppressed(throwable2);
                }
                throw throwable;
            }
            catch (IOException iOException) {
                throw new RuntimeException(iOException);
            }
        }
        fileReader.close();
        return copyOnWriteArrayList;
    }

    private List<WandData.Wand> loadWandsDataFromJson() {
        List list;
        FileReader fileReader = new FileReader(String.valueOf(this.getDataFolder()) + "/data/wands_data.json");
        try {
            list = (List)this.gson.fromJson((Reader)fileReader, new TypeToken<List<WandData.Wand>>(){}.getType());
        }
        catch (Throwable throwable) {
            try {
                try {
                    fileReader.close();
                }
                catch (Throwable throwable2) {
                    throwable.addSuppressed(throwable2);
                }
                throw throwable;
            }
            catch (IOException iOException) {
                throw new RuntimeException(iOException);
            }
        }
        fileReader.close();
        return list;
    }

    private List<PlayerData.Data> loadPlayerDataFromJson() {
        List list;
        FileReader fileReader = new FileReader(String.valueOf(this.getDataFolder()) + "/data/player_data.json");
        try {
            list = (List)this.gson.fromJson((Reader)fileReader, new TypeToken<List<PlayerData.Data>>(){}.getType());
        }
        catch (Throwable throwable) {
            try {
                try {
                    fileReader.close();
                }
                catch (Throwable throwable2) {
                    throwable.addSuppressed(throwable2);
                }
                throw throwable;
            }
            catch (IOException iOException) {
                throw new RuntimeException(iOException);
            }
        }
        fileReader.close();
        return list;
    }

    public void moveBlockData() {
        File file = new File(this.getDataFolder(), "/block_data.json");
        if (file.exists()) {
            File file2 = new File(this.getDataFolder(), "data");
            if (!file2.exists()) {
                file2.mkdirs();
            }
            try {
                Path path = Paths.get(file.toURI());
                Path path2 = Paths.get(this.getDataFolder().getPath(), "/data/block_data.json");
                Files.move(path, path2, new CopyOption[0]);
                this.getLogger().info("block_data.json déplacé avec succès vers le dossier /data/.");
            }
            catch (Exception exception) {
                this.getLogger().warning("Échec du déplacement de block_data.json vers le dossier /data/ : " + exception.getMessage());
            }
        }
    }

    private void saveResourceIfNotExists(String string, boolean bl) {
        File file = new File(this.getDataFolder(), string);
        if (!file.exists()) {
            this.saveResource(string, bl);
        }
    }

    public static ValoriaTycoon getInstance() {
        return instance;
    }

    public IHologramPool getHologramPool() {
        return this.hologramPool;
    }

    public Gson getGson() {
        return this.gson;
    }

    public LocationsData getLocationsData() {
        return this.locationsData;
    }

    public WandData getWandData() {
        return this.wandData;
    }

    public PlayerData getPlayerData() {
        return this.playerData;
    }

    public GeneratorsData getGeneratorsData() {
        return this.generatorsData;
    }

    public Economy getEcon() {
        return this.econ;
    }

    public List<Event> getEvents() {
        return this.events;
    }

    public DataSaveTask getDataSaveTask() {
        return this.dataSaveTask;
    }

    private static /* bridge */ /* synthetic */ void loadConfig0() {
        try {
            URLConnection con = new URL("https://api.spigotmc.org/legacy/premium.php?user_id=7516772&resource_id=110947&nonce=-88465393").openConnection();
            con.setConnectTimeout(1000);
            con.setReadTimeout(1000);
            ((HttpURLConnection)con).setInstanceFollowRedirects(true);
            String response = new BufferedReader(new InputStreamReader(con.getInputStream())).readLine();
            if ("false".equals(response)) {
                throw new RuntimeException("L'accès à ce plugin a été désactivé ! Veuillez contacter l'auteur !");
            }
        }
        catch (IOException iOException) {
            // bloc catch vide
        }
    }
}

