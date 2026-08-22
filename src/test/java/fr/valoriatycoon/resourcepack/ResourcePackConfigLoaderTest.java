package fr.valoriatycoon.resourcepack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ResourcePackConfigLoaderTest {

    @Test
    void loadsValoriaItemModelNamespace() {
        var stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("resource-pack.yml")
        );
        ResourcePackSettings settings = ResourcePackConfigLoader.load(
                YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8))
        );

        assertTrue(settings.customItemModels());
        assertEquals("valoriatycoon", settings.namespace());
    }

    @Test
    void rejectsUnsafeNamespacesAndPathSegments() {
        assertThrows(IllegalArgumentException.class, () -> new ResourcePackSettings(true, "Bad Namespace"));
        assertThrows(IllegalArgumentException.class, () -> ItemVisualService.segment("../invalid"));
        assertEquals("fishing_rod", ItemVisualService.segment("FISHING_ROD"));

        ItemVisualService visuals = new ItemVisualService(
                new ResourcePackSettings(true, "valoriatycoon")
        );
        assertEquals("valoriatycoon:ui/main/pets", visuals.key("ui/main/pets").toString());
        assertThrows(IllegalArgumentException.class, () -> visuals.key("../outside"));
    }
}
