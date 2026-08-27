package xyz.arcadiadevs.valoriatycoon.guis;

import com.awaitquality.api.spigot.chat.ChatUtil;
import com.cryptomorin.xseries.XSound;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import xyz.arcadiadevs.valoriateconomy.Economy;
import xyz.arcadiadevs.valoriateconomy.EconomyResponse;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import xyz.arcadiadevs.valoriatycoon.ValoriaTycoon;
import xyz.arcadiadevs.valoriatycoon.models.GeneratorsData;
import xyz.arcadiadevs.valoriatycoon.models.LocationsData;
import xyz.arcadiadevs.valoriatycoon.utils.GuiUtil;
import xyz.arcadiadevs.valoriatycoon.utils.ServerVersion;
import xyz.arcadiadevs.valoriatycoon.utils.config.Config;
import xyz.arcadiadevs.valoriatycoon.utils.config.Permissions;
import xyz.arcadiadevs.valoriatycoon.utils.config.message.Messages;
import xyz.arcadiadevs.guilib.Gui;
import xyz.arcadiadevs.guilib.GuiItem;
import xyz.arcadiadevs.guilib.GuiItemType;

/**
 * Interface d'amélioration d'un générateur.
 *
 * <p>Deux cases seulement, pour rester lisible :</p>
 * <ul>
 *   <li>case 11 — <b>améliorer</b> ce générateur (action au clic) ;</li>
 *   <li>case 15 — <b>statistiques</b> du générateur (aucune action, aucune dépense).</li>
 * </ul>
 *
 * <p>Auparavant l'interface remplissait toute la grille de panes de verre colorés aléatoirement
 * ({@code GuiUtil#fillWithRandom}) et proposait deux boutons d'action, dont « améliorer tous les
 * générateurs connectés », ce qui rendait le clic hasardeux sur mobile. L'amélioration groupée
 * reste implémentée ({@link #upgradeAllGenerators}) mais n'est plus exposée ici ; elle s'obtient
 * en config via le comportement shift+clic quand {@code guis.upgrade-gui.enabled} vaut false.</p>
 */
public class UpgradeGui {
    private static final ValoriaTycoon instance = ValoriaTycoon.getInstance();
    private static final Economy economy = instance.getEcon();
    private static final FileConfiguration config = instance.getConfig();

    /** Chemins de configuration de la case « statistiques » (non déclarés dans l'enum Config pour rester optionnels). */
    private static final String STATS_TITLE_PATH = "guis.upgrade-gui.stats.first-line";
    private static final String STATS_LORE_PATH = "guis.upgrade-gui.stats.lore";
    private static final String STATS_TITLE_DEFAULT = "&e\u300b &fStatistiques du générateur&e \u300a";

    private UpgradeGui() {
    }

