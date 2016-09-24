package de.redstoneworld.redsetblock;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.permissions.PermissionAttachment;

public class RedSetBlockCommand implements CommandExecutor {
    private final RedSetBlock plugin;

    public RedSetBlockCommand(RedSetBlock plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            if ("reload".equalsIgnoreCase(args[0]) && sender.hasPermission("rwm.setblock.cmd.reload")) {
                plugin.loadConfig();
                sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + " Config reloaded!");
                return true;
            } else if ("list".equalsIgnoreCase(args[0]) && sender.hasPermission("rwm.setblock.cmd.list")) {
                if (plugin.getBlockConfig().size() > 0) {
                    sender.sendMessage(ChatColor.DARK_RED + "Blocks configured:");
                    for (String configName : plugin.getBlockConfig().keySet()) {
                        sender.sendMessage(ChatColor.GRAY + "> " + ChatColor.WHITE + configName);
                    }
                    sender.sendMessage(ChatColor.RED + "Apply one by running /" + label + " <x> <y> <z> <name>");
                    sender.sendMessage(ChatColor.RED + "Get more info by running /" + label + " info <name>");
                } else {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + " No block data configured!");
                }
                return true;
            }
        } else if (args.length == 2) {
            if ("info".equalsIgnoreCase(args[0]) && sender.hasPermission("rwm.setblock.cmd.info")) {
                // Get the blockdata
                String blockData = plugin.getBlockData(args[1]);
                if (blockData == null) {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + " '" + args[1] + "' does not exist in the block data config?");
                    return true;
                }
                sender.sendMessage(plugin.getPrefix() + ChatColor.WHITE + " " + args[1] + ChatColor.DARK_RED + " - " + ChatColor.WHITE + blockData);
                return true;
            }
        }

        if (args.length < 4) {
            return false;
        }

        // Get world the sender is in, console uses the default/first world
        World world = plugin.getServer().getWorlds().get(0);
        if (sender instanceof Entity) {
            world = ((Entity) sender).getWorld();
        } else if (sender instanceof BlockCommandSender) {
            world = ((BlockCommandSender) sender).getBlock().getWorld();
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

        // Pass the inputted strings directly to the blockdata command
        String[] coordsStr = new String[3];

        // Parse the position strings to generate a location later on and check if they are valid beforehand
        for (int i = 0; i < 3; i++) {
            try {
                if (args[i].startsWith("~")) {
                    String numberStr = args[i].substring(1);
                    if (numberStr.length() > 0) {
                        coordsAbs[i] += Double.parseDouble(numberStr.startsWith(".") ? "0" + numberStr : numberStr);
                    }
                } else {
                    coordsAbs[i] = Double.parseDouble(args[i]);
                }

                coordsStr[i] = args[i];
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getPrefix() + ChatColor.RED + " '" + args[i] + "' is not a valid coordinate number!");
                return false;
            }
        }

        // Make sure chunk is loaded but don't generate it
        Location loc = new Location(world, coordsAbs[0], coordsAbs[1], coordsAbs[2]);
        if (!loc.getChunk().isLoaded()) {
            loc.getChunk().load(false);
        }

        // Get the blockdata
        String blockData = plugin.getBlockData(args[3]);
        if (blockData == null) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + " '" + args[3] + "' does not exist in the block data config?");
            return true;
        }

        // Build the blockdata command
        String command = "blockdata " + coordsStr[0] + " " + coordsStr[1] + " " + coordsStr[2] + " " + blockData;

        // Temporally add permission to execute blockdata
        PermissionAttachment permAtt = sender.addAttachment(plugin, "minecraft.command.blockdata", true);
        // Dispatch the command
        if (plugin.getServer().dispatchCommand(sender, command)) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + " Set block data to '" + args[3] + "'!");
        } else {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + " Error while setting block data to '" + args[3] + "'!");
        }
        // Remove permission again
        permAtt.remove();
        return true;
    }
}
