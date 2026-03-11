package org.Fluxoah.treeCutter.hooks;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.Fluxoah.treeCutter.TreeCutter;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;

public class WorldGuardHook {

    private static final String TREECUTTER_FLAG_NAME = "treecutter";
    private static boolean registrationAttempted;
    private final TreeCutter plugin;
    private static StateFlag treeCutterFlag;

    public WorldGuardHook(TreeCutter plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        if (!plugin.getConfigManager().isWorldGuardEnabled() || getPlugin() == null) {
            return false;
        }
        registerFlag(plugin);
        return true;
    }

    public boolean canBreakAll(Player player, Collection<Block> blocks) {
        if (!isEnabled() || plugin.getPermissionManager().canBypassWorldGuard(player)) {
            return true;
        }

        WorldGuardPlugin worldGuard = getPlugin();
        if (worldGuard == null) {
            return true;
        }

        RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = regionContainer.createQuery();
        LocalPlayer localPlayer = worldGuard.wrapPlayer(player);

        for (Block block : blocks) {
            var location = BukkitAdapter.adapt(block.getLocation());
            if (!query.testBuild(location, localPlayer) || !query.testState(location, localPlayer, Flags.BLOCK_BREAK)) {
                return false;
            }
            if (treeCutterFlag != null && !query.testState(location, localPlayer, treeCutterFlag)) {
                return false;
            }
        }
        return true;
    }

    public static void registerFlag(TreeCutter plugin) {
        if (treeCutterFlag != null || registrationAttempted) {
            return;
        }
        registrationAttempted = true;

        Plugin pluginRef = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        if (!(pluginRef instanceof WorldGuardPlugin)) {
            return;
        }

        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            StateFlag flag = new StateFlag(TREECUTTER_FLAG_NAME, true);
            registry.register(flag);
            treeCutterFlag = flag;
        } catch (RuntimeException exception) {
            Flag<?> existing = registry.get(TREECUTTER_FLAG_NAME);
            if (existing instanceof StateFlag stateFlag) {
                treeCutterFlag = stateFlag;
                return;
            }
            plugin.getLogger().warning("WorldGuard flag '" + TREECUTTER_FLAG_NAME + "' could not be registered.");
        }
    }

    private WorldGuardPlugin getPlugin() {
        Plugin pluginRef = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        if (pluginRef instanceof WorldGuardPlugin worldGuardPlugin) {
            return worldGuardPlugin;
        }
        return null;
    }
}
