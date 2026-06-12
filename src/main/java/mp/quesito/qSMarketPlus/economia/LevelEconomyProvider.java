package mp.quesito.qSMarketPlus.economia;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class LevelEconomyProvider implements EconomyProvider {

    private final String symbol;

    public LevelEconomyProvider(FileConfiguration config) {
        this.symbol = config.getString("economies.levels.symbol", "Lv");
    }

    @Override
    public String getName() {
        return "levels";
    }

    @Override
    public double getBalance(Player player) {
        return player.getLevel();
    }

    @Override
    public boolean withdraw(Player player, double amount) {

        if (player.getLevel() < amount) return false;

        player.setLevel(player.getLevel() - (int) amount);
        return true;
    }

    @Override
    public boolean deposit(Player player, double amount) {

        player.setLevel(player.getLevel() + (int) amount);
        return true;
    }

    @Override
    public String getSymbol() {
        return symbol;
    }
}