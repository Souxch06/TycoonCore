package fr.valoriatycoon.resourcepack;

import java.util.Objects;
import org.bukkit.configuration.file.FileConfiguration;

/** Strict resource-pack.yml parser with safe vanilla fallbacks. */
public final class ResourcePackConfigLoader {
    private ResourcePackConfigLoader() {
    }

    /** Loads item-model activation and namespace settings. */
    public static ResourcePackSettings load(FileConfiguration config) {
        Objects.requireNonNull(config, "config");
        String namespace = config.getString("item-models.namespace", "valoriatycoon");
        return new ResourcePackSettings(
                config.getBoolean("item-models.enabled", true),
                namespace
        );
    }
}
