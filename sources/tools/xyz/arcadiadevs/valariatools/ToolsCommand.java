package xyz.arcadiadevs.valariatools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * La commande unique du plugin, <code>/valariatools</code> (alias <code>/tools</code>, <code>/outil</code>).
 *
 * <h2>Qui a droit à quoi</h2>
 * <p>Un joueur non OP peut ouvrir son interface et se donner un outil (le plugin est prévu pour un
 * serveur où l'outil s'achète : le <em>give</em> n'est pas un privilège s'il est facturé ailleurs).
 * Il ne peut <b>pas</b> cibler un autre joueur, ni fixer un palier : les deux rendraient l'économie
 * sans objet. Ces deux vérifications sont faites sur le nom de la commande, pas seulement dans
 * <code>plugin.yml</code>, parce qu'un alias non déclaré dans le YAML passe outre la permission.</p>
 *
 * <h2>Le tab‑completion ne ment pas</h2>
 * <p>Les suggestions sont filtrées par permission : un joueur qui tape <code>/tools r…</code> ne doit
 * pas voir <code>reload</code> s'il ne peut pas l'exécuter — c'est la moitié des « mais ça marche pas »
 * qui finissent en ticket.</p>
 */
public final class ToolsCommand implements CommandExecutor, TabCompleter {

    private final ValoriaTools plugin;

