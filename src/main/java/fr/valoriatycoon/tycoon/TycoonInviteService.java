package fr.valoriatycoon.tycoon;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Short-lived in-memory invitations; acceptance still uses an authoritative database transaction. */
public final class TycoonInviteService {
    private final Duration expiration;
    private final Map<InviteKey, Long> invitations = new HashMap<>();

    public TycoonInviteService(Duration expiration) {
        this.expiration = expiration;
    }

    public void invite(UUID ownerId, UUID invitedId) {
        invitations.put(new InviteKey(ownerId, invitedId), System.currentTimeMillis() + expiration.toMillis());
    }

    public boolean consume(UUID ownerId, UUID invitedId) {
        InviteKey key = new InviteKey(ownerId, invitedId);
        Long deadline = invitations.remove(key);
        return deadline != null && deadline >= System.currentTimeMillis();
    }

    public void clearPlayer(UUID playerId) {
        invitations.keySet().removeIf(key -> key.ownerId.equals(playerId) || key.invitedId.equals(playerId));
    }

    private record InviteKey(UUID ownerId, UUID invitedId) {
    }
}
