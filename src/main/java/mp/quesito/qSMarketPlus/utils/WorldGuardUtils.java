package mp.quesito.qSMarketPlus.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.LocalPlayer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuardUtils {

    public static boolean canBuild(Player player, Location loc) {
        // Si WorldGuard no está activo, permitir
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) return true;

        // Obtener LocalPlayer correcto
        LocalPlayer lp = WorldGuardPlugin.inst().wrapPlayer(player);

        // Adaptar Location
        com.sk89q.worldedit.util.Location weLoc = BukkitAdapter.adapt(loc);

        // RegionQuery
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();

        // Testear flag BUILD
        return query.testState(weLoc, lp, Flags.BUILD);
    }
}
