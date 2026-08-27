/*
 * Decompiled with CFR 0.152.
 */
package xyz.arcadiadevs.gensplus.utils.config.objects;

import java.util.ArrayList;
import java.util.List;

public record GensPerLevel(int from, int to, int gain) {
    public boolean isIn(int n) {
        return n >= this.from && n <= this.to;
    }

    public static List<GensPerLevel> factory(ArrayList<String> arrayList) {
        ArrayList<GensPerLevel> arrayList2 = new ArrayList<GensPerLevel>();
        for (String string : arrayList) {
            String[] stringArray = string.split(":");
            arrayList2.add(new GensPerLevel(Integer.parseInt(stringArray[0]), Integer.parseInt(stringArray[1]), Integer.parseInt(stringArray[2])));
        }
        return arrayList2;
    }

    @Override
    public String toString() {
        return "GensPerLevel{from=" + this.from + ", to=" + this.to + ", gain=" + this.gain + "}";
    }
}

