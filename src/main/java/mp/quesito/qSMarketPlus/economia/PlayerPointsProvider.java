package mp.quesito.qSMarketPlus.economia;

import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class PlayerPointsProvider implements EconomyProvider {

    private final PlayerPointsAPI api;
    private final String symbol;

    public PlayerPointsProvider(PlayerPointsAPI api, FileConfiguration config) {
        this.api = api;
        this.symbol = config.getString("economies.points.symbol", "Points");
    }

    @Override
    public String getName() {
        return "points";
    }

    @Override
    public double getBalance(Player player) {
        return api.look(player.getUniqueId());
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        return api.take(player.getUniqueId(), (int) amount);
    }

    @Override
    public boolean deposit(Player player, double amount) {
        return api.give(player.getUniqueId(), (int) amount);
    }

    @Override
    public String getSymbol() {
        return symbol;
    }
}