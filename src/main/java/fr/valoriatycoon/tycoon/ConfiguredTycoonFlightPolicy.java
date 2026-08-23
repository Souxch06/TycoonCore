package fr.valoriatycoon.tycoon;

import java.util.UUID;

/** Current free-flight policy; replaceable later without changing movement/protection code. */
public final class ConfiguredTycoonFlightPolicy implements TycoonFlightAccessPolicy {
    private final TycoonSettings.Flight settings;

    public ConfiguredTycoonFlightPolicy(TycoonSettings.Flight settings) {
        this.settings = settings;
    }

    @Override
    public boolean canFly(UUID playerId, Tycoon tycoon, boolean owner, boolean trustedMember) {
        return settings.enabled() && (owner || (settings.allowMembers() && trustedMember));
    }
}
