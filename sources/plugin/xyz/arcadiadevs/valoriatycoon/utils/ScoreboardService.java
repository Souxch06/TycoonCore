package xyz.arcadiadevs.valoriatycoon.utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import xyz.arcadiadevs.valoriatycoon.ValoriaTycoon;

/**
 * Tableau d'information (sidebar) minimal : pseudo, solde, et lignes configurables.
 *
 * <p>Le contenu des lignes vient de la configuration ({@code scoreboard.lines}) avec les
 * placeholders {@code %player%}, {@code %money%}, {@code %balance%}, {@code %generators%},
 * {@code %server%} : pour ajouter une information plus tard, on ajoute une ligne dans
 * {@code config.yml} et, si besoin, un placeholder dans {@link #placeholder} — sans rien brancher
 * ailleurs. Le module est volontairement sans dépendance externe (ni PlaceholderAPI, ni plugin de
 * scoreboard).</p>
 *
 * <p>Les appel d'affichage passent par la réflexion : {@code registerNewObjective} et
 * {@code setDisplayName} ont changé de signature plusieurs fois (String, puis Component côté
 * Adventure) et les versions récentes suppriment les variantes dépréciées. Une résolution par nom
 * et arité sur la classe réellement chargée évite un {@code NoSuchMethodError} en jeu, et le
 * diagnostic listé dans {@link #missing()} dit ce qui a manqué sur le serveur concerné.</p>
 *
 * <p>Démarrage à la demande ({@link #show} ou {@link #toggle}) : le plugin n'a pas à être modifié
 * pour déclarer la tâche, le point d'entrée est le listener de commandes déjà enregistré.</p>
 */
public final class ScoreboardService {

    private static final Map<UUID, View> VIEWS = new HashMap<UUID, View>();
    private static boolean started = false;
    private static boolean enabledByDefault = true;
    private static long periodTicks = 40L;
    private static String title = "&a&lValoriaTycoon";
    private static List<String> lines = new ArrayList<String>();
    private static final List<String> MISSING = new ArrayList<String>();

    private ScoreboardService() {
    }

