package fr.valoriatycoon.crates;

import fr.valoriatycoon.tools.ToolType;
import java.util.EnumMap;
import java.util.Map;

/** Atomic reward-token result plus authoritative balances requiring main-thread cache synchronization. */
public record CrateClaimResult(
        CrateClaimStatus status,
        CrateReward reward,
        long resultingMoneyCents,
        Map<ToolType, Long> resultingToolCoins
) {
    public CrateClaimResult {
        EnumMap<ToolType, Long> copy = new EnumMap<>(ToolType.class);
        if (resultingToolCoins != null) {
            copy.putAll(resultingToolCoins);
        }
        resultingToolCoins = Map.copyOf(copy);
    }

    public boolean successful() {
        return status == CrateClaimStatus.SUCCESS && reward != null;
    }
}
