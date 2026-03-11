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

import java.util.Locale;
import java.util.concurrent.TimeUnit;

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
            try {
                WorldGuardHook.registerFlag(this);
            } catch (Throwable throwable) {
                getLogger().warning("WorldGuard flag registration skipped: " + throwable.getClass().getSimpleName()
                        + ": " + throwable.getMessage());
            }
        }
    }

    @Override
    public void onEnable() {
        if (!isFoliaServer()) {
            getLogger().severe("This TreeCutter branch is Folia-only. Disable it on non-Folia servers.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        this.permissionManager = new PermissionManager(this);
        this.statsManager = new StatsManager(this);
        this.noticeManager = new NoticeManager(this);
        this.placedLogManager = new PlacedLogManager(configManager);
        this.worldGuardHook = createWorldGuardHook();

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
        getServer().getAsyncScheduler().cancelTasks(this);
        getServer().getGlobalRegionScheduler().cancelTasks(this);
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
        getServer().getAsyncScheduler().runAtFixedRate(this, task -> {
            try {
                statsManager.saveStats();
            } catch (Exception exception) {
                getLogger().warning("Failed to auto-save stats: " + exception.getMessage());
            }
        }, 5L, 5L, TimeUnit.MINUTES);
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

    private WorldGuardHook createWorldGuardHook() {
        if (getServer().getPluginManager().getPlugin("WorldGuard") == null) {
            return null;
        }

        try {
            return new WorldGuardHook(this);
        } catch (Throwable throwable) {
            getLogger().warning("WorldGuard integration disabled: " + throwable.getClass().getSimpleName()
                    + ": " + throwable.getMessage());
            return null;
        }
    }

    private boolean isFoliaServer() {
        String serverName = getServer().getName().toLowerCase(Locale.ROOT);
        String serverVersion = getServer().getVersion().toLowerCase(Locale.ROOT);
        return serverName.contains("folia") || serverVersion.contains("folia");
    }
}
