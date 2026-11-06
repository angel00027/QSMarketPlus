package mp.quesito.qSMarketPlus.pwarp;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

public class PwarpManager {

    private final QSMarketPlus plugin;
    private final PwarpDAO dao;
    private final Map<String, Pwarp> warps = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public PwarpManager(QSMarketPlus plugin) {
        this.plugin = plugin;
        this.dao = new PwarpDAO(plugin.getSqlManager());
    }

    public void loadAll() {
        warps.clear();
        for (Pwarp warp : dao.loadAll()) {
            if (Bukkit.getWorld(warp.getWorldName()) == null) continue;
            warps.put(warp.getName(), warp);
        }
    }

    public Collection<Pwarp> getAll() {
        return warps.values();
    }

    public Pwarp get(String name) {
        if (name == null) return null;
        return warps.get(name.toLowerCase());
    }

    public List<Pwarp> getByOwner(UUID owner) {
        return warps.values().stream()
                .filter(w -> w.getOwner().equals(owner))
                .collect(Collectors.toList());
    }

    public int getWarpCount(UUID owner) {
        return getByOwner(owner).size();
    }

    public int getMaxWarps(Player p) {
        if (p.hasPermission("qsmarket.pwarp.unlimited")) return Integer.MAX_VALUE;
        for (int i = 50; i >= 1; i--) {
            if (p.hasPermission("qsmarket.pwarp.limit." + i)) return i;
        }
        return plugin.getConfig().getInt("pwarp.max-warps", 3);
    }

    public Pwarp createPwarp(String name, UUID owner, String ownerName, Location loc) {
        Pwarp warp = new Pwarp(name.toLowerCase(), name, owner, ownerName);
        warp.setLocation(loc);
        long now = System.currentTimeMillis();
        warp.setCreated(now);
        warp.setUpdated(now);
        dao.insert(warp);
        warps.put(warp.getName(), warp);
        return warp;
    }

    public void deletePwarp(String name) {
        Pwarp warp = warps.remove(name.toLowerCase());
        if (warp != null) dao.delete(warp.getName());
    }

    public void updateDescription(Pwarp warp, String description) {
        warp.setDescription(description);
        warp.setUpdated(System.currentTimeMillis());
        dao.update(warp);
    }

    public void updateLocation(Pwarp warp, Location loc) {
        warp.setLocation(loc);
        warp.setUpdated(System.currentTimeMillis());
        dao.update(warp);
    }

    public boolean toggleFavorite(UUID player, String warpName) {
        String key = warpName.toLowerCase();
        boolean isFav = isFavorite(player, key);
        if (isFav) {
            dao.removeFavorite(player, key);
        } else {
            dao.addFavorite(player, key);
        }
        return !isFav;
    }

    public boolean isFavorite(UUID player, String warpName) {
        return dao.isFavorite(player, warpName.toLowerCase());
    }

    public Set<String> getFavoriteNames(UUID player) {
        return dao.loadFavorites(player).stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    public List<Pwarp> getFavorites(UUID player) {
        Set<String> favs = getFavoriteNames(player);
        return warps.values().stream()
                .filter(w -> favs.contains(w.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Teletransporta SOLO si se encuentra una ubicación segura.
     * Devuelve false si el pwarp está mal configurado (bloqueado/peligroso).
     */
    public boolean teleport(Player player, Pwarp warp) {
        Location warpLoc = warp.getLocation();
        if (warpLoc == null) return false;
        Location safe = PwarpSafety.findSafeLocation(warpLoc);
        if (safe == null) return false;
        player.teleport(safe);
        return true;
    }
}