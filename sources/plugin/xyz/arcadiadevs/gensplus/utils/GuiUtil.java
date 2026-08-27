/*
 * Décompilé avec CFR 0.152.
 * 
 * Impossible de charger les classes suivantes :
 *  org.bukkit.inventory.ItemStack
 */
package xyz.arcadiadevs.gensplus.utils;

import com.cryptomorin.xseries.XMaterial;
import java.util.List;
import java.util.Random;
import org.bukkit.inventory.ItemStack;
import xyz.arcadiadevs.gensplus.GensPlus;
import xyz.arcadiadevs.gensplus.utils.config.Config;
import xyz.arcadiadevs.guilib.Gui;
import xyz.arcadiadevs.guilib.GuiItem;
import xyz.arcadiadevs.guilib.GuiItemType;
import xyz.arcadiadevs.guilib.ItemBuilder;

public class GuiUtil {
    public static void addBorder(Gui gui, String string) {
        ItemBuilder itemBuilder = new ItemBuilder(XMaterial.matchXMaterial(string).orElse(XMaterial.WHITE_STAINED_GLASS_PANE).parseItem());
        if (!string.equals("AIR")) {
            itemBuilder.name(GensPlus.getInstance().getConfig().getString(Config.GUIS_GENERATORS_GUI_BORDER_NAME.getPath()));
        }
        int n = 0;
        while (n < 9) {
            gui.setItem(n, new GuiItem(GuiItemType.BORDER, itemBuilder.build(), null));
            ++n;
        }
        n = 0;
        while (n < gui.getRows()) {
            gui.setItem(n * 9, new GuiItem(GuiItemType.BORDER, XMaterial.matchXMaterial(string).orElse(XMaterial.WHITE_STAINED_GLASS_PANE).parseItem(), null));
            ++n;
        }
        n = 0;
        while (n < gui.getRows()) {
            gui.setItem(n * 9 + 8, new GuiItem(GuiItemType.BORDER, XMaterial.matchXMaterial(string).orElse(XMaterial.WHITE_STAINED_GLASS_PANE).parseItem(), null));
            ++n;
        }
        n = (gui.getRows() - 1) * 9;
        while (n < (gui.getRows() - 1) * 9 + 9) {
            gui.setItem(n, new GuiItem(GuiItemType.BORDER, XMaterial.matchXMaterial(string).orElse(XMaterial.WHITE_STAINED_GLASS_PANE).parseItem(), null));
            ++n;
        }
    }

    @SafeVarargs
    public static void fillWithRandom(Gui gui, List<String> ... listArray) {
        Random random = new Random();
        int n = gui.getRows();
        int n2 = 0;
        while (n2 < n) {
            int n3 = 0;
            while (n3 < 9) {
                gui.setItem(n2 * 9 + n3, GuiUtil.getRandomItem(listArray[random.nextInt(listArray.length)]));
                ++n3;
            }
            ++n2;
        }
    }

    private static GuiItem getRandomItem(List<String> list) {
        Random random = new Random();
        String string = list.get(random.nextInt(list.size()));
        ItemStack itemStack = new ItemBuilder(XMaterial.matchXMaterial(string).orElseThrow().parseItem()).name(" ").build();
        return new GuiItem(GuiItemType.BORDER, itemStack, null);
    }
}

