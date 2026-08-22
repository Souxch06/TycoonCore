package fr.valoriatycoon.tools;

import java.util.List;
import org.bukkit.Material;

/** Display, selector, XP and dedicated-currency definition for one tool category. */
public record ToolDefinition(
        ToolType type,
        Material icon,
        String displayName,
        int menuSlot,
        List<String> menuLore,
        long experiencePerAction,
        String currencyName,
        Material currencyIcon,
        long coinsPerAction
) {
    public ToolDefinition {
        menuLore = List.copyOf(menuLore);
    }
}
