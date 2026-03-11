package org.Fluxoah.treeCutter.listeners;

import org.Fluxoah.treeCutter.TreeCutter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final TreeCutter plugin;

    public PlayerListener(TreeCutter plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getNoticeManager().deliverPendingNotice(event.getPlayer());
        plugin.getPermissionManager().refreshPlayerState(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getNoticeManager().cancel(event.getPlayer().getUniqueId());
    }
}
