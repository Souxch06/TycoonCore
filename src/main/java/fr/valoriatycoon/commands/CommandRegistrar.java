package fr.valoriatycoon.commands;

import java.util.Objects;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

/** Registers plugin.yml commands and fails startup when metadata is inconsistent. */
public final class CommandRegistrar {
    private final JavaPlugin plugin;

    public CommandRegistrar(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void register(String name, CommandExecutor executor) {
        PluginCommand command = Objects.requireNonNull(
                plugin.getCommand(name),
                "Command is missing from plugin.yml: " + name
        );
        command.setExecutor(executor);
        if (executor instanceof TabCompleter completer) {
            command.setTabCompleter(completer);
        }
    }
}
