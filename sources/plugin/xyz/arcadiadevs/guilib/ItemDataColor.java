package xyz.arcadiadevs.guilib;

public enum ItemDataColor {
    WHITE(0),
    ORANGE(1),
    MAGENTA(2),
    LIGHT_BLUE(3),
    YELLOW(4),
    LIME(5),
    PINK(6),
    GRAY(7),
    LIGHT_GRAY(8),
    CYAN(9),
    PURPLE(10),
    BLUE(11),
    BROWN(12),
    GREEN(13),
    RED(14),
    BLACK(15);

    private final short value;

    private ItemDataColor(short s) {
        this.value = s;
    }

    public short getValue() {
        return this.value;
    }

    public static ItemDataColor getByValue(short s) {
        ItemDataColor[] itemDataColorArray;
        for (ItemDataColor itemDataColor : itemDataColorArray = ItemDataColor.values()) {
            if (s != itemDataColor.value) continue;
            return itemDataColor;
        }
        return null;
    }
}

