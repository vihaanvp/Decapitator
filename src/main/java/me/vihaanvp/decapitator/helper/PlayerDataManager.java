package me.vihaanvp.decapitator.helper;

import me.vihaanvp.decapitator.Decapitator;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class PlayerDataManager {

    private final Decapitator plugin;
    private final File dataFile;
    private YamlConfiguration dataConfig;

    public PlayerDataManager(Decapitator plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playercounts.yml");
    }

    public synchronized void load() throws IOException {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new IOException("Could not create plugin data folder: " + plugin.getDataFolder().getAbsolutePath());
        }

        if (!dataFile.exists()) {
            // create an empty file
            if (!dataFile.createNewFile()) {
                throw new IOException("Could not create data file: " + dataFile.getAbsolutePath());
            }
        }

        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public synchronized void save() throws IOException {
        if (this.dataConfig == null) return;
        try {
            this.dataConfig.save(dataFile);
        } catch (IOException ex) {
            throw new IOException("Failed to save player data file: " + ex.getMessage(), ex);
        }
    }

    public synchronized int getCount(UUID playerUuid) {
        if (this.dataConfig == null) return 0;
        return this.dataConfig.getInt(playerUuid.toString(), 0);
    }

    /**
     * Increment the stored count for the player by 1 and persist to disk.
     * Returns the new count.
     */
    public synchronized int incrementCount(UUID playerUuid) {
        int current = getCount(playerUuid);
        int next = current + 1;
        this.dataConfig.set(playerUuid.toString(), next);
        try {
            save();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save playercounts after increment: " + e.getMessage());
            e.printStackTrace();
        }
        return next;
    }

    /**
     * Optional: set the count for a player (not used currently, but useful for admin actions).
     */
    public synchronized void setCount(UUID playerUuid, int count) {
        this.dataConfig.set(playerUuid.toString(), count);
        try {
            save();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save playercounts after setCount: " + e.getMessage());
            e.printStackTrace();
        }
    }
}