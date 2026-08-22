package fr.valoriatycoon.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class MultiToolItemServiceTest {

    @Test
    void derivesOneStableIdentityPerPlayerAcrossEveryForm() {
        UUID owner = UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID other = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

        UUID first = MultiToolItemService.identityFor(owner);

        assertEquals(first, MultiToolItemService.identityFor(owner));
        assertNotEquals(first, MultiToolItemService.identityFor(other));
    }
}
