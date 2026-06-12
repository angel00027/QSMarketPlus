package mp.quesito.qSMarketPlus.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.UUID;

public class PlatformUtil {

    /**
     * Verifica si un jugador está conectado desde Bedrock Edition a través de Floodgate.
     */
    public static boolean isBedrock(Player player) {
        if (Bukkit.getPluginManager().isPluginEnabled("Floodgate")) {
            try {
                // Utiliza la API de Floodgate para comprobar el UUID
                return org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
            } catch (NoClassDefFoundError | Exception e) {
                // Fallback por si la API cambia o falla
                return player.getName().startsWith("*");
            }
        }
        // Si no está Floodgate, asumimos que todos son Java o usa el prefijo común
        return player.getName().startsWith("*");
    }
}