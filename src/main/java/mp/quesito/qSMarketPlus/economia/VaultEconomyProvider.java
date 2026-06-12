package mp.quesito.qSMarketPlus.economia;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class VaultEconomyProvider implements EconomyProvider {

    private final Economy economy;
    private final String symbol;

    public VaultEconomyProvider(Economy economy, FileConfiguration config) {
        this.economy = economy;
        this.symbol = config.getString("economies.vault.symbol", "$");
    }

    @Override
    public String getName() {
        return "vault";
    }

    /**
     * Obtiene el perfil de economía correcto resolviendo conflictos de Floodgate/Linking
     */
    private OfflinePlayer getTargetAccount(Player player) {
        // 1. Intentar por el nombre plano del jugador (Ej: .Quesito).
        // Esto es lo más seguro si plugins como Essentials crearon la cuenta al entrar.
        OfflinePlayer accountByName = Bukkit.getOfflinePlayer(player.getName());

        if (accountByName.getName() != null && economy.hasAccount(accountByName)) {
            // Si el plugin de economía reconoce explícitamente esta cuenta por nombre, la usamos.
            if (economy.getBalance(accountByName) > 0) {
                return accountByName;
            }
        }

        // 2. Si tiene 0 o no tiene cuenta activa por nombre, verificamos por su UUID
        // (Por si Floodgate reemplazó el UUID por el de Java debido al Global Linking)
        OfflinePlayer accountById = Bukkit.getOfflinePlayer(player.getUniqueId());
        if (accountById.getName() != null && economy.hasAccount(accountById)) {
            return accountById;
        }

        // Fallback final: Si ninguna tiene dinero registrado, devolvemos la cuenta por nombre predeterminada
        return accountByName;
    }

    @Override
    public double getBalance(Player player) {
        OfflinePlayer target = getTargetAccount(player);
        return economy.getBalance(target);
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        OfflinePlayer target = getTargetAccount(player);
        return economy.withdrawPlayer(target, amount).transactionSuccess();
    }

    @Override
    public boolean deposit(Player player, double amount) {
        OfflinePlayer target = getTargetAccount(player);
        return economy.depositPlayer(target, amount).transactionSuccess();
    }

    @Override
    public String getSymbol() {
        return symbol;
    }
}