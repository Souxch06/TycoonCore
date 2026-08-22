package fr.valoriatycoon.quests;

import fr.valoriatycoon.economy.MoneyCodec;
import fr.valoriatycoon.tools.ToolType;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public final class QuestConfigLoader {
    private QuestConfigLoader() {}

    public static QuestSettings load(FileConfiguration config) {
        Map<String, QuestDefinition> quests = new LinkedHashMap<>();
        ConfigurationSection section = required(config, "quests");
        for (String id : section.getKeys(false)) {
            ConfigurationSection q = required(section, id);
            quests.put(id, new QuestDefinition(
                    id,
                    QuestRarity.valueOf(text(q, "rarity").toUpperCase(Locale.ROOT)),
                    ToolType.valueOf(text(q, "tool").toUpperCase(Locale.ROOT)),
                    positiveLong(q, "target-actions"),
                    money(q, "reward-money")
            ));
        }
        return new QuestSettings(config.getInt("flush-interval-ticks", 20), quests);
    }

    private static long money(ConfigurationSection s, String p) {
        return MoneyCodec.toCents(new BigDecimal(text(s, p)));
    }
    private static long positiveLong(ConfigurationSection s, String p) {
        long v=s.getLong(p,-1); if(v<=0) throw new IllegalArgumentException(p); return v;
    }
    private static String text(ConfigurationSection s,String p) {
        String v=s.getString(p); if(v==null||v.isBlank()) throw new IllegalArgumentException(p); return v.trim();
    }
    private static ConfigurationSection required(ConfigurationSection s,String p) {
        ConfigurationSection v=s.getConfigurationSection(p); if(v==null) throw new IllegalArgumentException(p); return v;
    }
}