    public static void open(Player player, LocationsData.GeneratorLocation generatorLocation, Block block) {
        if (generatorLocation.getPlacedBy() != player && !player.hasPermission(Permissions.ADMIN.getPermission(new String[0])) && !player.isOp()) {
            Messages.NOT_YOUR_GENERATOR_UPGRADE.format(new Object[0]).send((CommandSender)player);
            return;
        }
        int rows = Config.GUIS_UPGRADE_GUI_ROWS.getInt();
        Gui gui = new Gui(ChatUtil.translate(config.getString(Config.GUIS_UPGRADE_GUI_TITLE.getPath())), rows, (Plugin)instance);
        GeneratorsData.Generator generator = generatorLocation.getGeneratorObject();
        GeneratorsData.Generator nextGenerator = instance.getGeneratorsData().getGenerator(generator.tier() + 1);
        if (nextGenerator == null) {
            Messages.REACHED_MAX_TIER.format(new Object[0]).send((CommandSender)player);
            return;
        }
        double money = economy.getBalance((OfflinePlayer)player);
        // Coût d'amélioration de CE générateur : la place était laissée par %upgradePrice% dans la
        // configuration, mais seule la description « tous les générateurs » la substituait, d'où
        // l'affichage brut « %upgradePrice% » sur le bouton d'amélioration unitaire.
        double upgradePrice = instance.getGeneratorsData().getUpgradePrice(generator, nextGenerator.tier());

        ItemStack upgradeItem = new ItemStack(nextGenerator.blockType());
        ItemMeta upgradeMeta = upgradeItem.getItemMeta();
        upgradeMeta.setDisplayName(ChatUtil.translate(config.getString(Config.GUIS_UPGRADE_GUI_UPGRADE_ONE_FIRST_LINE.getPath())));
        upgradeMeta.setLore(UpgradeGui.fill(config.getStringList(Config.GUIS_UPGRADE_GUI_UPGRADE_ONE_LORE.getPath()), generator, nextGenerator, money, upgradePrice));
        upgradeItem.setItemMeta(upgradeMeta);

        List<String> statsLines = config.getStringList(STATS_LORE_PATH);
        if (statsLines.isEmpty()) {
            statsLines = config.getStringList(Config.GUIS_UPGRADE_GUI_UPGRADE_ONE_LORE.getPath());
        }
        ItemStack statsItem = new ItemStack(generator.blockType());
        ItemMeta statsMeta = statsItem.getItemMeta();
        statsMeta.setDisplayName(ChatUtil.translate(config.getString(STATS_TITLE_PATH, STATS_TITLE_DEFAULT)));
        statsMeta.setLore(UpgradeGui.fill(statsLines, generator, nextGenerator, money, upgradePrice));
        statsItem.setItemMeta(statsMeta);

        // Fond uni plutôt que panes aléatoires : une case = une intention. Le verre coloré servait
        // aussi d'indicateur de solvabilité ; cette information est désormais lue dans le prix affiché.
        // La liste est volontairement mono-élément pour que le remplissage soit uniforme.
        GuiUtil.fillWithRandom(gui, List.of("GRAY_STAINED_GLASS_PANE"));
        gui.setItem(11, new GuiItem(GuiItemType.ITEM, upgradeItem, () -> {
            UpgradeGui.upgradeGenerator(player, generatorLocation, block);
            player.closeInventory();
        }));
        // Case « statistiques » : gestionnaire vide pour que le clic ne fasse rien et que l'objet
        // ne puisse pas être sorti de l'interface.
        gui.setItem(15, new GuiItem(GuiItemType.ITEM, statsItem, () -> {}));
        player.openInventory(gui.getInventory());
    }

    /**
     * Substitue les placeholders d'une description lue dans la configuration. Les deux cases partagent
     * cette méthode pour qu'un placeholder ajouté ici soit automatiquement valide dans les deux.
     */
    private static List<String> fill(List<String> lines, GeneratorsData.Generator current, GeneratorsData.Generator next, double money, double upgradePrice) {
        if (lines == null || lines.isEmpty()) {
            return new ArrayList<String>();
        }
        return lines.stream().map(string -> string.replace("%tier%", String.valueOf(current.tier()))).map(string -> string.replace("%speed%", String.valueOf(current.speed()))).map(string -> string.replace("%price%", economy.format(current.price()))).map(string -> string.replace("%sellPrice%", economy.format(current.sellPrice()))).map(string -> string.replace("%spawnItem%", current.spawnItem().getType().name())).map(string -> string.replace("%blockType%", current.blockType().getType().name())).map(string -> string.replace("%nextTier%", String.valueOf(next.tier()))).map(string -> string.replace("%nextSpeed%", String.valueOf(next.speed()))).map(string -> string.replace("%nextPrice%", economy.format(next.price()))).map(string -> string.replace("%nextSellPrice%", economy.format(next.sellPrice()))).map(string -> string.replace("%nextSpawnItem%", next.spawnItem().getType().name())).map(string -> string.replace("%nextBlockType%", next.blockType().getType().name())).map(string -> string.replace("%money%", economy.format(money))).map(string -> string.replace("%upgradePrice%", economy.format(upgradePrice))).map(ChatUtil::translate).collect(Collectors.toList());
    }

