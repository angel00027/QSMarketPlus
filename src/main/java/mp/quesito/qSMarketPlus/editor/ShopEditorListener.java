package mp.quesito.qSMarketPlus.editor;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import mp.quesito.qSMarketPlus.utils.MetaUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ShopEditorListener implements Listener {

    private final ShopEditorManager manager;

    public ShopEditorListener(ShopEditorManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof AdminHolder holder)) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        String btn = MetaUtil.getTag(meta, "btn");
        if (btn == null) return;

        String value = MetaUtil.getTag(meta, "value");
        String item = MetaUtil.getTag(meta, "item");

        switch (btn) {
            case "close" -> player.closeInventory();

            case "reload" -> {
                manager.reloadAll(player);
                ShopEditorMenu.main(player);
            }

            case "cat_list" -> ShopEditorMenu.categories(player, 0);
            case "main_back" -> ShopEditorMenu.main(player);
            case "cat_back" -> ShopEditorMenu.categories(player, 0);

            case "cat_add" -> {
                manager.setPending(player, "cat_new", null, null);
                prompt(player, "<yellow>Escribe el ID de la nueva categoría</yellow> <gray>(dejar el valor, luego la editas).</gray>");
            }

            case "cat_item" -> ShopEditorMenu.categoryEdit(player, value);

            case "cat_name" -> {
                manager.setPending(player, "cat_name", value, null);
                prompt(player, "<yellow>Escribe el nuevo nombre</yellow> <gray>(puedes usar MiniMessage).</gray>");
            }
            case "cat_material" -> {
                manager.setPending(player, "cat_material", value, null);
                prompt(player, "<yellow>Escribe el material</yellow> <gray>(ej: DIAMOND, REDSTONE, PLAYER_HEAD).</gray>");
            }
            case "cat_slot" -> {
                manager.setPending(player, "cat_slot", value, null);
                prompt(player, "<yellow>Escribe el slot</yellow> <gray>(número) o <white>auto</white>.</gray>");
            }
            case "cat_perm" -> {
                manager.setPending(player, "cat_perm", value, null);
                prompt(player, "<yellow>Escribe el permiso requerido</yellow> <gray>o <white>ninguno</white>.</gray>");
            }
            case "cat_group" -> {
                manager.setPending(player, "cat_group", value, null);
                prompt(player, "<yellow>Escribe el grupo requerido</yellow> <gray>o <white>ninguno</white>.</gray>");
            }
            case "cat_items" -> ShopEditorMenu.items(player, value, 0);
            case "cat_delete" -> manager.deleteCategory(player, value);

            case "items_back" -> ShopEditorMenu.categoryEdit(player, value);

            case "item_add" -> manager.addItemFromHand(player, value);
            case "item_item" -> ShopEditorMenu.itemEdit(player, value, item);

            case "it_back" -> ShopEditorMenu.items(player, value, 0);

            case "it_buy" -> {
                manager.setPending(player, "item_buy", value, item);
                prompt(player, "<yellow>Escribe el precio de compra</yellow> <gray>(0 = gratis).</gray>");
            }
            case "it_sell" -> {
                manager.setPending(player, "item_sell", value, item);
                prompt(player, "<yellow>Escribe el precio de venta</yellow> <gray>(0 = no se compra).</gray>");
            }
            case "it_eco" -> {
                manager.setPending(player, "item_eco", value, item);
                prompt(player, "<yellow>Escribe la moneda</yellow> <gray>(vault, xp, levels, points).</gray>");
            }
            case "it_name" -> {
                manager.setPending(player, "item_name", value, item);
                prompt(player, "<yellow>Escribe el nuevo nombre</yellow> <gray>(puedes usar MiniMessage).</gray>");
            }
            case "it_mat" -> {
                manager.setPending(player, "item_material", value, item);
                prompt(player, "<yellow>Escribe el material</yellow> <gray>(ej: DIAMOND, PAPER).</gray>");
            }
            case "it_delete" -> manager.deleteItem(player, value, item);

            case "page_nav" -> {
                int page = holder.page;
                boolean prev = "prev".equals(item);
                if ("items".equals(value)) {
                    ShopEditorMenu.items(player, holder.categoryId, prev ? page - 1 : page + 1);
                } else {
                    ShopEditorMenu.categories(player, prev ? page - 1 : page + 1);
                }
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        if (manager.getPending(player.getUniqueId()) == null) return;

        // El jugador está escribiendo un valor para el editor
        e.setCancelled(true);

        final String msg = e.getMessage();
        Bukkit.getScheduler().runTask(QSMarketPlus.getInstance(),
                () -> manager.handleChat(player, msg));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        manager.clearPending(e.getPlayer().getUniqueId());
    }

    private void prompt(Player player, String text) {
        MessageUtil.msg(player, text + " <gray>» escribe <white>cancelar</white> para salir.</gray>");
    }
}