package fr.valoriatycoon.ranks;

import java.util.List;
import java.util.Optional;

/** Immutable configuration for the medieval-rank progression. */
public record RankSettings(List<RankRequirement> levels) {

    public RankSettings {
        levels = List.copyOf(levels);
    }

    public Optional<RankRequirement> level(int level) {
        return level < 1 || level > levels.size()
                ? Optional.empty()
                : Optional.of(levels.get(level - 1));
    }

    public int maximumLevel() {
        return levels.size();
    }

    public String name(int level) {
        return level <= 0 ? "Sans rang" : level(level).map(RankRequirement::name).orElse("Duc");
    }
}
