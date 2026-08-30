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
            case "max":
            case "maximum":
                return maxOut(sender, args);
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
        // Payer deux fois pour le même objet serait une arnaque, et « un seul multi-outil » veut dire
        // aussi qu'un second achat n'apporte strictement rien : on refuse avant de débiter.
        if (this.plugin.guard() != null && this.plugin.toolsConfig().singlePerPlayer()
                && this.plugin.guard().count(target) > 0) {
            target.sendMessage(color("&eTu as déjà ton multi-outil &7(un seul par joueur&7)."));
            target.sendMessage(color("&7&f/tools&7 ouvre le panneau d'amélioration, &7accroupi + clic droit"
                    + " dans le jeu."));
            return true;
        }
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
        // L'item est posé par la garde : un exemplaire, dans la main courante, jamais une seconde copie
        // rejetée à terre (le « drop » d'un inventaire plein contredirait le non-droppable promis).
        if (this.plugin.guard() != null) {
            this.plugin.guard().grant(target);
        } else {
            target.getInventory().addItem(MultiTool.create(this.plugin.toolsConfig(), this.plugin.store(),
                    target.getUniqueId()));
        }
        refreshHeld(target);
        target.sendMessage(color("&a" + (price > 0.0D
                ? "Multi-outil acheté (-" + this.plugin.economy().format(price) + ")."
                : "Multi-outil reçu (gratuit : tool.price = 0).")
                + " &7Il change d'âme selon le bloc regardé ; &fsneak + clic droit&7 ouvre le panneau."));
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
        // Le palier annoncé doit être ÉCRIT. Avant ce correctif, `/tools give <joueur> 50` répondait
        // « âmes au palier 50 » sans rien poser dans tools.yml : le joueur restait au palier 1, donc sans
        // une seule capacité débloquée — une des faces du « j'ai max ma multi-tool mais rien ne s'active ».
        if (tier > 1) {
            for (ToolKind kind : ToolKind.values()) {
                ToolsConfig.KindConfig kindConfig = this.plugin.toolsConfig().kind(kind);
                if (kindConfig == null) {
                    continue;
                }
                int max = this.plugin.toolsConfig().maxTier(kindConfig);
                this.plugin.store().setTier(target, kind, Math.min(tier, max), max);
            }
            // Le store ne s'écrit jamais lui-même : sans ceci, le palier posé serait perdu au reload.
            this.plugin.saveSoon();
        }
        // Un seul exemplaire par joueur : c'est ToolGuard qui pose l'item, give compris — un give qui
        // ajouterait une deuxième copie rendrait la règle fausse juste là où on la croit sûre.
        boolean alreadyHadOne = this.plugin.guard() != null && this.plugin.guard().count(target) > 0;
        if (this.plugin.guard() != null) {
            this.plugin.guard().grant(target);
        } else {
            target.getInventory().addItem(MultiTool.create(this.plugin.toolsConfig(), this.plugin.store(),
                    target.getUniqueId()));
        }
        refreshHeld(target);
        target.updateInventory();
        target.sendMessage(color("&aTon multi-outil est prêt &7(âmes au palier &f" + tier + "&7)"
                + (alreadyHadOne ? "&8— un exemplaire suffisait : aucune copie ajoutée&7."
                        : "&7, il ne se lâche pas.")));
        if (!sender.equals(target)) {
            sender.sendMessage(color("&aDonné à &f" + target.getName() + "&a."));
        }
        return true;
    }

    /**
     * Vente déclenchée depuis une case du panneau : le menu passe par la commande, il ne reduplique pas la
     * grille de prix ni le remboursement en cas de dépôt refusé.
     */
    public void sellFromGui(Player player) {
        sell(player, new String[]{"sell", "all"});
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
        refreshHeld(target);
        target.sendMessage(color("&a" + MultiTool.capitalize(kind.label()) + "&7 → palier &f" + applied
                + (sender.equals(target) ? "&7." : "&7 (décision de &f" + sender.getName() + "&7)")));
        if (!sender.equals(target)) {
            sender.sendMessage(color("&a" + target.getName() + " : " + kind.label() + " au palier " + applied));
        }
        this.plugin.saveSoon();
        return true;
    }

    /**
     * <code>/tools ability &lt;joueur&gt; &lt;âme|all&gt; &lt;capacité|all&gt; [niveau|max|+N|-N]</code>.
     *
     * <p>Réservé à l'admin, et volontairement sans contrôle d'argent : c'est l'outil de réglage et de
     * test, pas un raccourci de jeu. Trois raccourcis évitent de taper vingt-deux fois la même ligne
     * quand on veut équiper un outil d'un coup : <code>all</code> en âme (les quatre), <code>all</code>
     * en capacité (tout le barème de l'âme), et un niveau <code>max</code> ou relatif
     * (<code>+5</code>, <code>-3</code>). Sans niveau, la commande ne modifie rien : elle affiche.</p>
     *
     * <p>Le niveau reste borné par <code>max-level</code> de la config, parce qu'un niveau hors barème
     * afficherait dans le menu une capacité plus forte que ce que le fichier déclare.</p>
     *
     * <p>Le joueur peut être omis par un admin en jeu qui se règle lui-même : si le premier argument
     * n'est pas un joueur en ligne mais se lit comme une âme, la commande vise son auteur — et
     * seulement dans ce cas, un joueur réellement nommé « pioche » garde la priorité.</p>
     */
    private boolean ability(CommandSender sender, String[] args) {
        if (!sender.hasPermission("valoria.tools.admin")) {
            sender.sendMessage(color("&cPermission manquante (&fvaloria.tools.admin&c)."));
            return true;
        }
        if (args.length < 3) {
            usageAbility(sender);
            return true;
        }
        int offset = selfShift(sender, args);
        Player target = offset == 0 ? (Player) sender : Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(color("&cJoueur hors ligne : les niveaux sont lus en jeu."));
            return true;
        }
        if (args.length < offset + 3) {
            usageAbility(sender);
            return true;
        }
        List<ToolKind> kinds = new ArrayList<ToolKind>();
        if (!resolveKinds(kinds, args[offset + 1], sender)) {
            return true;
        }
        String wanted = args[offset + 2];
        String asked = args.length > offset + 3 ? args[offset + 3] : null;

        int touched = 0;
        int locked = 0;
        int unknown = 0;
        int shown = 0;
        for (ToolKind kind : kinds) {
            ToolsConfig.KindConfig kindConfig = this.plugin.toolsConfig().kind(kind);
            if (kindConfig == null) {
                sender.sendMessage(color("&cAucune configuration pour " + kind.label() + "."));
                unknown++;
                continue;
            }
            List<ToolsConfig.Ability> chosen = new ArrayList<ToolsConfig.Ability>();
            if (isAll(wanted)) {
                chosen.addAll(this.plugin.toolsConfig().abilities(kindConfig));
            } else {
                ToolsConfig.Ability found = findAbility(kindConfig, wanted);
                if (found == null) {
                    sender.sendMessage(color("&cCapacité inconnue pour &f" + kind.label() + "&c : &f" + wanted));
                    if (kinds.size() == 1) {
                        sender.sendMessage(color("&7Choix : &f" + String.join(", ", knownIds(kindConfig))
                                + "&7, ou &fall&7 pour tout le barème."));
                    }
                    unknown++;
                    continue;
                }
                chosen.add(found);
            }
            int maxTier = this.plugin.toolsConfig().maxTier(kindConfig);
            int tier = this.plugin.store().tierOf(target.getUniqueId(), kind, maxTier);
            Map<String, Integer> levels = this.plugin.store().levelsOf(target.getUniqueId(), kind);
            if (asked == null) {
                describe(sender, target, kind, chosen, levels, tier, maxTier);
                shown++;
                continue;
            }
            for (ToolsConfig.Ability ability : chosen) {
                int level = resolveLevel(asked, ToolsConfig.levelOf(ability, levels, tier), ability.maxLevel());
                if (level < 0) {
                    sender.sendMessage(color("&cNiveau invalide : &f" + asked + "&c — un entier, &fmax&c,"
                            + " &f+N&c ou &f-N&c."));
                    return true;
                }
                this.plugin.store().setLevel(target, kind, ability.id(), level, ability.maxLevel());
                if (tier < ability.unlock()) {
                    locked++;
                }
                touched++;
            }
        }
        if (touched == 0) {
            // `shown > 0` = la commande a affiché sans écrire (pas de niveau demandé) : ce n'est pas un
            // échec, et le dire ferait croire que le réglage n'a pas été enregistré.
            if (unknown == 0 && shown == 0) {
                sender.sendMessage(color("&eRien à régler : aucune capacité ne correspond."));
            }
            return true;
        }
        refreshHeld(target);
        sender.sendMessage(color("&a" + target.getName() + " &7: &f" + touched + "&7 niveau(x) de capacité"
                + " réglé(s) sur " + labelOf(kinds) + "."));
        if (!sender.equals(target)) {
            target.sendMessage(color("&aTes capacités d'outil ont été réglées par &f" + sender.getName() + "&a."));
        }
        if (locked > 0) {
            sender.sendMessage(color("&e" + locked + " réglage(s) au-dessus du palier actuel : le niveau est"
                    + " stocké, pas perdu, mais il ne fera effet qu'au palier requis (&f/tools set "
                    + target.getName() + " <âme> <palier>&e)."));
        }
        this.plugin.saveSoon();
        return true;
    }

    /**
     * <code>/tools max [joueur] [âme] [niveau]</code> : le palier au maximum configuré et tout le barème
     * de l'âme à son <code>max-level</code> — ou à <code>niveau</code> si l'admin en donne un.
     *
     * <p>C'est la commande d'équipement d'un coup : sans elle, préparer un outil de test demande un
     * <code>/tools set</code> puis un <code>/tools ability</code> par capacité. Le palier est monté
     * AVANT les niveaux, sinon chaque capacité réglée tomberait sous le verrou de palier et
     * n'afficherait aucun effet — exactement ce qui fait croire que la commande « ne marche pas ».</p>
     */
    private boolean maxOut(CommandSender sender, String[] args) {
        if (!sender.hasPermission("valoria.tools.admin")) {
            sender.sendMessage(color("&cPermission manquante (&fvaloria.tools.admin&c)."));
            return true;
        }
        int offset = selfShift(sender, args);
        Player target;
        if (offset == 0) {
            target = (Player) sender;
        } else if (args.length > 1) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(color("&cJoueur hors ligne : les paliers et les niveaux se règlent en jeu."));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(color("&cUsage console : /tools max <joueur> [ame] [niveau]"));
            return true;
        }
        List<ToolKind> kinds = new ArrayList<ToolKind>();
        if (args.length > offset + 1 && !resolveKinds(kinds, args[offset + 1], sender)) {
            return true;
        }
        if (kinds.isEmpty()) {
            Collections.addAll(kinds, ToolKind.values());
        }
        String asked = args.length > offset + 2 ? args[offset + 2] : "max";

        int done = 0;
        for (ToolKind kind : kinds) {
            ToolsConfig.KindConfig kindConfig = this.plugin.toolsConfig().kind(kind);
            if (kindConfig == null) {
                sender.sendMessage(color("&cAucune configuration pour " + kind.label() + "."));
                continue;
            }
            int maxTier = this.plugin.toolsConfig().maxTier(kindConfig);
            this.plugin.store().setTier(target, kind, maxTier, maxTier);
            int tier = this.plugin.store().tierOf(target.getUniqueId(), kind, maxTier);
            int before = this.plugin.store().totalLevels(target.getUniqueId(), kind);
            int perKind = 0;
            for (ToolsConfig.Ability ability : this.plugin.toolsConfig().abilities(kindConfig)) {
                int level = resolveLevel(asked, 0, ability.maxLevel());
                if (level < 0) {
                    sender.sendMessage(color("&cNiveau invalide : &f" + asked + "&c — un entier ou &fmax&c."));
                    return true;
                }
                this.plugin.store().setLevel(target, kind, ability.id(), level, ability.maxLevel());
                perKind++;
            }
            done += perKind;
            int after = this.plugin.store().totalLevels(target.getUniqueId(), kind);
            sender.sendMessage(color("&a" + MultiTool.capitalize(kind.label()) + "&7 : palier &f" + tier
                    + "&7/&f" + maxTier + "&7, " + perKind + " capacité(s) — niveaux " + before + " → &f" + after));
        }
        if (done == 0) {
            return true;
        }
        refreshHeld(target);
        if (!sender.equals(target)) {
            target.sendMessage(color("&aTon multi-outil a été porté au maximum par &f" + sender.getName() + "&a."));
        }
        this.plugin.saveSoon();
        return true;
    }

    /** Affiche l'état d'une liste de capacités, sans rien écrire : c'est le mode sans niveau. */
    private void describe(CommandSender sender, Player target, ToolKind kind,
                          List<ToolsConfig.Ability> abilities, Map<String, Integer> levels,
                          int tier, int maxTier) {
        int total = 0;
        int locked = 0;
        for (ToolsConfig.Ability ability : abilities) {
            int level = ToolsConfig.levelOf(ability, levels, tier);
            total += level;
            if (level > 0 && tier < ability.unlock()) {
                locked++;
            }
        }
        sender.sendMessage(color("&6" + target.getName() + " &7— " + MultiTool.capitalize(kind.label())
                + " &8(palier &f" + tier + "&8/&f" + maxTier + "&8)"));
        for (ToolsConfig.Ability ability : abilities) {
            int level = ToolsConfig.levelOf(ability, levels, tier);
            sender.sendMessage(color("  &7" + ability.name() + " &8(" + ability.id() + ")&7 : &f" + level
                    + "&7/&f" + ability.maxLevel()
                    + (tier < ability.unlock() ? " &8— palier " + ability.unlock() + " requis" : "")));
        }
        sender.sendMessage(color("&7  total &f" + total + "&7 niveau(x), " + abilities.size()
                + " capacité(s)" + (locked > 0 ? ", &e" + locked + " sous verrou de palier" : "") + "&7."));
    }

    /**
     * Rafraîchit la lore de l'outil tenu, si c'en est un : les niveaux y sont résumés. C'est aussitôt
     * l'occasion de redemander les effets « outil en main » — un palier ou un niveau changé ici doit se
     * voir au bloc suivant, pas à la prochaine tick de la tâche périodique.
     */
    private void refreshHeld(Player target) {
        ItemStack held = target.getInventory().getItemInMainHand();
        if (MultiTool.isMultiTool(held)) {
            MultiTool.refresh(held, this.plugin.toolsConfig(), this.plugin.store(), target.getUniqueId());
            target.updateInventory();
        }
        this.plugin.refreshPassive(target);
    }

    private void usageAbility(CommandSender sender) {
        sender.sendMessage(color("&cUsage : /tools ability <joueur> <ame|all> <capacite|all> [niveau|max|+N|-N]"));
        sender.sendMessage(color("&7Le joueur est facultatif en jeu : &f/tools ability pioche all max"));
    }

    /** Nom d'une liste d'âmes pour un message : « pioche » ou « 4 âmes ». */
    private static String labelOf(List<ToolKind> kinds) {
        if (kinds.size() == 1) {
            return kinds.get(0).label();
        }
        return kinds.size() + " âmes";
    }

    /**
     * Remplit {@code out} avec les âmes demandées : une seule, ou toutes si l'argument vaut
     * <code>all</code>. Retourne {@code false} si l'âme est inconnue (message déjà envoyé).
     */
    private static boolean resolveKinds(List<ToolKind> out, String text, CommandSender sender) {
        if (isAll(text)) {
            Collections.addAll(out, ToolKind.values());
            return true;
        }
        ToolKind kind = ToolKind.parse(text);
        if (kind == null) {
            sender.sendMessage(color("&cÂme inconnue : &f" + text + "&c. Choix : pioche, hache, canne,"
                    + " epee, all."));
            return false;
        }
        out.add(kind);
        return true;
    }

    /** Vrai si l'argument vaut « tout » : les quatre âmes, ou tout le barème d'une âme. */
    private static boolean isAll(String text) {
        if (text == null) {
            return false;
        }
        String wanted = text.trim().toLowerCase(Locale.ROOT);
        return wanted.equals("all") || wanted.equals("*") || wanted.equals("tout") || wanted.equals("toutes");
    }

    /**
     * Traduit ce que l'admin a tapé en niveau applicable, borné par le plafond de la capacité.
     *
     * <p><code>7</code> pose 7 ; <code>max</code> pose le <code>max-level</code> ; <code>+5</code> et
     * <code>-3</code> sont relatifs au niveau courant — le réglage d'un barème à vingt-deux entrées se
     * fait en tâtonnant, et retaper le total à chaque essai est la première raison d'abandonner.</p>
     *
     * @return le niveau, jamais négatif ni au-dessus du plafond ; {@code -1} si le texte n'est aucun
     *         des trois
     */
    private static int resolveLevel(String text, int current, int maxLevel) {
        if (text == null) {
            return -1;
        }
        String wanted = text.trim().toLowerCase(Locale.ROOT);
        if (wanted.equals("max") || wanted.equals("maximum")) {
            return Math.max(0, maxLevel);
        }
        try {
            int raw = Integer.parseInt(wanted);
            int level = wanted.startsWith("+") || wanted.startsWith("-") ? current + raw : raw;
            return Math.max(0, Math.min(level, Math.max(0, maxLevel)));
        } catch (NumberFormatException malformed) {
            return -1;
        }
    }

    /**
     * 0 quand la commande vise son auteur (le premier argument est déjà une âme), 1 quand args[1] est
     * le joueur. Le repli ne s'applique que si ce premier argument n'est pas un joueur en ligne : un
     * joueur nommé « pioche » doit rester ciblable.
     */
    private static int selfShift(CommandSender sender, String[] args) {
        if (args.length < 2 || !(sender instanceof Player)) {
            return 1;
        }
        return Bukkit.getPlayerExact(args[1]) == null && ToolKind.parse(args[1]) != null ? 0 : 1;
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
        refreshHeld(target);
        sender.sendMessage(color("&a" + target.getName() + " &7: âmes remises au palier 1 &7("
                + kinds.size() + " âme(s), capacités effacées)."));
        target.sendMessage(color("&e" + kinds.size() + " âme(s) remise(s) au palier 1 par l'administration"
                + " : capacités effacées."));
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
        // Le diagnostic qui répond à « mes capacités ne s'activent pas » : est-ce que les âmes reconnaissent
        // les blocs, est-ce que la garde tient, et quel effet l'outil en main donne vraiment.
        for (String line : this.plugin.matcher().diagnose().split("\n")) {
            sender.sendMessage(color("  &8» &r") + line);
        }
        ToolGuard guard = this.plugin.guard();
        if (guard != null) {
            sender.sendMessage(color("  &8» &rgarde de l'item : &f" + guard.describe()));
        }
        if (sender instanceof Player) {
            Player player = (Player) sender;
            ToolsConfig.Effect haste = this.plugin.passiveHaste(player);
            if (haste == null || !haste.active()) {
                sender.sendMessage(color("  &8» &rvitesse de minage : &crien n'est dû &7(pas d'outil en main,"
                        + " capacité non achetée, monde hors liste, ou &ftool.haste-while-held: false&7)."));
            } else {
                sender.sendMessage(color("  &8» &rvitesse de minage : &fHaste " + Abilities.grade(haste)
                        + " &7(&f" + haste.level() + " &7niveau(x) sur &f" + haste.sources()
                        + " capacité(s)) &8— posée tant que l'outil est en main&7."));
            }
            if (guard != null) {
                int copies = guard.count(player);
                sender.sendMessage(color("  &8» &rmulti-outils dans son sac : &f" + copies
                        + (copies == 1 ? " &7(le bon compte)" : " &c(incohérent)")));
            }
        }
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
        sender.sendMessage(color("&7  /tools ability [joueur] <âme|all> <capacité|all> [niveau|max|+N|-N] &8— &7régler &8(admin)"));
        sender.sendMessage(color("&7  /tools max [joueur] [âme] [niveau] &8— &7palier max et tout le barème au max &8(admin)"));
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
            add(out, "buy", true);
            add(out, "top", true);
            add(out, "aide", true);
            add(out, "give", sender.hasPermission("valoria.tools.give"));
            add(out, "sell", true);
            add(out, "set", sender.hasPermission("valoria.tools.admin"));
            add(out, "ability", sender.hasPermission("valoria.tools.admin"));
            add(out, "max", sender.hasPermission("valoria.tools.admin"));
            add(out, "reset", sender.hasPermission("valoria.tools.admin"));
            add(out, "stats", sender.hasPermission("valoria.tools.admin"));
            add(out, "help", true);
            add(out, "reload", sender.hasPermission("valoria.tools.admin"));
            return filtered(out, args[0]);
        }
        boolean admin = args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("tier")
                || args[0].equalsIgnoreCase("ability") || args[0].equalsIgnoreCase("capacite")
                || args[0].equalsIgnoreCase("max") || args[0].equalsIgnoreCase("maximum")
                || args[0].equalsIgnoreCase("reset");
        // `ability` et `max` acceptent d'omettre le joueur : l'admin en jeu se règle lui-même en tapant
        // l'âme en premier. Les âmes sont donc proposées AUSSI en deuxième position — mais seulement si
        // la commande sait se cibler, sinon le Tab proposerait une âme là où un joueur est attendu.
        boolean selfTarget = args[0].equalsIgnoreCase("ability") || args[0].equalsIgnoreCase("capacite")
                || args[0].equalsIgnoreCase("max") || args[0].equalsIgnoreCase("maximum");
        if (args.length == 2 && admin) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                out.add(online.getName());
            }
            if (selfTarget && sender instanceof Player) {
                Collections.addAll(out, "pickaxe", "axe", "rod", "sword");
            }
            return filtered(out, args[1]);
        }
        if (args.length == 3 && admin) {
            // forme courte déjà engagée (`/tools ability pioche …`) : le troisième mot est une capacité
            if (selfTarget && sender instanceof Player && ToolKind.parse(args[1]) != null
                    && Bukkit.getPlayerExact(args[1]) == null) {
                ToolsConfig.KindConfig shortConfig =
                        this.plugin.toolsConfig().kind(ToolKind.parse(args[1]));
                if (shortConfig != null) {
                    for (ToolsConfig.Ability ability : this.plugin.toolsConfig().abilities(shortConfig)) {
                        out.add(ability.id());
                    }
                    out.add("all");
                }
                return filtered(out, args[2]);
            }
            Collections.addAll(out, "pickaxe", "axe", "rod", "sword", "all");
            return filtered(out, args[2]);
        }
        if (args.length == 4 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("tier"))) {
            for (int i = 1; i <= 5; i++) {
                out.add(String.valueOf(i));
            }
            return filtered(out, args[3]);
        }
        boolean abilitySub = args[0].equalsIgnoreCase("ability") || args[0].equalsIgnoreCase("capacite");
        boolean maxSub = args[0].equalsIgnoreCase("max") || args[0].equalsIgnoreCase("maximum");
        // forme courte (`/tools ability pioche fortune 5`) : tout est décalé d'un cran vers la gauche
        boolean shortForm = selfTarget && sender instanceof Player && args.length > 1
                && ToolKind.parse(args[1]) != null && Bukkit.getPlayerExact(args[1]) == null;
        if (args.length == 4 && abilitySub) {
            if (shortForm) {
                levelCandidates(out);
                return filtered(out, args[3]);
            }
            ToolKind kind = ToolKind.parse(args[2]);
            ToolsConfig.KindConfig kindConfig = kind == null ? null : this.plugin.toolsConfig().kind(kind);
            if (kindConfig != null) {
                for (ToolsConfig.Ability ability : this.plugin.toolsConfig().abilities(kindConfig)) {
                    out.add(ability.id());
                }
            }
            out.add("all");
            return filtered(out, args[3]);
        }
        if (args.length == 5 && abilitySub) {
            levelCandidates(out);
            return filtered(out, args[4]);
        }
        if (args.length == 4 && maxSub && !shortForm) {
            levelCandidates(out);
            return filtered(out, args[3]);
        }
        return out;
    }

    /**
     * Les niveaux proposés au Tab : les petits entiers, <code>max</code>, et les pas relatifs les plus
     * utiles. Proposer <code>+10</code> n'est pas décoratif : sans lui, l'admin qui monte un barème de
     * vingt-deux capacités retape vingt-deux fois le même total.
     */
    private static void levelCandidates(List<String> out) {
        for (int i = 0; i <= 10; i++) {
            out.add(String.valueOf(i));
        }
        Collections.addAll(out, "max", "+1", "+5", "+10", "-1", "-5");
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

    private static String color(String text) {
        return MultiTool.color(text);
    }

            }