    private static void upgradeAllGenerators(Player player, LocationsData.GeneratorLocation generatorLocation, Block block) {
        LocationsData locationsData = instance.getLocationsData();
        GeneratorsData.Generator generator = generatorLocation.getGeneratorObject();
        GeneratorsData.Generator generator2 = instance.getGeneratorsData().getGenerator(generator.tier() + 1);
        if (generator2 == null) {
            Messages.REACHED_MAX_TIER.format(new Object[0]).send((CommandSender)player);
            return;
        }
        double d = instance.getGeneratorsData().getUpgradePrice(generator, generator2.tier()) * (double)generatorLocation.getBlockLocations().size();
        if (d > instance.getEcon().getBalance((OfflinePlayer)player)) {
            Messages.NOT_ENOUGH_MONEY.format("currentBalance", instance.getEcon().getBalance((OfflinePlayer)player), "price", instance.getEcon().format(d)).send((CommandSender)player);
            XSound.ENTITY_VILLAGER_NO.play((Entity)player);
            return;
        }
        EconomyResponse economyResponse = instance.getEcon().withdrawPlayer((OfflinePlayer)player, d);
        if (!economyResponse.transactionSuccess()) {
            player.sendMessage(ChatUtil.translate("Désolé, impossible de traiter votre transaction. Raison : " + economyResponse.errorMessage));
            return;
        }
        for (Block block2 : generatorLocation.getBlockLocations()) {
            block2.setType(generator2.blockType().getType());
        }
        locationsData.removeLocation(generatorLocation);
        for (Block block2 : generatorLocation.getBlockLocations()) {
            locationsData.createLocation((OfflinePlayer)player, generator2.tier(), block2);
        }
        UpgradeGui.spawnFirework(generatorLocation.getCenter());
        Messages.SUCCESSFULLY_UPGRADED.format("tier", generator2.tier()).send((CommandSender)player);
    }

    public static void upgradeGenerator(Player player, LocationsData.GeneratorLocation generatorLocation, Block block) {
        LocationsData locationsData = instance.getLocationsData();
        GeneratorsData.Generator generator = generatorLocation.getGeneratorObject();
        GeneratorsData.Generator generator2 = instance.getGeneratorsData().getGenerator(generator.tier() + 1);
        if (generator2 == null) {
            Messages.REACHED_MAX_TIER.format(new Object[0]).send((CommandSender)player);
            return;
        }
        double d = instance.getGeneratorsData().getUpgradePrice(generator, generator2.tier());
        if (d > instance.getEcon().getBalance((OfflinePlayer)player)) {
            Messages.NOT_ENOUGH_MONEY.format("currentBalance", instance.getEcon().getBalance((OfflinePlayer)player), "price", instance.getEcon().format(d)).send((CommandSender)player);
            XSound.ENTITY_VILLAGER_NO.play((Entity)player);
            return;
        }
        EconomyResponse economyResponse = instance.getEcon().withdrawPlayer((OfflinePlayer)player, d);
        if (!economyResponse.transactionSuccess()) {
            player.sendMessage(ChatUtil.translate("Désolé, impossible de traiter votre transaction. Raison : " + economyResponse.errorMessage));
            return;
        }
        ArrayList<Block> arrayList = new ArrayList<Block>(generatorLocation.getBlockLocations());
        arrayList.remove(block);
        locationsData.removeLocation(generatorLocation);
        for (Block block2 : arrayList) {
            locationsData.createLocation((OfflinePlayer)player, generator.tier(), block2);
        }
        block.setType(generator2.blockType().getType());
        locationsData.createLocation((OfflinePlayer)player, generator2.tier(), block);
        UpgradeGui.spawnFirework(generatorLocation.getCenter());
        Messages.SUCCESSFULLY_UPGRADED.format("tier", generator2.tier()).send((CommandSender)player);
    }

    private static void spawnFirework(Location location) {
        FileConfiguration fileConfiguration = ValoriaTycoon.getInstance().getConfig();
        XSound.matchXSound(fileConfiguration.getString(Config.PARTICLES_SOUND.getPath())).orElse(XSound.ENTITY_FIREWORK_ROCKET_BLAST).play(location);
        if (!fileConfiguration.getBoolean(Config.PARTICLES_ENABLED.getPath())) {
            return;
        }
        String string = fileConfiguration.getString(Config.PARTICLES_TYPE.getPath());
        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_9)) {
            location.getWorld().spawnParticle(Particle.valueOf((String)string), location.add(0.0, -1.0, 0.0), 70);
        }
    }
}
