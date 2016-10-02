package de.redstoneworld.redcommandsystem;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

public class RedCommandSystem extends JavaPlugin {

    private ConfigAccessor commandsConfig = new ConfigAccessor(this, "commands.yml");;
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

        commandsConfig.saveDefaultConfig();
        commandsConfig.reloadConfig();

        if (getConfig().getInt("configversion", 1) == 1) {
            Map<String, String> blockConfig = new LinkedHashMap<>();
            ConfigurationSection blocks = getConfig().getConfigurationSection("blocks");
            if (blocks != null && blocks.getKeys(false).size() > 0) {
                for (String configName : blocks.getKeys(false)) {
                    blockConfig.put(configName, blocks.getString(configName));
                }
                getLogger().log(Level.INFO, "Loaded " + blockConfig.size() + " block data configs!");
            }

            Configuration oldConfig = new MemoryConfiguration(getConfig());

            File configFile = new File(getDataFolder(), "config.yml");
            configFile.delete();
            reloadConfig();
            // Update keys from new config with changed ones from old config
            for (String key : getConfig().getKeys(true)) {
                if (oldConfig.get(key) != null) {
                    getConfig().set(key, oldConfig.get(key));
                }
            }
            saveConfig();

            getCommandsConfig().set("blockdata.presets", blockConfig);
            commandsConfig.saveConfig();
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
