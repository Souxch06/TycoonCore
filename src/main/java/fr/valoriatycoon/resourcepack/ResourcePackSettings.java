package fr.valoriatycoon.resourcepack;

import java.util.Objects;
import java.util.regex.Pattern;

/** Immutable settings controlling custom item-model components used by ValoriaTycoon. */
public record ResourcePackSettings(boolean customItemModels, String namespace) {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9._-]{1,64}");

    public ResourcePackSettings {
        namespace = Objects.requireNonNull(namespace, "namespace").trim();
        if (!NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalArgumentException("Invalid resource-pack namespace: " + namespace);
        }
    }
}
