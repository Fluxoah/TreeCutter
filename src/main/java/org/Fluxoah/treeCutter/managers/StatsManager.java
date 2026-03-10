package org.Fluxoah.treeCutter.managers;

import org.Fluxoah.treeCutter.TreeCutter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsManager {

    private final TreeCutter plugin;
    private final Map<UUID, Map<String, Integer>> playerStats = new HashMap<>();
    private final Map<UUID, Integer> treesCut = new HashMap<>();
    private final Map<UUID, Long> firstCut = new HashMap<>();
    private final Map<UUID, Long> lastCut = new HashMap<>();
    private File statsFile;
    private FileConfiguration statsConfig;

    public StatsManager(TreeCutter plugin) {
        this.plugin = plugin;
    }

    public synchronized void loadStats() {
        statsFile = new File(plugin.getDataFolder(), "stats.yml");
        if (!statsFile.exists()) {
            try {
                if (statsFile.getParentFile() != null) {
                    statsFile.getParentFile().mkdirs();
                }
                statsFile.createNewFile();
            } catch (IOException exception) {
                plugin.getLogger().severe("Could not create stats.yml: " + exception.getMessage());
            }
        }

        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        playerStats.clear();
        treesCut.clear();
        firstCut.clear();
        lastCut.clear();

        for (String uuidString : statsConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                ConfigurationSection section = statsConfig.getConfigurationSection(uuidString);
                if (section == null) {
                    continue;
                }

                Map<String, Integer> stats = new HashMap<>();
                ConfigurationSection logsSection = section.getConfigurationSection("logs");
                if (logsSection != null) {
                    for (String key : logsSection.getKeys(false)) {
                        stats.put(key, logsSection.getInt(key, 0));
                    }
                } else {
                    for (String key : section.getKeys(false)) {
                        if (key.equals("trees-cut") || key.equals("first-cut") || key.equals("last-cut")) {
                            continue;
                        }
                        stats.put(key, section.getInt(key, 0));
                    }
                }

                playerStats.put(uuid, stats);
                int treeCount = section.getInt("trees-cut", 0);
                if (treeCount == 0 && !stats.isEmpty()) {
                    treeCount = stats.values().stream().mapToInt(Integer::intValue).sum();
                }
                treesCut.put(uuid, treeCount);

                long first = section.getLong("first-cut", 0L);
                long last = section.getLong("last-cut", 0L);
                if (first > 0L) {
                    firstCut.put(uuid, first);
                }
                if (last > 0L) {
                    lastCut.put(uuid, last);
                }
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Invalid UUID in stats.yml: " + uuidString);
            }
        }
    }

    public synchronized void saveStats() {
        if (statsConfig == null || statsFile == null) {
            return;
        }

        for (String key : statsConfig.getKeys(false)) {
            statsConfig.set(key, null);
        }

        for (Map.Entry<UUID, Map<String, Integer>> entry : playerStats.entrySet()) {
            String uuidString = entry.getKey().toString();
            statsConfig.set(uuidString + ".trees-cut", treesCut.getOrDefault(entry.getKey(), 0));
            Long first = firstCut.get(entry.getKey());
            Long last = lastCut.get(entry.getKey());
            if (first != null) {
                statsConfig.set(uuidString + ".first-cut", first);
            }
            if (last != null) {
                statsConfig.set(uuidString + ".last-cut", last);
            }
            for (Map.Entry<String, Integer> statEntry : entry.getValue().entrySet()) {
                statsConfig.set(uuidString + ".logs." + statEntry.getKey(), statEntry.getValue());
            }
        }

        try {
            statsConfig.save(statsFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save stats.yml: " + exception.getMessage());
        }
    }

    public synchronized void recordTreeCut(UUID playerId, Map<String, Integer> logsByType) {
        Map<String, Integer> stats = playerStats.computeIfAbsent(playerId, ignored -> new HashMap<>());
        for (Map.Entry<String, Integer> entry : logsByType.entrySet()) {
            stats.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        treesCut.merge(playerId, 1, Integer::sum);

        long now = System.currentTimeMillis();
        if (!firstCut.containsKey(playerId)) {
            firstCut.put(playerId, now);
        }
        lastCut.put(playerId, now);
    }

    public synchronized int getTotalLogsBroken(UUID playerId) {
        return playerStats.getOrDefault(playerId, Collections.emptyMap())
                .values()
                .stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    public synchronized Map<String, Integer> getPlayerStats(UUID playerId) {
        return new HashMap<>(playerStats.getOrDefault(playerId, Collections.emptyMap()));
    }

    public synchronized boolean hasStats(UUID playerId) {
        return treesCut.getOrDefault(playerId, 0) > 0
                || !playerStats.getOrDefault(playerId, Collections.emptyMap()).isEmpty();
    }

    public synchronized int getTreesCut(UUID playerId) {
        return treesCut.getOrDefault(playerId, 0);
    }

    public synchronized long getFirstCut(UUID playerId) {
        return firstCut.getOrDefault(playerId, 0L);
    }

    public synchronized long getLastCut(UUID playerId) {
        return lastCut.getOrDefault(playerId, 0L);
    }
}
