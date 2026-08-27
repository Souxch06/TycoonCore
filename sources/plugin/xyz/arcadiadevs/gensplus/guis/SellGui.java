/*
 * Décompilé avec CFR 0.152.
 * 
 * Impossible de charger les classes suivantes :
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package xyz.arcadiadevs.gensplus.guis;

import com.awaitquality.api.spigot.chat.ChatUtil;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import xyz.arcadiadevs.gensplus.GensPlus;
import xyz.arcadiadevs.gensplus.utils.config.Config;
import xyz.arcadiadevs.guilib.Gui;

public class SellGui {
    public static void open(Player player) {
        if (!Config.GUIS_SELL_GUI_ENABLED.getBoolean()) {
            return;
        }
        GensPlus gensPlus = GensPlus.getInstance();
        int n = Config.GUIS_SELL_GUI_ROWS.getInt();
        Gui gui = new Gui(ChatUtil.translate(Config.GUIS_SELL_GUI_TITLE.getString()), n, (Plugin)gensPlus);
        player.openInventory(gui.getInventory());
    }
}

