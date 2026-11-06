package mp.quesito.qSMarketPlus.auction.menu;

import mp.quesito.qSMarketPlus.auction.AuctionItem;
import mp.quesito.qSMarketPlus.auction.holder.AHAdminHolder;
import mp.quesito.qSMarketPlus.utils.MenuItems;
import mp.quesito.qSMarketPlus.utils.MetaUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class AHAdminMenu {

    private static final int SIZE = 27;

    public static void open(Player admin, AuctionItem auc, String source, UUID subject, int page) {

        AHAdminHolder holder = new AHAdminHolder(auc, source, subject, page);
        Inventory inv = Bukkit.createInventory(holder, SIZE,
                AHUi.title("admin", "<dark_red>Admin</dark_red> » Subasta"));

        // Ítem principal (lo que se subastó / se subastó)
        boolean corrupt = !auc.isBulk() && (auc.item == null || auc.item.getType().isAir());
        ItemStack main;
        if (auc.isBulk()) {
            main = new ItemStack(Material.CHEST);
        } else if (!corrupt) {
            main = auc.item.clone();
        } else {
            main = new ItemStack(Material.BARRIER);
        }
        ItemMeta mainMeta = main.getItemMeta();
        mainMeta.setDisplayName(MenuItems.miniToLegacy(
                "<yellow>" + (corrupt ? "Item corrupto" : (auc.isBulk() ? "Pack de items" : auc.getReadableName()))));

        List<String> lore = new ArrayList<>();
        if (corrupt) {
            lore.add(MenuItems.miniToLegacy("<red>No se pudo deserializar (base64 inválido)."));
            lore.add(MenuItems.miniToLegacy("<gray>Puedes eliminarla con el botón rojo."));
        }
        String sellerName = Bukkit.getOfflinePlayer(auc.seller).getName();
        lore.add(MenuItems.miniToLegacy("<gray>Vendedor: <white>" + (sellerName != null ? sellerName : "Desconocido")));

        if (auc.buyer != null) {
            String buyerName = Bukkit.getOfflinePlayer(auc.buyer).getName();
            lore.add(MenuItems.miniToLegacy("<gray>Comprador: <white>" + (buyerName != null ? buyerName : "Desconocido")));
        }

        lore.add(MenuItems.miniToLegacy("<gray>Precio: <green>$" + AHUi.money(auc.price)));
        lore.add(MenuItems.miniToLegacy("<gray>ID: <white>" + auc.id));
        lore.add(MenuItems.miniToLegacy("<gray>Estado: <white>" + auc.status.name()));

        if (auc.isBulk()) {
            int count = 0;
            for (ItemStack it : auc.container)
                if (it != null && !it.getType().isAir()) count++;
            lore.add(MenuItems.miniToLegacy("<gray>Items en pack: <white>" + count));
        }

        mainMeta.setLore(lore);
        main.setItemMeta(mainMeta);
        inv.setItem(4, main);

        if (auc.isBulk()) {
            inv.setItem(11, AHUi.button("view", Material.CHEST, "<yellow>Ver Contenido", "action", "view"));
        }

        inv.setItem(12, AHUi.button("copy", Material.BOOK, "<aqua>Obtener Copia", "action", "copy"));
        inv.setItem(13, AHUi.button("delete", Material.BARRIER, "<red>Eliminar Subasta", "action", "delete"));
        inv.setItem(15, AHUi.button("back", Material.ARROW, "<yellow>Volver", "action", "back"));
        AHUi.fill(inv);

        admin.openInventory(inv);
    }

    private static ItemStack button(Material mat, String name, String tag) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MenuItems.miniToLegacy(name));
        MetaUtil.setTag(meta, "action", tag);
        item.setItemMeta(meta);
        return item;
    }

    private static void fill(Inventory inv) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);

        for (int i = 0; i < inv.getSize(); i++)
            inv.setItem(i, filler);
    }
}