package xyz.arcadiadevs.valoriatycoon.guis;

import com.awaitquality.api.spigot.chat.ChatUtil;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import xyz.arcadiadevs.valoriatycoon.ValoriaTycoon;
import xyz.arcadiadevs.valoriatycoon.utils.config.Config;
import xyz.arcadiadevs.guilib.Gui;

public class SellGui {
    public static void open(Player player) {
        if (!Config.GUIS_SELL_GUI_ENABLED.getBoolean()) {
            return;
        }
        ValoriaTycoon valoriaTycoon = ValoriaTycoon.getInstance();
        int n = Config.GUIS_SELL_GUI_ROWS.getInt();
        Gui gui = new Gui(ChatUtil.translate(Config.GUIS_SELL_GUI_TITLE.getString()), n, (Plugin)valoriaTycoon);
        player.openInventory(gui.getInventory());
    }
}

