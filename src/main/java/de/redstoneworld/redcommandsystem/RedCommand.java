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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
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

    private Map<String, CachedPosition> cachedPositions = new HashMap<>();

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
            plugin.sendMessage(sender, "nopermission", "permission", getPermission());
            return true;
        }
        if (args.length == 0 || args.length == 1 && "help".equalsIgnoreCase(args[0])) {
            showHelp(sender);
            return true;
        } else if (args.length == 4 && "setpos".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission(getPermission() + ".setpos")) {
                plugin.sendMessage(sender, "nopermission", "permission", getPermission() + ".setpos");
                return true;
            }

            // Get the start location for relative coordinates. When run from the console it's 0,0,0
            String worldName = plugin.getServer().getWorlds().get(0).getName();
            double[] senderCoords = {0, 0, 0};
            float yaw = 0;
            float pitch = 0;
            if (sender instanceof Entity) {
                worldName = ((Entity) sender).getLocation().getWorld().getName();
                senderCoords[0] = ((Entity) sender).getLocation().getX();
                senderCoords[1] = ((Entity) sender).getLocation().getY();
                senderCoords[2] = ((Entity) sender).getLocation().getZ();
                if (sender instanceof LivingEntity) {
                    yaw = ((LivingEntity) sender).getEyeLocation().getYaw();
                    pitch = ((LivingEntity) sender).getEyeLocation().getPitch();
                } else {
                    yaw = ((Entity) sender).getLocation().getYaw();
                    pitch = ((Entity) sender).getLocation().getPitch();
                }
            } else if (sender instanceof BlockCommandSender) {
                worldName = ((BlockCommandSender) sender).getBlock().getWorld().getName();
                senderCoords[0] = ((BlockCommandSender) sender).getBlock().getLocation().getX();
                senderCoords[1] = ((BlockCommandSender) sender).getBlock().getLocation().getY();
                senderCoords[2] = ((BlockCommandSender) sender).getBlock().getLocation().getZ();
            }

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
                } catch (NumberFormatException e) {
                    plugin.sendMessage(sender, "invalidnumber", "input", arg);
                    return true;
                }
            }
            cachedPositions.put(sender.getName(), new CachedPosition(worldName, senderCoords, Arrays.copyOfRange(args, 1, 4), yaw, pitch));
            plugin.sendMessage(sender, "cachedposition", "position", args[0] + " " + args[1] + " " + args[2]);
            return true;
        }

        // Get world the sender is in, console uses the default/first world
        World world = plugin.getServer().getWorlds().get(0);
        float senderYaw = 0;
        float senderPitch = 0;
        if (sender instanceof Entity) {
            world = ((Entity) sender).getWorld();
            if (sender instanceof LivingEntity) {
                senderYaw = ((LivingEntity) sender).getEyeLocation().getYaw();
                senderPitch = ((LivingEntity) sender).getEyeLocation().getPitch();
            } else {
                senderYaw = ((Entity) sender).getLocation().getYaw();
                senderPitch = ((Entity) sender).getLocation().getPitch();
            }
        } else if (sender instanceof BlockCommandSender) {
            world = ((BlockCommandSender) sender).getBlock().getWorld();
        }
        String senderWorld = world.getName();

        // Pass the inputted strings directly to the command
        String[] coordsStr = new String[3];
        int presetIndex = -1;

        float posYaw = senderYaw;
        float posPitch = senderPitch;

        // Get the start location for relative coordinates. When run from the console it's 0,0,0
        double[] senderCoords = {0, 0, 0};
        if (sender instanceof Entity) {
            senderCoords[0] = ((Entity) sender).getLocation().getX();
            senderCoords[1] = ((Entity) sender).getLocation().getY();
            senderCoords[2] = ((Entity) sender).getLocation().getZ();
        } else if (sender instanceof BlockCommandSender) {
            senderCoords[0] = ((BlockCommandSender) sender).getBlock().getLocation().getX();
            senderCoords[1] = ((BlockCommandSender) sender).getBlock().getLocation().getY();
            senderCoords[2] = ((BlockCommandSender) sender).getBlock().getLocation().getZ();
        }

        double[] originalSenderCoords = Arrays.copyOf(senderCoords, 3);

        if (args.length == 4){
            coordsStr = Arrays.copyOf(args, 3);
            presetIndex = 3;
        } else if (args.length > 1 && ("position".equalsIgnoreCase(args[0]) || "-p".equalsIgnoreCase(args[0]))) {
            CachedPosition position = cachedPositions.get(sender.getName());
            if (position == null) {
                plugin.sendMessage(sender, "noposition", "command", label);
                return true;
            }
            if (!world.getName().equals(position.getWorld()) && !sender.hasPermission(getPermission() + ".position.otherworld")) {
                plugin.sendMessage(sender, "nopermission", "permission", getPermission() + ".position.otherworld");
                return true;
            }
            originalSenderCoords = position.getCoordinates();
            world = plugin.getServer().getWorld(position.getWorld());
            coordsStr[0] = String.valueOf(position.getCoordinateInput()[0]);
            coordsStr[1] = String.valueOf(position.getCoordinateInput()[1]);
            coordsStr[2] = String.valueOf(position.getCoordinateInput()[2]);
            posYaw = position.getYaw();
            posPitch = position.getPitch();
            presetIndex = 1;
        }

        if (presetIndex == -1) {
            showHelp(sender);
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

        double[] targetCoords = new double[3];
        // Parse the position strings to generate a location later on and check if they are valid beforehand
        for (int i = 0; i < 3; i++) {
            try {
                if (coordsStr[i].startsWith("~")) {
                    String numberStr = coordsStr[i].substring(1);
                    if (numberStr.length() > 0) {
                        targetCoords[i] = senderCoords[i] + Double.parseDouble(numberStr.startsWith(".") ? "0" + numberStr : numberStr);
                    }
                } else {
                    targetCoords[i] = Double.parseDouble(coordsStr[i]);
                }
            } catch (NumberFormatException e) {
                plugin.sendMessage(sender, "invalidnumber", "input", coordsStr[i]);
                return true;
            }
        }

        // Make sure chunk is loaded but don't generate it
        Location loc = new Location(world, targetCoords[0], targetCoords[1], targetCoords[2]);
        if (!loc.getChunk().isLoaded()) {
            loc.getChunk().load(false);
        }

        // Build the to be executed command
        String command = plugin.translate(getExecuteCommand(),
                "preset", preset,
                "position", coordsStr[0] + " " + coordsStr[1] + " " + coordsStr[2],
                "possenderx", String.valueOf(Math.floor(originalSenderCoords[0])),
                "possendery", String.valueOf(Math.floor(originalSenderCoords[1])),
                "possenderz", String.valueOf(Math.floor(originalSenderCoords[2])),
                "possenderexactx", String.valueOf(originalSenderCoords[0]),
                "possenderexacty", String.valueOf(originalSenderCoords[1]),
                "possenderexactz", String.valueOf(originalSenderCoords[2]),
                "world", world.getName(),
                "x", String.valueOf(Math.floor(targetCoords[0])),
                "y", String.valueOf(Math.floor(targetCoords[1])),
                "z", String.valueOf(Math.floor(targetCoords[2])),
                "exactx", String.valueOf(targetCoords[0]),
                "exacty", String.valueOf(targetCoords[1]),
                "exactz", String.valueOf(targetCoords[2]),
                "yaw", String.valueOf(Math.floor(posYaw)),
                "pitch", String.valueOf(Math.floor(posPitch)),
                "exactyaw", String.valueOf(posYaw),
                "exactpitch", String.valueOf(posPitch),
                "senderworld", senderWorld,
                "senderx", String.valueOf(Math.floor(senderCoords[0])),
                "sendery", String.valueOf(Math.floor(senderCoords[1])),
                "senderz", String.valueOf(Math.floor(senderCoords[2])),
                "senderexactx", String.valueOf(senderCoords[0]),
                "senderexacty", String.valueOf(senderCoords[1]),
                "senderexactz", String.valueOf(senderCoords[2]),
                "senderyaw", String.valueOf(Math.floor(senderYaw)),
                "senderpitch", String.valueOf(Math.floor(senderPitch)),
                "senderexactyaw", String.valueOf(senderYaw),
                "senderexactpitch", String.valueOf(senderPitch),
                "sender", sender.getName()
        );
        if (sender instanceof Player) {
            command = command.replace("%player%", sender.getName());
        }

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

    private void showHelp(CommandSender sender) {
        sender.sendMessage(plugin.getPrefix() + " " + getName() + " Help:");
        sender.sendMessage(ChatColor.RED + "/" + getName() + " " + getSyntax().replace("<position>", "<x> <y> <z>"));
        sender.sendMessage(ChatColor.GRAY + " Execute a preset at a certain position");
        sender.sendMessage(ChatColor.RED + "/" + getName() + " setpos <x> <y> <z>");
        sender.sendMessage(ChatColor.GRAY + " Store position to later be used in /" + getName() + " position <name>. Data is stored until server restart or plugin reload!");
        sender.sendMessage(ChatColor.RED + "/" + getName() + " " + getSyntax().replace("<position>", "position"));
        sender.sendMessage(ChatColor.GRAY + " Execute command preset with stored position");
        sender.sendMessage(ChatColor.RED + "/" + getName() + " help");
        sender.sendMessage(ChatColor.GRAY + " Show this help");
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

    public Map<String, CachedPosition> getCachedPositions() {
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
