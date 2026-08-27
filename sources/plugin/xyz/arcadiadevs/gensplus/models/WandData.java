/*
 * Décompilé avec CFR 0.152.
 * 
 * Impossible de charger les classes suivantes :
 *  lombok.Generated
 */
package xyz.arcadiadevs.gensplus.models;

import com.awaitquality.api.spigot.chat.formatter.Formattable;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import lombok.Generated;
import xyz.arcadiadevs.gensplus.GensPlus;
import xyz.arcadiadevs.gensplus.utils.config.Config;

public record WandData(List<Wand> wands) {
    public Wand getWand(UUID uUID) {
        return this.wands.stream().filter(wand -> wand.getUuid().equals(uUID)).findFirst().orElse(null);
    }

    public Wand create(Wand.WandType wandType, int n, double d) {
        Wand wand = new Wand(UUID.randomUUID(), wandType, n, d, 0L, 0L);
        this.wands.add(wand);
        return wand;
    }

    public Wand remove(UUID uUID) {
        Wand wand = this.getWand(uUID);
        this.wands.remove(wand);
        return wand;
    }

    public static class Wand
    implements Formattable {
        private UUID uuid;
        private WandType type;
        private int uses;
        private double multiplier;
        private long totalEarned;
        private long totalItemsSold;

        @Override
        public HashMap<String, String> getPlaceHolders() {
            HashMap<String, String> hashMap = new HashMap<String, String>();
            hashMap.put("%uses%", this.uses == -1 ? Config.SELL_WAND_UNLIMITED_USES_PREFIX.getString() : String.valueOf(this.uses));
            hashMap.put("%multiplier%", String.valueOf(this.multiplier));
            hashMap.put("%total_earned%", GensPlus.getInstance().getEcon().format((double)this.totalEarned));
            hashMap.put("%total_items_sold%", String.valueOf(this.totalItemsSold));
            return hashMap;
        }

        @Generated
        public Wand(UUID uUID, WandType wandType, int n, double d, long l, long l2) {
            this.uuid = uUID;
            this.type = wandType;
            this.uses = n;
            this.multiplier = d;
            this.totalEarned = l;
            this.totalItemsSold = l2;
        }

        @Generated
        public UUID getUuid() {
            return this.uuid;
        }

        @Generated
        public WandType getType() {
            return this.type;
        }

        @Generated
        public int getUses() {
            return this.uses;
        }

        @Generated
        public double getMultiplier() {
            return this.multiplier;
        }

        @Generated
        public long getTotalEarned() {
            return this.totalEarned;
        }

        @Generated
        public long getTotalItemsSold() {
            return this.totalItemsSold;
        }

        @Generated
        public void setUuid(UUID uUID) {
            this.uuid = uUID;
        }

        @Generated
        public void setType(WandType wandType) {
            this.type = wandType;
        }

        @Generated
        public void setUses(int n) {
            this.uses = n;
        }

        @Generated
        public void setMultiplier(double d) {
            this.multiplier = d;
        }

        @Generated
        public void setTotalEarned(long l) {
            this.totalEarned = l;
        }

        @Generated
        public void setTotalItemsSold(long l) {
            this.totalItemsSold = l;
        }

        public static enum WandType {
            SELL_WAND;

        }
    }
}

