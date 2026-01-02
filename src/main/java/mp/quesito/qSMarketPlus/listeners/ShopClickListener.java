package mp.quesito.qSMarketPlus.listeners;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.auction.AuctionItem;
import mp.quesito.qSMarketPlus.auction.AuctionManager;
import mp.quesito.qSMarketPlus.auction.holder.*;
import mp.quesito.qSMarketPlus.auction.menu.*;
import mp.quesito.qSMarketPlus.holder.*;
import mp.quesito.qSMarketPlus.manager.CategoryManager;
import mp.quesito.qSMarketPlus.manager.ItemManager;
import mp.quesito.qSMarketPlus.manager.UniquePurchaseManager;
import mp.quesito.qSMarketPlus.shop.*;
import mp.quesito.qSMarketPlus.utils.*;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.BiConsumer;

public class ShopClickListener implements Listener {

    private final CategoryManager categoryManager;
    private final ItemManager itemManager;
    private final QSMarketPlus plugin = QSMarketPlus.getInstance();

    // Valid holders centralizados
    private static final Set<Class<?>> VALID_HOLDERS = Set.of(
            CategoryHolder.class, ItemsHolder.class, ActionHolder.class,
            AmountHolder.class, ConfirmHolder.class, AHHolder.class,
            AHConfirmHolder.class, AHPreviewHolder.class,
            AHHistoryHolder.class, AHActionHolder.class, AHExpiredHolder.class
    );

    // Map de handlers centralizados
    private final Map<Class<?>, BiConsumer<InventoryClickEvent, Player>> handlers = new HashMap<>();


    public ShopClickListener(CategoryManager categoryManager, ItemManager itemManager) {
        this.categoryManager = categoryManager;
        this.itemManager = itemManager;


        handlers.put(CategoryHolder.class, (e, player) -> {
            CategoryHolder holder = (CategoryHolder) e.getView().getTopInventory().getHolder();
            handleCategoryClick(e, player);
        });

        handlers.put(ItemsHolder.class, (e, player) -> {
            ItemsHolder holder = (ItemsHolder) e.getView().getTopInventory().getHolder();
            handleItemsClick(e, player, holder);
        });

        handlers.put(ActionHolder.class, (e, player) -> {
            ActionHolder holder = (ActionHolder) e.getView().getTopInventory().getHolder();
            handleActionClick(e, player, holder);
        });

        handlers.put(AmountHolder.class, (e, player) -> {
            AmountHolder holder = (AmountHolder) e.getView().getTopInventory().getHolder();
            handleAmountClick(e, player, holder);
        });

        handlers.put(ConfirmHolder.class, (e, player) -> {
            ConfirmHolder holder = (ConfirmHolder) e.getView().getTopInventory().getHolder();
            handleConfirmClick(e, player, holder);
        });

        handlers.put(AHHolder.class, (e, player) -> {
            AHHolder holder = (AHHolder) e.getView().getTopInventory().getHolder();
            handleAHClick(e, player, holder);
        });

        handlers.put(AHConfirmHolder.class, (e, player) -> {
            AHConfirmHolder holder = (AHConfirmHolder) e.getView().getTopInventory().getHolder();
            handleAHConfirmClick(e, player, holder);
        });

        handlers.put(AHPreviewHolder.class, (e, player) -> {
            AHPreviewHolder holder = (AHPreviewHolder) e.getView().getTopInventory().getHolder();
            handleAHPreviewClick(e, player, holder);
        });

        handlers.put(AHHistoryHolder.class, (e, player) -> {
            AHHistoryHolder holder = (AHHistoryHolder) e.getView().getTopInventory().getHolder();
            handleAHHistoryClick(e, player, holder);
        });

        handlers.put(AHActionHolder.class, (e, player) -> {
            AHActionHolder holder = (AHActionHolder) e.getView().getTopInventory().getHolder();
            handleAHActionClick(e, player, holder);
        });

        handlers.put(AHExpiredHolder.class, (e, player) -> {
            AHExpiredHolder holder = (AHExpiredHolder) e.getView().getTopInventory().getHolder();
            handleAHExpiredClick(e, player, holder);
        });

    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {

        if (e.getClickedInventory() == null) return;

        Inventory top = e.getView().getTopInventory();
        Inventory clickedInv = e.getClickedInventory();

        if (!VALID_HOLDERS.contains(top.getHolder().getClass())) return;

        // Bloquear shift click fuera del menú
        if (clickedInv != top && e.isShiftClick()) {
            e.setCancelled(true);
            return;
        }

        e.setCancelled(true);

        Player player = (Player) e.getWhoClicked();

        // Llamada al handler correspondiente
        handlers.getOrDefault(top.getHolder().getClass(), (ev, pl) -> {}).accept(e, player);
    }

    // -----------------------------
    // ---------- NBT UTILS --------
    // -----------------------------
// Contar cuántos items iguales al Base64 del ShopItem tiene el jugador
    private int countMatchingItems(Player player, ItemStack template) {
        int count = 0;
        String templateBase64 = ItemSerializer.toBase64(template);

        for (ItemStack invItem : player.getInventory().getContents()) {
            if (invItem == null) continue;
            String itemBase64 = ItemSerializer.toBase64(invItem);
            if (templateBase64.equals(itemBase64)) {
                count += invItem.getAmount();
            }
        }

        return count;
    }

    // Remover una cantidad exacta de items iguales al Base64 del ShopItem
    private void removeMatchingItems(Player player, ItemStack template, int amount) {
        String templateBase64 = ItemSerializer.toBase64(template);

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack invItem = player.getInventory().getItem(i);
            if (invItem == null) continue;

            String itemBase64 = ItemSerializer.toBase64(invItem);
            if (templateBase64.equals(itemBase64)) {
                int itemAmount = invItem.getAmount();
                if (itemAmount <= amount) {
                    player.getInventory().setItem(i, null);
                    amount -= itemAmount;
                } else {
                    invItem.setAmount(itemAmount - amount);
                    player.getInventory().setItem(i, invItem);
                    amount = 0;
                }

                if (amount <= 0) break;
            }
        }
    }


