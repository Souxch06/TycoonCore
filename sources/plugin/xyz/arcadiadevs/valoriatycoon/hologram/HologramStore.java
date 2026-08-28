package xyz.arcadiadevs.valoriatycoon.hologram;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

/**
 * Sauvegarde texte des hologrammes : <code>plugins/ValoriaTycoon/holograms.txt</code>.
 *
 * <pre>
 * #version 1
 * &lt;uuid&gt;|&lt;monde&gt;|&lt;x&gt;|&lt;y&gt;|&lt;z&gt;|&lt;yaw&gt;|&lt;pitch&gt;|&lt;materiau ou -&gt;|&lt;ligne 1&gt;|&lt;ligne 2&gt;|...
 * </pre>
 *
 * <p>Format en lignes plutôt que JSON : les lignes d'hologramme contiennent du texte arbitraire
 * choisi par le admin (§, accents, barres obliques) et doivent pouvoir être relues après une
 * édition manuelle. Un enregistrement par ligne, séparé par {@code |}, avec échappement de
 * {@code \}, {@code |}, newline et tabulation — donc jamais de parseur à réécrire si le admin
 * corrige une coquille dans le fichier.</p>
 *
 * <p>Écriture <b>atomique</b> : fichier temporaire puis {@code ATOMIC_MOVE}. Un arrêt du serveur
 * pendant l'écriture ne peut pas laisser un fichier à moitié rempli, donc jamais d'hologrammes
 * perdus par une redémarrage brutal.</p>
 */
final class HologramStore {

    private static final String HEADER = "#version 1";
    private static final char SEP = '|';

    private final Path file;
    private final Plugin plugin;
    private boolean warnedMissingWorld;

    HologramStore(Plugin plugin) {
        this.plugin = plugin;
        this.file = plugin.getDataFolder().toPath().resolve("holograms.txt");
    }

    Path path() {
        return this.file;
    }

    /** Enregistre l'état complet. Une erreur remonte : le pool la journalise et continue. */
    void save(Collection<Hologram> holograms) {
        List<String> lines = new ArrayList<String>();
        lines.add(HEADER);
        for (Hologram hologram : holograms) {
            Location location = hologram.getLocation();
            if (location == null || location.getWorld() == null) {
                continue;
            }
            StringBuilder line = new StringBuilder();
            line.append(escape(hologram.getId().toString())).append(SEP);
            line.append(escape(location.getWorld().getName())).append(SEP);
            line.append(num(location.getX())).append(SEP);
            line.append(num(location.getY())).append(SEP);
            line.append(num(location.getZ())).append(SEP);
            line.append(num(location.getYaw())).append(SEP);
            line.append(num(location.getPitch())).append(SEP);
            Material item = hologram.getItem();
            line.append(escape(item == null ? "-" : item.name())).append(SEP);
            List<String> content = hologram.getLines();
            for (int i = 0; i < content.size(); i++) {
                if (i > 0) {
                    line.append(SEP);
                }
                line.append(escape(content.get(i)));
            }
            lines.add(line.toString());
        }
        Path tmp = this.file.resolveSibling(this.file.getFileName().toString() + ".tmp");
        try {
            Path parent = this.file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(tmp, lines, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(tmp, this.file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException failed) {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
                // rien de plus à faire
            }
            throw new IllegalStateException("ecriture de " + this.file.getFileName() + " impossible : "
                    + failed.getMessage(), failed);
        }
    }

    /** Lit le fichier ; une ligne illisible est ignorée et signalée, jamais fatale. */
    List<Hologram> load() {
        List<Hologram> out = new ArrayList<Hologram>();
        if (!Files.isRegularFile(this.file)) {
            return out;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(this.file, StandardCharsets.UTF_8);
        } catch (IOException failed) {
            this.plugin.getLogger().warning("holograms.txt illisible (" + failed.getMessage()
                    + ") : hologrammes recréés depuis la configuration uniquement.");
            return out;
        }
        int skipped = 0;
        for (String line : lines) {
            if (line == null || line.trim().isEmpty() || line.startsWith("#")) {
                continue;
            }
            Hologram hologram = parse(line);
            if (hologram == null) {
                skipped++;
                continue;
            }
            out.add(hologram);
        }
        if (skipped > 0) {
            this.plugin.getLogger().warning("holograms.txt : " + skipped + " ligne(s) ignorée(s) (format inconnu).");
        }
        return out;
    }

    private Hologram parse(String line) {
        String[] parts = split(line);
        if (parts.length < 9) {
            return null;
        }
        java.util.UUID id;
        try {
            id = java.util.UUID.fromString(unescape(parts[0]));
        } catch (RuntimeException malformed) {   // UUID.fromString leve un IllegalArgumentException
            return null;                          // (une RuntimeException) : un multi-catch serait illégal
        }
        String worldName = unescape(parts[1]);
        double x;
        double y;
        double z;
        float yaw;
        float pitch;
        try {
            x = Double.parseDouble(parts[2]);
            y = Double.parseDouble(parts[3]);
            z = Double.parseDouble(parts[4]);
            yaw = Float.parseFloat(parts[5]);
            pitch = Float.parseFloat(parts[6]);
        } catch (NumberFormatException malformed) {
            return null;
        }
        Location location = HoloEasy.parseLocation(worldName, x, y, z, yaw, pitch);
        if (location == null) {
            if (!this.warnedMissingWorld) {
                this.plugin.getLogger().warning("holograms.txt : un monde du fichier n'est pas chargé ("
                        + worldName + ") : hologrammes non restaurés pour ce monde.");
                this.warnedMissingWorld = true;
            }
            return null;
        }
        Material item = HoloEasy.material(unescape(parts[7]));
        List<String> lines = new ArrayList<String>();
        for (int i = 8; i < parts.length; i++) {
            String text = unescape(parts[i]);
            if (!text.isEmpty()) {
                lines.add(HoloEasy.color(text));
            }
        }
        Hologram hologram = new Hologram(id, location, lines, item);
        return hologram;
    }

    /** Sépare sur {@code |} non échappé. */
    private static String[] split(String line) {
        List<String> out = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }
            if (c == '\\') {
                current.append(c);
                escaped = true;
                continue;
            }
            if (c == SEP) {
                out.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        out.add(current.toString());
        return out.toArray(new String[0]);
    }

    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length() + 8);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\':
                    out.append("\\\\");
                    break;
                case '|':
                    out.append("\\|");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    out.append(c);
            }
        }
        return out.toString();
    }

    private static String unescape(String text) {
        if (text == null || text.indexOf('\\') < 0) {
            return text == null ? "" : text;
        }
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '\\' || i + 1 >= text.length()) {
                out.append(c);
                continue;
            }
            char next = text.charAt(++i);
            switch (next) {
                case 'n':
                    out.append('\n');
                    break;
                case 'r':
                    out.append('\r');
                    break;
                case 't':
                    out.append('\t');
                    break;
                default:
                    out.append(next);
            }
        }
        return out.toString();
    }

    /**
     * Nombre sans séparateur de millier ni locale : {@code Double.toString} garde la virgule
     * décimale en toutes langues, alors qu'un {@code String.format} localisé écrirait « 1,5 » et
     * rendrait la ligne illisible.
     */
    private static String num(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value) && Math.abs(value) < 1.0E15D) {
            return String.valueOf((long) value) + ".0";
        }
        return String.valueOf(value);
    }

    private static String num(float value) {
        if (value == Math.rint(value) && !Float.isInfinite(value)) {
            return String.valueOf((int) value) + ".0";
        }
        return String.valueOf(value);
    }
}