    /** Charge la configuration et planifie le rafraîchissement, une seule fois. */
    private static synchronized void ensureStarted() {
        if (started) {
            return;
        }
        started = true;
        ValoriaTycoon plugin = ValoriaTycoon.getInstance();
        enabledByDefault = plugin.getConfig().getBoolean("scoreboard.enabled", true);
        periodTicks = Math.max(10L, (long) plugin.getConfig().getInt("scoreboard.update-ticks", 40));
        title = plugin.getConfig().getString("scoreboard.title", "&a&lValoriaTycoon");
        List<String> configured = plugin.getConfig().getStringList("scoreboard.lines");
        lines = configured.isEmpty() ? defaultLines() : configured;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> ScoreboardService.refreshAll(), 20L, periodTicks);
    }

    private static List<String> defaultLines() {
        List<String> defaults = new ArrayList<String>();
        defaults.add("&7Joueur : &f%player%");
        defaults.add("&7Solde : &a%money%");
        defaults.add("&7Générateurs : &e%generators%");
        defaults.add("&7Serveur : &f%server%");
        return defaults;
    }

    public static void show(Player player) {
        ensureStarted();
        if (!available(player)) {
            return;
        }
        View view = VIEWS.get(player.getUniqueId());
        if (view == null) {
            view = new View();
            VIEWS.put(player.getUniqueId(), view);
        }
        view.enabled = true;
        ScoreboardService.paint(player, view);
    }

    public static void hide(Player player) {
        View view = VIEWS.remove(player.getUniqueId());
        if (view != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    /** `/sb` : active ou coupe le tableau pour ce joueur (et le remet à jour immédiatement). */
    public static String toggle(Player player) {
        ensureStarted();
        View view = VIEWS.get(player.getUniqueId());
        boolean on = view == null ? enabledByDefault : !view.enabled;
        if (!on) {
            ScoreboardService.hide(player);
            return ScoreboardService.color("&7Tableau de bord désactivé.");
        }
        if (!available(player)) {
            return ScoreboardService.color("&cLe tableau de bord n'est pas disponible sur ce serveur.");
        }
        ScoreboardService.show(player);
        return ScoreboardService.color("&aTableau de bord activé.");
    }

    private static boolean available(Player player) {
        if (!player.hasPermission("valoriatycoon.scoreboard")) {
            return false;
        }
        return Bukkit.getScoreboardManager() != null;
    }

    private static void refreshAll() {
        if (VIEWS.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, View> entry : new HashMap<UUID, View>(VIEWS).entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline() || !entry.getValue().enabled) {
                continue;
            }
            ScoreboardService.paint(player, entry.getValue());
        }
    }

    /** Reconstruit le contenu du tableau d'un joueur, en réutilisant son objectif pour ne pas faire clignoter la sidebar. */
    private static void paint(Player player, View view) {
        try {
            if (view.objective == null || view.board == null) {
                Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
                Objective objective = createObjective(board, "vt", ScoreboardService.color(title));
                if (objective == null) {
                    return;
                }
                view.board = board;
                view.objective = objective;
                player.setScoreboard(board);
            }
            for (String previous : view.entries) {
                reset(view.board, previous);
            }
            List<String> rendered = ScoreboardService.render(player);
            view.entries.clear();
            for (int index = 0; index < rendered.size(); ++index) {
                // Les entrées d'un objectif doivent être distinctes : les espaces finales rendent la
                // ligne unique sans changer ce que voit le joueur.
                String entry = rendered.get(index) + repeat(' ', index);
                view.entries.add(entry);
                score(view.objective, entry, rendered.size() - index);
            }
        }
        catch (Throwable throwable) {
            // un tableau qui ne s'affiche pas ne doit jamais casser le jeu
            if (MISSING.size() < 8) {
                MISSING.add(throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
            }
        }
    }

    private static List<String> render(Player player) {
        List<String> rendered = new ArrayList<String>();
        for (String line : lines) {
            if (rendered.size() >= 15) {
                break;
            }
            rendered.add(ScoreboardService.color(ScoreboardService.placeholder(line, player)));
        }
        return rendered;
    }

    /** Remplace les placeholders d'une ligne. Pour en ajouter un, il suffit d'étendre cette liste. */
    private static String placeholder(String line, Player player) {
        String result = line;
        if (result.contains("%player%")) {
            result = result.replace("%player%", player.getName());
        }
        if (result.contains("%money%") || result.contains("%balance%")) {
            result = result.replace("%money%", ScoreboardService.money(player)).replace("%balance%", ScoreboardService.rawBalance(player));
        }
        if (result.contains("%generators%")) {
            result = result.replace("%generators%", ScoreboardService.generators(player));
        }
        if (result.contains("%server%")) {
            result = result.replace("%server%", String.valueOf(Bukkit.getOnlinePlayers().size()));
        }
        if (result.contains("%ping%")) {
            result = result.replace("%ping%", String.valueOf(player.getPing()));
        }
        return result;
    }

    private static String money(Player player) {
        return String.format("%s", ScoreboardService.rawBalance(player));
    }

    private static String rawBalance(Player player) {
        Economy economy = ValoriaTycoon.getInstance().getEcon();
        if (economy == null) {
            return "0";
        }
        try {
            return economy.format(economy.getBalance(Bukkit.getOfflinePlayer(player.getUniqueId())));
        }
        catch (Throwable throwable) {
            return "0";
        }
    }

    private static String generators(Player player) {
        try {
            return String.valueOf(ValoriaTycoon.getInstance().getLocationsData().getGeneratorsCountByPlayer(player));
        }
        catch (Throwable throwable) {
            return "0";
        }
    }

    // ------------------------------------------------------------------ réflexion d'affichage

    /**
     * Crée l'objectif en essayant les signatures connues, de la plus récente à la plus ancienne :
     * {@code (String, Criteria, Component)}, {@code (String, String, String)}, {@code (String, String)}.
     */
    private static Objective createObjective(Scoreboard board, String name, String displayName) {
        Method two = method(board.getClass(), "registerNewObjective", 2, String.class, Object.class);
        if (two != null) {
            try {
                Object criteria = criteria();
                Objective objective = (Objective) two.invoke(board, name, criteria);
                setDisplayName(objective, displayName);
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);
                return objective;
            }
            catch (Throwable throwable) {
                // on tente la variante suivante
            }
        }
        Method three = method(board.getClass(), "registerNewObjective", 3, String.class, String.class, String.class);
        if (three != null) {
            try {
                Objective objective = (Objective) three.invoke(board, name, "dummy", displayName);
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);
                return objective;
            }
            catch (Throwable throwable) {
                // on tente la variante suivante
            }
        }
        Method legacy = method(board.getClass(), "registerNewObjective", 3, String.class, String.class, Object.class);
        if (legacy != null) {
            try {
                Objective objective = (Objective) legacy.invoke(board, name, "dummy", component(displayName));
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);
                return objective;
            }
            catch (Throwable throwable) {
                MISSING.add("registerNewObjective(3) : " + throwable);
            }
        }
        if (MISSING.size() < 8) {
            MISSING.add("aucune signature de Scoreboard#registerNewObjective explovable");
        }
        return null;
    }

    private static Object criteria() {
        try {
            Class<?> type = Class.forName("org.bukkit.scoreboard.Criteria");
            Object value = type.getField("DUMMY").get(null);
            return value;
        }
        catch (Throwable throwable) {
            return "dummy";
        }
    }

    private static void setDisplayName(Object objective, String displayName) {
        try {
            Method plain = method(objective.getClass(), "setDisplayName", 1, String.class);
            if (plain != null) {
                plain.invoke(objective, displayName);
                return;
            }
        }
        catch (Throwable throwable) {
            // variante Component ci-dessous
        }
        try {
            Method adventure = method(objective.getClass(), "setDisplayName", 1, Object.class);
            if (adventure != null) {
                adventure.invoke(objective, component(displayName));
            }
        }
        catch (Throwable throwable) {
            if (MISSING.size() < 8) {
                MISSING.add("Objective#setDisplayName : " + throwable);
            }
        }
    }

    private static Object component(String text) {
        try {
            Class<?> type = Class.forName("net.kyori.adventure.text.Component");
            return type.getMethod("text", String.class).invoke(null, text);
        }
        catch (Throwable throwable) {
            return text;
        }
    }

    /**
     * Pose un score. Reflexion obligatoire : {@code Score#setScore(int)} rendait {@code void} en
     * 1.20 et rend {@code ScoreSet} depuis 1.21 — un appel compilé contre l'ancienne forme lève un
     * NoSuchMethodError sur les serveurs récents. Résoudre la méthode à l'exécution rend le module
     * indépendant de ce changement de signature.
     */
    private static void score(Objective objective, String entry, int value) {
        try {
            Object holder = objective.getClass().getMethod("getScore", String.class).invoke(objective, entry);
            if (holder == null) {
                return;
            }
            Method setter = method(holder.getClass(), "setScore", 1, int.class);
            if (setter != null) {
                setter.invoke(holder, value);
                return;
            }
            if (MISSING.size() < 8) {
                MISSING.add("Score#setScore(int) introuvable");
            }
        }
        catch (Throwable throwable) {
            if (MISSING.size() < 8) {
                MISSING.add("Score#setScore : " + throwable);
            }
        }
    }

    private static void reset(Scoreboard board, String entry) {
        try {
            Method reset = method(board.getClass(), "resetScores", 1, String.class);
            if (reset != null) {
                reset.invoke(board, entry);
            }
        }
        catch (Throwable throwable) {
            // ligne déjà absente : sans conséquence
        }
    }

    private static Method method(Class<?> owner, String name, int arity, Class<?>... expected) {
        if (owner == null) {
            return null;
        }
        for (Method candidate : owner.getMethods()) {
            if (!candidate.getName().equals(name) || candidate.getParameterTypes().length != arity) {
                continue;
            }
            boolean compatible = true;
            for (int index = 0; index < expected.length; ++index) {
                if (!expected[index].isAssignableFrom(candidate.getParameterTypes()[index])
                        && !candidate.getParameterTypes()[index].isAssignableFrom(expected[index])) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                return candidate;
            }
        }
        return null;
    }

    /** Ce qui n'a pas pu être résolu sur ce serveur — à coller dans un rapport de bug. */
    public static String missing() {
        return MISSING.isEmpty() ? "aucun" : String.join(" | ", MISSING);
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static String repeat(char character, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int index = 0; index < count; ++index) {
            builder.append(character);
        }
        return builder.toString();
    }

    /** État par joueur : le tableau est conservé entre deux rafraîchissements pour éviter le clignotement. */
    private static final class View {
        private Scoreboard board;
        private Objective objective;
        private final List<String> entries = new ArrayList<String>();
        private boolean enabled = true;
    }
}
