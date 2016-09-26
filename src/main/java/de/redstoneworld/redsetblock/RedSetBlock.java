package de.redstoneworld.redsetblock;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class RedSetBlock extends JavaPlugin {

    private Map<String, String> blockConfig = new HashMap<String, String>();
    private Map<String, String[]> cachedPositions = new HashMap<String, String[]>();

    public void onEnable() {
        loadConfig();
        getCommand("redsetblock").setExecutor(new RedSetBlockCommand(this));
    }

    public void loadConfig() {
        saveDefaultConfig();
        reloadConfig();
        blockConfig = new HashMap<String, String>();
        ConfigurationSection blocks = getConfig().getConfigurationSection("blocks");
        if (blocks != null && blocks.getKeys(false).size() > 0) {
            for (String configName : blocks.getKeys(false)){
                blockConfig.put(configName.toLowerCase(), blocks.getString(configName));
            }
            getLogger().log(Level.INFO, "Loaded " + blockConfig.size() + " block data configs!");
        } else {
            getLogger().log(Level.WARNING, "No block data defined in the config's blocks section!");
        }
    }

    public String getPrefix() {
        return ChatColor.WHITE + "[" + ChatColor.DARK_RED + getName() + ChatColor.WHITE + "]" + ChatColor.RESET;
    }

    public String getBlockData(String configName) {
        return blockConfig.get(configName.toLowerCase());
    }

    public Map<String, String> getBlockConfig() {
        return blockConfig;
    }

    public Map<String, String[]> getCachedPositions() {
        return cachedPositions;
    }

    public void sendMessage(CommandSender sender, String key, String... repl) {
        String msg = getConfig().getString("messages." + key, null);
        if (msg == null) {
            msg = ChatColor.RED + "Unknown language key " + ChatColor.WHITE + "messages." + key;
        }
        if (!msg.isEmpty()) {
            for (int i = 0; i + 1 < repl.length; i += 2) {
                msg = msg.replace("%" + repl[i] + "%", repl[i+1]);
            }
            sender.sendMessage(getPrefix() + " " + ChatColor.translateAlternateColorCodes('&', msg));
        }
    }
}
