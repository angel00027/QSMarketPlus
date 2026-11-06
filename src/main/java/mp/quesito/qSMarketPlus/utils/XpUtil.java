package mp.quesito.qSMarketPlus.utils;

import org.bukkit.entity.Player;

public class XpUtil {

    private XpUtil() {
    }

    // XP necesaria para pasar de "level" a "level + 1"
    public static int xpNeededForLevel(int level) {
        if (level >= 31) return 9 * level - 158;
        if (level >= 16) return 5 * level - 38;
        return 2 * level + 7;
    }

    // XP total acumulada necesaria para ALCANZAR el nivel indicado (desde 0)
    public static int xpForLevel(int level) {
        int exp = 0;
        for (int l = 0; l < level; l++) {
            exp += xpNeededForLevel(l);
        }
        return exp;
    }

    // Quita "levels" niveles reales al jugador, conservando el % de progreso
    public static void removeLevels(Player player, int levels) {
        if (levels <= 0) return;

        int level = player.getLevel();
        if (level <= 0) return;

        int toRemove = Math.min(levels, level);
        int newLevel = level - toRemove;

        int currentProgress = Math.round(player.getExp() * xpNeededForLevel(level));
        int newBase = xpNeededForLevel(Math.max(0, newLevel));
        float newProgress = newBase > 0 ? Math.min(1f, currentProgress / (float) newBase) : 0f;

        player.setLevel(newLevel);
        player.setExp(newProgress);
    }

    // Añade "levels" niveles reales al jugador (conserva el progreso actual)
    public static void giveLevels(Player player, int levels) {
        if (levels <= 0) return;
        player.setLevel(player.getLevel() + levels);
    }
}