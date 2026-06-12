package mp.quesito.qSMarketPlus.commands;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.bedrock.BedrockFormMenu; // 🔥 Importamos tu nuevo paquete Bedrock
import mp.quesito.qSMarketPlus.manager.CategoryManager;
import mp.quesito.qSMarketPlus.shop.CategoryMenu;
import mp.quesito.qSMarketPlus.shop.ProShopMenu;
import mp.quesito.qSMarketPlus.utils.PlatformUtil; // 🔥 Importamos el utilitario de plataforma
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {

    private final CategoryManager manager;

    public ShopCommand(QSMarketPlus plugin, CategoryManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores pueden usar este comando.");
            return true;
        }

        // ========================================================
        // 🔥 FILTRO INTERCEPTOR PARA JUGADORES DE BEDROCK (Geyser)
        // ========================================================
        if (PlatformUtil.isBedrock(player)) {
            // Se ejecuta de manera asíncrona o directa según la API de Floodgate
            BedrockFormMenu.openCategories(player, manager);
            return true;
        }

        // ========================================================
        // FLUJO NORMAL PARA JUGADORES DE JAVA
        // ========================================================
        String type = QSMarketPlus.getInstance()
                .getConfig()
                .getString("menu", "normal");

        if (type.equalsIgnoreCase("pro")) {
            ProShopMenu.open(
                    player,
                    manager,
                    QSMarketPlus.getInstance().getItemManager(),
                    0,
                    0
            );
        } else {
            CategoryMenu.open(player, manager);
        }

        return true;
    }
}