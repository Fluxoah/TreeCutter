package org.Fluxoah.treeCutter.listeners;

import org.Fluxoah.treeCutter.TreeCutter;
import org.Fluxoah.treeCutter.managers.ConfigManager;
import org.Fluxoah.treeCutter.managers.PlacedLogManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.Locale;

public class PlacedLogListener implements Listener {

    private final TreeCutter plugin;
    private final ConfigManager config;
    private final PlacedLogManager placedLogs;

    public PlacedLogListener(TreeCutter plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.placedLogs = plugin.getPlacedLogManager();
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!config.isTrackPlayerPlacedLogs()) {
            return;
        }
        Block placed = event.getBlockPlaced();
        if (config.getLogTypes().contains(placed.getType())) {
            placedLogs.trackPlacedLog(placed);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (config.getLogTypes().contains(event.getBlock().getType())) {
            placedLogs.untrack(event.getBlock());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!config.isTrackCommandPlacedLogs()) {
            return;
        }
        trackPlacedLogsFromCommand(event.getPlayer(), event.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerCommand(ServerCommandEvent event) {
        if (!config.isTrackCommandPlacedLogs()) {
            return;
        }
        trackPlacedLogsFromCommand(event.getSender(), event.getCommand());
    }

    private void trackPlacedLogsFromCommand(CommandSender sender, String rawCommand) {
        if (rawCommand == null || rawCommand.isBlank()) {
            return;
        }

        String trimmed = rawCommand.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.isBlank()) {
            return;
        }

        String[] args = trimmed.split("\\s+");
        if (args.length < 2) {
            return;
        }

        String root = normalizeCommandLabel(args[0]);
        CommandContext context = resolveContext(sender);
        if (context == null) {
            return;
        }

        if ("setblock".equals(root)) {
            trackFromSetBlock(args, context);
            return;
        }

        if ("fill".equals(root)) {
            trackFromFill(args, context);
        }
    }

    private void trackFromSetBlock(String[] args, CommandContext context) {
        if (args.length < 5) {
            return;
        }

        Material material = parseMaterial(args[4]);
        if (material == null || !config.getLogTypes().contains(material)) {
            return;
        }

        Integer x = parseCoordinate(args[1], context.baseX());
        Integer y = parseCoordinate(args[2], context.baseY());
        Integer z = parseCoordinate(args[3], context.baseZ());
        if (x == null || y == null || z == null) {
            return;
        }

        plugin.getServer().getRegionScheduler().runDelayed(plugin, context.world(), x >> 4, z >> 4,
                task -> placedLogs.trackPlacedLog(context.world(), x, y, z), 1L);
    }

    private void trackFromFill(String[] args, CommandContext context) {
        if (args.length < 8) {
            return;
        }

        Material material = parseMaterial(args[7]);
        if (material == null || !config.getLogTypes().contains(material)) {
            return;
        }

        Integer x1 = parseCoordinate(args[1], context.baseX());
        Integer y1 = parseCoordinate(args[2], context.baseY());
        Integer z1 = parseCoordinate(args[3], context.baseZ());
        Integer x2 = parseCoordinate(args[4], context.baseX());
        Integer y2 = parseCoordinate(args[5], context.baseY());
        Integer z2 = parseCoordinate(args[6], context.baseZ());
        if (x1 == null || y1 == null || z1 == null || x2 == null || y2 == null || z2 == null) {
            return;
        }

        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        long volume = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (volume > config.getMaxTrackedCommandFillVolume()) {
            return;
        }

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    final int fx = x;
                    final int fy = y;
                    final int fz = z;
                    plugin.getServer().getRegionScheduler().runDelayed(plugin, context.world(), fx >> 4, fz >> 4,
                            task -> placedLogs.trackPlacedLog(context.world(), fx, fy, fz), 1L);
                }
            }
        }
    }

    private CommandContext resolveContext(CommandSender sender) {
        if (sender instanceof Entity entity) {
            Location location = entity.getLocation();
            return new CommandContext(entity.getWorld(), location.getX(), location.getY(), location.getZ());
        }
        if (sender instanceof BlockCommandSender blockCommandSender) {
            Block block = blockCommandSender.getBlock();
            Location location = block.getLocation();
            return new CommandContext(block.getWorld(), location.getX(), location.getY(), location.getZ());
        }
        return null;
    }

    private String normalizeCommandLabel(String raw) {
        String lowered = raw.toLowerCase(Locale.ROOT);
        if (lowered.startsWith("minecraft:")) {
            return lowered.substring("minecraft:".length());
        }
        return lowered;
    }

    private Material parseMaterial(String token) {
        if (token == null || token.isBlank() || token.startsWith("#")) {
            return null;
        }

        String value = token.toUpperCase(Locale.ROOT);
        int stateIndex = value.indexOf('[');
        if (stateIndex >= 0) {
            value = value.substring(0, stateIndex);
        }

        int nbtIndex = value.indexOf('{');
        if (nbtIndex >= 0) {
            value = value.substring(0, nbtIndex);
        }

        if (value.startsWith("MINECRAFT:")) {
            value = value.substring("MINECRAFT:".length());
        }

        return Material.matchMaterial(value);
    }

    private Integer parseCoordinate(String token, double base) {
        if (token == null || token.isBlank() || token.startsWith("^")) {
            return null;
        }

        if (token.startsWith("~")) {
            if (token.length() == 1) {
                return (int) Math.floor(base);
            }
            try {
                return (int) Math.floor(base + Double.parseDouble(token.substring(1)));
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        try {
            return (int) Math.floor(Double.parseDouble(token));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private record CommandContext(World world, double baseX, double baseY, double baseZ) {
    }
}
