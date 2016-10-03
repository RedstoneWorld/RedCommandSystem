package de.redstoneworld.redcommandsystem;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RedCommand extends Command implements PluginIdentifiableCommand {
    private final RedCommandSystem plugin;

    private final String syntax;

    private final String executeCommand;
    private final boolean executeOutput;
    private final List<String> executePermissions;

    private final boolean presetPermissions;
    private final Map<String, String> presets = new LinkedHashMap<>();

    private Map<String, String[]> cachedPositions = new HashMap<>();

    public RedCommand(RedCommandSystem plugin, String name, String syntax, List<String> aliases, String permission, String executeCommand, List<String> executePermissions, boolean presetPermissions, boolean executeOutput) {
        super(name, executeCommand, "/" + name + " " + syntax.replace("<position>", "<x> <y> <z>"), aliases);
        this.plugin = plugin;
        this.syntax = syntax;
        this.setPermission(permission);
        this.executeCommand = executeCommand;
        this.executeOutput = executeOutput;
        this.executePermissions = executePermissions;
        this.presetPermissions = presetPermissions;

        ConfigurationSection presets = plugin.getCommandsConfig().getConfigurationSection(getName() + ".presets");
        for (String key : presets.getKeys(false)) {
            addPreset(key, presets.getString(key));
        }
    }

    public RedCommand(RedCommandSystem plugin, ConfigurationSection section) {
        this(
                plugin,
                section.getName(),
                section.getString("syntax"),
                section.getStringList("aliases"),
                section.getString("permission"),
                section.getString("execute.command"),
                section.getStringList("execute.permissions"),
                section.getBoolean("presetpermissions"),
                section.getBoolean("execute.output")
        );
    }

    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(getPermission())) {
            sender.sendMessage("You don't have the permission " + getPermission());
            return true;
        }
        if (args.length == 0 || args.length == 1 && "help".equalsIgnoreCase(args[0])) {
            showHelp(sender, label);
            return true;
        } else if (args.length == 4 && "setpos".equalsIgnoreCase(args[0])) {
            String[] coordsStr = new String[3];
            for (int i = 0; i < 3; i++) {
                String arg = args[i + 1];
                try {
                    if (arg.startsWith("~")) {
                        String numberStr = arg.substring(1);
                        if (numberStr.length() > 0) {
                            Double.parseDouble(numberStr.startsWith(".") ? "0" + numberStr : numberStr);
                        }
                    } else {
                        Double.parseDouble(arg);
                    }

                    coordsStr[i] = arg;
                } catch (NumberFormatException e) {
                    plugin.sendMessage(sender, "invalidnumber", "input", arg);
                    return true;
                }
            }
            getCachedPositions().put(sender.getName(), coordsStr);
            plugin.sendMessage(sender, "cachedposition", "position", coordsStr[0] + " " + coordsStr[1] + " " + coordsStr[2]);
            return true;
        }

        // Pass the inputted strings directly to the command
        String[] coordsStr = new String[3];
        int presetIndex = -1;

        if (args.length == 4){
            System.arraycopy(args, 0, coordsStr, 0, 3);
            presetIndex = 3;
        }else if (args.length > 1 && ("position".equalsIgnoreCase(args[0]) || "-p".equalsIgnoreCase(args[0]))) {
            coordsStr = getCachedPositions().get(sender.getName());
            if (coordsStr == null) {
                plugin.sendMessage(sender, "noposition", "command", label);
                return true;
            }
            presetIndex = 1;
        }

        if (presetIndex == -1) {
            showHelp(sender, label);
            return true;
        }

        // Get the configured preset string
        String preset = getPreset(args[presetIndex]);
        if (preset == null) {
            plugin.sendMessage(sender, "presetnotfound", "preset", args[presetIndex], "command", getName());
            return true;
        }

        if (perPresetPermissions() && !sender.hasPermission(getPermission() + "." + preset.toLowerCase())) {
            plugin.sendMessage(sender, "nopresetpermission", "preset", args[presetIndex], "command", getName());
            return true;
        }

        // Get the start location for relative coordinates. When run from the console it's 0,0,0
        double[] coordsAbs = {0, 0, 0};
        if (sender instanceof Entity) {
            coordsAbs[0] = ((Entity) sender).getLocation().getX();
            coordsAbs[1] = ((Entity) sender).getLocation().getY();
            coordsAbs[2] = ((Entity) sender).getLocation().getZ();
        } else if (sender instanceof BlockCommandSender) {
            coordsAbs[0] = ((BlockCommandSender) sender).getBlock().getLocation().getX();
            coordsAbs[1] = ((BlockCommandSender) sender).getBlock().getLocation().getY();
            coordsAbs[2] = ((BlockCommandSender) sender).getBlock().getLocation().getZ();
        }

        // Parse the position strings to generate a location later on and check if they are valid beforehand
        for (int i = 0; i < 3; i++) {
            try {
                if (coordsStr[i].startsWith("~")) {
                    String numberStr = coordsStr[i].substring(1);
                    if (numberStr.length() > 0) {
                        coordsAbs[i] += Double.parseDouble(numberStr.startsWith(".") ? "0" + numberStr : numberStr);
                    }
                } else {
                    coordsAbs[i] = Double.parseDouble(coordsStr[i]);
                }
            } catch (NumberFormatException e) {
                plugin.sendMessage(sender, "invalidnumber", "input", coordsStr[i]);
                return true;
            }
        }

        // Get world the sender is in, console uses the default/first world
        World world = plugin.getServer().getWorlds().get(0);
        if (sender instanceof Entity) {
            world = ((Entity) sender).getWorld();
        } else if (sender instanceof BlockCommandSender) {
            world = ((BlockCommandSender) sender).getBlock().getWorld();
        }

        // Make sure chunk is loaded but don't generate it
        Location loc = new Location(world, coordsAbs[0], coordsAbs[1], coordsAbs[2]);
        if (!loc.getChunk().isLoaded()) {
            loc.getChunk().load(false);
        }

        // Build the to be executed command
        String command = plugin.translate(getExecuteCommand(), "preset", preset, "position", coordsStr[0] + " " + coordsStr[1] + " " + coordsStr[2]);

        // Temporally add permission to execute blockdata
        PermissionAttachment permAtt = sender.addAttachment(plugin);
        for (String perm : getExecutePermissions()) {
            permAtt.setPermission(perm, !perm.startsWith("-"));
        }

        String sendCommandFeedback = world.getGameRuleValue("sendCommandFeedback");
        world.setGameRuleValue("sendCommandFeedback", String.valueOf(!(sender instanceof Player) || showExecuteOutput()));
        // Dispatch the command
        if (plugin.getServer().dispatchCommand(sender, command)) {
            plugin.sendMessage(sender, "command.success", "command", getName(), "preset", args[presetIndex]);
        } else {
            plugin.sendMessage(sender, "command.failure", "command", getName(), "preset", args[presetIndex]);
        }
        world.setGameRuleValue("sendCommandFeedback", sendCommandFeedback);
        // Remove permission again
        permAtt.remove();
        return true;
    }

    private void showHelp(CommandSender sender, String label) {
        sender.sendMessage(plugin.getPrefix() + " Commands:");
        sender.sendMessage(ChatColor.RED + "/" + label + " " + getSyntax()
                + ChatColor.GRAY + " Execute a preset at a certain position");
        sender.sendMessage(ChatColor.RED + "/" + label + " setpos <x> <y> <z>"
                + ChatColor.GRAY + " Store position to later be used in /" + label + " position <name>. Data is stored until server restart!");
        sender.sendMessage(ChatColor.RED + "/" + label + " " + getSyntax().replace("<position>", "position")
                + ChatColor.GRAY + " Execute command preset with stored position");
        sender.sendMessage(ChatColor.RED + "/" + label + " help"
                + ChatColor.GRAY + " Show this help");
    }

    public String getSyntax() {
        return syntax;
    }

    public String getExecuteCommand() {
        return executeCommand;
    }

    public boolean showExecuteOutput() {
        return executeOutput;
    }

    public List<String> getExecutePermissions() {
        return executePermissions;
    }

    public boolean perPresetPermissions() {
        return presetPermissions;
    }

    public String getPreset(String configName) {
        return presets.get(configName.toLowerCase());
    }

    public Map<String, String> getPresets() {
        return presets;
    }

    public Map<String, String[]> getCachedPositions() {
        return cachedPositions;
    }

    private void addPreset(String name, String preset) {
        presets.put(name.toLowerCase(), preset);
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }
}
