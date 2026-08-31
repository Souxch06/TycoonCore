package xyz.arcadiadevs.valoriatycoon.commands;

import java.util.ArrayList;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xyz.arcadiadevs.valoriatycoon.utils.ScoreboardService;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import xyz.arcadiadevs.valoriatycoon.guis.AuctionGui;
import xyz.arcadiadevs.valoriatycoon.guis.SellGui;
import xyz.arcadiadevs.valoriatycoon.guis.ShopGui;
import xyz.arcadiadevs.valoriatycoon.utils.SellUtil;
import xyz.arcadiadevs.valoriatycoon.utils.config.Config;
import xyz.arcadiadevs.valoriatycoon.utils.config.Permissions;
import xyz.arcadiadevs.valoriatycoon.utils.config.message.Messages;

public class SellCommandListener
implements Listener {
    /** Tableau de bord : même point d'ancrage que /sell, pour ne pas dépendre de la classe principale. */
    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent playerJoinEvent) {
        Player player = playerJoinEvent.getPlayer();
        ScoreboardService.show(player);
        // Rien n'est déposé d'office au connect : le joueur voit ce qui l'attend dans le coffre du
        // marché (case « Coffre de récupération », ou /ah returns) et vient le chercher lui-même.
        AuctionHouse.notifyReturns(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent playerQuitEvent) {
        ScoreboardService.hide(playerQuitEvent.getPlayer());
        AuctionGui.forget(playerQuitEvent.getPlayer().getUniqueId());
        ShopGui.forget(playerQuitEvent.getPlayer().getUniqueId());
    }

    /** /ah sell <prix> [quantité] — la quantité par défaut est la pile tenue en main. */
    private String executeSell(Player player, String[] args) {
        if (args.length < 3) {
            return AuctionHouse.color("&7Utilise &f/ah sell <prix> [quantité]&7, prix à la pièce.");
        }
        double price;
        try {
            price = Double.parseDouble(args[2]);
        }
        catch (NumberFormatException exception) {
            return AuctionHouse.color("&cPrix invalide : &f" + args[2]);
        }
        int quantity = -1;
        if (args.length > 3) {
            quantity = parseId(args[3]);
            if (quantity <= 0) {
                return AuctionHouse.color("&cQuantité invalide : &f" + args[3]);
            }
        }
        if (quantity <= 0) {
            ItemStack held = player.getInventory().getItemInMainHand();
            quantity = held == null ? 1 : Math.max(1, held.getAmount());
        }
        String message = AuctionHouse.list(player, price, quantity);
        return message == null ? AuctionHouse.color("&aMise en vente effectuée.") : message;
    }

    private static int parseId(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        }
        catch (NumberFormatException exception) {
            return -1;
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent playerCommandPreprocessEvent) {
        String string;
        Player player = playerCommandPreprocessEvent.getPlayer();

        // Marché des joueurs (/ah) : intercepté ici comme /sell, pour ne pas dépendre d'une
        // inscription de commande dans la classe principale du plugin.
        String[] ahArgs = playerCommandPreprocessEvent.getMessage().split(" ");
        String ahLabel = ahArgs.length > 0 ? ahArgs[0].toLowerCase() : "";
        if (ahLabel.equals("/ah") || ahLabel.equals("/auctionhouse") || ahLabel.equals("/marche")) {
            playerCommandPreprocessEvent.setCancelled(true);
            AuctionHouse.ensureStarted();
            if (!AuctionHouse.isEnabled() && (ahArgs.length < 2 || !ahArgs[1].equalsIgnoreCase("reload"))) {
                player.sendMessage(AuctionHouse.color("&cLe marché des joueurs est désactivé (auction-house.enabled)."));
                return;
            }
            if (ahArgs.length < 2) {
                AuctionGui.open(player);
                return;
            }
            String ahAction = ahArgs[1].toLowerCase();
            if (ahAction.equals("sell") || ahAction.equals("vendre")) {
                player.sendMessage(executeSell(player, ahArgs));
                return;
            }
            if (ahAction.equals("search") || ahAction.equals("recherche")) {
                AuctionGui.search(player, ahArgs.length > 2 ? ahArgs[2] : "");
                return;
            }
            if (ahAction.equals("returns") || ahAction.equals("coffre")) {
                AuctionGui.openReturns(player);
                return;
            }
            if (ahAction.equals("claim")) {
                if (ahArgs.length > 2 && ahArgs[2].equalsIgnoreCase("all")) {
                    player.sendMessage(AuctionHouse.claimAll(player));
                } else if (ahArgs.length > 2) {
                    player.sendMessage(AuctionHouse.claim(player, parseId(ahArgs[2])));
                } else {
                    player.sendMessage(AuctionHouse.claimAll(player));
                }
                return;
            }
            if (ahAction.equals("own") || ahAction.equals("mes")) {
                AuctionGui.openOwn(player);
                return;
            }
            if (ahAction.equals("cancel") || ahAction.equals("annuler")) {
                int ahId = ahArgs.length > 2 ? parseId(ahArgs[2]) : 0;
                player.sendMessage(AuctionHouse.cancel(player, ahId));
                return;
            }
            if (ahAction.equals("stats") || ahAction.equals("info")) {
                player.sendMessage(AuctionHouse.summary(player));
                return;
            }
            if (ahAction.equals("remove") || ahAction.equals("retirer")) {
                if (!player.hasPermission("valoriatycoon.ah.admin")) {
                    player.sendMessage(AuctionHouse.color("&cRéservé à l'administration (&fvaloriatycoon.ah.admin&c)."));
                    return;
                }
                if (ahArgs.length < 3) {
                    player.sendMessage(AuctionHouse.color("&7Utilise &f/ah remove <id>&7."));
                    return;
                }
                int ahTarget = parseId(ahArgs[2]);
                if (ahTarget <= 0) {
                    player.sendMessage(AuctionHouse.color("&cNuméro d'annonce invalide."));
                    return;
                }
                player.sendMessage(AuctionHouse.adminRemove(ahTarget, player));
                return;
            }
            if (ahAction.equals("reload")) {
                if (!player.hasPermission("valoriatycoon.ah.admin")) {
                    player.sendMessage(AuctionHouse.color("&cRéservé à l'administration."));
                    return;
                }
                player.sendMessage(AuctionHouse.reload(xyz.arcadiadevs.valoriatycoon.ValoriaTycoon.getInstance()));
                return;
            }
            player.sendMessage(AuctionHouse.color("&8[&aAH&8] &f/ah&7 ouvrir · &f/ah sell <prix> [qté]&7 · "
                    + "&f/ah cancel [id]&7 · &f/ah search <motif>&7 · &f/ah own&7 · &f/ah returns&7 · &f/ah claim [all]&7 · &f/ah stats"));
            return;
        }

        // Comptoir d'achat (/shop) : intercepté ici comme /ah, pour ne pas dépendre d'une inscription de
        // commande dans la classe principale. `ShopGui.command` reçoit le message brut — args[0] est le
        // libellé `/shop` lui-même, la première valeur utile est donc args[1] (même convention que /ah).
        if (ahLabel.equals("/shop") || ahLabel.equals("/comptoir")) {
            playerCommandPreprocessEvent.setCancelled(true);
            String message = ShopGui.command(player, ahArgs);
            if (message != null) {
                player.sendMessage(message);
            }
            return;
        }


        String[] stringArray = playerCommandPreprocessEvent.getMessage().split(" ");
        stringArray[0] = stringArray[0].toLowerCase();
        if (!Config.SELL_COMMAND_ENABLED.getBoolean()) {
            return;
        }
        ArrayList<String> arrayList = Config.SELL_COMMAND_ALLIASES.getStringList();
        if (!arrayList.contains(string = stringArray[0].replace("/", ""))) {
            return;
        }
        if (stringArray.length < 2) {
            if (string.equalsIgnoreCase("sell")) {
                if (!player.hasPermission(Permissions.GENERATOR_DROPS_SELL_GUI.getPermission(new String[0]))) {
                    Messages.NO_PERMISSION.format(new Object[0]).send((CommandSender)player);
                    playerCommandPreprocessEvent.setCancelled(true);
                    return;
                }
                SellGui.open(player);
                playerCommandPreprocessEvent.setCancelled(true);
                return;
            }
            Messages.NOT_ENOUGH_ARGUMENTS.format(new Object[0]).send((CommandSender)player);
            playerCommandPreprocessEvent.setCancelled(true);
            return;
        }
        if (stringArray[1].equalsIgnoreCase("all")) {
            if (!player.hasPermission(Permissions.GENERATOR_DROPS_SELL_ALL.getPermission(new String[0]))) {
                Messages.NO_PERMISSION.format(new Object[0]).send((CommandSender)player);
                return;
            }
            SellUtil.sellAll(player, (Inventory)player.getInventory(), new boolean[0]);
            playerCommandPreprocessEvent.setCancelled(true);
            return;
        }
        if (stringArray[1].equalsIgnoreCase("hand")) {
            if (!player.hasPermission(Permissions.GENERATOR_DROPS_SELL_HAND.getPermission(new String[0]))) {
                Messages.NO_PERMISSION.format(new Object[0]).send((CommandSender)player);
                return;
            }
            SellUtil.sellHand(player);
            playerCommandPreprocessEvent.setCancelled(true);
            return;
        }
        if (stringArray[1].equalsIgnoreCase("gui")) {
            if (!player.hasPermission(Permissions.GENERATOR_DROPS_SELL_GUI.getPermission(new String[0]))) {
                Messages.NO_PERMISSION.format(new Object[0]).send((CommandSender)player);
                return;
            }
            SellGui.open(player);
            playerCommandPreprocessEvent.setCancelled(true);
        }
    }
}

