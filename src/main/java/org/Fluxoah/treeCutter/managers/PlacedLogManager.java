package org.Fluxoah.treeCutter.managers;

import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PlacedLogManager {

    private final ConfigManager config;
    private final Set<String> trackedPlacedLogs = ConcurrentHashMap.newKeySet();

    public PlacedLogManager(ConfigManager config) {
        this.config = config;
    }

    public void trackPlacedLog(Block block) {
        if (block == null) {
            return;
        }
        if (!config.getLogTypes().contains(block.getType())) {
            return;
        }
        trackedPlacedLogs.add(blockKey(block));
    }

    public void trackPlacedLog(World world, int x, int y, int z) {
        if (world == null) {
            return;
        }
        if (!config.getLogTypes().contains(world.getBlockAt(x, y, z).getType())) {
            return;
        }
        trackedPlacedLogs.add(blockKey(world, x, y, z));
    }

    public void untrack(Block block) {
        if (block == null) {
            return;
        }
        trackedPlacedLogs.remove(blockKey(block));
    }

    public boolean containsPlacedLog(List<Block> blocks) {
        for (Block block : blocks) {
            if (trackedPlacedLogs.contains(blockKey(block))) {
                return true;
            }
        }
        return false;
    }

    private String blockKey(Block block) {
        return blockKey(block.getWorld(), block.getX(), block.getY(), block.getZ());
    }

    private String blockKey(World world, int x, int y, int z) {
        return world.getUID() + ":" + x + ":" + y + ":" + z;
    }
}
