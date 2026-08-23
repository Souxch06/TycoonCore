package fr.valoriatycoon.quests;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record QuestSettings(int flushIntervalTicks, Map<String, QuestDefinition> quests) {
    public QuestSettings {
        quests = Collections.unmodifiableMap(new LinkedHashMap<>(quests));
    }
}
