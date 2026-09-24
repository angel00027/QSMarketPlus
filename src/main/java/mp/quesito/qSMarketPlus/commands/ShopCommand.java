package mp.quesito.qSMarketPlus.commands;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.bedrock.BedrockFormMenu;
import mp.quesito.qSMarketPlus.manager.CategoryManager;
import mp.quesito.qSMarketPlus.shop.CategoryMenu;
import mp.quesito.qSMarketPlus.shop.ProShopMenu;
import mp.quesito.qSMarketPlus.utils.PlatformUtil;
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
        // 🔒 PERMISO PARA USAR /SHOP
        // ========================================================
        if (!player.hasPermission("qsmarketplus.shop")) {
            player.sendMessage("§cNo tienes permiso para usar este comando.");
            return true;
        }

        // ========================================================
        // 🔥 FILTRO PARA JUGADORES DE BEDROCK
        // ========================================================
        boolean bedrockGui = QSMarketPlus.getInstance()
                .getConfig()
                .getBoolean("bedrock.shop-gui", true);

        if (PlatformUtil.isBedrock(player) && bedrockGui) {
            BedrockFormMenu.openCategories(player, manager);
            return true;
        }

        // ========================================================
        // 🖥️ FLUJO NORMAL PARA JAVA
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