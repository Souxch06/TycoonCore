package fr.valoriatycoon.tycoon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConfiguredTycoonFlightPolicyTest {
    @Test
    void supportsOwnerAndOptionalMemberFlight() {
        TycoonFlightAccessPolicy ownersAndMembers = new ConfiguredTycoonFlightPolicy(
                new TycoonSettings.Flight(true, true, 32)
        );
        assertTrue(ownersAndMembers.canFly(UUID.randomUUID(), null, true, false));
        assertTrue(ownersAndMembers.canFly(UUID.randomUUID(), null, false, true));

        TycoonFlightAccessPolicy ownersOnly = new ConfiguredTycoonFlightPolicy(
                new TycoonSettings.Flight(true, false, 32)
        );
        assertFalse(ownersOnly.canFly(UUID.randomUUID(), null, false, true));
    }
}
