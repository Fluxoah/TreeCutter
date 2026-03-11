package org.Fluxoah.treeCutter.managers;

import org.Fluxoah.treeCutter.TreeCutter;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

public class PermissionManager {

    public static final String USE_PERMISSION = "treecutter.use";
    public static final String PLAYER_COMMAND_PERMISSION = "treecutter.command.tc";
    public static final String PLAYER_MESSAGE_COMMAND_PERMISSION = "treecutter.command.tcmsg";
    public static final String PLAYER_HELP_PERMISSION = "treecutter.command.tc.help";
    public static final String PLAYER_ON_PERMISSION = "treecutter.command.tc.on";
    public static final String PLAYER_OFF_PERMISSION = "treecutter.command.tc.off";
    public static final String PLAYER_STATUS_PERMISSION = "treecutter.command.tc.status";
    public static final String ADMIN_COMMAND_PERMISSION = "treecutter.command.tcadmin";
    public static final String ADMIN_HELP_PERMISSION = "treecutter.command.tcadmin.help";
    public static final String ADMIN_ON_PERMISSION = "treecutter.command.tcadmin.on";
    public static final String ADMIN_OFF_PERMISSION = "treecutter.command.tcadmin.off";
    public static final String ADMIN_RELOAD_PERMISSION = "treecutter.command.tcadmin.reload";
    public static final String ADMIN_BLACKLIST_PERMISSION = "treecutter.command.tcadmin.blacklist";
    public static final String ADMIN_CHECK_PERMISSION = "treecutter.command.tcadmin.check";
    public static final String WG_BYPASS_PERMISSION = "treecutter.bypass.worldguard";

    private final TreeCutter plugin;
    private final boolean luckPermsPresent;

    public PermissionManager(TreeCutter plugin) {
        this.plugin = plugin;
        this.luckPermsPresent = plugin.getServer().getPluginManager().getPlugin("LuckPerms") != null;
    }

    public boolean isLuckPermsMode() {
        return luckPermsPresent;
    }

    public boolean canUseTreeFeller(Player player) {
        return player.hasPermission(USE_PERMISSION);
    }

    public boolean canUseCommand(Player player) {
        return player.hasPermission(PLAYER_COMMAND_PERMISSION);
    }

    public boolean canUseMessageCommand(Player player) {
        return player.hasPermission(PLAYER_MESSAGE_COMMAND_PERMISSION);
    }

    public boolean canUseAdminCommand(Player player) {
        return player.hasPermission(ADMIN_COMMAND_PERMISSION);
    }

    public boolean canUsePlayerSubcommand(Player player, String subcommand) {
        if (!canUseCommand(player)) {
            return false;
        }
        return switch (subcommand.toLowerCase()) {
            case "on" -> player.hasPermission(PLAYER_ON_PERMISSION);
            case "off" -> player.hasPermission(PLAYER_OFF_PERMISSION);
            case "status" -> player.hasPermission(PLAYER_STATUS_PERMISSION);
            case "help" -> player.hasPermission(PLAYER_HELP_PERMISSION);
            default -> false;
        };
    }

    public boolean canUseAdminSubcommand(Player player, String subcommand) {
        if (!canUseAdminCommand(player)) {
            return false;
        }
        return switch (subcommand.toLowerCase()) {
            case "on" -> player.hasPermission(ADMIN_ON_PERMISSION);
            case "off" -> player.hasPermission(ADMIN_OFF_PERMISSION);
            case "reload" -> player.hasPermission(ADMIN_RELOAD_PERMISSION);
            case "blacklist" -> player.hasPermission(ADMIN_BLACKLIST_PERMISSION);
            case "check" -> player.hasPermission(ADMIN_CHECK_PERMISSION);
            case "help" -> player.hasPermission(ADMIN_HELP_PERMISSION);
            default -> false;
        };
    }

    public boolean canBypassWorldGuard(Player player) {
        return player.hasPermission(WG_BYPASS_PERMISSION);
    }

    public boolean isTreeCuttingEnabled(Player player) {
        ConfigManager config = plugin.getConfigManager();
        return config.isServerEnabled()
                && canUseTreeFeller(player)
                && !config.isBlacklisted(player.getUniqueId())
                && !config.isPlayerDisabled(player.getUniqueId());
    }

    public String getModeDisplayKey() {
        return luckPermsPresent ? "mode-luckperms" : "mode-fallback";
    }

    public void applyCommandPermissions() {
        applyPermissionDefault(USE_PERMISSION, luckPermsPresent ? PermissionDefault.FALSE : PermissionDefault.TRUE);
        applyPermissionDefault(PLAYER_COMMAND_PERMISSION, luckPermsPresent ? PermissionDefault.FALSE : PermissionDefault.TRUE);
        applyPermissionDefault(PLAYER_MESSAGE_COMMAND_PERMISSION, luckPermsPresent ? PermissionDefault.FALSE : PermissionDefault.TRUE);
        applyPermissionDefault(PLAYER_HELP_PERMISSION, luckPermsPresent ? PermissionDefault.FALSE : PermissionDefault.TRUE);
        applyPermissionDefault(PLAYER_ON_PERMISSION, luckPermsPresent ? PermissionDefault.FALSE : PermissionDefault.TRUE);
        applyPermissionDefault(PLAYER_OFF_PERMISSION, luckPermsPresent ? PermissionDefault.FALSE : PermissionDefault.TRUE);
        applyPermissionDefault(PLAYER_STATUS_PERMISSION, luckPermsPresent ? PermissionDefault.FALSE : PermissionDefault.TRUE);

        PermissionDefault adminDefault = luckPermsPresent ? PermissionDefault.FALSE : PermissionDefault.OP;
        applyPermissionDefault(ADMIN_COMMAND_PERMISSION, adminDefault);
        applyPermissionDefault(ADMIN_HELP_PERMISSION, adminDefault);
        applyPermissionDefault(ADMIN_ON_PERMISSION, adminDefault);
        applyPermissionDefault(ADMIN_OFF_PERMISSION, adminDefault);
        applyPermissionDefault(ADMIN_RELOAD_PERMISSION, adminDefault);
        applyPermissionDefault(ADMIN_BLACKLIST_PERMISSION, adminDefault);
        applyPermissionDefault(ADMIN_CHECK_PERMISSION, adminDefault);
        applyPermissionDefault(WG_BYPASS_PERMISSION, PermissionDefault.FALSE);

        PluginCommand tc = plugin.getCommand("tc");
        PluginCommand tcadmin = plugin.getCommand("tcadmin");
        PluginCommand tcmsg = plugin.getCommand("tcmsg");
        if (tc != null) {
            tc.setPermission(PLAYER_COMMAND_PERMISSION);
        }
        if (tcadmin != null) {
            tcadmin.setPermission(ADMIN_COMMAND_PERMISSION);
        }
        if (tcmsg != null) {
            tcmsg.setPermission(PLAYER_MESSAGE_COMMAND_PERMISSION);
        }
        plugin.getServer().getGlobalRegionScheduler().execute(plugin,
                () -> plugin.getServer().getOnlinePlayers().forEach(this::refreshPlayerState));
    }

    public void refreshPlayerState(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        player.getScheduler().run(plugin, task -> {
            player.recalculatePermissions();
            player.updateCommands();
        }, null);
    }

    private void applyPermissionDefault(String node, PermissionDefault permissionDefault) {
        Permission permission = plugin.getServer().getPluginManager().getPermission(node);
        if (permission != null) {
            permission.setDefault(permissionDefault);
        }
    }
}
