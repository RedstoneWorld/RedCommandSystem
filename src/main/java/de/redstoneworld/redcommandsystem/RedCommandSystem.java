package de.redstoneworld.redcommandsystem;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public class RedCommandSystem extends JavaPlugin {

    private ConfigAccessor commandsConfig;
    private RedCommandManager rcm;

    public void onEnable() {
        loadConfig();
        getCommand(getName().toLowerCase()).setExecutor(new RedPluginCommand(this));
    }

    public boolean loadConfig() {
        try {
            rcm = new RedCommandManager(this);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            getLogger().log(Level.SEVERE, "Error while trying to initialize the CommandManager! The Bukkit's SimpleCommandMap reflection failed!", e);
            return false;
        }

        saveDefaultConfig();
        reloadConfig();

        commandsConfig = new ConfigAccessor(this, "commands.yml");
        commandsConfig.saveDefaultConfig();
        commandsConfig.reloadConfig();

        if (!getConfig().getBoolean("imported", true)) {
            getConfig().set("imported", null);

            File oldDataFolder = new File(getDataFolder().getParentFile(), "RedSetBlock");
            File oldConfigFile = new File(oldDataFolder, "config.yml");
            if (oldDataFolder.exists() && oldConfigFile.exists()) {
                getLogger().log(Level.INFO, "Found old RedSetBlock installation. Trying to import data!");
                FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(oldConfigFile);

                ConfigurationSection blocks = oldConfig.getConfigurationSection("blocks");
                if (blocks != null && blocks.getKeys(false).size() > 0) {
                    for (String configName : blocks.getKeys(false)) {
                        getCommandsConfig().set("redsetblock.presets." + configName, blocks.getString(configName));
                    }
                    getLogger().log(Level.INFO, "Imported " + blocks.getKeys(false).size() + " block data configs!");
                }
                commandsConfig.saveConfig();
            }
            saveConfig();
        }

        for (String key : getCommandsConfig().getKeys(false)) {
            RedCommand command = new RedCommand(this, getCommandsConfig().getConfigurationSection(key));
            rcm.register(command);
        }

        return true;
    }

    public String getPrefix() {
        return ChatColor.WHITE + "[" + ChatColor.DARK_RED + getName() + ChatColor.WHITE + "]" + ChatColor.RESET;
    }

    public void sendMessage(CommandSender sender, String key, String... repl) {
        String msg = getConfig().getString("messages." + key, null);
        if (msg == null) {
            msg = ChatColor.RED + "Unknown language key " + ChatColor.WHITE + "messages." + key;
        }
        if (!msg.isEmpty()) {
            sender.sendMessage(getPrefix() + " " + ChatColor.translateAlternateColorCodes('&', translate(msg, repl)));
        }
    }

    public String translate(String text, String... repl) {
        for (int i = 0; i + 1 < repl.length; i += 2) {
            text = text.replace("%" + repl[i] + "%", repl[i+1]);
        }
        return text;
    }

    public ConfigurationSection getCommandsConfig() {
        return commandsConfig.getConfig();
    }

    public RedCommandManager getCommandManager() {
        return rcm;
    }
}
