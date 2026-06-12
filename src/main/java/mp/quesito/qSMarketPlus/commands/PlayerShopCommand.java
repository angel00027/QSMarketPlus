package mp.quesito.qSMarketPlus.commands;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.manager.PlayerShopManager;
import mp.quesito.qSMarketPlus.shop.PlayerShop;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class PlayerShopCommand implements CommandExecutor {

    private final PlayerShopManager playerShopManager;

    public PlayerShopCommand(QSMarketPlus plugin) {
        this.playerShopManager = plugin.getShopManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // 1. Verificar que sea un jugador
        if (!(sender instanceof Player p)) {
            // ✨ CORRECCIÓN: Usar el método nativo de Bukkit para la consola
            sender.sendMessage("§cEste comando solo puede ser ejecutado por jugadores.");
            return true;
        }

        // 2. Verificar el permiso único para este comando
        if (!p.hasPermission("qsmarket.shop.set")) {
            MessageUtil.msg(p, "<red>No tienes permisos para gestionar tu tienda.</red>");
            return true;
        }

        // 3. Validar argumentos
        if (args.length != 2) {
            MessageUtil.msg(p, "<red>Uso correcto:</red> <gray>/" + label + " <precio> <cantidad></gray>");
            return true;
        }

        // 4. Obtener la tienda del jugador
        List<PlayerShop> shops = playerShopManager.getShops(p.getUniqueId());
        PlayerShop shop = shops.isEmpty() ? null : shops.get(0);

        if (shop == null) {
            MessageUtil.msg(p, "<red>No posees ninguna tienda activa actualmente.</red>");
            return true;
        }

        // 5. Procesar y actualizar los valores
        try {
            double price = Double.parseDouble(args[0]);
            int amount = Integer.parseInt(args[1]);

            if (price < 0 || amount <= 0) {
                MessageUtil.msg(p, "<red>El precio y la cantidad deben ser valores positivos superiores a cero.</red>");
                return true;
            }

            shop.setPrice(price);
            shop.setAmountPerSale(amount);
            playerShopManager.updateShop(shop);
            shop.updateSign();

            MessageUtil.msg(p, "<green>✔ Tu tienda ha sido actualizada con éxito.</green>");

        } catch (NumberFormatException e) {
            MessageUtil.msg(p, "<red>Error:</red> <gray>Por favor, introduce valores numéricos válidos.</gray>");
        }

        return true;
    }
}