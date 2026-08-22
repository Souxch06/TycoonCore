package fr.valoriatycoon.tools;

import java.util.UUID;

/** Cache and batch key for one player's tool category. */
public record ToolProfileKey(UUID playerId, ToolType toolType) {
}
