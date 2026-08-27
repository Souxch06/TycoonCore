/*
 * Décompilé avec CFR 0.152.
 * 
 * Impossible de charger les classes suivantes :
 *  org.bukkit.scheduler.BukkitRunnable
 */
package xyz.arcadiadevs.gensplus.tasks;

import java.io.FileWriter;
import java.io.IOException;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.arcadiadevs.gensplus.GensPlus;

public class DataSaveTask
extends BukkitRunnable {
    private final GensPlus instance;

    public DataSaveTask(GensPlus gensPlus) {
        this.instance = gensPlus;
    }

    public void run() {
        this.saveBlockDataToJson();
        this.saveWandDataToJson();
        this.savePlayerDataToJson();
    }

    public void saveBlockDataToJson() {
        try {
            Throwable throwable = null;
            Object var2_4 = null;
            try (FileWriter fileWriter = new FileWriter(String.valueOf(this.instance.getDataFolder()) + "/data/block_data.json");){
                this.instance.getGson().toJson(this.instance.getLocationsData().locations(), (Appendable)fileWriter);
            }
            catch (Throwable throwable2) {
                if (throwable == null) {
                    throwable = throwable2;
                } else if (throwable != throwable2) {
                    throwable.addSuppressed(throwable2);
                }
                throw throwable;
            }
        }
        catch (IOException iOException) {
            iOException.printStackTrace();
        }
    }

    public void saveWandDataToJson() {
        try {
            Throwable throwable = null;
            Object var2_4 = null;
            try (FileWriter fileWriter = new FileWriter(String.valueOf(this.instance.getDataFolder()) + "/data/wands_data.json");){
                this.instance.getGson().toJson(this.instance.getWandData().wands(), (Appendable)fileWriter);
            }
            catch (Throwable throwable2) {
                if (throwable == null) {
                    throwable = throwable2;
                } else if (throwable != throwable2) {
                    throwable.addSuppressed(throwable2);
                }
                throw throwable;
            }
        }
        catch (IOException iOException) {
            iOException.printStackTrace();
        }
    }

    public void savePlayerDataToJson() {
        try {
            Throwable throwable = null;
            Object var2_4 = null;
            try (FileWriter fileWriter = new FileWriter(String.valueOf(this.instance.getDataFolder()) + "/data/player_data.json");){
                this.instance.getGson().toJson(this.instance.getPlayerData().data(), (Appendable)fileWriter);
            }
            catch (Throwable throwable2) {
                if (throwable == null) {
                    throwable = throwable2;
                } else if (throwable != throwable2) {
                    throwable.addSuppressed(throwable2);
                }
                throw throwable;
            }
        }
        catch (IOException iOException) {
            iOException.printStackTrace();
        }
    }
}

