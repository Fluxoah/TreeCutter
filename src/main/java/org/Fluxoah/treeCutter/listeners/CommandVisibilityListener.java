package org.Fluxoah.treeCutter.listeners;

import org.Fluxoah.treeCutter.TreeCutter;
import org.Fluxoah.treeCutter.managers.ConfigManager;
import org.Fluxoah.treeCutter.managers.PermissionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;

public class CommandVisibilityListener implements Listener {

    private final PermissionManager permissionManager;
    private final ConfigManager config;

    public CommandVisibilityListener(TreeCutter plugin) {
        this.permissionManager = plugin.getPermissionManager();
        this.config = plugin.getConfigManager();
    }

    @EventHandler
    public void onCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();

        if (!permissionManager.canUseCommand(player)) {
            event.getCommands().remove("tc");
            event.getCommands().remove("tree");
            event.getCommands().remove("treecutter");
        }

        if (!permissionManager.canUseMessageCommand(player) || !config.isBlacklisted(player.getUniqueId())) {
            event.getCommands().remove("tcmsg");
        }

        if (!permissionManager.canUseAdminCommand(player)) {
            event.getCommands().remove("tcadmin");
        }
    }
}
