package de.redstoneworld.redcommandsystem;

import org.bukkit.command.CommandMap;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

public class RedCommandManager {
    private final RedCommandSystem plugin;
    private CommandMap bukkitCommandMap;

    private Map<String, RedCommand> commandMap = new LinkedHashMap<>();

    public RedCommandManager(RedCommandSystem plugin) throws NoSuchFieldException, IllegalAccessException {
        this.plugin = plugin;

        Field commandMapField = plugin.getServer().getClass().getDeclaredField("commandMap");
        commandMapField.setAccessible(true);
        bukkitCommandMap = (CommandMap) commandMapField.get(plugin.getServer());
    }

    public void register(final RedCommand command) {
        bukkitCommandMap.register(plugin.getName().toLowerCase(), command);
        commandMap.put(command.getName().toLowerCase(), command);
    }

    public Map<String, RedCommand> getCommands() {
        return commandMap;
    }

    public void destroy() {
        for (RedCommand command : commandMap.values()) {
            command.unregister(bukkitCommandMap);
        }
    }
}
