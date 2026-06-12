package mp.quesito.qSMarketPlus.listeners;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.auction.AuctionItem;
import mp.quesito.qSMarketPlus.auction.AuctionManager;
import mp.quesito.qSMarketPlus.auction.holder.*;
import mp.quesito.qSMarketPlus.auction.menu.*;
import mp.quesito.qSMarketPlus.economia.EconomyProvider;
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
            CategoryHolder.class,
            ItemsHolder.class,
            ActionHolder.class,
            AmountHolder.class,
            ConfirmHolder.class,
            ProShopHolder.class,
            AHHolder.class,
            AHConfirmHolder.class,
            AHPreviewHolder.class,
            AHHistoryHolder.class,
            AHActionHolder.class,
            AHExpiredHolder.class
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

        handlers.put(ProShopHolder.class, (e, player) -> {
            ProShopHolder holder = (ProShopHolder) e.getView().getTopInventory().getHolder();
            handleProShopClick(e, player, holder);
        });

    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {

        if (e.getClickedInventory() == null) return;

        Inventory top = e.getView().getTopInventory();
        // ✅ Corregido: verificar que getHolder() no sea null
        if (top.getHolder() == null) return;

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

    private void handleProShopClick(InventoryClickEvent e, Player player, ProShopHolder holder) {

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();

        String categoryId = MetaUtil.getTag(meta, "category");
        String itemId = MetaUtil.getTag(meta, "shop_item");
        String btn = MetaUtil.getTag(meta, "btn");

        int categoryIndex = holder.getCategoryIndex();
        int page = holder.getPage();

        List<ShopCategory> categories = new ArrayList<>(categoryManager.getCategories().values());
        ShopCategory category = categories.get(categoryIndex);

        UniquePurchaseManager upManager = plugin.getUniquePurchaseManager();

        // ================================
        // BOTONES DE NAVEGACIÓN Y CATEGORÍAS
        // ================================
        if (btn != null) {
            switch (btn) {
                case "prev-page" -> ProShopMenu.open(player, categoryManager, itemManager, categoryIndex, page - 1);
                case "next-page" -> ProShopMenu.open(player, categoryManager, itemManager, categoryIndex, page + 1);
                case "prev-category" -> ProShopMenu.open(player, categoryManager, itemManager,
                        Math.max(0, categoryIndex - 1), 0);
                case "next-category" -> ProShopMenu.open(player, categoryManager, itemManager,
                        Math.min(categories.size() - 1, categoryIndex + 1), 0);
                case "back" -> player.closeInventory();
            }
            return;
        }

        // ================================
        // CLICK EN CATEGORÍA
        // ================================
        if (categoryId != null) {
            int newIndex = -1;
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).getId().equals(categoryId)) {
                    newIndex = i;
                    break;
                }
            }
            if (newIndex != -1) {
                ProShopMenu.open(player, categoryManager, itemManager, newIndex, 0);
            }
            return;
        }

        // ================================
        // CLICK EN ITEM
        // ================================
        if (itemId != null) {

            ShopItem item = itemManager.getItem(category.getId(), itemId);
            if (item == null) return;

            ItemStack real = item.getRealItem();

            // BLOQUEO ITEM ÚNICO
            if (item.isOnlyOnce() && upManager.hasPurchased(player, item)) {
                Lang.msg(player, "already_bought");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.closeInventory();
                return;
            }

            // VENTA RÁPIDA (OFFHAND)
            if (e.getClick().name().equalsIgnoreCase("SWAP_OFFHAND")) {
                if (item.getSell() <= 0) {
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

                double money = item.getSell() * total;
                EconomyProvider eco = eco(item);
                eco.deposit(player, money);

                Lang.msg(player, "sell_success",
                        "amount", total,
                        "item", MessageUtil.toLegacy(item.getName()),
                        "price", money,
                        "currency", currency(eco),
                        "symbol", eco.getSymbol()
                );

                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1.3f);
                return;
            }

            // COMPRA RÁPIDA (SHIFT_CLICK)
            if (e.isShiftClick()) {
                int amount = real.getMaxStackSize() == 1 ? 1 : (e.isLeftClick() ? 1 : 64);
                double cost = item.getBuy() * amount;
                EconomyProvider eco = eco(item);
                if (eco.getBalance(player) < cost) {

                    Lang.msg(player, "no_money",
                            "currency", currency(eco));
                    return;

                }

                if (!hasInventorySpace(player, real, amount)) {
                    Lang.msg(player, "not_enough_space");
                    return;
                }

                eco.withdraw(player, cost);

                if (!item.getCommands().isEmpty()) {
                    item.executeBuyCommands(player);
                } else {
                    giveExactItems(player, real, amount);
                }

                if (item.isOnlyOnce()) upManager.markPurchased(player, item);

                Lang.msg(player, "buy_success",
                        "amount", amount,
                        "item", MessageUtil.toLegacy(item.getName()),
                        "price", cost,
                        "currency", currency(eco),
                        "symbol", eco.getSymbol()
                );

                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                return;
            }

            // VERIFICAR ACCESO
            if (!item.canAccess(player)) {
                // Mensaje configurable si el jugador no puede acceder al ítem
                String noAccessMsg = Lang.get("item_unavailable"); // tu key en el YAML
                if (noAccessMsg == null || noAccessMsg.isEmpty()) {
                    noAccessMsg = "<red>⛔ No disponible</red>"; // fallback
                }
                player.sendMessage(MessageUtil.toLegacy(noAccessMsg));

                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // COMANDOS DE COMPRA
            if (!item.getCommands().isEmpty()) {

                double cost = item.getBuy();
                EconomyProvider eco = eco(item);

                if (eco.getBalance(player) < cost) {
                    Lang.msg(player, "no_money",
                            "currency", currency(eco));
                    return;
                }

                eco.withdraw(player, cost);

                item.executeBuyCommands(player);

                if (item.isOnlyOnce()) upManager.markPurchased(player, item);

                player.closeInventory();
                return;
            }

            // ABRIR MENÚ DE ACCIÓN NORMAL
            ActionMenu.open(player, item, category);
        }
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

            boolean soldSomething = false;

            for (ShopCategory cat : categoryManager.getCategories().values()) {
                itemManager.loadCategoryItems(cat);

                for (ShopItem item : itemManager.getItems(cat.getId()).values()) {

                    if (item.getSell() <= 0) continue;

                    ItemStack real = item.getRealItem();
                    int total = countMatchingItems(player, real);
                    if (total <= 0) continue;

                    removeMatchingItems(player, real, total);

                    double money = total * item.getSell();

                    EconomyProvider eco = eco(item);
                    eco.deposit(player, money);

                    soldSomething = true;
                }
            }

            if (soldSomething) {
                Lang.msg(player, "sell_all_success");
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
            Lang.msg(player, "no_permission_categori");
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
            Lang.msg(player, "already_bought");
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

            EconomyProvider eco = eco(clickedItem);

            double money = clickedItem.getSell() * total;
            eco.deposit(player, money);

            Lang.msg(player, "sell_success",
                    "amount", total,
                    "item", MessageUtil.toLegacy(clickedItem.getName()),
                    "price", money,
                    "currency", currency(eco),
                    "symbol", eco.getSymbol()
            );

            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1.3f);
        }

        // Compra rápida SHIFT_CLICK
        if (e.isShiftClick()) {

            int amount = real.getMaxStackSize() == 1 ? 1 : (e.isLeftClick() ? 1 : 64);
            double cost = clickedItem.getBuy() * amount;

            EconomyProvider eco = eco(clickedItem);

            if (eco.getBalance(player) < cost) {
                Lang.msg(player, "no_money",
                        "currency", currency(eco));
                return;
            }

            if (!hasInventorySpace(player, real, amount)) {
                Lang.msg(player, "not_enough_space");
                return;
            }

            eco.withdraw(player, cost);

            if (!clickedItem.getCommands().isEmpty()) {
                clickedItem.executeBuyCommands(player);
            } else {
                giveExactItems(player, real, amount);
            }

            if (clickedItem.isOnlyOnce()) {
                upManager.markPurchased(player, clickedItem);
            }

            Lang.msg(player, "buy_success",
                    "amount", amount,
                    "item", MessageUtil.toLegacy(clickedItem.getName()),
                    "price", cost,
                    "currency", currency(eco),
                    "symbol", eco.getSymbol()
            );

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            return;
        }

        // Verifica permisos de acceso
        if (!clickedItem.canAccess(player)) {

            Lang.msg(player, "no_permission");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Si ejecuta comando de compra, cerrar menú
        if (!clickedItem.getCommands().isEmpty()) {

            double cost = clickedItem.getBuy();
            EconomyProvider eco = eco(clickedItem);

            if (eco.getBalance(player) < cost) {
                Lang.msg(player, "no_money",
                        "currency", currency(eco));
                return;
            }

            eco.withdraw(player, cost);

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

            String menu = QSMarketPlus.getInstance()
                    .getConfig()
                    .getString("menu", "normal");

            if (menu.equalsIgnoreCase("pro")) {

                List<ShopCategory> categories = new ArrayList<>(
                        categoryManager.getCategories().values()
                );

                int index = 0;

                for (int i = 0; i < categories.size(); i++) {
                    if (categories.get(i).getId().equals(category.getId())) {
                        index = i;
                        break;
                    }
                }

                ProShopMenu.open(player, categoryManager, itemManager, index, 0);

            } else {

                ItemsMenu.open(player, category, itemManager, 1);

            }
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
        EconomyProvider eco = eco(item); // economía del item

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
            if ("confirm".equals(btn)) {
                ConfirmMenu.open(player, item, category, 1, buying);
            } else {
                AmountMenu.open(player, holder);
            }
            return;
        }

        if ("amount_mod".equals(btn) && value != null) {
            int mod = Integer.parseInt(value);
            int newAmount = Math.max(1, Math.min(maxStack, amount + mod));

            if (buying && eco.getBalance(player) < newAmount * item.getBuy()) {
                Lang.msg(player, "no_money",
                        "currency", currency(eco));
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

            int maxAmount = buying
                    ? (int) Math.floor(eco.getBalance(player) / item.getBuy())
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

                if (eco.getBalance(player) < price) {

                    Lang.msg(player, "no_money",
                            "currency", currency(eco));

                    return;
                }

                if (!hasInventorySpace(player, item.getRealItem(), amount)) {
                    Lang.msg(player, "not_enough_space");
                    return;
                }

                eco.withdraw(player, price);
                giveExactItems(player, item.getRealItem(), amount);
                Lang.msg(player, "buy_success",
                        "amount", amount,
                        "item", MessageUtil.toLegacy(item.getName()),
                        "price", price,
                        "currency", currency(eco),
                        "symbol", eco.getSymbol());
            } else {

                if (countMatchingItems(player, item.getRealItem()) < amount) {
                    Lang.msg(player, "no_items");
                    return;
                }

                removeMatchingItems(player, item.getRealItem(), amount);
                eco.deposit(player, price);
                String moneyFormatted = eco.getSymbol() + price;
                Lang.msg(player, "sell_success",
                        "amount", amount,
                        "item", MessageUtil.toLegacy(item.getName()),
                        "price", price,
                        "currency", currency(eco),
                        "symbol", eco.getSymbol());
            }

            player.playSound(
                    player.getLocation(),
                    buying ? Sound.ENTITY_PLAYER_LEVELUP : Sound.ENTITY_VILLAGER_YES,
                    1f,
                    1.2f
            );

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

        EconomyProvider eco = eco(item); // ← economía del item
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

            if (eco.getBalance(player) < price) {
                Lang.msg(player, "no_money",
                        "currency", currency(eco));
                return;
            }

            // Verificación item único
            if (item.isOnlyOnce() && upManager.hasPurchased(player, item)) {
                Lang.msg(player, "already_bought");
                player.closeInventory();
                return;
            }

            if (!item.getCommands().isEmpty()) {

                eco.withdraw(player, price);
                item.executeBuyCommands(player);

            } else {

                if (!hasInventorySpace(player, template, amount)) {
                    Lang.msg(player, "not_enough_space");
                    return;
                }

                eco.withdraw(player, price);
                giveExactItems(player, template, amount);
            }

            if (item.isOnlyOnce()) {
                upManager.markPurchased(player, item);
            }

            Lang.msg(player, "buy_success",
                    "amount", amount,
                    "item", MessageUtil.toLegacy(item.getName()),
                    "price", price,
                    "currency", currency(eco),
                    "symbol", eco.getSymbol());

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

            eco.deposit(player, price);

            Lang.msg(player, "sell_success",
                    "amount", amount,
                    "item", MessageUtil.toLegacy(item.getName()),
                    "price", price,
                    "currency", currency(eco),
                    "symbol", eco.getSymbol());

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
        boolean bulk = holder.isBulk();

        List<ItemStack> itemsList = new ArrayList<>();

        if (bulk) {
            for (ItemStack it : player.getInventory().getContents()) {
                if (it != null && it.getType() != Material.AIR) itemsList.add(it.clone());
            }

            if (itemsList.isEmpty()) {
                Lang.msg(player, "no_items");
                return;
            }
        } else {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) {
                Lang.msg(player, "no_items");
                return;
            }
            itemsList.add(hand.clone());
        }

        // El copiado y remoción de ítems ahora se sincroniza de forma segura
        if (bulk) {
            // Pasamos el array clonado; 'createBulkAuction' se encargará de remover exactamente lo necesario del inventario del jugador
            manager.createBulkAuction(player, itemsList.toArray(new ItemStack[0]), price);
            Lang.msg(player, "ah_created_bulk", "count", itemsList.size(), "price", price);
        } else {
            // Subasta de un solo ítem (mano)
            manager.createAuction(player, itemsList.get(0), price);
            player.getInventory().setItemInMainHand(null);

            String itemName = itemsList.get(0).getItemMeta().hasDisplayName()
                    ? itemsList.get(0).getItemMeta().getDisplayName()
                    : itemsList.get(0).getType().name().replace("_", " ").toLowerCase();

            Lang.msg(player, "ah_created", "item", itemName, "price", price);
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

                if (!manager.getAuctions().contains(auc)) {
                    Lang.msg(player, "ah_not_found");
                    AHMenu.open(player, holder.getPage());
                    return;
                }

                manager.cancelAuction(player, auc);
                AHMenu.open(player, holder.getPage());
            }
            case "buy" -> {
                if (!manager.getAuctions().contains(auc)) {
                    Lang.msg(player, "ah_not_found");
                    AHMenu.open(player, holder.getPage());
                    return;
                }

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

                if (!QSMarketPlus.economy.has(player, price)) {
                    Lang.msg(player, "no_money");
                    return;
                }

                boolean useChest = bulk || items.size() > 1;

                // Guardamos el nombre legible del ítem principal para las notificaciones
                String itemName = items.get(0).getItemMeta().hasDisplayName()
                        ? items.get(0).getItemMeta().getDisplayName()
                        : items.get(0).getType().name().replace("_", " ").toLowerCase();

                if (useChest) {
                    if (player.getInventory().firstEmpty() == -1) {
                        Lang.msg(player, "not_enough_space_chest");
                        return;
                    }

                    if (!manager.buyAuction(player, auc)) {
                        Lang.msg(player, "ah_error");
                        return;
                    }

                    Inventory tempChest = Bukkit.createInventory(null, 54, "Items de Subasta");
                    for (ItemStack it : items) givePartialToInventory(tempChest, it, it.getAmount());

                    ItemStack chestItem = new ItemStack(Material.CHEST);
                    BlockStateMeta meta = (BlockStateMeta) chestItem.getItemMeta();
                    Chest chest = (Chest) meta.getBlockState();
                    chest.getInventory().setContents(tempChest.getContents());
                    meta.setBlockState(chest);
                    chestItem.setItemMeta(meta);

                    player.getInventory().addItem(chestItem);
                    QSMarketPlus.economy.withdrawPlayer(player, price);

                    Lang.msg(player, "ah_bought_chest", "money", price, "item_count", items.size());

                } else {
                    ItemStack singleItem = items.get(0);
                    if (!hasInventorySpace(player, singleItem, singleItem.getAmount())) {
                        Lang.msg(player, "not_enough_space");
                        return;
                    }

                    if (!manager.buyAuction(player, auc)) {
                        Lang.msg(player, "ah_error");
                        return;
                    }

                    forceGiveAuctionItem(player, auc);
                    QSMarketPlus.economy.withdrawPlayer(player, price);

                    Lang.msg(player, "ah_bought", "money", price, "item", itemName);
                }

                // Pagar de manera segura al vendedor original de la subasta
                OfflinePlayer seller = Bukkit.getOfflinePlayer(auc.seller);
                QSMarketPlus.economy.depositPlayer(seller, price);

                // 🔥 NOTIFICACIÓN EN TIEMPO REAL PARA EL VENDEDOR
                // Si el dueño de la subasta está conectado, le avisamos de inmediato
                if (seller.isOnline() && seller.getPlayer() != null) {
                    Player onlineSeller = seller.getPlayer();

                    if (bulk) {
                        Lang.msg(onlineSeller, "ah_item_sold_bulk",
                                "buyer", player.getName(),
                                "item_count", items.size(),
                                "money", price);
                    } else {
                        Lang.msg(onlineSeller, "ah_item_sold",
                                "buyer", player.getName(),
                                "item", itemName,
                                "money", price);
                    }

                    // Sonido sutil de campana/monedas para el vendedor indicando que recibió dinero
                    onlineSeller.playSound(onlineSeller.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.8f);
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
            // Reclamación de la subasta expirada (Individual o Bulk)
            Integer index = MetaUtil.getInt(clicked.getItemMeta(), "expired_index");
            if (index != null) {
                AuctionItem auc = holder.getItems().get(index);
                if (!auc.seller.equals(player.getUniqueId())) {
                    player.sendMessage("§cEste item no te pertenece.");
                    return;
                }

                int itemsClaimed = 0;

                // CASO 1: Es una subasta en lote (Bulk / Paquete de ítems)
                if (auc.isBulk() && auc.container != null) {
                    for (ItemStack item : auc.container) {
                        if (item == null || item.getType().isAir()) continue;

                        ItemStack toGive = item.clone();
                        // Entrega al inventario del jugador
                        java.util.HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(toGive);

                        // Si el inventario se llena, lo tira al suelo de forma natural
                        for (ItemStack drop : leftOver.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        }
                        itemsClaimed += toGive.getAmount(); // Contamos la cantidad total de ítems devueltos
                    }
                }
                // CASO 2: Es una subasta normal (Un solo tipo de ítem)
                else if (auc.item != null && !auc.item.getType().isAir()) {
                    ItemStack toGive = auc.item.clone();
                    java.util.HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(toGive);

                    for (ItemStack drop : leftOver.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                    itemsClaimed += toGive.getAmount();
                }

                // Validación de seguridad por si acaso la subasta estaba corrupta o vacía
                if (itemsClaimed == 0) {
                    player.sendMessage("§cError: No se encontraron ítems válidos para reclamar en esta subasta.");
                    return;
                }

                // Envia el mensaje usando tu sistema de placeholders (pasa el total de ítems reclamados)
                Lang.msg(player, "ah_claimed_chest", "count", itemsClaimed);

                // Guardar el cambio de estado en SQL llamando a tu Manager
                QSMarketPlus.getInstance().getAuctionManager().markAsTaken(auc);

                // Remover de la vista actual del holder de la GUI y actualizar el menú
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


    public EconomyProvider eco(ShopItem item) {
        return plugin.getEconomyManager().get(item.getEconomy());
    }

    private String currency(EconomyProvider eco) {

        String id = eco.getName();

        return plugin.getConfig().getString(
                "economies." + id + ".display-name",
                id
        );
    }
}
