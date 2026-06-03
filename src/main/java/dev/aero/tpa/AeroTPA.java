package dev.aero.tpa;

import dev.aero.tpa.command.*;
import dev.aero.tpa.gui.GUIListener;
import dev.aero.tpa.gui.GUIManager;
import dev.aero.tpa.manager.PlayerDataManager;
import dev.aero.tpa.manager.RequestManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

// Main plugin class
public final class AeroTPA extends JavaPlugin {

    private static AeroTPA instance;

    private RequestManager requestManager;
    private PlayerDataManager playerDataManager;
    private GUIManager guiManager;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Initialize managers
        this.playerDataManager = new PlayerDataManager();
        this.requestManager = new RequestManager(this);
        this.guiManager = new GUIManager(this);

        // Register commands
        registerCommands();

        // Register event listeners
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        // Initialize bStats if enabled
        if (getConfig().getBoolean("general.metrics", true)) {
            int pluginId = 31775;
            new Metrics(this, pluginId);
            getLogger().info("bStats metrics enabled.");
        }

        getLogger().info("AeroTPA v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        // Cancel running tasks on shutdown
        getServer().getScheduler().cancelTasks(this);

        // Cleanup
        if (requestManager != null) {
            requestManager.clearAll();
        }

        getLogger().info("AeroTPA disabled.");
        instance = null;
    }

    private void registerCommands() {
        getCommand("tpa").setExecutor(new TPACommand(this));
        getCommand("tpahere").setExecutor(new TPAHereCommand(this));
        getCommand("tpaccept").setExecutor(new TPAcceptCommand(this));
        getCommand("tpadeny").setExecutor(new TPADenyCommand(this));
        getCommand("tpauto").setExecutor(new TPAutoCommand(this));
        getCommand("tpaconfirm").setExecutor(new TPAConfirmCommand(this));
        getCommand("tpatoggle").setExecutor(new TPAToggleCommand(this));
        getCommand("tpaui").setExecutor(new TPAUICommand(this));
        getCommand("tpareload").setExecutor(new TPAReloadCommand(this));
    }

    // Reloads the plugin configuration
    public void reloadPluginConfig() {
        reloadConfig();
    }

    // Getters

    public static AeroTPA getInstance() {
        return instance;
    }

    public RequestManager getRequestManager() {
        return requestManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }
}
