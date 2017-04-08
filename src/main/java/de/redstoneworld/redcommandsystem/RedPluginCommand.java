package de.redstoneworld.redcommandsystem;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RedPluginCommand implements CommandExecutor {
    private final RedCommandSystem plugin;

    public RedPluginCommand(RedCommandSystem plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            return showHelp(sender, label);
        } else if (args.length == 1) {
            if ("reload".equalsIgnoreCase(args[0]) && sender.hasPermission("rwm.commandsystem.cmd.reload")) {
                if (plugin.loadConfig()) {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + " Config reloaded!");
                } else {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + " Error while reloading config!");
                }
                return true;
            } else if ("list".equalsIgnoreCase(args[0]) && sender.hasPermission("rwm.commandsystem.cmd.list")) {
                if (plugin.getCommandManager().getCommands().size() > 0) {
                    sender.sendMessage(ChatColor.DARK_RED + "Commands configured:");
                    for (RedCommand command : plugin.getCommandManager().getCommands().values()) {
                        if (sender.hasPermission(command.getPermission())) {
                            sender.sendMessage(ChatColor.GRAY + "> /" + ChatColor.WHITE + command.getName() + ChatColor.GRAY + " (Presets: " + command.getPresets().size() + ")");
                        }
                    }
                    if (sender.hasPermission("rwm.setblock.cmd.info")) {
                        sender.sendMessage(ChatColor.RED + "Get more info by running /" + label + " info <name>");
                    }
                } else {
                    plugin.sendMessage(sender, "noblockdata", "name", args[1]);
                }
                return true;
            } else if ("help".equalsIgnoreCase(args[0])) {
                return showHelp(sender, label);
            }
        } else if (args.length == 2) {
            if ("info".equalsIgnoreCase(args[0]) && sender.hasPermission("rwm.commandsystem.cmd.info")) {
                // Get the blockdata
                RedCommand command = plugin.getCommandManager().getCommands().get(args[1].toLowerCase());
                if (command == null) {
                    plugin.sendMessage(sender, "commandnotfound", "name", args[1]);
                    return true;
                }
                sender.sendMessage(plugin.getPrefix() + " " + ChatColor.WHITE + command.getName() + ChatColor.DARK_RED + " Info:");
                sender.sendMessage(ChatColor.RED + " Aliases:");
                for (String alias : command.getAliases()) {
                    sender.sendMessage(ChatColor.RED + " > " + ChatColor.WHITE + alias);
                }
                if (command.getAliases().size() == 0) {
                    sender.sendMessage(ChatColor.WHITE + " No aliases configured!");
                }
                sender.sendMessage(ChatColor.RED + " Syntax: " + ChatColor.WHITE + command.getSyntax());
                sender.sendMessage(ChatColor.RED + " Permission: " + ChatColor.WHITE + command.getPermission());
                if (command.getWrongWorldCommands().size() > 0) {
                    sender.sendMessage(ChatColor.RED + " Wrong world commands: ");
                    for (String wrongWorldCommand : command.getWrongWorldCommands()) {
                        sender.sendMessage(ChatColor.WHITE + "  " + wrongWorldCommand);
                    }
                }
                sender.sendMessage(ChatColor.RED + " Commands to be executed: ");
                if (command.getWrongWorldCommands().size() > 0) {
                    for (String executeCommand : command.getExecuteCommands()) {
                        sender.sendMessage(ChatColor.WHITE + "  " + executeCommand);
                    }
                } else {
                    sender.sendMessage(ChatColor.WHITE + "  None?");
                }
                sender.sendMessage(ChatColor.RED + " Show command output: " + ChatColor.WHITE + command.showExecuteOutput());
                sender.sendMessage(ChatColor.RED + " Execute with the following additional permissions:");
                for (String perm : command.getExecutePermissions()) {
                    sender.sendMessage(ChatColor.RED + " > " + ChatColor.WHITE + perm);
                }
                if (command.getExecutePermissions().size() == 0) {
                    sender.sendMessage(ChatColor.WHITE + " Only uses the player's permissions and no special ones!");
                }
                sender.sendMessage(ChatColor.RED + " Check permissions for each preset (" + command.getPermission() + ".<name>): " + ChatColor.WHITE + command.perPresetPermissions());
                List<String> presets = new ArrayList<>();
                for (String preset : command.getPresets().keySet()) {
                    if (!command.perPresetPermissions() || sender.hasPermission(command.getPermission() + "." + preset.toLowerCase())) {
                        presets.add(preset);
                    }
                }
                sender.sendMessage(ChatColor.RED + " Presets: " + ChatColor.GRAY + "(" + presets.size() + ")");
                for (String preset : presets) {
                    sender.sendMessage(ChatColor.RED + " > " + ChatColor.WHITE + preset);
                }
                if (presets.size() == 0) {
                    sender.sendMessage(ChatColor.RED + " None");
                } else {
                    sender.sendMessage(ChatColor.RED + " Use " + ChatColor.WHITE + "/" + label + " presets " + command.getName() + ChatColor.RED + " to view the presets in more detail!");
                }
                return true;
            } else if ("presets".equalsIgnoreCase(args[0]) && sender.hasPermission("rwm.commandsystem.cmd.presets")) {
                // Get the blockdata
                RedCommand command = plugin.getCommandManager().getCommands().get(args[1].toLowerCase());
                if (command == null) {
                    plugin.sendMessage(sender, "commandnotfound", "name", args[1]);
                    return true;
                }
                Map<String, String> presets = new HashMap<>();
                for (Map.Entry<String, String> preset : command.getPresets().entrySet()) {
                    if (!command.perPresetPermissions() || sender.hasPermission(command.getPermission() + "." + preset.getKey().toLowerCase())) {
                        String value = preset.getValue();
                        if (value.length() > 210) {
                            value = value.substring(0, 200) + ChatColor.GRAY + "...";
                        }
                        presets.put(preset.getKey(), value);
                    }
                }
                sender.sendMessage(plugin.getPrefix() + ChatColor.DARK_RED + " Presets for " + ChatColor.WHITE + command.getName() + ChatColor.DARK_RED + ":");
                for (Map.Entry<String, String> preset : presets.entrySet()) {
                    sender.sendMessage(ChatColor.RED + " " + preset.getKey() + ": " + ChatColor.WHITE + preset.getValue());
                }
                if (presets.size() == 0) {
                    sender.sendMessage(ChatColor.RED + " None");
                }
                return true;
            }
        }
        return showHelp(sender, label);
    }

    private boolean showHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.DARK_RED + plugin.getName() + " Commands:");
        if (sender.hasPermission("rwm.commandsystem.cmd.list")) {
            sender.sendMessage(ChatColor.RED + "/" + label + " list"
                    + ChatColor.GRAY + " List all configured commands");
        }
        if (sender.hasPermission("rwm.commandsystem.cmd.info")) {
            sender.sendMessage(ChatColor.RED + "/" + label + " info <name>"
                    + ChatColor.GRAY + " Show some info about a command");
        }
        if (sender.hasPermission("rwm.commandsystem.cmd.presets")) {
            sender.sendMessage(ChatColor.RED + "/" + label + " presets <name>"
                    + ChatColor.GRAY + " Show the presets of a command");
        }
        if (sender.hasPermission("rwm.commandsystem.cmd.reload")) {
            sender.sendMessage(ChatColor.RED + "/" + label + " reload"
                    + ChatColor.GRAY + " Reload the config of the plugin");
        }
        sender.sendMessage(ChatColor.RED + "/" + label + " help"
                + ChatColor.GRAY + " Show this help");
        return true;
    }
}
