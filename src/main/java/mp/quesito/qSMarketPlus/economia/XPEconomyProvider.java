package mp.quesito.qSMarketPlus.economia;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class XPEconomyProvider implements EconomyProvider {

    private final String symbol;

    public XPEconomyProvider(FileConfiguration config) {
        this.symbol = config.getString("economies.xp.symbol", "XP");
    }

    @Override
    public String getName() {
        return "xp";
    }

    @Override
    public double getBalance(Player player) {
        return player.getTotalExperience();
    }

    @Override
    public boolean withdraw(Player player, double amount) {

        int xp = player.getTotalExperience();

        if (xp < amount) return false;

        player.setTotalExperience(xp - (int) amount);
        return true;
    }

    @Override
    public boolean deposit(Player player, double amount) {

        player.giveExp((int) amount);
        return true;
    }

    @Override
    public String getSymbol() {
        return symbol;
    }
}