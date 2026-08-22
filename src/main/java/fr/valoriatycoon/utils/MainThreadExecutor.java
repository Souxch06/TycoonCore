package fr.valoriatycoon.utils;

import java.util.Objects;
import java.util.concurrent.Executor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/** Dispatches completion handlers safely to the Bukkit primary thread. */
public final class MainThreadExecutor implements Executor {
    private final JavaPlugin plugin;

    public MainThreadExecutor(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public void execute(Runnable command) {
        if (!plugin.isEnabled()) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            command.run();
        } else {
            plugin.getServer().getScheduler().runTask(plugin, command);
        }
    }
}