    private void giveExactItems(Player player, ItemStack target, int amount) {
        ItemStack clone = target.clone();
        clone.setAmount(amount);
        player.getInventory().addItem(clone);
    }

    private boolean hasInventorySpace(Player p, ItemStack item, int amount) {
        int free = 0;
        for (ItemStack s : p.getInventory().getStorageContents()) {
            if (s == null || s.getType() == Material.AIR) {
                free += item.getMaxStackSize();
            } else if (s.isSimilar(item)) {
                free += (item.getMaxStackSize() - s.getAmount());
            }
        }
        return free >= amount;
    }

    // -----------------------------
    // -------- SHOP MENU ----------
    // -----------------------------

    private void handleCategoryClick(InventoryClickEvent e, Player player) {
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        String btn = MetaUtil.getTag(meta, "btn");
        String categoryId = MetaUtil.getTag(meta, "category");

        if ("close".equals(btn)) {
            player.closeInventory();
            return;
        }

        if ("sell_all".equals(btn)) {

            double totalMoney = 0;

            for (ShopCategory cat : categoryManager.getCategories().values()) {
                itemManager.loadCategoryItems(cat);

                for (ShopItem item : itemManager.getItems(cat.getId()).values()) {
                    if (item.getSell() <= 0) continue;

                    ItemStack real = item.getRealItem();
                    int total = countMatchingItems(player, real);
                    if (total <= 0) continue;

                    removeMatchingItems(player, real, total);
                    totalMoney += total * item.getSell();
                }
            }

            if (totalMoney > 0) {
                QSMarketPlus.economy.depositPlayer(player, totalMoney);
                Lang.msg(player, "sell_all_success", "money", totalMoney);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1.3f);
            } else {
                Lang.msg(player, "sell_all_empty");
            }
            return;
        }

        if (categoryId == null) return;

        ShopCategory cat = categoryManager.getCategories().get(categoryId);
        if (cat == null) return;

