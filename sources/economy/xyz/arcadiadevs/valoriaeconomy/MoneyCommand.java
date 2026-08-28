package xyz.arcadiadevs.valoriaeconomy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * Commandes d'argent : {@code /bal}, {@code /pay}, {@code /baltop} pour les joueurs, {@code /eco} pour
 * l'administration.
 *
 * <p>{@code /pay} est le seul chemin qui déplace de l'argent entre deux joueurs, il est donc écrit en
 * <b>retirer d'abord, déposer ensuite, rembourser en cas d'échec</b> : le solde du destinataire est
 * incrémenté seulement après que l'émetteur a été décrémenté avec succès, et si le dépôt échouait, le
 * retrait est annulé. Aucun des deux ordres ne peut laisser de l'argent créée ou détruite.</p>
 */
public final class MoneyCommand implements CommandExecutor, TabCompleter {

    private final ValoriaEconomy plugin;
    private final Balances balances;

    public MoneyCommand(ValoriaEconomy plugin, Balances balances) {
        this.plugin = plugin;
        this.balances = balances;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("bal") || name.equals("balance")) {
            return this.handleBal(sender, args);
        }
        if (name.equals("pay")) {
            return this.handlePay(sender, args);
        }
        if (name.equals("baltop") || name.equals("topmoney")) {
            return this.handleTop(sender);
        }
        if (name.equals("eco")) {
            return this.handleEco(sender, args);
        }
        return false;
    }

    private boolean handleBal(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(color("&7Utilise &f/bal <joueur>&7 depuis la console."));
                return true;
            }
            Player player = (Player) sender;
            sender.sendMessage(color("&6Solde&7 : &a" + balances.format(balances.balance(player))));
            return true;
        }
        String target = args[0];
        if (!balances.exists(target)) {
            sender.sendMessage(color("&cCompte inconnu : &f" + target + "&c doit se connecter au moins une fois."));
            return true;
        }
        sender.sendMessage(color("&6" + target + "&7 : &a" + balances.format(balances.balance(target))));
        return true;
    }

    private boolean handlePay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cLa console n'a pas de solde : utilise &f/eco give&c."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(color("&7Utilise &f/pay <joueur> <montant>&7."));
            return true;
        }
        Player payer = (Player) sender;
        String target = args[0];
        if (target.equalsIgnoreCase(payer.getName())) {
            sender.sendMessage(color("&cTu ne peux pas te payer toi-même."));
            return true;
        }
        Double amount = money(args[1]);
        if (amount == null) {
            sender.sendMessage(color("&cMontant invalide : &f" + args[1]));
            return true;
        }
        if (!balances.exists(target)) {
            sender.sendMessage(color("&cCompte inconnu : &f" + target + "&c doit se connecter au moins une fois."));
            return true;
        }
        double after = balances.withdraw(payer, amount.doubleValue());
        if (after < 0.0D) {
            sender.sendMessage(color("&cSolde insuffisant : tu as &f"
                    + balances.format(balances.balance(payer)) + "&c, la transaction demande &f"
                    + balances.format(amount.doubleValue())) + "&c.");
            return true;
        }
        double credited = balances.deposit(target, amount.doubleValue());
        if (credited <= 0.0D && amount.doubleValue() > 0.0D) {
            balances.deposit(payer, amount.doubleValue());
            sender.sendMessage(color("&cLe dépôt a échoué, le retrait est annulé."));
            return true;
        }
        sender.sendMessage(color("&aTu as envoyé &f" + balances.format(amount.doubleValue()) + "&a à &f" + target
                + "&a. Nouveau solde : &f" + balances.format(after) + "&a."));
        Player online = Bukkit.getPlayerExact(target);
        if (online != null && online.isOnline()) {
            online.sendMessage(color("&a" + payer.getName() + " t'envoie &f"
                    + balances.format(amount.doubleValue()) + "&a."));
        }
        return true;
    }

    private boolean handleTop(CommandSender sender) {
        List<String> top = balances.top(10);
        if (top.isEmpty()) {
            sender.sendMessage(color("&7Aucun compte enregistré pour l'instant."));
            return true;
        }
        sender.sendMessage(color("&6&lClassement des soldes"));
        for (int i = 0; i < top.size(); ++i) {
            String[] parts = top.get(i).split("\\|");
            sender.sendMessage(color("&e" + (i + 1) + ". &f" + parts[0] + " &7- &a"
                    + (parts.length > 1 ? parts[1] : "?")));
        }
        return true;
    }

    private boolean handleEco(CommandSender sender, String[] args) {
        if (!sender.hasPermission("valoriaeconomy.eco")) {
            sender.sendMessage(color("&cPermission manquante (&fvaloriaeconomy.eco&c)."));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(color("&7Utilise &f/eco give|set|take|stats|reload&7."));
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("reload")) {
            plugin.reload();
            balances.load();
            sender.sendMessage(color("&aConfiguration de l'économie rechargée."));
            return true;
        }
        if (action.equals("stats")) {
            sender.sendMessage(color("&6Comptes&7 : &f" + balances.accountCount() + "&6, monnaie&7 : &f"
                    + balances.currencyPlural()));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(color("&7Utilise &f/eco " + action + " <joueur> <montant>&7."));
            return true;
        }
        String target = args[1];
        Double amount = money(args[2]);
        if (amount == null) {
            sender.sendMessage(color("&cMontant invalide : &f" + args[2]));
            return true;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(target);
        if (action.equals("give")) {
            double after = balances.deposit(offline, amount.doubleValue());
            sender.sendMessage(color("&aCrédité &f" + balances.format(amount.doubleValue()) + "&a à &f" + target
                    + "&a (solde : &f" + balances.format(after) + "&a)."));
            notify(offline, color("&aTu reçois &f" + balances.format(amount.doubleValue()) + "&a de l'administration."));
            return true;
        }
        if (action.equals("take")) {
            double after = balances.withdraw(offline, amount.doubleValue());
            if (after < 0.0D) {
                sender.sendMessage(color("&cSolde insuffisant pour &f" + target + "&c."));
                return true;
            }
            sender.sendMessage(color("&aRetiré &f" + balances.format(amount.doubleValue()) + "&a à &f" + target
                    + "&a (solde : &f" + balances.format(after) + "&a)."));
            notify(offline, color("&cL'administration retire &f" + balances.format(amount.doubleValue()) + "&c de ton solde."));
            return true;
        }
        if (action.equals("set")) {
            double value = balances.set(offline, amount.doubleValue());
            sender.sendMessage(color("&aSolde de &f" + target + "&a fixé à &f" + balances.format(value) + "&a."));
            notify(offline, color("&eTon solde a été ajusté par l'administration."));
            return true;
        }
        sender.sendMessage(color("&7Action inconnue : &f" + action));
        return true;
    }

    private static void notify(OfflinePlayer player, String message) {
        if (player == null || !player.isOnline()) {
            return;
        }
        Player online = player.getPlayer();
        if (online != null) {
            online.sendMessage(message);
        }
    }

    /** Parse un montant positif, fini, arrondi à 2 décimales ; {@code null} si la valeur est refusée. */
    private static Double money(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim().replace(",", ".");
        if (text.isEmpty() || text.startsWith("-") || text.startsWith("+")) {
            return null;
        }
        try {
            double value = Double.parseDouble(text);
            if (!Double.isFinite(value) || value <= 0.0D || value > 1.0E12D) {
                return null;
            }
            return Double.valueOf(Math.round(value * 100.0D) / 100.0D);
        }
        catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<String>();
        if (args.length == 1) {
            if (name.equals("eco")) {
                out.add("give");
                out.add("take");
                out.add("set");
                out.add("stats");
                out.add("reload");
            } else if (!name.equals("baltop") && !name.equals("topmoney")) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    out.add(online.getName());
                }
            }
        } else if (args.length == 2 && name.equals("eco")) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                out.add(online.getName());
            }
        } else if (args.length == 3 && name.equals("eco")) {
            out.add("100");
        }
        if (args.length > 1) {
            String needle = args[args.length - 1].toLowerCase(Locale.ROOT);
            out.removeIf(entry -> !entry.toLowerCase(Locale.ROOT).startsWith(needle));
        }
        Collections.sort(out);
        return out;
    }
}
