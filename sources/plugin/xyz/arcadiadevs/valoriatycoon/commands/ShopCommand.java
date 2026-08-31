package xyz.arcadiadevs.valoriatycoon.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import xyz.arcadiadevs.valoriatycoon.guis.ShopGui;

/**
 * Le comptoir d'achat ({@code /shop}) en vraie commande déclarée de {@code plugin.yml}.
 *
 * <p>Jusqu'ici {@code /shop} était intercepté dans {@link SellCommandListener} (comme {@code /ah}), le moyen
 * de tenir la commande sans rien déclarer. L'interception a un coût : Bukkit ne propose <em>aucune</em>
 * suggestion pour une commande non déclarée, donc {@code /shop} restait muet à la complétion Tab. En
 * déclarant la commande et en branchant ce {@link CommandExecutor} + {@link TabCompleter}, {@code /shop}
 * apparaît dans la suggestion et laisse {@code /ah} et {@code /sell} à leur interception.
 *
 * <p>La logique de caisse n'est pas recopiée ici : elle vit dans {@link ShopGui#command(Player, String[])}.
 * Ce {@code CommandExecutor} ne fait que reconstituer le tableau que cette méthode attend ({@code args[0]}
 * est le libellé, comme le lui donnait le hook d'interception) puis renvoie au joueur le message éventuel.
 */
public final class ShopCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION_ADMIN = "valoriatycoon.shop.admin";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("&cRéservé aux joueurs.");
            return true;
        }
        // `args` n'inclut pas le libellé : on remet `/shop` (ou l'alias `/comptoir`) en tête, comme le
        // faisait le message du hook d'interception, pour que ShopGui.command lise args[1] comme action.
        String[] full = new String[args.length + 1];
        full[0] = "/" + label;
        System.arraycopy(args, 0, full, 1, args.length);
        String message = ShopGui.command((Player) sender, full);
        if (message != null) {
            sender.sendMessage(message);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            List<String> out = new ArrayList<String>();
            if (sender.hasPermission(PERMISSION_ADMIN)) {
                out.add("reload");
            }
            out.add("acheter");
            out.add("vendre");
            out.addAll(ShopGui.materialNames());
            return out;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("acheter") || args[0].equalsIgnoreCase("buy")
                || args[0].equalsIgnoreCase("vendre") || args[0].equalsIgnoreCase("sell")
                || args[0].equalsIgnoreCase("rendre"))) {
            return new ArrayList<String>(ShopGui.materialNames());
        }
        return Collections.emptyList();
    }
}
