package xyz.arcadiadevs.valoriatycoon.tasks;

import java.io.FileWriter;
import java.io.IOException;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.arcadiadevs.valoriatycoon.ValoriaTycoon;

public class DataSaveTask
extends BukkitRunnable {
    private final ValoriaTycoon instance;

    public DataSaveTask(ValoriaTycoon valoriaTycoon) {
        this.instance = valoriaTycoon;
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

