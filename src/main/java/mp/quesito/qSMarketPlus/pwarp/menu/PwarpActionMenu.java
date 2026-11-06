package mp.quesito.qSMarketPlus.pwarp.menu;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.pwarp.Pwarp;
import mp.quesito.qSMarketPlus.pwarp.PwarpManager;
import mp.quesito.qSMarketPlus.pwarp.holder.PwarpActionHolder;
import mp.quesito.qSMarketPlus.pwarp.holder.PwarpHolder;
import mp.quesito.qSMarketPlus.utils.MenuItems;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import mp.quesito.qSMarketPlus.utils.MetaUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class PwarpActionMenu {

    private static final int SIZE = 27;

    public static void open(Player player, Pwarp warp, int page, PwarpHolder.Filter filter) {
        PwarpManager manager = QSMarketPlus.getInstance().getPwarpManager();
        boolean fav = manager.isFavorite(player.getUniqueId(), warp.getName());

        PwarpActionHolder holder = new PwarpActionHolder(player, warp, page, filter);
        Inventory inv = Bukkit.createInventory(holder, SIZE,
                MessageUtil.toLegacy("<dark_gray>» Pwarp: <yellow>" + warp.getDisplayName() + " <dark_gray>«"));

        ItemStack filler = MenuItems.glass();
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler.clone());
        }

        inv.setItem(13, infoItem(warp, player, manager));
        inv.setItem(11, teleportItem(warp));
        inv.setItem(15, favoriteItem(warp, fav));
        inv.setItem(22, MenuItems.backButton());

        player.openInventory(inv);
    }

    private static ItemStack infoItem(Pwarp warp, Player viewer, PwarpManager manager) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        try {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(warp.getOwner()));
        } catch (Exception ignored) {
        }

        meta.setDisplayName(MessageUtil.toLegacy("<yellow>" + warp.getDisplayName()));

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.toLegacy("<gray>Dueño: <white>" + warp.getOwnerName()));

        if (warp.getDescription() != null && !warp.getDescription().isEmpty()) {
            lore.addAll(MessageUtil.loreToLegacy(PwarpMenu.wrapDescription(warp.getDescription())));
        }

        lore.add("");
        boolean fav = manager.isFavorite(viewer.getUniqueId(), warp.getName());
        lore.add(MessageUtil.toLegacy(fav ? "<red>❤ En favoritos</red>" : "<gray>Aún no es favorito</gray>"));

        meta.setLore(lore);
        MetaUtil.setTag(meta, "btn", "info");
        head.setItemMeta(meta);
        return head;
    }

    private static ItemStack teleportItem(Pwarp warp) {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy("<green>» Ir al warp</green>"));
        meta.setLore(MessageUtil.loreToLegacy(List.of(
                "",
                "<gray>Teleportarse a la tienda de <white>" + warp.getOwnerName() + "</white>.",
                "<dark_green>» Click para ir</dark_green>"
        )));
        MetaUtil.setTag(meta, "btn", "teleport");
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack favoriteItem(Pwarp warp, boolean fav) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (fav) {
            meta.setDisplayName(MessageUtil.toLegacy("<gold>★ Quitar de favoritos</gold>"));
            meta.setLore(MessageUtil.loreToLegacy(List.of(
                    "",
                    "<gray>Este pwarp está en tus favoritos.",
                    "<yellow>» Click para quitarlo</yellow>"
            )));
        } else {
            meta.setDisplayName(MessageUtil.toLegacy("<red>☆ Añadir a favoritos</red>"));
            meta.setLore(MessageUtil.loreToLegacy(List.of(
                    "",
                    "<gray>Guárdalo para encontrarlo más rápido.",
                    "<yellow>» Click para añadirlo</yellow>"
            )));
        }
        MetaUtil.setTag(meta, "btn", "fav");
        item.setItemMeta(meta);
        return item;
    }
}