    public ToolsCommand(ValoriaTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // La permission du plugin est verifiee ICI et pas seulement dans plugin.yml : un alias
        // (`/tools`) ne herite d'aucune permission Bukkit, seul le code la rend effective.
        if (!sender.hasPermission("valoria.tools.use")) {
            sender.sendMessage(color("&cPermission manquante (&fvaloria.tools.use&c)."));
            return true;
        }
        String sub = args.length == 0 ? "gui" : args[0].toLowerCase(Locale.ROOT);
        if (args.length == 0 && sender instanceof Player) {
            open((Player) sender);
            return true;
        }
        switch (sub) {
            case "gui":
            case "menu":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(color("&cOuvre le jeu pour voir l'interface, la console ne peut pas."));
                    return true;
                }
                if (args.length > 1) {
                    ToolKind asked = ToolKind.parse(args[1]);
                    if (asked == null) {
                        sender.sendMessage(color("&cÂme inconnue : &f" + args[1]
                                + "&c. Choix : pioche, hache, canne, epee."));
                        return true;
                    }
                    ToolsGui.open((Player) sender, asked);
                    return true;
                }
                open((Player) sender);
                return true;
            case "buy":
            case "acheter":
                return buy(sender, args);
            case "top":
            case "classement":
                return top(sender, args);
            case "aide":
            case "fiche":
                return sheet(sender, args);
            case "give":
                return give(sender, args);
            case "sell":
                return sell(sender, args);
            case "tier":
            case "set":
                return setTier(sender, args);
            case "ability":
            case "capacite":
                return ability(sender, args);
            case "reset":
                return reset(sender, args);
            case "stats":
                return stats(sender);
            case "reload":
                if (!sender.hasPermission("valoria.tools.admin")) {
                    sender.sendMessage(color("&cPermission manquante (&fvaloria.tools.admin&c)."));
                    return true;
                }
                this.plugin.reload();
                sender.sendMessage(color("&aConfiguration de ValoriaTools rechargée &7("
                        + this.plugin.describeKinds() + ")&a."));
                return true;
            case "help":
            default:
                help(sender);
                return true;
        }
    }

    // ------------------------------------------------------------------ sous-commandes

    private void open(Player player) {
        if (!this.plugin.active()) {
            player.sendMessage(color("&eValoriaTools est désactivé dans la configuration."));
            return;
        }
        if (!this.plugin.guiLive()) {
            player.sendMessage(color("&cL'interface n'est pas active (événements non enregistrés) :"
                    + " regarde le log du serveur."));
            return;
        }
        ToolsGui.open(player);
    }

    /**
     * <code>/tools buy [joueur]</code> : l'outil s'achète. Le prix vient de <code>tool.price</code>, et un
     * prix à zéro rend l'achat gratuit — un serveur qui n'a pas encore décidé son tarif ne doit pas être
     * bloqué par notre propre économie. C'est cette commande qui est ouverte aux joueurs ;
     * <code>/tools give</code> reste l'outil de l'administration (palier imposé, autres joueurs).
     */
    private boolean buy(CommandSender sender, String[] args) {
        Player target;
        if (sender instanceof Player) {
            target = (Player) sender;
        } else if (args.length > 1) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(color("&cJoueur introuvable : &f" + args[1]));
                return true;
            }
        } else {
            sender.sendMessage(color("&cPrécise un joueur : /tools buy <joueur>."));
            return true;
        }
        double price = this.plugin.toolsConfig().toolPrice();
        if (price > 0.0D && this.plugin.economy().available()) {
            if (!this.plugin.economy().canAfford(target, price)) {
                target.sendMessage(color("&cLe multi-outil coûte &f" + this.plugin.economy().format(price)
                        + "&c : il te manque &f"
                        + this.plugin.economy().format(price - this.plugin.economy().balance(target)) + "&c."));
                return true;
            }
            EconomyService.Outcome taken = this.plugin.economy().withdraw(target, price);
            if (!taken.success()) {
                target.sendMessage(color("&cPaiement refusé : &f" + taken.reason()));
                return true;
            }
        }
        ItemStack tool = MultiTool.create(this.plugin.toolsConfig(), this.plugin.store(), target.getUniqueId());
        Map<Integer, ItemStack> overflow = target.getInventory().addItem(tool);
        for (ItemStack left : overflow.values()) {
            target.getWorld().dropItemNaturally(target.getLocation(), left);
        }
        target.sendMessage(color("&a" + (price > 0.0D
                ? "Multi-outil acheté (-" + this.plugin.economy().format(price) + ")."
                : "Multi-outil reçu (gratuit : tool.price = 0).")
                + " &7Il change d'âme selon le bloc que tu regardes, &f/tools&7 ouvre le menu."));
        if (!sender.equals(target)) {
            sender.sendMessage(color("&a" + target.getName() + " a reçu son multi-outil."));
        }
        playLevelUp(target);
        return true;
    }

    /** <code>/tools top [mesure] [âme] [nombre]</code> : le classement, mesuré par l'outil lui-même. */
    private boolean top(CommandSender sender, String[] args) {
        ToolStats.Metric metric = ToolStats.Metric.BLOCKS;
        ToolKind kind = null;
        int limit = 10;
        for (int i = 1; i < args.length; i++) {
            ToolStats.Metric asked = ToolStats.Metric.parse(args[i]);
            if (asked != null) {
                metric = asked;
                continue;
            }
            ToolKind askedKind = ToolKind.parse(args[i]);
            if (askedKind != null) {
                kind = askedKind;
                continue;
            }
            try {
                limit = Math.max(1, Math.min(50, Integer.parseInt(args[i])));
            } catch (NumberFormatException notANumber) {
                sender.sendMessage(color("&7Ignoré : &f" + args[i] + "&7 (ni mesure, ni âme, ni nombre)."));
            }
        }
        List<ToolStats.Entry> entries = this.plugin.stats().top(metric, kind, limit);
        sender.sendMessage(color("&6Classement &7— " + metric.label()
                + (kind == null ? " &7(toutes âmes)" : " &7(" + kind.label() + ")")));
        if (entries.isEmpty()) {
            sender.sendMessage(color("&7Rien de mesuré pour l'instant"
                    + (this.plugin.stats().enabled() ? "" : " — les stats sont désactivées (stats.enabled: false)")
                    + ". &8" + this.plugin.stats().measured() + " joueur(s) suivis."));
            return true;
        }
        int rank = 1;
        for (ToolStats.Entry entry : entries) {
            sender.sendMessage(color("&e#" + rank++ + " &f" + entry.name() + " &7— &a" + format(entry.value(), metric)));
        }
        return true;
    }

    /** <code>/tools aide &lt;capacité&gt;</code> : la fiche d'une capacité, telle que le wiki la décrit. */
    private boolean sheet(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage : /tools aide <capacité>  (ex. /tools aide fortune)"));
            sender.sendMessage(color("&7Les noms du wiki, les ids de config et les noyaux fonctionnent."));
            return true;
        }
        for (ToolKind kind : ToolKind.values()) {
            ToolsConfig.KindConfig kindConfig = this.plugin.toolsConfig().kind(kind);
            if (kindConfig == null) {
                continue;
            }
            for (ToolsConfig.Ability ability : this.plugin.toolsConfig().abilities(kindConfig)) {
                if (!matches(ability, args[1])) {
                    continue;
                }
                sender.sendMessage(color("&6" + ability.name() + " &7— " + kind.label() + " &8(" + ability.type() + ")"));
                if (!ability.description().isEmpty()) {
                    sender.sendMessage(color("&7" + ability.description()));
                }
                sender.sendMessage(color("&7Verrou : palier d'âme &f" + ability.unlock() + " &7· niveau max &f"
                        + ability.maxLevel() + (ability.free() ? " &7· niveau 1 offert" : "")));
                for (String key : ability.keys()) {
                    if (ability.numbers(key).isEmpty()) {
                        continue;
                    }
                    double first = ability.levelDecimal(key, 1, 0.0D);
                    double second = ability.levelDecimal(key, 2, 0.0D);
                    sender.sendMessage(color("&8  " + key.replace('-', ' ') + " : &7base &f" + cut(first)
                            + " &7· pas &f" + cut(second - first)
                            + " &7· au max &f" + cut(ability.levelDecimal(key, ability.maxLevel(), 0.0D))));
                }
                sender.sendMessage(color("&7Prix du premier niveau : &f" + cut(ability.priceAt(1))));
                return true;
            }
        }
        sender.sendMessage(color("&cCapacité inconnue : &f" + args[1] + "&c. Essaie un nom du wiki (`fortune`)"
                + " ou un noyau (`FORTUNE`)."));
        return true;
    }

    private static boolean matches(ToolsConfig.Ability ability, String needle) {
        String wanted = needle.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        String label = ability.name().toLowerCase(Locale.ROOT).replace(' ', '_');
        return ability.id().toLowerCase(Locale.ROOT).equals(wanted) || label.contains(wanted)
                || ability.type().toLowerCase(Locale.ROOT).equals(wanted);
    }

    private static String format(double value, ToolStats.Metric metric) {
        if (metric == ToolStats.Metric.MONEY && ValoriaTools.get() != null) {
            return ValoriaTools.get().economy().format(value);
        }
        return String.valueOf((long) value);
    }

    private static String cut(double value) {
        if (!Double.isFinite(value)) {
            return "0";
        }
        if (Math.abs(value - Math.rint(value)) < 0.0005D) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.3f", Double.valueOf(value));
    }

    private void playLevelUp(Player player) {
        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.4F);
        } catch (RuntimeException | LinkageError legacy) {
            // son decoratif
        }
    }

    private boolean give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("valoria.tools.give")) {
            sender.sendMessage(color("&cPermission manquante (&fvaloria.tools.give&c)."));
            return true;
        }
        Player target;
        int from = 1;
        if (args.length > 1 && !args[1].equalsIgnoreCase("all") && !isTier(args[1])) {
            target = Bukkit.getPlayerExact(args[1]);
            from = 2;
            if (target == null) {
                sender.sendMessage(color("&cJoueur introuvable : &f" + args[1]));
                return true;
            }
            if (sender instanceof Player && !sender.hasPermission("valoria.tools.admin")
                    && !sender.getName().equals(target.getName())) {
                sender.sendMessage(color("&cTu ne peux donner l'outil qu'à toi‑même."));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(color("&cPrécise un joueur : /tools give <joueur> [palier]."));
            return true;
        }
        int tier = 1;
        if (args.length > from && isTier(args[from])) {
            tier = Math.max(1, Integer.parseInt(args[from]));
        }
        if (tier > 1 && !sender.hasPermission("valoria.tools.admin")) {
            sender.sendMessage(color("&cRéserver un palier de départ à l'administration (&fvaloria.tools.admin&c)."));
            tier = 1;
        }
        if (this.plugin.economy().available() && tier > 1 && sender.hasPermission("valoria.tools.admin")) {
            sender.sendMessage(color("&8Note : le palier " + tier + " est offert (aucun débit n'est fait"
                    + " par /tools give)."));
        }
        ItemStack tool = MultiTool.create(this.plugin.toolsConfig(), this.plugin.store(), target.getUniqueId());
        target.getInventory().addItem(tool);
        target.updateInventory();
        target.sendMessage(color("&aTon multi-outil est prêt &7(âmes au palier &f" + tier + "&7)."));
        if (!sender.equals(target)) {
            sender.sendMessage(color("&aDonné à &f" + target.getName() + "&a."));
        }
        return true;
    }

    private boolean sell(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cLa vente se fait en jeu."));
            return true;
        }
        Player player = (Player) sender;
        boolean wholeStacks = args.length > 1 && args[1].equalsIgnoreCase("all");
        ToolsConfig config = this.plugin.toolsConfig();
        double total = 0.0D;
        int sold = 0;
        int kept = 0;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || stack.getType() == Material.AIR || MultiTool.isMultiTool(stack)) {
                continue;
            }
            ToolKind kind = this.plugin.matcher().kindOf(stack.getType());
            if (kind == null) {
                kept++;
                continue;
            }
            double price = this.plugin.sellPrice(kind, stack.getType());
            if (price <= 0.0D) {
                kept++;
                continue;
            }
            int amount = wholeStacks ? stack.getAmount() : 1;
            total += price * amount;
            sold += amount;
            if (wholeStacks || stack.getAmount() - amount <= 0) {
                player.getInventory().setItem(slot, null);
            } else {
                stack.setAmount(stack.getAmount() - amount);
                player.getInventory().setItem(slot, stack);
            }
        }
        if (sold == 0) {
            player.sendMessage(color("&7Rien à vendre" + (kept > 0
                    ? " : &f" + kept + " &7stack(s) non reconnu(s) ou sans prix." : ".")));
            return true;
        }
        double amount = ToolListener.round(total);
        EconomyService.Outcome credited = this.plugin.economy().deposit(player, amount);
        if (!credited.success()) {
            player.updateInventory();
            player.sendMessage(color("&cVente impossible : " + credited.reason()));
            return true;
        }
        player.sendMessage(color("&a+" + this.plugin.economy().format(amount) + "&7 (" + sold
                + " vendu(s), " + kept + " gardé(s))"));
        player.updateInventory();
        return true;
    }

    private boolean setTier(CommandSender sender, String[] args) {
        if (!sender.hasPermission("valoria.tools.admin")) {
            sender.sendMessage(color("&cPermission manquante (&fvaloria.tools.admin&c)."));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(color("&cUsage : /tools set <joueur> <pioche|hache|canne|epee> <palier>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(color("&cJoueur hors ligne : la grille de paliers se lit en jeu (le"
                    + " fichier tools.yml n'est pas une porte d'entrée)."));
            return true;
        }
        ToolKind kind = ToolKind.parse(args[2]);
        if (kind == null) {
            sender.sendMessage(color("&cÂme inconnue : &f" + args[2] + "&c. Choix : pioche, hache, canne, epee."));
            return true;
        }
        if (args.length < 4 || !isTier(args[3])) {
            ToolsConfig.KindConfig kindConfig = this.plugin.toolsConfig().kind(kind);
            int tier = this.plugin.store().tierOf(target.getUniqueId(), kind,
                    kindConfig == null ? 1 : this.plugin.toolsConfig().maxTier(kindConfig));
            target.sendMessage(color("&b" + MultiTool.capitalize(kind.label()) + "&7 : palier &f" + tier));
            sender.sendMessage(color("&b" + MultiTool.capitalize(kind.label()) + "&7 : palier &f" + tier));
            return true;
        }
        int tier = Integer.parseInt(args[3]);
        ToolsConfig.KindConfig kindConfig = this.plugin.toolsConfig().kind(kind);
        if (kindConfig == null) {
            sender.sendMessage(color("&cAucune configuration pour cette âme."));
            return true;
        }
        int max = this.plugin.toolsConfig().maxTier(kindConfig);
        int applied = Math.max(1, Math.min(tier, max));
        if (applied != tier) {
            sender.sendMessage(color("&8Palier ramené à " + applied + " (le max configuré est " + max + ")."));
        }
        this.plugin.store().setTier(target, kind, applied, max);
        ItemStack held = target.getInventory().getItemInMainHand();
        if (MultiTool.isMultiTool(held)) {
            MultiTool.refresh(held, this.plugin.toolsConfig(), this.plugin.store(), target.getUniqueId());
        }
        target.sendMessage(color("&a" + MultiTool.capitalize(kind.label()) + "&7 → palier &f" + applied
                + (sender.equals(target) ? "&7." : "&7 (décision de &f" + sender.getName() + "&7)")));
        if (!sender.equals(target)) {
            sender.sendMessage(color("&a" + target.getName() + " : " + kind.label() + " au palier " + applied));
        }
        this.plugin.saveSoon();
        return true;
    }

    /**
     * <code>/tools ability &lt;joueur&gt; &lt;âme&gt; &lt;capacité&gt; &lt;niveau&gt;</code> : pose le niveau d'une capacité.
     *
     * <p>Réservé à l'admin, et volontairement sans contrôle d'argent : c'est l'outil de réglage et de
     * test, pas un raccourci de jeu. Le niveau est borné par <code>max-level</code> de la config, parce
     * qu'un niveau hors barème afficherait dans le menu une capacité plus forte que ce que le admin a
     * déclaré.</p>
     */
    private boolean ability(CommandSender sender, String[] args) {
        if (!sender.hasPermission("valoria.tools.admin")) {
            sender.sendMessage(color("&cPermission manquante (&fvaloria.tools.admin&c)."));
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage(color("&cUsage : /tools ability <joueur> <ame> <capacite> [niveau]"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(color("&cJoueur hors ligne : les niveaux sont lus en jeu."));
            return true;
        }
        ToolKind kind = ToolKind.parse(args[2]);
        if (kind == null) {
            sender.sendMessage(color("&cÂme inconnue : &f" + args[2] + "&c. Choix : pioche, hache, canne, epee."));
            return true;
        }
        ToolsConfig.KindConfig kindConfig = this.plugin.toolsConfig().kind(kind);
        if (kindConfig == null) {
            sender.sendMessage(color("&cAucune configuration pour cette âme."));
            return true;
        }
        ToolsConfig.Ability found = findAbility(kindConfig, args[3]);
        if (found == null) {
            sender.sendMessage(color("&cCapacité inconnue pour &f" + kind.label() + "&c : &f" + args[3]));
            sender.sendMessage(color("&7Choix : &f" + String.join(", ", knownIds(kindConfig))));
            return true;
        }
        if (args.length < 5) {
            int tier = this.plugin.store().tierOf(target.getUniqueId(), kind,
                    this.plugin.toolsConfig().maxTier(kindConfig));
            sender.sendMessage(color("&b" + found.name() + "&7 : niveau &f"
                    + ToolsConfig.levelOf(found, this.plugin.store().levelsOf(target.getUniqueId(), kind), tier)
                    + "&7/&f" + found.maxLevel() + "&7 (palier requis &f" + found.unlock() + "&7)"));
            return true;
        }
        int level = number(args[4]);
        if (level < 0) {
            sender.sendMessage(color("&cNiveau invalide : &f" + args[4] + "&c (un entier, 0 pour retirer)."));
            return true;
        }
        int tier = this.plugin.store().tierOf(target.getUniqueId(), kind,
                this.plugin.toolsConfig().maxTier(kindConfig));
        this.plugin.store().setLevel(target, kind, found.id(), level, found.maxLevel());
        ItemStack held = target.getInventory().getItemInMainHand();
        if (MultiTool.isMultiTool(held)) {
            MultiTool.refresh(held, this.plugin.toolsConfig(), this.plugin.store(), target.getUniqueId());
        }
        if (tier < found.unlock()) {
            sender.sendMessage(color("&eAttention : le palier " + tier + " ne permet pas encore d'utiliser "
                    + found.name() + " (palier " + found.unlock() + " requis). Le niveau est stocké, pas perdu."));
        }
        sender.sendMessage(color("&a" + target.getName() + " &7: " + found.name() + " au niveau &f"
                + Math.min(level, found.maxLevel()) + "&7/&f" + found.maxLevel()));
        target.sendMessage(color("&a" + found.name() + "&7 : niveau &f" + Math.min(level, found.maxLevel())
                + "&7 (réglage de l'administration)."));
        this.plugin.saveSoon();
        return true;
    }

    /** <code>/tools reset &lt;joueur&gt; [âme]</code> : remet une âme à zéro, palier et capacités. */
    private boolean reset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("valoria.tools.admin")) {
            sender.sendMessage(color("&cPermission manquante (&fvaloria.tools.admin&c)."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage : /tools reset <joueur> [ame]"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(color("&cJoueur hors ligne."));
            return true;
        }
        List<ToolKind> kinds = new ArrayList<ToolKind>();
        if (args.length > 2) {
            ToolKind kind = ToolKind.parse(args[2]);
            if (kind == null) {
                sender.sendMessage(color("&cÂme inconnue : &f" + args[2]));
                return true;
            }
            kinds.add(kind);
        } else {
            Collections.addAll(kinds, ToolKind.values());
        }
        for (ToolKind kind : kinds) {
            this.plugin.store().reset(target, kind);
        }
        ItemStack held = target.getInventory().getItemInMainHand();
        if (MultiTool.isMultiTool(held)) {
            MultiTool.refresh(held, this.plugin.toolsConfig(), this.plugin.store(), target.getUniqueId());
        }
        sender.sendMessage(color("&a" + target.getName() + " &7: âmes remises au palier 1 &7("
                + kinds.size() + " âme(s), capacités effacées)."));
        target.sendMessage(color("&eToutes tes capacités d'outil ont été remises à zéro par l'administration."));
        this.plugin.saveSoon();
        return true;
    }

    /** Recherche par id, puis par nom de wiki, puis par noyau : l'admin ne doit pas retenir les ids. */
    private static ToolsConfig.Ability findAbility(ToolsConfig.KindConfig kindConfig, String needle) {
        String wanted = needle.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        ToolsConfig.Ability byLabel = null;
        ToolsConfig.Ability byKernel = null;
        for (ToolsConfig.Ability ability : kindConfig.abilities()) {
            if (ability.id().toLowerCase(Locale.ROOT).equals(wanted)) {
                return ability;
            }
            String label = ability.name().toLowerCase(Locale.ROOT).replace(' ', '_');
            if (byLabel == null && label.equals(wanted)) {
                byLabel = ability;
            }
            if (byKernel == null && ability.type().toLowerCase(Locale.ROOT).equals(wanted)) {
                byKernel = ability;
            }
        }
        return byLabel != null ? byLabel : byKernel;
    }

    private static List<String> knownIds(ToolsConfig.KindConfig kindConfig) {
        List<String> out = new ArrayList<String>();
        for (ToolsConfig.Ability ability : kindConfig.abilities()) {
            out.add(ability.id());
        }
        return out;
    }

    private boolean stats(CommandSender sender) {
        sender.sendMessage(color("&6ValoriaTools &7— état"));
        sender.sendMessage(color("&7  économie       : &f" + this.plugin.economy().providerName()
                + (this.plugin.economy().available() ? "" : " &8(aucune : améliorations gratuites)")));
        sender.sendMessage(color("&7  âmes           : &f" + this.plugin.describeKinds()));
        sender.sendMessage(color("&7  capacités vues : &f"
                + String.join(", ", Abilities.known())));
        sender.sendMessage(color("&7  interface      : &f" + (this.plugin.guiLive() ? "active" : "&chéchouée")));
        return true;
    }

    private void help(CommandSender sender) {
        sender.sendMessage(color("&6ValoriaTools &7— un seul outil, quatre âmes"));
        sender.sendMessage(color("&7  /tools &8— &louvre l'interface d'amélioration"));
        sender.sendMessage(color("&7  /tools give [joueur] [palier] &8— &7reçois l'outil (admin ou gratuit)"));
        sender.sendMessage(color("&7  /tools buy &8— &7achète l'outil au prix de `tool.price`"));
        sender.sendMessage(color("&7  /tools top [mesure] [âme] [n] &8— &7classement (blocs, argent, niveaux…)"));
        sender.sendMessage(color("&7  /tools aide <capacité> &8— &7fiche d'une capacité (nom, id ou noyau)"));
        sender.sendMessage(color("&7  /tools gui [âme] &8— &7menu, éventuellement ouvert sur une âme"));
        sender.sendMessage(color("&7  /tools sell [all] &8— &7vend ce que l'outil reconnaît"));
        sender.sendMessage(color("&7  /tools set <joueur> <âme> [palier] &8— &7voir/fixer un palier &8(admin)"));
        sender.sendMessage(color("&7  /tools ability <joueur> <âme> <capacité> [niveau] &8— &7régler une capacité &8(admin)"));
        sender.sendMessage(color("&7  /tools reset <joueur> [âme] &8— &7remettre une âme à zéro &8(admin)"));
        sender.sendMessage(color("&7  /tools stats &8— &7état des services"));
        if (sender.hasPermission("valoria.tools.admin")) {
            sender.sendMessage(color("&7  /tools reload &8— &7recharge la configuration &8(admin)"));
        }
    }

    // ------------------------------------------------------------------ tab completion

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 0) {
            return out;
        }
        if (args.length == 1) {
            add(out, "buy", true);
            add(out, "top", true);
            add(out, "aide", true);
            return filtered(out, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("gui")) {
            Collections.addAll(out, "pickaxe", "axe", "rod", "sword");
            return filtered(out, args[1]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("top") || args[0].equalsIgnoreCase("classement"))) {
            for (ToolStats.Metric metric : ToolStats.Metric.values()) {
                out.add(metric.key());
            }
            Collections.addAll(out, "pickaxe", "axe", "rod", "sword");
            return filtered(out, args[1]);
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("top") || args[0].equalsIgnoreCase("classement"))) {
            Collections.addAll(out, "pickaxe", "axe", "rod", "sword");
            return filtered(out, args[2]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("aide") || args[0].equalsIgnoreCase("fiche"))) {
            for (ToolKind kind : ToolKind.values()) {
                ToolsConfig.KindConfig kindConfig = this.plugin.toolsConfig().kind(kind);
                if (kindConfig == null) {
                    continue;
                }
                for (ToolsConfig.Ability ability : this.plugin.toolsConfig().abilities(kindConfig)) {
                    out.add(ability.id());
                }
            }
            return filtered(out, args[1]);
        }
        if (args.length == 1) {
            add(out, "gui", true);
            add(out, "give", sender.hasPermission("valoria.tools.give"));
            add(out, "sell", true);
            add(out, "set", sender.hasPermission("valoria.tools.admin"));
            add(out, "ability", sender.hasPermission("valoria.tools.admin"));
            add(out, "reset", sender.hasPermission("valoria.tools.admin"));
            add(out, "stats", sender.hasPermission("valoria.tools.admin"));
            add(out, "reload", sender.hasPermission("valoria.tools.admin"));
            return filtered(out, args[0]);
        }
        boolean admin = args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("tier")
                || args[0].equalsIgnoreCase("ability") || args[0].equalsIgnoreCase("reset");
        if (args.length == 2 && admin) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                out.add(online.getName());
            }
            return filtered(out, args[1]);
        }
        if (args.length == 3 && admin) {
            Collections.addAll(out, "pickaxe", "axe", "rod", "sword");
            return filtered(out, args[2]);
        }
        if (args.length == 4 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("tier"))) {
            for (int i = 1; i <= 5; i++) {
                out.add(String.valueOf(i));
            }
            return filtered(out, args[3]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("ability")) {
            ToolKind kind = ToolKind.parse(args[2]);
            ToolsConfig.KindConfig kindConfig = kind == null ? null : this.plugin.toolsConfig().kind(kind);
            if (kindConfig != null) {
                for (ToolsConfig.Ability ability : this.plugin.toolsConfig().abilities(kindConfig)) {
                    out.add(ability.id());
                }
            }
            return filtered(out, args[3]);
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("ability")) {
            for (int i = 0; i <= 10; i++) {
                out.add(String.valueOf(i));
            }
            return filtered(out, args[4]);
        }
        return out;
    }

    private static void add(List<String> out, String name, boolean allowed) {
        if (allowed) {
            out.add(name);
        }
    }

    private static List<String> filtered(List<String> candidates, String prefix) {
        List<String> out = new ArrayList<String>();
        String needle = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        for (String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(needle)) {
                out.add(candidate);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ helpers

    private static boolean isTier(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                return false;
            }
        }
        try {
            return Integer.parseInt(text) > 0;
        } catch (NumberFormatException malformed) {
            return false;
        }
    }

    /** Un entier borné, 0 compris (retirer un niveau) ; {@code -1} quand ce n'est pas un nombre. */
    private static int number(String text) {
        if (text == null) {
            return -1;
        }
        try {
            return Math.max(0, Integer.parseInt(text.trim()));
        } catch (NumberFormatException malformed) {
            return -1;
        }
    }

    private static String color(String text) {
        return MultiTool.color(text);
    }
}
