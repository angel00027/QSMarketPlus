package mp.quesito.qSMarketPlus.commands;

import mp.quesito.qSMarketPlus.shop.browser.ShopBrowser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /tiendas: buscador de tiendas de jugadores. */
public class TiendasCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }
        ShopBrowser.openMain(player, 1);
        return true;
    }
}
