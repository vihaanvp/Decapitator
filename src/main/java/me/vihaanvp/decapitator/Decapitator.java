package me.vihaanvp.decapitator;

import me.vihaanvp.decapitator.command.*;
import me.vihaanvp.decapitator.helper.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class Decapitator extends JavaPlugin {

    private PlayerDataManager dataManager;

    @Override
    public void onEnable() {
        // ensure default config exists
        saveDefaultConfig();

        // load player data
        this.dataManager = new PlayerDataManager(this);
        try {
            this.dataManager.load();
        } catch (Exception e) {
            getLogger().severe("Failed to load player data: " + e.getMessage());
            e.printStackTrace();
        }

        // Register command executor
        if (this.getCommand("head") != null) {
            this.getCommand("head").setExecutor(new HeadCommand(this));
        } else {
            getLogger().severe("Command 'head' not defined in plugin.yml!");
        }

        getLogger().info("Decapitator enabled.");
    }

    @Override
    public void onDisable() {
        // save player data
        try {
            if (this.dataManager != null) this.dataManager.save();
        } catch (Exception e) {
            getLogger().severe("Failed to save player data on disable: " + e.getMessage());
            e.printStackTrace();
        }
        getLogger().info("Decapitator disabled.");
    }

    public PlayerDataManager getDataManager() {
        return dataManager;
    }
}