package mp.quesito.qSMarketPlus.utils;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import org.bukkit.entity.Player;

public class EconomyUtil {

    public static boolean has(Player player, double amount) {
        return QSMarketPlus.economy.has(player, amount);
    }

    public static boolean withdraw(Player player, double amount) {
        return QSMarketPlus.economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public static boolean deposit(Player player, double amount) {
        return QSMarketPlus.economy.depositPlayer(player, amount).transactionSuccess();
    }

    public static double balance(Player player) {
        return QSMarketPlus.economy.getBalance(player);
    }
}
