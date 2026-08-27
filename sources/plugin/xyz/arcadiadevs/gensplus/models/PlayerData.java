/*
 * Décompilé avec CFR 0.152.
 * 
 * Impossible de charger les classes suivantes :
 *  lombok.Generated
 */
package xyz.arcadiadevs.gensplus.models;

import java.util.List;
import java.util.UUID;
import lombok.Generated;

public record PlayerData(List<Data> data) {
    public Data getData(UUID uUID) {
        return this.data.stream().filter(data -> data.uuid.equals(uUID)).findFirst().orElse(null);
    }

    public void create(UUID uUID, int n) {
        Data data = new Data(uUID, n);
        this.data.add(data);
    }

    public static class Data {
        private UUID uuid;
        private int limit;

        public static void addToLimit(Data data, int n) {
            data.limit += n;
        }

        public String toString() {
            return "Data(uuid=" + String.valueOf(this.getUuid()) + ", limit=" + this.getLimit() + ")";
        }

        @Generated
        public void setLimit(int n) {
            this.limit = n;
        }

        @Generated
        public Data(UUID uUID, int n) {
            this.uuid = uUID;
            this.limit = n;
        }

        @Generated
        public UUID getUuid() {
            return this.uuid;
        }

        @Generated
        public int getLimit() {
            return this.limit;
        }
    }
}

