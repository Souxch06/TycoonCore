package fr.valoriatycoon.tutorial;

import fr.valoriatycoon.economy.MoneyCodec;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** Strict parser for the sequential first-rank onboarding tutorial. */
public final class TutorialConfigLoader {
    private TutorialConfigLoader() {
    }

    public static TutorialSettings load(FileConfiguration config) {
        ConfigurationSection configuredSteps = required(config, "steps");
        Map<TutorialStep, TutorialSettings.StepDefinition> steps = new EnumMap<>(TutorialStep.class);
        for (TutorialStep step : TutorialStep.values()) {
            if (!step.actionable()) {
                continue;
            }
            ConfigurationSection value = required(configuredSteps, step.configKey());
            steps.put(step, new TutorialSettings.StepDefinition(
                    step,
                    positiveLong(value, "target"),
                    positiveMoney(value, "reward-money"),
                    text(value, "objective")
            ));
        }
        if (configuredSteps.getKeys(false).size() != steps.size()) {
            throw new IllegalArgumentException("tutorial.yml contains an unsupported or missing step");
        }
        return new TutorialSettings(
                config.getBoolean("enabled", true),
                integer(config, "tick-interval", 40, 1, 1_200),
                text(config, "actionbar.no-island"),
                text(config, "actionbar.step"),
                text(config, "actionbar.ready"),
                steps
        );
    }

    private static int integer(
            ConfigurationSection section,
            String path,
            int fallback,
            int minimum,
            int maximum
    ) {
        int value = section.getInt(path, fallback);
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(path + " must be between " + minimum + " and " + maximum);
        }
        return value;
    }

    private static long positiveLong(ConfigurationSection section, String path) {
        long value = section.getLong(path, -1L);
        if (value < 1L) {
            throw new IllegalArgumentException(path + " must be positive");
        }
        return value;
    }

    private static long positiveMoney(ConfigurationSection section, String path) {
        String raw = section.getString(path);
        try {
            long cents = MoneyCodec.toCents(new BigDecimal(raw));
            if (cents < 1L) {
                throw new IllegalArgumentException("Tutorial reward must be positive");
            }
            return cents;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(path + " must be an exact positive amount", exception);
        }
    }

    private static String text(ConfigurationSection section, String path) {
        String value = section.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(path + " must not be blank");
        }
        return value.trim();
    }

    private static ConfigurationSection required(ConfigurationSection section, String path) {
        ConfigurationSection value = section.getConfigurationSection(path);
        if (value == null) {
            throw new IllegalArgumentException("Missing section: " + path);
        }
        return value;
    }
}
