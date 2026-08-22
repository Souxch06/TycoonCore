package fr.valoriatycoon.professions;

import fr.valoriatycoon.tools.ToolType;
import java.util.Locale;

/** Permanent profession associated with one form of the multi-tool. */
public enum ProfessionType {
    MINER(ToolType.PICKAXE, "Mineur"),
    LUMBERJACK(ToolType.AXE, "Bûcheron"),
    FARMER(ToolType.HOE, "Farmer"),
    FISHER(ToolType.FISHING_ROD, "Pêcheur");

    private final ToolType toolType;
    private final String displayName;

    ProfessionType(ToolType toolType, String displayName) {
        this.toolType = toolType;
        this.displayName = displayName;
    }

    /** Tool whose validated actions progress this profession. */
    public ToolType toolType() {
        return toolType;
    }

    /** Built-in French display name. */
    public String displayName() {
        return displayName;
    }

    /** Stable lowercase value stored in SQLite and YAML. */
    public String storageKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Resolves the permanent profession associated with a multi-tool form. */
    public static ProfessionType fromTool(ToolType toolType) {
        return switch (toolType) {
            case PICKAXE -> MINER;
            case HOE -> FARMER;
            case AXE -> LUMBERJACK;
            case FISHING_ROD -> FISHER;
        };
    }
}