        if (!cat.canAccess(player)) {
            player.sendMessage("§cNo tienes acceso a esta categoría.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        itemManager.loadCategoryItems(cat);
        ItemsMenu.open(player, cat, itemManager, 1);
    }

    private void handleItemsClick(InventoryClickEvent e, Player player, ItemsHolder holder) {
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        String btn = MetaUtil.getTag(meta, "btn");
        String itemId = MetaUtil.getTag(meta, "shop_item");
        ShopCategory category = holder.getCategory();
        int page = holder.getPage();

        List<ShopItem> itemsList = new ArrayList<>(itemManager.getOrderedItems(category.getId()));
        UniquePurchaseManager upManager = plugin.getUniquePurchaseManager();

        // Botones del menú
        switch (btn != null ? btn : "") {
            case "back" -> CategoryMenu.open(player, categoryManager);
            case "prev_page" -> ItemsMenu.open(player, category, itemManager, page - 1);
            case "next_page" -> ItemsMenu.open(player, category, itemManager, page + 1);
            case "sort_price_asc" -> {
                itemsList.sort(Comparator.comparingDouble(ShopItem::getBuy));
                itemManager.setSortedItems(category.getId(), itemsList);
                ItemsMenu.refreshPage(player, category, itemManager, page, itemsList);
            }
            case "sort_price_desc" -> {
                itemsList.sort((a, b) -> Double.compare(b.getBuy(), a.getBuy()));
                itemManager.setSortedItems(category.getId(), itemsList);
                ItemsMenu.refreshPage(player, category, itemManager, page, itemsList);
            }
            case "sort_name" -> {
                itemsList.sort(Comparator.comparing(i -> MetaUtil.clean(MessageUtil.toLegacy(i.getName()))));
                itemManager.setSortedItems(category.getId(), itemsList);
                ItemsMenu.refreshPage(player, category, itemManager, page, itemsList);
            }
        }

        if (itemId == null) return;

        ShopItem clickedItem = itemManager.getItem(category.getId(), itemId);
        if (clickedItem == null) return;

        ItemStack real = clickedItem.getRealItem();

        // =========================
        // BLOQUEO ITEM ÚNICO
        // =========================
        if (clickedItem.isOnlyOnce() && upManager.hasPurchased(player, clickedItem)) {
            Lang.msg(player, "already_bought"); // Mensaje que ya lo compró
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            player.closeInventory();
            return;
        }

        // Venta rápida (SWAP_OFFHAND)
        if (e.getClick().name().equalsIgnoreCase("SWAP_OFFHAND")) {
            if (clickedItem.getSell() <= 0) {
                Lang.msg(player, "swapped_no_items");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            int total = countMatchingItems(player, real);
            if (total <= 0) {
                Lang.msg(player, "swapped_no_items");
                return;
            }

            removeMatchingItems(player, real, total);
            double money = clickedItem.getSell() * total;
            QSMarketPlus.economy.depositPlayer(player, money);

            Lang.msg(player, "sell_success",
                    "amount", total,
                    "item", MessageUtil.toLegacy(clickedItem.getName()),
                    "money", money
            );

            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1.3f);
            return;
        }

        // Compra rápida SHIFT_CLICK
        if (e.isShiftClick()) {

            int amount = real.getMaxStackSize() == 1 ? 1 : (e.isLeftClick() ? 1 : 64);
            double cost = clickedItem.getBuy() * amount;

            if (!QSMarketPlus.economy.has(player, cost)) {
                Lang.msg(player, "no_money");
                return;
            }

            if (!hasInventorySpace(player, real, amount)) {
                Lang.msg(player, "not_enough_space");
                return;
            }

            QSMarketPlus.economy.withdrawPlayer(player, cost);

            if (!clickedItem.getCommands().isEmpty()) {
                clickedItem.executeBuyCommands(player);
            } else {
                giveExactItems(player, real, amount);
            }

            // Marcar como comprado si es único
            if (clickedItem.isOnlyOnce()) {
                upManager.markPurchased(player, clickedItem);
            }

            Lang.msg(player, "buy_success",
                    "amount", amount,
                    "item", MessageUtil.toLegacy(clickedItem.getName()),
                    "money", cost
            );

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            return;
        }

        // Verifica permisos de acceso
        if (!clickedItem.canAccess(player)) {
            player.sendMessage("§cNo puedes comprar este ítem.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Si ejecuta comando de compra, cerrar menú
        if (!clickedItem.getCommands().isEmpty()) {
            clickedItem.executeBuyCommands(player);
            if (clickedItem.isOnlyOnce()) {
                upManager.markPurchased(player, clickedItem);
            }
            player.closeInventory();
            return;
        }

        // Abrir menú de acción normal
        ActionMenu.open(player, clickedItem, category);
    }


    // -----------------------------
    // -------- ACTION MENU --------
    // -----------------------------
    private void handleActionClick(InventoryClickEvent e, Player player, ActionHolder holder) {
        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ShopItem item = holder.getItem();
        ShopCategory category = holder.getCategory();

        if (MetaUtil.is(clicked, "buy")) {
            if (item.getMaterial().getMaxStackSize() == 1) {
                ConfirmMenu.open(player, item, category, 1, true);
            } else {
                AmountHolder amountHolder = new AmountHolder(item, category, true);
                AmountMenu.open(player, amountHolder);
            }
        } else if (MetaUtil.is(clicked, "sell")) {
            if (item.getSell() <= 0) {
                Lang.msg(player, "swapped_no_items");
                return;
            }
            if (item.getMaterial().getMaxStackSize() == 1) {
                ConfirmMenu.open(player, item, category, 1, false);
            } else {
                AmountHolder amountHolder = new AmountHolder(item, category, false);
                AmountMenu.open(player, amountHolder);
            }
        } else if (MetaUtil.is(clicked, "back")) {
            ItemsMenu.open(player, category, itemManager, 1);
        }
    }

    // -----------------------------
    // -------- AMOUNT MENU --------
    // -----------------------------
    private void handleAmountClick(InventoryClickEvent e, Player player, AmountHolder holder) {
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        e.setCancelled(true);

        ItemMeta meta = clicked.getItemMeta();
        ShopItem item = holder.getItem();
        ShopCategory category = holder.getCategory();
        boolean buying = holder.isBuying();
        int amount = holder.getAmount();
        int maxStack = item.getMaterial().getMaxStackSize();

        String btn = MetaUtil.getTag(meta, "btn");
        String value = MetaUtil.getTag(meta, "value");
        String special = MetaUtil.getTag(meta, "special");

        if ("back".equals(btn)) {
            ActionMenu.open(player, item, category);
            return;
        }

        if ("cancel".equals(btn)) {
            player.closeInventory();
            return;
        }

        if (maxStack == 1) {
            holder.setAmount(1);
            if ("confirm".equals(btn)) ConfirmMenu.open(player, item, category, 1, buying);
            else AmountMenu.open(player, holder);
            return;
        }

        if ("amount_mod".equals(btn) && value != null) {
            int mod = Integer.parseInt(value);
            int newAmount = Math.max(1, Math.min(maxStack, amount + mod));

            if (buying && !QSMarketPlus.economy.has(player, newAmount * item.getBuy())) {
                Lang.msg(player, "no_money");
                return;
            } else if (!buying && countMatchingItems(player, item.getRealItem()) < newAmount) {
                Lang.msg(player, "no_items");
                return;
            }

            holder.setAmount(newAmount);
            AmountMenu.open(player, holder);
            return;
        }

        if ("MAX".equals(special)) {
            int maxAmount = buying ? (int) Math.floor(QSMarketPlus.economy.getBalance(player) / item.getBuy())
                    : countMatchingItems(player, item.getRealItem());
            holder.setAmount(Math.max(1, Math.min(maxStack, maxAmount)));
            AmountMenu.open(player, holder);
            return;
        }

        if ("MIN".equals(special)) {
            holder.setAmount(1);
            AmountMenu.open(player, holder);
            return;
        }

        if ("confirm".equals(btn)) {
            double price = amount * (buying ? item.getBuy() : item.getSell());
            if (buying) {
                if (!QSMarketPlus.economy.has(player, price)) {
                    Lang.msg(player, "no_money");
                    return;
                }
                if (!hasInventorySpace(player, item.getRealItem(), amount)) {
                    Lang.msg(player, "not_enough_space");
                    return;
                }
                QSMarketPlus.economy.withdrawPlayer(player, price);
                giveExactItems(player, item.getRealItem(), amount);

                Lang.msg(player, "buy_success",
                        "amount", amount,
                        "item", MessageUtil.toLegacy(item.getName()),
                        "money", price);
            } else {
                if (countMatchingItems(player, item.getRealItem()) < amount) {
                    Lang.msg(player, "no_items");
                    return;
                }
                removeMatchingItems(player, item.getRealItem(), amount);
                QSMarketPlus.economy.depositPlayer(player, price);

                Lang.msg(player, "sell_success",
                        "amount", amount,
                        "item", MessageUtil.toLegacy(item.getName()),  // NO stripLegacy
                        "money", price);
            }
            player.playSound(player.getLocation(), buying ? Sound.ENTITY_PLAYER_LEVELUP : Sound.ENTITY_VILLAGER_YES, 1f, 1.2f);
            player.closeInventory();
        }
    }

    // -----------------------------
    // -------- CONFIRM MENU --------
    // -----------------------------

    private void handleConfirmClick(InventoryClickEvent e, Player player, ConfirmHolder holder) {
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        e.setCancelled(true);

        ShopItem item = holder.getItem();
        ShopCategory category = holder.getCategory();
        boolean buying = holder.isBuying();
        int amount = holder.getAmount();
        ItemStack template = item.getRealItem();

        UniquePurchaseManager upManager = plugin.getUniquePurchaseManager();

        if (MetaUtil.is(clicked, "back")) {
            ActionMenu.open(player, item, category);
            return;
        }

        if (MetaUtil.is(clicked, "cancel")) {
            player.closeInventory();
            return;
        }

        if (!MetaUtil.is(clicked, "confirm")) return;

        double price = amount * (buying ? item.getBuy() : item.getSell());

        if (buying) {

            if (!QSMarketPlus.economy.has(player, price)) {
                Lang.msg(player, "no_money");
                return;
            }

            // ❌ Aquí también va la verificación
            if (item.isOnlyOnce() && upManager.hasPurchased(player, item)) {
                Lang.msg(player, "already_bought");
                player.closeInventory();
                return;
            }

            if (!item.getCommands().isEmpty()) {
                item.executeBuyCommands(player);
            } else {
                if (!hasInventorySpace(player, template, amount)) {
                    Lang.msg(player, "not_enough_space");
                    return;
                }
                giveExactItems(player, template, amount);
            }

            QSMarketPlus.economy.withdrawPlayer(player, price);

            // Marca item como comprado si es único
            if (item.isOnlyOnce()) upManager.markPurchased(player, item);

            MessageUtil.lang(player, "buy_success",
                    Placeholder.parsed("amount", String.valueOf(amount)),
                    Placeholder.parsed("item", item.getName()),
                    Placeholder.parsed("money", String.valueOf(price))
            );
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            player.closeInventory();

        } else { // venta

            if (!item.getSellCommands().isEmpty()) {
                item.executeSellCommands(player);
            } else {
                if (countMatchingItems(player, template) < amount) {
                    Lang.msg(player, "no_items");
                    return;
                }
                removeMatchingItems(player, template, amount);
            }

            QSMarketPlus.economy.depositPlayer(player, price);

            MessageUtil.lang(player, "sell_success",
                    Placeholder.parsed("amount", String.valueOf(amount)),
                    Placeholder.parsed("item", item.getName()),
                    Placeholder.parsed("money", String.valueOf(price))
            );
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1.2f);
            player.closeInventory();
        }
    }


    // -----------------------------
    // ---------- AH MENU -----------
    // -----------------------------

    private void handleAHClick(InventoryClickEvent e, Player player, AHHolder holder) {
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String btn = MetaUtil.getTag(clicked.getItemMeta(), "btn");
        String filterTag = MetaUtil.getTag(clicked.getItemMeta(), "filter");
        String indexTag = MetaUtil.getTag(clicked.getItemMeta(), "auction_index");

        // Filtros
        if (filterTag != null) {
            handleFilterClick(player, holder, filterTag);
            return;
        }

        // Navegación
        if (btn != null && handleNavigationClick(player, holder, btn)) return;

        // Clic en subasta
        if (indexTag != null) {
            int index;
            try { index = Integer.parseInt(indexTag); } catch (NumberFormatException ex) { return; }

            List<AuctionItem> auctions = holder.getPageItems();
            if (index < 0 || index >= auctions.size()) return;

            AuctionItem auction = auctions.get(index);
            AHActionMenu.open(player, auction, holder.getPage());
        }
    }


    private void handleAHConfirmClick(InventoryClickEvent e, Player player, AHConfirmHolder holder) {
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String btn = MetaUtil.getTag(clicked.getItemMeta(), "btn");
        if (btn == null) return;

        if ("cancel".equals(btn)) {
            player.closeInventory();
            Lang.msg(player, "ah_cancelled");
            return;
        }

        if (!"confirm".equals(btn)) return;

        AuctionManager manager = QSMarketPlus.getInstance().getAuctionManager();
        double price = holder.getPrice();
        boolean bulk = holder.isBulk(); // true si es sell inventario, false si es sell mano

        List<ItemStack> itemsList = new ArrayList<>();

        if (bulk) {
            // Tomar todos los items del inventario que no sean AIR
            for (ItemStack it : player.getInventory().getContents()) {
                if (it != null && it.getType() != Material.AIR) itemsList.add(it.clone());
            }

            if (itemsList.isEmpty()) {
                Lang.msg(player, "no_items");
                return;
            }

        } else {
            // Solo item en la mano
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) {
                Lang.msg(player, "no_items");
                return;
            }
            itemsList.add(hand.clone());
        }

        // Validaciones completadas, ahora eliminar items del inventario
        if (bulk) {
            for (ItemStack it : itemsList) {
                player.getInventory().removeItem(it);
            }
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        // Crear la subasta según sea bulk o single
        if (bulk || itemsList.size() > 1) {
            // Subasta de varios items → cofre
            manager.createBulkAuction(player, itemsList.toArray(new ItemStack[0]), price);
            Lang.msg(player, "ah_created_bulk", "count", itemsList.size(), "price", price);
        } else {
            // Subasta de un solo item → single
            manager.createAuction(player, itemsList.get(0), price);
            Lang.msg(player, "ah_created", "item", itemsList.get(0).getType().name(), "price", price);
        }

        player.closeInventory();
    }

    private void handleAHPreviewClick(InventoryClickEvent e, Player player, AHPreviewHolder holder) {
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String btn = MetaUtil.getTag(clicked.getItemMeta(), "btn");
        if ("back".equals(btn)) {
            AHMenu.open(player);
        }
    }

    private void handleAHHistoryClick(InventoryClickEvent e, Player player, AHHistoryHolder holder) {
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String btn = MetaUtil.getTag(clicked.getItemMeta(), "btn");
        if ("prev".equals(btn)) {
            AHHistoryMenu.open(player, holder.getPage() - 1);
        } else if ("next".equals(btn)) {
            AHHistoryMenu.open(player, holder.getPage() + 1);
        } else if ("close".equals(btn)) {
            player.closeInventory();
        }
    }
    private void handleAHActionClick(InventoryClickEvent e, Player player, AHActionHolder holder) {
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String action = MetaUtil.getTag(clicked.getItemMeta(), "action");
        if (action == null) return;

        AuctionItem auc = holder.getAuction();
        AuctionManager manager = QSMarketPlus.getInstance().getAuctionManager();

        switch (action) {
            case "back" -> AHMenu.open(player, holder.getPage());
            case "view" -> AHPreviewMenu.open(player, auc);
            case "cancel" -> {
                if (!auc.seller.equals(player.getUniqueId())) {
                    Lang.msg(player, "ah_not_your_auction");
                    return;
                }
                manager.cancelAuction(player, auc);
                Lang.msg(player, "ah_cancelled");
                AHMenu.open(player, holder.getPage());
            }
            case "buy" -> {
                // Validar items
                List<ItemStack> items = new ArrayList<>();
                boolean bulk = auc.isBulk();

                if (bulk) {
                    if (auc.container == null || auc.container.length == 0) {
                        Lang.msg(player, "ah_error");
                        return;
                    }
                    for (ItemStack it : auc.container) {
                        if (it != null && it.getType() != Material.AIR) items.add(it.clone());
                    }
                    if (items.isEmpty()) {
                        Lang.msg(player, "ah_error");
                        return;
                    }
                } else {
                    if (auc.item == null || auc.item.getType() == Material.AIR) {
                        Lang.msg(player, "ah_error");
                        return;
                    }
                    items.add(auc.item.clone());
                }

                double price = auc.price;

                // Validar dinero
                if (!QSMarketPlus.economy.has(player, price)) {
                    Lang.msg(player, "no_money");
                    return;
                }

                boolean useChest = bulk || items.size() > 1;

                if (useChest) {
                    // Verificar espacio para al menos 1 cofre
                    if (player.getInventory().firstEmpty() == -1) {
                        Lang.msg(player, "not_enough_space_chest");
                        return;
                    }

                    // Crear cofre portátil con los items
                    Inventory tempChest = Bukkit.createInventory(null, 54, "Items de Subasta");
                    for (ItemStack it : items) givePartialToInventory(tempChest, it, it.getAmount());

                    ItemStack chestItem = new ItemStack(Material.CHEST);
                    BlockStateMeta meta = (BlockStateMeta) chestItem.getItemMeta();
                    Chest chest = (Chest) meta.getBlockState();
                    chest.getInventory().setContents(tempChest.getContents());
                    meta.setBlockState(chest);
                    chestItem.setItemMeta(meta);

                    // Dar cofre y cobrar
                    player.getInventory().addItem(chestItem);
                    QSMarketPlus.economy.withdrawPlayer(player, price);

                    Lang.msg(player, "ah_bought_chest", "money", price, "item_count", items.size());

                } else {
                    // Solo 1 item, dar directo al inventario
                    ItemStack singleItem = items.get(0);
                    if (!hasInventorySpace(player, singleItem, singleItem.getAmount())) {
                        Lang.msg(player, "not_enough_space");
                        return;
                    }

                    forceGiveAuctionItem(player, auc);
                    QSMarketPlus.economy.withdrawPlayer(player, price);

                    Lang.msg(player, "ah_bought", "money", price, "item", singleItem.getType().name());
                }

                // Pagar al vendedor
                OfflinePlayer seller = Bukkit.getOfflinePlayer(auc.seller);
                QSMarketPlus.economy.depositPlayer(seller, price);

                // Marcar como comprado
                if (!manager.buyAuction(player, auc)) {
                    Lang.msg(player, "ah_error");
                }

                AHMenu.open(player, holder.getPage());
            }
        }
    }

    private void forceGiveAuctionItem(Player p, AuctionItem auc) {
        if (auc == null) return;

        if (!auc.isBulk() && auc.item != null) {
            ItemStack clone = auc.item.clone();
            p.getInventory().addItem(clone);
            auc.item = null;
            QSMarketPlus.getInstance().getAuctionManager().markAsTaken(auc);
        } else if (auc.isBulk() && auc.container != null) {
            for (int i = 0; i < auc.container.length; i++) {
                ItemStack it = auc.container[i];
                if (it != null && it.getType() != Material.AIR) {
                    p.getInventory().addItem(it.clone());
                    auc.container[i] = null;
                }
            }
            QSMarketPlus.getInstance().getAuctionManager().markAsTaken(auc);
        }
    }




    private void handleAHExpiredClick(InventoryClickEvent e, Player player, AHExpiredHolder holder) {
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String btn = MetaUtil.getTag(clicked.getItemMeta(), "btn");

        if ("prev".equals(btn)) {
            AHExpiredMenu.open(player, holder.getPage() - 1);
        } else if ("next".equals(btn)) {
            AHExpiredMenu.open(player, holder.getPage() + 1);
        } else if ("close".equals(btn)) {
            player.closeInventory();
        } else {
            // Reclamación individual
            Integer index = MetaUtil.getInt(clicked.getItemMeta(), "expired_index");
            if (index != null) {
                AuctionItem auc = holder.getItems().get(index);
                if (!auc.seller.equals(player.getUniqueId())) {
                    player.sendMessage("§cEste item no te pertenece.");
                    return;
                }

                // Crear un cofre temporal
                Inventory tempChest = Bukkit.createInventory(null, 54, "Item Expirado");
                claimSingleExpiredToChest(player, auc, tempChest);

                // Dar cofre portátil al jugador
                ItemStack chestItem = new ItemStack(Material.CHEST);
                BlockStateMeta meta = (BlockStateMeta) chestItem.getItemMeta();
                Chest chest = (Chest) meta.getBlockState();
                chest.getInventory().setContents(tempChest.getContents());
                meta.setBlockState(chest);
                chestItem.setItemMeta(meta);

                player.getInventory().addItem(chestItem);
                Lang.msg(player, "ah_claimed_chest", "count", tempChest.getContents().length);

                // Actualizar lista de items expirados y refrescar menú
                holder.getItems().remove(auc);
                AHExpiredMenu.open(player, holder.getPage());
            }
        }
    }


    // -------------------------
    // FILTROS
    // -------------------------
    private void handleFilterClick(Player player, AHHolder holder, String filterTag) {
        try {
            AHHolder.FilterMode mode = AHHolder.FilterMode.valueOf(filterTag);
            AHHolder.setFilter(player, mode);

            switch (mode) {
                case PRICE_ASC -> Lang.msg(player, "ah_filter_price_asc");
                case PRICE_DESC -> Lang.msg(player, "ah_filter_price_desc");
                case ONLY_ITEMS -> Lang.msg(player, "ah_filter_only_items");
                case ONLY_BULK  -> Lang.msg(player, "ah_filter_only_bulk");
                case NONE       -> Lang.msg(player, "ah_filter_reset");
            }

            AHMenu.open(player, holder.getPage());

        } catch (IllegalArgumentException ignored) {
            // ignorar si no existe
        }
    }

    // -------------------------
    // NAVEGACIÓN
    // -------------------------
    private boolean handleNavigationClick(Player player, AHHolder holder, String btn) {
        return switch (btn) {
            case "close" -> { player.closeInventory(); yield true; }
            case "next" -> { AHMenu.open(player, holder.getPage() + 1); yield true; }
            case "prev" -> { AHMenu.open(player, holder.getPage() - 1); yield true; }
            default -> false;
        };
    }


    // -------------------------
    // AYUDA: Dar items a un inventario
    // -------------------------
    private void givePartialToInventory(Inventory inv, ItemStack item, int amount) {
        int remaining = amount;

        // Primero llenar stacks existentes similares
        for (int i = 0; i < inv.getSize() && remaining > 0; i++) {
            ItemStack slot = inv.getItem(i);
            if (slot == null || slot.getType() == Material.AIR) continue;
            if (!slot.isSimilar(item)) continue;

            int free = slot.getMaxStackSize() - slot.getAmount();
            if (free <= 0) continue;

            int take = Math.min(free, remaining);
            slot.setAmount(slot.getAmount() + take);
            inv.setItem(i, slot);
            remaining -= take;
        }

        // Luego llenar slots vacíos
        for (int i = 0; i < inv.getSize() && remaining > 0; i++) {
            ItemStack slot = inv.getItem(i);
            if (slot == null || slot.getType() == Material.AIR) {
                int take = Math.min(item.getMaxStackSize(), remaining);
                ItemStack clone = item.clone();
                clone.setAmount(take);
                inv.setItem(i, clone);
                remaining -= take;
            }
        }
    }


    // Reclamación de un item expirado al cofre
    private void claimSingleExpiredToChest(Player player, AuctionItem auc, Inventory chest) {
        if (auc.isBulk()) {
            for (int i = 0; i < auc.container.length; i++) {
                ItemStack item = auc.container[i];
                if (item == null || item.getType() == Material.AIR) continue;

                // Dar todo al cofre
                givePartialToInventory(chest, item, item.getAmount());

                // Limpiar lo que se entregó
                auc.container[i] = null;
            }

            // Marcar como reclamado si todos los items fueron entregados al cofre
            boolean allTaken = Arrays.stream(auc.container)
                    .allMatch(it -> it == null || it.getType() == Material.AIR);
            if (allTaken) QSMarketPlus.getInstance().getAuctionManager().markAsTaken(auc);

        } else {
            ItemStack item = auc.item;
            if (item == null || item.getType() == Material.AIR) return;

            givePartialToInventory(chest, item, item.getAmount());
            auc.item = null;
            QSMarketPlus.getInstance().getAuctionManager().markAsTaken(auc);
        }
    }



}
