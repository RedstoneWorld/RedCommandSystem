package de.redstoneworld.redcommandsystem;

import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.List;

/**
 * Created by Max on 08.04.2017.
 */
public class RedCommandExecutor {
    private final RedCommandSystem plugin;
    private final List<String> commands;
    private final boolean output;
    private final List<String> permissions;
    private final boolean runAsOp;
    private final boolean runAsConsole;

    public RedCommandExecutor(RedCommandSystem plugin, ConfigurationSection config) throws IllegalArgumentException {
        if (config == null) {
            throw new IllegalArgumentException("Config section is null?");
        }
        this.plugin = plugin;
        List<String> commands = config.getStringList("commands");
        if (config.contains("command")) {
            commands.add(config.getString("command"));
        }
        this.commands = commands;
        this.output = config.getBoolean("output", true);
        this.permissions = config.getStringList("permissions");
        this.runAsOp = config.getBoolean("run-as-op", false);
        this.runAsConsole = config.getBoolean("run-as-console", false);
    }

    public List<String> getCommands() {
        return commands;
    }

    public boolean showOutput() {
        return output;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public boolean runAsOp() {
        return runAsOp;
    }

    public boolean runAsConsole() {
        return runAsConsole;
    }

    public boolean execute(CommandSender sender, String preset, String[] coordsStr, double[] originalSenderCoords, String targetWorld, double[] targetCoords, float posYaw, float posPitch, double[] senderCoords, float senderYaw, float senderPitch) {
        boolean success = true;
        boolean wasOp = sender.isOp();
        // Temporally add permission to execute commands
        PermissionAttachment permAtt = sender.addAttachment(plugin);
        for (String perm : getPermissions()) {
            permAtt.setPermission(perm, !perm.startsWith("-"));
        }

        World senderWorld;
        if (sender instanceof Entity) {
            senderWorld = ((Entity) sender).getWorld();
        } else if (sender instanceof BlockCommandSender) {
            senderWorld = ((BlockCommandSender) sender).getBlock().getWorld();
        } else {
            senderWorld = sender.getServer().getWorlds().get(0);
        }
        String sendCommandFeedback = senderWorld.getGameRuleValue("sendCommandFeedback");
        senderWorld.setGameRuleValue("sendCommandFeedback", String.valueOf(!(sender instanceof Player) || showOutput()));

        try {
            if (runAsOp() && !wasOp) {
                sender.setOp(true);
            }
            // Dispatch the command
            for (String command : getCommands()) {
                command = addVariables(command,
                    sender,
                    preset,
                    coordsStr,
                    originalSenderCoords,
                    targetWorld,
                    targetCoords,
                    posYaw,
                    posPitch,
                    senderWorld.getName(),
                    senderCoords,
                    senderYaw,
                    senderPitch
                );
                success = plugin.getServer().dispatchCommand(runAsConsole() ? plugin.getServer().getConsoleSender() : sender, command) && success;
            }
            // Remove permission again
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        } finally {
            if (runAsOp() && !wasOp) {
                sender.setOp(false);
            }
            permAtt.remove();
            senderWorld.setGameRuleValue("sendCommandFeedback", sendCommandFeedback);
        }
        return success;
    }

    private String addVariables(String command, CommandSender sender, String preset, String[] coordsStr, double[] originalSenderCoords, String worldName, double[] targetCoords, float posYaw, float posPitch, String senderWorld, double[] senderCoords, float senderYaw, float senderPitch) {
        command = plugin.translate(command,
                "preset", preset,
                "position", coordsStr[0] + " " + coordsStr[1] + " " + coordsStr[2],
                "possenderx", String.valueOf(Math.floor(originalSenderCoords[0])),
                "possendery", String.valueOf(Math.floor(originalSenderCoords[1])),
                "possenderz", String.valueOf(Math.floor(originalSenderCoords[2])),
                "possenderexactx", String.valueOf(originalSenderCoords[0]),
                "possenderexacty", String.valueOf(originalSenderCoords[1]),
                "possenderexactz", String.valueOf(originalSenderCoords[2]),
                "world", worldName,
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
            command = plugin.translate(command, "player", sender.getName());
        }
        return command;
    }
}
