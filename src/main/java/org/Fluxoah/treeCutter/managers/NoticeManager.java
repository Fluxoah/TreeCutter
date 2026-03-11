package org.Fluxoah.treeCutter.managers;

import org.Fluxoah.treeCutter.TreeCutter;
import org.Fluxoah.treeCutter.utils.ActionBarUtil;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class NoticeManager {

    private final TreeCutter plugin;
    private final ConfigManager config;
    private final MessageManager msg;
    private final Map<UUID, ScheduledTask> activeNotices = new ConcurrentHashMap<>();

    public NoticeManager(TreeCutter plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.msg = plugin.getMessageManager();
    }

    public void showBlacklistWarning(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (!config.isBlacklisted(player.getUniqueId()) || config.isBlacklistMessageDisabled(player.getUniqueId())) {
            return;
        }
        showTimedActionBar(player, msg.getMessage("blacklisted"), config.getBlacklistNoticeSeconds());
    }

    public void notifyUnblacklisted(UUID playerId) {
        Player online = plugin.getServer().getPlayer(playerId);
        if (online != null && online.isOnline()) {
            showTimedActionBar(online, msg.getMessage("unblacklisted-actionbar"), config.getUnblacklistNoticeSeconds());
            return;
        }
        config.queueUnblacklistNotice(playerId);
    }

    public void deliverPendingNotice(Player player) {
        if (player == null) {
            return;
        }
        if (config.consumePendingUnblacklistNotice(player.getUniqueId())) {
            player.getScheduler().runDelayed(plugin, task ->
                    showTimedActionBar(player, msg.getMessage("unblacklisted-actionbar"),
                            config.getUnblacklistNoticeSeconds()), null, 20L);
        }
    }

    public void cancel(UUID playerId) {
        ScheduledTask task = activeNotices.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    public void shutdown() {
        for (UUID playerId : activeNotices.keySet().toArray(new UUID[0])) {
            cancel(playerId);
        }
    }

    private void showTimedActionBar(Player player, String message, int seconds) {
        UUID playerId = player.getUniqueId();
        cancel(playerId);

        AtomicInteger remainingSends = new AtomicInteger(Math.max(1, seconds));
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, scheduledTask -> {
            if (!player.isOnline()) {
                cancel(playerId);
                return;
            }

            ActionBarUtil.send(player, message);
            if (remainingSends.decrementAndGet() <= 0) {
                cancel(playerId);
            }
        }, null, 1L, 20L);

        if (task != null) {
            activeNotices.put(playerId, task);
        }
    }
}
