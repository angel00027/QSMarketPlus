package mp.quesito.qSMarketPlus.economia;

import org.bukkit.entity.Player;

public interface EconomyProvider {

    String getName(); // ID interno

    double getBalance(Player player);

    boolean withdraw(Player player, double amount);

    boolean deposit(Player player, double amount);

    String getSymbol();
}