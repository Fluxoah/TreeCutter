package org.Fluxoah.treeCutter.utils;

import org.Fluxoah.treeCutter.managers.ConfigManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public final class TreeUtils {

    private static final BlockFace[] LOG_SCAN_DIRECTIONS = {
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST,
            BlockFace.NORTH_EAST,
            BlockFace.NORTH_WEST,
            BlockFace.SOUTH_EAST,
            BlockFace.SOUTH_WEST
    };

    private TreeUtils() {
    }

    public static TreeScanResult scanTree(Block start, ConfigManager config) {
        if (!config.getLogTypes().contains(start.getType())) {
            return TreeScanResult.invalid();
        }

        Queue<Block> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        List<Block> blocks = new ArrayList<>();
        int baseX = start.getX();
        int baseZ = start.getZ();
        int maxTreeSize = config.getMaxTreeSize();
        int leafCount = 0;
        int minY = start.getY();
        int maxY = start.getY();
        boolean foundUpwardGrowth = false;

        queue.add(start);
        visited.add(key(start));

        while (!queue.isEmpty()) {
            Block current = queue.poll();
            blocks.add(current);

            if (blocks.size() > maxTreeSize) {
                return new TreeScanResult(blocks, false, true, false);
            }

            minY = Math.min(minY, current.getY());
            maxY = Math.max(maxY, current.getY());
            if (current.getY() > start.getY()) {
                foundUpwardGrowth = true;
            }

            int spreadX = Math.abs(current.getX() - baseX);
            int spreadZ = Math.abs(current.getZ() - baseZ);
            if (spreadX > config.getMaxHorizontalSpread() || spreadZ > config.getMaxHorizontalSpread()) {
                return new TreeScanResult(blocks, false, true, false);
            }

            leafCount += countNearbyLeaves(current, config);

            for (BlockFace face : LOG_SCAN_DIRECTIONS) {
                Block relative = current.getRelative(face);
                if (!config.getLogTypes().contains(relative.getType())) {
                    continue;
                }

                String key = key(relative);
                if (visited.add(key)) {
                    queue.add(relative);
                }
            }
        }

        leafCount = Math.min(leafCount, maxTreeSize * 4);
        boolean validLeaves = !config.isNaturalLogsOnly() || leafCount >= config.getMinLeafCount();
        boolean validGrowth = !config.isRequireUpwardGrowth() || foundUpwardGrowth || maxY > minY;
        boolean naturalMismatch = config.isNaturalLogsOnly() && !(validLeaves && validGrowth);

        return new TreeScanResult(blocks, validLeaves && validGrowth, false, naturalMismatch);
    }

    private static int countNearbyLeaves(Block block, ConfigManager config) {
        int count = 0;
        for (int x = -config.getMaxLeafSearchRadius(); x <= config.getMaxLeafSearchRadius(); x++) {
            for (int y = -1; y <= config.getMaxLeafSearchUp(); y++) {
                for (int z = -config.getMaxLeafSearchRadius(); z <= config.getMaxLeafSearchRadius(); z++) {
                    Material type = block.getRelative(x, y, z).getType();
                    if (config.getLeafTypes().contains(type)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public static List<Block> sortBottomToTop(List<Block> blocks) {
        blocks.sort(Comparator
                .comparingInt(Block::getY)
                .thenComparingInt(Block::getX)
                .thenComparingInt(Block::getZ));
        return blocks;
    }

    private static String key(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ':' + block.getY() + ':' + block.getZ();
    }

    public record TreeScanResult(List<Block> blocks, boolean validTree, boolean tooLarge, boolean naturalMismatch) {
        public static TreeScanResult invalid() {
            return new TreeScanResult(List.of(), false, false, false);
        }
    }
}
