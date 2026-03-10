package org.Fluxoah.treeCutter.managers;

import org.Fluxoah.treeCutter.TreeCutter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class ConfigManager {

    private final TreeCutter plugin;
    private FileConfiguration config;

    private boolean serverEnabled;
    private boolean sneakRequired;
    private boolean naturalLogsOnly;
    private boolean requireAxe;
    private boolean dropItems;
    private boolean preserveDurability;
    private boolean allowVanillaDrops;
    private boolean confirmationRequired;
    private boolean worldGuardEnabled;
    private boolean denyPartialBreaks;
    private boolean placeholderApiEnabled;
    private boolean requireUpwardGrowth;
    private boolean trackPlayerPlacedLogs;
    private boolean trackCommandPlacedLogs;
    private int maxTreeSize;
    private int minLeafCount;
    private int maxLeafSearchRadius;
    private int maxLeafSearchUp;
    private int maxHorizontalSpread;
    private int maxTrackedCommandFillVolume;
    private int blacklistNoticeSeconds;
    private int unblacklistNoticeSeconds;
    private double cooldownSeconds;
    private int confirmationTimeSeconds;
    private Set<Material> logTypes = new HashSet<>();
    private Set<Material> leafTypes = new HashSet<>();
    private final Set<UUID> blacklist = new HashSet<>();
    private final Set<UUID> disabledPlayers = new HashSet<>();
    private final Set<UUID> blacklistMessageDisabled = new HashSet<>();
    private final Set<UUID> pendingUnblacklistNotice = new HashSet<>();

    public ConfigManager(TreeCutter plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        ensureMissingDefaults();

        serverEnabled = config.getBoolean("enabled", true);
        sneakRequired = config.getBoolean("behavior.sneak-required", true);
        naturalLogsOnly = config.getBoolean("behavior.block-player-made-trees",
                config.getBoolean("behavior.natural-logs-only", true));
        requireAxe = config.getBoolean("behavior.require-axe", false);
        maxTreeSize = config.getInt("behavior.max-tree-size", 256);
        cooldownSeconds = config.getDouble("behavior.cooldown-seconds", 1.0D);
        confirmationRequired = config.getBoolean("behavior.confirmation-required", true);
        confirmationTimeSeconds = config.getInt("behavior.confirmation-time-seconds", 10);
        dropItems = config.getBoolean("drops.drop-items", true);
        preserveDurability = config.getBoolean("drops.preserve-tool-durability", false);
        allowVanillaDrops = config.getBoolean("drops.allow-vanilla-drops", false);

        ConfigurationSection treeDetection = config.getConfigurationSection("behavior.tree-detection");
        if (treeDetection == null) {
            treeDetection = config.createSection("behavior.tree-detection");
        }

        maxLeafSearchRadius = treeDetection.getInt("max-leaf-search-radius", 3);
        maxLeafSearchUp = treeDetection.getInt("max-leaf-search-up", 5);
        minLeafCount = treeDetection.getInt("min-leaf-count", 4);
        maxHorizontalSpread = treeDetection.getInt("max-horizontal-spread", 6);
        requireUpwardGrowth = treeDetection.getBoolean("require-upward-growth", true);

        ConfigurationSection placedTracking = config.getConfigurationSection("placed-log-tracking");
        if (placedTracking == null) {
            placedTracking = config.createSection("placed-log-tracking");
        }

        trackPlayerPlacedLogs = placedTracking.getBoolean("player-placed", true);
        trackCommandPlacedLogs = placedTracking.getBoolean("command-placed", true);
        maxTrackedCommandFillVolume = Math.max(1, placedTracking.getInt("max-fill-volume", 50000));

        ConfigurationSection notifications = config.getConfigurationSection("notifications");
        if (notifications == null) {
            notifications = config.createSection("notifications");
        }
        blacklistNoticeSeconds = Math.max(1, notifications.getInt("blacklist-actionbar-seconds", 10));
        unblacklistNoticeSeconds = Math.max(1, notifications.getInt("unblacklist-actionbar-seconds", 10));

        worldGuardEnabled = config.getBoolean("hooks.worldguard.enabled", true);
        denyPartialBreaks = config.getBoolean("hooks.worldguard.deny-partial-breaks", true);
        placeholderApiEnabled = config.getBoolean("hooks.placeholderapi.enabled", true);

        logTypes = parseMaterials(config.getStringList("logs"), "logs");
        leafTypes = parseMaterials(config.getStringList("leaves"), "leaves");

        blacklist.clear();
        loadUuidList(config.getStringList("blacklist"), blacklist, "blacklist");

        disabledPlayers.clear();
        loadUuidList(config.getStringList("disabled-players"), disabledPlayers, "disabled-players");

        blacklistMessageDisabled.clear();
        loadUuidList(config.getStringList("blacklist-message-disabled"), blacklistMessageDisabled, "blacklist-message-disabled");

        pendingUnblacklistNotice.clear();
        loadUuidList(config.getStringList("pending-unblacklist-notice"), pendingUnblacklistNotice, "pending-unblacklist-notice");
    }

    private void ensureMissingDefaults() {
        boolean changed = false;
        changed |= setIfMissing("prefix", "&8[&3T&bC&8]");
        changed |= setIfMissing("pending-unblacklist-notice", List.of());
        changed |= setIfMissing("behavior.block-player-made-trees", config.getBoolean("behavior.natural-logs-only", true));
        changed |= setIfMissing("placed-log-tracking.player-placed", true);
        changed |= setIfMissing("placed-log-tracking.command-placed", true);
        changed |= setIfMissing("placed-log-tracking.max-fill-volume", 50000);
        changed |= setIfMissing("notifications.blacklist-actionbar-seconds", 10);
        changed |= setIfMissing("notifications.unblacklist-actionbar-seconds", 10);
        if (changed) {
            plugin.saveConfig();
        }
    }

    private boolean setIfMissing(String path, Object value) {
        if (config.contains(path)) {
            return false;
        }
        config.set(path, value);
        return true;
    }

    private Set<Material> parseMaterials(List<String> rawValues, String path) {
        Set<Material> parsed = new HashSet<>();
        for (String value : rawValues) {
            try {
                parsed.add(Material.valueOf(value.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Invalid material in " + path + ": " + value);
            }
        }
        return parsed;
    }

    private void loadUuidList(List<String> values, Set<UUID> target, String path) {
        for (String raw : values) {
            try {
                target.add(UUID.fromString(raw));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Invalid UUID in " + path + ": " + raw);
            }
        }
    }

    private void saveState() {
        config.set("enabled", serverEnabled);
        config.set("blacklist", blacklist.stream().map(UUID::toString).toList());
        config.set("disabled-players", disabledPlayers.stream().map(UUID::toString).toList());
        config.set("blacklist-message-disabled", blacklistMessageDisabled.stream().map(UUID::toString).toList());
        config.set("pending-unblacklist-notice", pendingUnblacklistNotice.stream().map(UUID::toString).toList());
        plugin.saveConfig();
    }

    public void setServerEnabled(boolean enabled) {
        this.serverEnabled = enabled;
        saveState();
    }

    public void addToBlacklist(UUID uuid) {
        blacklist.add(uuid);
        pendingUnblacklistNotice.remove(uuid);
        saveState();
    }

    public void removeFromBlacklist(UUID uuid) {
        blacklist.remove(uuid);
        saveState();
    }

    public void addDisabledPlayer(UUID uuid) {
        disabledPlayers.add(uuid);
        saveState();
    }

    public void removeDisabledPlayer(UUID uuid) {
        disabledPlayers.remove(uuid);
        saveState();
    }

    public boolean isServerEnabled() {
        return serverEnabled;
    }

    public boolean isSneakRequired() {
        return sneakRequired;
    }

    public boolean isNaturalLogsOnly() {
        return naturalLogsOnly;
    }

    public boolean shouldBlockPlayerMadeTrees() {
        return naturalLogsOnly;
    }

    public boolean isRequireAxe() {
        return requireAxe;
    }

    public boolean shouldDropItems() {
        return dropItems;
    }

    public boolean shouldPreserveDurability() {
        return preserveDurability;
    }

    public boolean allowVanillaDrops() {
        return allowVanillaDrops;
    }

    public boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public int getMaxTreeSize() {
        return maxTreeSize;
    }

    public double getCooldownSeconds() {
        return cooldownSeconds;
    }

    public int getConfirmationTimeSeconds() {
        return confirmationTimeSeconds;
    }

    public Set<Material> getLogTypes() {
        return Collections.unmodifiableSet(logTypes);
    }

    public Set<Material> getLeafTypes() {
        return Collections.unmodifiableSet(leafTypes);
    }

    public boolean isBlacklisted(UUID uuid) {
        return blacklist.contains(uuid);
    }

    public Set<UUID> getBlacklist() {
        return Collections.unmodifiableSet(blacklist);
    }

    public boolean isPlayerDisabled(UUID uuid) {
        return disabledPlayers.contains(uuid);
    }

    public boolean isBlacklistMessageDisabled(UUID uuid) {
        return blacklistMessageDisabled.contains(uuid);
    }

    public void disableBlacklistMessage(UUID uuid) {
        blacklistMessageDisabled.add(uuid);
        saveState();
    }

    public void enableBlacklistMessage(UUID uuid) {
        blacklistMessageDisabled.remove(uuid);
        saveState();
    }

    public void queueUnblacklistNotice(UUID uuid) {
        pendingUnblacklistNotice.add(uuid);
        saveState();
    }

    public boolean consumePendingUnblacklistNotice(UUID uuid) {
        boolean removed = pendingUnblacklistNotice.remove(uuid);
        if (removed) {
            saveState();
        }
        return removed;
    }

    public int getMinLeafCount() {
        return minLeafCount;
    }

    public int getMaxLeafSearchRadius() {
        return maxLeafSearchRadius;
    }

    public int getMaxLeafSearchUp() {
        return maxLeafSearchUp;
    }

    public int getMaxHorizontalSpread() {
        return maxHorizontalSpread;
    }

    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }

    public boolean isDenyPartialBreaks() {
        return denyPartialBreaks;
    }

    public boolean isPlaceholderApiEnabled() {
        return placeholderApiEnabled;
    }

    public boolean isRequireUpwardGrowth() {
        return requireUpwardGrowth;
    }

    public boolean isTrackPlayerPlacedLogs() {
        return trackPlayerPlacedLogs;
    }

    public boolean isTrackCommandPlacedLogs() {
        return trackCommandPlacedLogs;
    }

    public int getMaxTrackedCommandFillVolume() {
        return maxTrackedCommandFillVolume;
    }

    public int getBlacklistNoticeSeconds() {
        return blacklistNoticeSeconds;
    }

    public int getUnblacklistNoticeSeconds() {
        return unblacklistNoticeSeconds;
    }
}
