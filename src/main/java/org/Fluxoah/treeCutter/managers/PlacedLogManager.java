package org.Fluxoah.treeCutter.managers;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Set;

public class PlacedLogManager {

   private final ConfigManager config;
   private final Set<Location> trackedPlacedLogs = new HashSet<>();

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
      trackedPlacedLogs.add(block.getLocation());
   }

   public void trackPlacedLog(World world, int x, int y, int z) {
      if (world == null) {
         return;
      }
      if (!config.getLogTypes().contains(world.getBlockAt(x, y, z).getType())) {
         return;
      }
      trackedPlacedLogs.add(new Location(world, x, y, z));
   }

   public void untrack(Block block) {
      if (block == null) {
         return;
      }
      trackedPlacedLogs.remove(block.getLocation());
   }

   public boolean filterPlacedLogs(Iterable<Block> blocks) {

      for (Block block : blocks) {
         if (trackedPlacedLogs.contains(block.getLocation())) {
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
