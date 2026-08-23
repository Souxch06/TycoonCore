package fr.valoriatycoon.resourcepack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ResourcePackAssetTest {

    @Test
    void everyItemDefinitionHasAModelAndTexture() throws IOException {
        Path root = Path.of(System.getProperty("user.dir"), "resource-pack", "assets", "valoriatycoon");
        Set<String> items = relativeFiles(root.resolve("items"), ".json");
        Set<String> models = relativeFiles(root.resolve("models/item"), ".json");
        Set<String> textures = relativeFiles(root.resolve("textures/item"), ".png");

        assertEquals(259, items.size());
        assertEquals(items, models);
        assertEquals(items, textures);
        assertTrue(items.contains("ui/main/ranks"));
        assertTrue(items.contains("ui/main/leaderboards"));
        assertTrue(items.contains("ui/warp/tutorial"));
        assertTrue(items.contains("ui/warp/crates"));
        assertTrue(items.contains("ui/leaderboard/money"));
        assertTrue(items.contains("ui/leaderboard/entry/gold"));
        assertTrue(items.contains("item/key/crate_vote"));
        assertTrue(items.contains("item/key/crate_legendary"));
        assertTrue(items.contains("item/key/crate_valoria"));
        assertTrue(items.contains("item/crate/valoria"));
        assertTrue(items.contains("item/crate/pets"));
        assertTrue(items.contains("item/reward/money_bag/5"));
        assertTrue(items.contains("item/reward/coin_bag/universal"));
        assertTrue(items.contains("item/reward/xp_vial/4"));
        assertTrue(items.contains("item/reward/resource_bundle/3"));
        assertTrue(items.contains("item/reward/voucher/generator"));
        assertTrue(items.contains("ui/tool/capability/farm_key_finder"));
        assertTrue(items.contains("item/compact/diamond/3"));
        assertTrue(items.contains("item/multitool/rank/0/pickaxe"));
        assertTrue(items.contains("item/multitool/rank/10/fishing_rod"));
    }

    @Test
    void everyCrateHasItsOwnCustomGeometryAndTexture() throws Exception {
        Path root = Path.of(System.getProperty("user.dir"), "resource-pack", "assets", "valoriatycoon");
        Path models = root.resolve("models/item/item/crate");
        Path textures = root.resolve("textures/item/item/crate");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        Set<String> textureHashes = new HashSet<>();
        Set<Integer> geometryFingerprints = new HashSet<>();

        for (String crate : Set.of(
                "vote", "quest", "farm", "common", "rare", "epic", "legendary", "valoria", "pets"
        )) {
            String model = Files.readString(models.resolve(crate + ".json"));
            assertTrue(model.contains("\"elements\""), crate + " must be a real custom 3D model");
            assertTrue(!model.contains("minecraft:block/cube_all"), crate + " must not reuse a vanilla cube");
            textureHashes.add(HexFormat.of().formatHex(
                    digest.digest(Files.readAllBytes(textures.resolve(crate + ".png")))
            ));
            String geometry = model.replace("valoriatycoon:item/item/crate/" + crate, "crate-texture");
            geometryFingerprints.add(geometry.hashCode());
        }

        assertEquals(9, textureHashes.size(), "Every crate texture must be visually independent");
        assertEquals(9, geometryFingerprints.size(), "Every crate silhouette must be independent");
    }

    @Test
    void everyRankAndFormHasItsOwnFullTexture() throws Exception {
        Path textures = Path.of(
                System.getProperty("user.dir"),
                "resource-pack", "assets", "valoriatycoon", "textures", "item", "item", "multitool", "rank"
        );
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        Set<String> hashes = new HashSet<>();
        for (int rank = 0; rank <= 10; rank++) {
            for (String form : Set.of("pickaxe", "hoe", "axe", "fishing_rod")) {
                byte[] content = Files.readAllBytes(textures.resolve(rank + "/" + form + ".png"));
                hashes.add(HexFormat.of().formatHex(digest.digest(content)));
            }
        }

        assertEquals(44, hashes.size(), "Every rank/form texture must be visually independent");
    }

    @Test
    void usesPremiumThirtyTwoPixelItemsAndAnOrnateContainerInterface() throws IOException {
        Path pack = Path.of(System.getProperty("user.dir"), "resource-pack");
        Path textures = pack.resolve("assets/valoriatycoon/textures/item");
        try (var paths = Files.walk(textures)) {
            for (Path texture : paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".png"))
                    .toList()) {
                assertEquals(32, pngWidth(texture), texture.toString());
                assertEquals(32, pngHeight(texture), texture.toString());
            }
        }
        Path container = pack.resolve("assets/minecraft/textures/gui/container/generic_54.png");
        assertTrue(Files.isRegularFile(container));
        assertEquals(256, pngWidth(container));
        assertEquals(256, pngHeight(container));
        Path slot = pack.resolve("assets/minecraft/textures/gui/sprites/container/slot.png");
        assertEquals(18, pngWidth(slot));
        assertEquals(18, pngHeight(slot));
        Path crafting = pack.resolve("assets/minecraft/textures/gui/container/crafting_table.png");
        assertEquals(256, pngWidth(crafting));
        Path header = pack.resolve("assets/valoriatycoon/textures/font/gui_header.png");
        assertEquals(176, pngWidth(header));
        assertEquals(64, pngHeight(header));
        assertTrue(Files.isRegularFile(pack.resolve("assets/valoriatycoon/font/gui.json")));
        for (String block : Set.of(
                "stone", "grass_block_top", "crafting_table_top", "furnace_front",
                "coal_ore", "deepslate_diamond_ore", "oak_log", "wheat_stage7"
        )) {
            Path texture = pack.resolve("assets/minecraft/textures/block/" + block + ".png");
            assertEquals(32, pngWidth(texture), block);
            assertEquals(32, pngHeight(texture), block);
        }
    }

    @Test
    void targetsMinecraftTwentySixPointTwoPackFormat() throws IOException {
        Path metadata = Path.of(System.getProperty("user.dir"), "resource-pack", "pack.mcmeta");
        String content = Files.readString(metadata).replaceAll("\\s+", "");

        assertTrue(content.contains("\"min_format\":[88,0]"));
        assertTrue(content.contains("\"max_format\":[88,0]"));
    }

    private int pngWidth(Path path) throws IOException {
        return ByteBuffer.wrap(Files.readAllBytes(path), 16, 4).getInt();
    }

    private int pngHeight(Path path) throws IOException {
        return ByteBuffer.wrap(Files.readAllBytes(path), 20, 4).getInt();
    }

    private Set<String> relativeFiles(Path root, String suffix) throws IOException {
        try (var paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .map(root::relativize)
                    .map(Path::toString)
                    .map(path -> path.replace('\\', '/'))
                    .filter(path -> path.endsWith(suffix))
                    .map(path -> path.substring(0, path.length() - suffix.length()))
                    .collect(Collectors.toUnmodifiableSet());
        }
    }
}
