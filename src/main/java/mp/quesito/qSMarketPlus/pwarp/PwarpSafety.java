package mp.quesito.qSMarketPlus.pwarp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.Material;

/**
 * Busca una posición segura para teletransportar:
 * evita pwarp mal configurados que pudieran ahogar/tapar (afixiar)
 * o matar al jugador (lava, cactus, etc.).
 */
public final class PwarpSafety {

    private static final int SEARCH_RADIUS = 4;
    private static final int SEARCH_HEIGHT = 3;

    private PwarpSafety() {
    }

    public static Location findSafeLocation(Location preferred) {
        if (preferred == null || preferred.getWorld() == null) return null;

        World w = preferred.getWorld();
        int minY = w.getMinHeight();
        int maxY = w.getMaxHeight() - 1;

        int bx = preferred.getBlockX();
        int by = preferred.getBlockY();
        int bz = preferred.getBlockZ();

        // 1) Intentar la posición exacta del warp
        if (isSafeColumn(w, bx, by, bz, minY, maxY)) {
            return new Location(w, bx + 0.5, by, bz + 0.5, preferred.getYaw(), preferred.getPitch());
        }

        // 2) Buscar cerca (espiral por radio, luego desplazamiento vertical)
        for (int r = 1; r <= SEARCH_RADIUS; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    for (int dy = -SEARCH_HEIGHT; dy <= SEARCH_HEIGHT; dy++) {
                        int y = by + dy;
                        if (y < minY || y + 1 > maxY) continue;
                        if (isSafeColumn(w, bx + dx, y, bz + dz, minY, maxY)) {
                            return new Location(w, bx + dx + 0.5, y, bz + dz + 0.5, preferred.getYaw(), preferred.getPitch());
                        }
                    }
                }
            }
        }

        return null;
    }

    private static boolean isSafeColumn(World w, int x, int y, int z, int minY, int maxY) {
        if (y < minY || y > maxY || y + 1 > maxY || y - 1 < minY) return false;

        Block feet = w.getBlockAt(x, y, z);
        Block head = w.getBlockAt(x, y + 1, z);
        Block ground = w.getBlockAt(x, y - 1, z);

        return isPassable(feet) && isPassable(head) && isSupport(ground);
    }

    private static boolean isPassable(Block block) {
        Material m = block.getType();
        if (!m.isSolid()) {
            return m != Material.LAVA
                    && m != Material.CACTUS
                    && m != Material.FIRE
                    && m != Material.CAMPFIRE
                    && m != Material.SOUL_CAMPFIRE;
        }
        return false;
    }

    private static boolean isSupport(Block block) {
        Material m = block.getType();
        return m.isSolid() && m != Material.MAGMA_BLOCK;
    }
}