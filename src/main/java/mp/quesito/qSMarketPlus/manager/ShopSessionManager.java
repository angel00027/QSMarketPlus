package mp.quesito.qSMarketPlus.manager;

import mp.quesito.qSMarketPlus.shop.ShopSession;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShopSessionManager {

    private final Map<UUID, ShopSession> sessions = new HashMap<>();

    public ShopSession get(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), k -> new ShopSession(0,0));
    }

    public void set(Player player, int category, int page) {
        sessions.put(player.getUniqueId(), new ShopSession(category,page));
    }

    public void remove(Player player) {
        sessions.remove(player.getUniqueId());
    }
}