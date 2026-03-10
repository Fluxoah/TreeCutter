package org.Fluxoah.treeCutter;

import org.Fluxoah.treeCutter.commands.AdminCommand;
import org.Fluxoah.treeCutter.commands.MessageCommand;
import org.Fluxoah.treeCutter.commands.TreeCommand;
import org.Fluxoah.treeCutter.hooks.WorldGuardHook;
import org.Fluxoah.treeCutter.listeners.CommandVisibilityListener;
import org.Fluxoah.treeCutter.listeners.PlacedLogListener;
import org.Fluxoah.treeCutter.listeners.PlayerListener;
import org.Fluxoah.treeCutter.listeners.TreeListener;
import org.Fluxoah.treeCutter.managers.ConfigManager;
import org.Fluxoah.treeCutter.managers.MessageManager;
import org.Fluxoah.treeCutter.managers.NoticeManager;
import org.Fluxoah.treeCutter.managers.PermissionManager;
import org.Fluxoah.treeCutter.managers.PlacedLogManager;
import org.Fluxoah.treeCutter.managers.StatsManager;
import org.Fluxoah.treeCutter.placeholders.TimberPlaceholderExpansion;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class TreeCutter extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private PermissionManager permissionManager;
    private StatsManager statsManager;
    private NoticeManager noticeManager;
    private PlacedLogManager placedLogManager;
    private WorldGuardHook worldGuardHook;

    @Override
    public void onLoad() {
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            WorldGuardHook.registerFlag(this);
        }
    }

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        this.permissionManager = new PermissionManager(this);
        this.statsManager = new StatsManager(this);
        this.noticeManager = new NoticeManager(this);
        this.placedLogManager = new PlacedLogManager(configManager);
        this.worldGuardHook = new WorldGuardHook(this);

        configManager.loadConfig();
        messageManager.loadMessages();
        statsManager.loadStats();

        registerCommands();
        registerListeners();
        registerPlaceholders();
        permissionManager.applyCommandPermissions();
        startAutoSaveTask();

        getLogger().info("Enabled TreeCutter v" + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        if (noticeManager != null) {
            noticeManager.shutdown();
        }
        if (statsManager != null) {
            statsManager.saveStats();
        }
        getLogger().info("Disabled TreeCutter v" + getDescription().getVersion());
    }

    private void registerCommands() {
        TreeCommand treeCommand = new TreeCommand(this);
        AdminCommand adminCommand = new AdminCommand(this);
        MessageCommand messageCommand = new MessageCommand(this);

        PluginCommand tc = getCommand("tc");
        PluginCommand tcAdmin = getCommand("tcadmin");
        PluginCommand tcMsg = getCommand("tcmsg");
        if (tc == null || tcAdmin == null || tcMsg == null) {
            throw new IllegalStateException("Commands are missing from plugin.yml");
        }

        tc.setExecutor(treeCommand);
        tc.setTabCompleter(treeCommand);
        tcAdmin.setExecutor(adminCommand);
        tcAdmin.setTabCompleter(adminCommand);
        tcMsg.setExecutor(messageCommand);
        tcMsg.setTabCompleter(messageCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new TreeListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandVisibilityListener(this), this);
        getServer().getPluginManager().registerEvents(new PlacedLogListener(this), this);
    }

    private void registerPlaceholders() {
        if (configManager.isPlaceholderApiEnabled() && getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TimberPlaceholderExpansion(this).register();
            getLogger().info("Registered PlaceholderAPI placeholder: %timber_status%.");
        }
    }

    private void startAutoSaveTask() {
        long intervalTicks = 20L * 60L * 5L;
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                statsManager.saveStats();
            } catch (Exception exception) {
                getLogger().warning("Failed to auto-save stats: " + exception.getMessage());
            }
        }, intervalTicks, intervalTicks);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public NoticeManager getNoticeManager() {
        return noticeManager;
    }

    public PlacedLogManager getPlacedLogManager() {
        return placedLogManager;
    }

    public WorldGuardHook getWorldGuardHook() {
        return worldGuardHook;
    }
}
