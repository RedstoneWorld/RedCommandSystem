package de.redstoneworld.redcommandsystem;

import org.bukkit.command.CommandMap;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

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
        try {
            plugin.getServer().getPluginManager().addPermission(new Permission(command.getPermission(), command.getName() + " permission", PermissionDefault.OP));
            plugin.getServer().getPluginManager().addPermission(new Permission(command.getPermission() + ".setpos", command.getName() + " permission to set position", PermissionDefault.OP));
            plugin.getServer().getPluginManager().addPermission(new Permission(command.getPermission() + ".otherworld", command.getName() + " permission to set position in another world", PermissionDefault.OP));
            plugin.getServer().getPluginManager().addPermission(new Permission(command.getPermission() + ".position.otherworld", command.getName() + " permission to use a position from another world", PermissionDefault.OP));
            plugin.getServer().getPluginManager().addPermission(new Permission(command.getPermission() + ".position.manual", command.getName() + " permission to manually set the position", PermissionDefault.OP));
            plugin.getServer().getPluginManager().addPermission(new Permission(command.getPermission() + ".position.cached", command.getName() + " permission to use the cached position", PermissionDefault.OP));
        } catch (IllegalArgumentException e) {
            // permission is already registered
        }

        for (String preset : command.getPresets().keySet()) {
            try {
                plugin.getServer().getPluginManager().addPermission(new Permission(command.getPermission() + ".preset." + preset.toLowerCase(), command.getName() + " permission to use preset " + preset, PermissionDefault.OP));
            } catch (IllegalArgumentException e) {
                // permission is already registered
            }
        }
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
