package mp.quesito.qSMarketPlus.shop;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.economia.EconomyProvider;
import mp.quesito.qSMarketPlus.holder.AmountHolder;
import mp.quesito.qSMarketPlus.utils.MenuItems;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import mp.quesito.qSMarketPlus.utils.MetaUtil;
import mp.quesito.qSMarketPlus.utils.NexoGui;
import mp.quesito.qSMarketPlus.utils.ShopInventory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu de cantidad. Las primeras 4 filas son una copia del inventario del jugador
 * (la fila de abajo es la barra rapida) y muestran donde entra lo que compra o
 * de donde sale lo que vende. El tope se calcula solo: en compra, lo que cabe y
 * lo que puede pagar; en venta, lo que tiene.
 */
public final class AmountMenu {

    private static final int SIZE = 54;
    private static final int PREVIEW_SLOTS = 36;

    private AmountMenu() {
    }

    // ===================================================
    //                     LIMITES
    // ===================================================
    public static int affordable(Player player, ShopItem item) {
        if (item.getBuy() <= 0) return Integer.MAX_VALUE;

        EconomyProvider eco = economy(item);
        if (eco == null) return 0;

        return (int) Math.min(Integer.MAX_VALUE, Math.floor(eco.getBalance(player) / item.getBuy()));
    }

    public static int maxAmount(Player player, AmountHolder holder) {
        ItemStack real = holder.getItem().getRealItem();

        if (!holder.isBuying()) {
            return ShopInventory.countForSale(player, real);
        }
        return Math.min(ShopInventory.spaceFor(player, real), affordable(player, holder.getItem()));
    }

    public static int clamp(int amount, int max) {
        return Math.max(1, Math.min(Math.max(1, max), amount));
    }

    // ===================================================
    //                     ABRIR
    // ===================================================

    /** Si el menu ya esta abierto lo actualiza en el sitio, sin parpadeo ni mover el raton. */
    public static void open(Player player, AmountHolder holder) {

        FileConfiguration cfg = QSMarketPlus.getInstance().amountMenuConfig.getConfig();

        Inventory top = player.getOpenInventory().getTopInventory();
        boolean alreadyOpen = top.getHolder() == holder;

        Inventory inv = alreadyOpen ? top : Bukkit.createInventory(holder, SIZE, MessageUtil.toLegacy(NexoGui.title(
                holder.isBuying()
                        ? cfg.getString("menu.title-buy", "<green>Selecciona cantidad")
                        : cfg.getString("menu.title-sell", "<red>Selecciona cantidad"))));

        int max = maxAmount(player, holder);
        holder.setAmount(clamp(holder.getAmount(), max));

        inv.clear();

        // Las esquinas quedan vacias: los controles forman un bloque centrado
        List<Integer> empty = cfg.getIntegerList("layout.empty-slots");
        ItemStack filler = pane(cfg, "preview.filler", Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = PREVIEW_SLOTS; i < SIZE; i++) {
            if (filler != null && !empty.contains(i)) inv.setItem(i, filler);
        }

        if (holder.isBuying()) {
            renderBuyPreview(inv, cfg, player, holder, max);
        } else {
            renderSellPreview(inv, cfg, player, holder);
        }

        ConfigurationSection buttons = cfg.getConfigurationSection("buttons");
        if (buttons != null) {
            for (String key : buttons.getKeys(false)) {
                loadConfiguredButton(cfg, inv, "buttons." + key);
            }
        }

        if (holder.isBuying()) {
            inv.setItem(cfg.getInt("buttons.coupon.slot", 50), couponButton(cfg, player, holder));
        } else {
            int couponSlot = cfg.getInt("buttons.coupon.slot", -1);
            if (couponSlot >= 0) inv.setItem(couponSlot, null);
        }

        inv.setItem(cfg.getInt("buttons.info-slot", 40), infoItem(cfg, player, holder, max));

        renderConfirm(inv, cfg, player, holder, max);
        addLore(inv, cfg.getInt("buttons.max.slot", 53), cfg, "panel.max-lore", holder, max);

        if (!alreadyOpen) {
            player.openInventory(inv);
        }
    }

    // ===================================================
    //                 VISTA PREVIA
    // ===================================================

    private static void renderBuyPreview(Inventory inv, FileConfiguration cfg, Player player, AmountHolder holder, int max) {

        ItemStack real = holder.getItem().getRealItem();
        int maxStack = real.getMaxStackSize();
        ItemStack[] storage = player.getInventory().getStorageContents();


        // Mismo orden que addItem: primero completa pilas iguales, luego slots vacios
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < storage.length; i++) {
            if (storage[i] != null && storage[i].isSimilar(real) && storage[i].getAmount() < maxStack) order.add(i);
        }
        for (int i = 0; i < storage.length; i++) {
            if (storage[i] == null || storage[i].getType().isAir()) order.add(i);
        }

        int remaining = holder.getAmount();
        int reachable = 0;
        boolean anything = max > 0;
        int next = 0; // se rellena en orden, sin huecos: no importa donde este cada cosa en el inventario

        for (int slot : order) {
            int current = storage[slot] == null ? 0 : storage[slot].getAmount();
            int room = maxStack - current;
            int take = anything ? Math.min(remaining, room) : 0;
            int before = reachable;
            reachable += room;
            remaining -= take;

            ItemStack shown;
            if (take > 0) {
                shown = real.clone();
                shown.setAmount(take);
                ItemMeta meta = shown.getItemMeta();
                meta.setLore(lines(cfg, "preview.filling", "<green>+<take> aquí",
                        "<take>", take, "<total>", current + take));
                shown.setItemMeta(meta);
            } else if (before < max) {
                shown = pane(cfg, "preview.free", Material.LIME_STAINED_GLASS_PANE, "<green>Libre",
                        lines(cfg, "preview.free-lore", "<gray>Caben <white><room></white> aquí", "<room>", room));
            } else {
                shown = pane(cfg, "preview.no-money", Material.RED_STAINED_GLASS_PANE, "<red>Sin dinero para más", null);
            }

            if (before < max) {
                tagFill(shown, Math.min(reachable, max));
            }

            inv.setItem(next++, shown);
        }
    }

    private static void renderSellPreview(Inventory inv, FileConfiguration cfg, Player player, AmountHolder holder) {

        ItemStack real = holder.getItem().getRealItem();
        ItemStack[] storage = player.getInventory().getStorageContents();

        int remaining = holder.getAmount();
        int cumulative = 0;
        int next = 0;

        for (int i = 0; i < PREVIEW_SLOTS; i++) {

            if (!ShopInventory.matchesForSale(real, storage[i])) {
                continue;
            }

            int have = storage[i].getAmount();
            int take = Math.min(remaining, have);
            remaining -= take;
            cumulative += have;

            ItemStack shown;
            if (take > 0) {
                shown = real.clone();
                shown.setAmount(take);
                ItemMeta meta = shown.getItemMeta();
                meta.setLore(lines(cfg, "preview.selling", "<red>Vendes <take> de <have>",
                        "<take>", take, "<have>", have));
                shown.setItemMeta(meta);
            } else {
                shown = pane(cfg, "preview.keeping", Material.CYAN_STAINED_GLASS_PANE, "<aqua>Se queda contigo",
                        lines(cfg, "preview.keeping-lore", "<gray>Tienes <white><have></white> aquí", "<have>", have));
                shown.setAmount(have);
            }

            tagFill(shown, cumulative);
            inv.setItem(next++, shown);
        }
    }

    private static void tagFill(ItemStack stack, int amount) {
        ItemMeta meta = stack.getItemMeta();
        MetaUtil.setTag(meta, "btn", "fill");
        MetaUtil.setTag(meta, "value", String.valueOf(amount));
        stack.setItemMeta(meta);
    }

    // ===================================================
    //                 INFO Y CONFIRMAR
    // ===================================================
    private static ItemStack infoItem(FileConfiguration cfg, Player player, AmountHolder holder, int max) {

        ShopItem item = holder.getItem();

        ItemStack stack = item.getRealItem().clone();
        stack.setAmount(Math.max(1, Math.min(holder.getAmount(), stack.getMaxStackSize())));

        ItemMeta meta = stack.getItemMeta();
        if (!meta.hasDisplayName()) {
            meta.setDisplayName(MessageUtil.toLegacy("<aqua>" + item.getName()));
        }
        meta.setLore(panelLines(cfg, holder.isBuying() ? "panel.lore-buy" : "panel.lore-sell", holder, max, player));
        stack.setItemMeta(meta);
        return stack;
    }

    private static void renderConfirm(Inventory inv, FileConfiguration cfg, Player player, AmountHolder holder, int max) {

        int slot = cfg.getInt("buttons.confirm.slot", 49);

        if (max > 0) {
            ItemStack confirm = MenuItems.amountConfirmItem(holder.getItem(), holder.getAmount(), holder.isBuying());
            ItemMeta meta = confirm.getItemMeta();
            meta.setLore(panelLines(cfg, "panel.confirm-lore", holder, max, player));
            confirm.setItemMeta(meta);
            inv.setItem(slot, confirm);
            return;
        }

        String reason;
        if (!holder.isBuying()) {
            reason = cfg.getString("panel.blocked-no-items", "<red>No tienes este objeto");
        } else if (ShopInventory.spaceFor(player, holder.getItem().getRealItem()) == 0) {
            reason = cfg.getString("panel.blocked-no-space", "<red>Inventario lleno");
        } else {
            reason = cfg.getString("panel.blocked-no-money", "<red>No te alcanza el dinero");
        }

        ItemStack blocked = NexoGui.item(cfg.getString("panel.blocked-nexo", null));
        if (blocked == null) blocked = new ItemStack(Material.BARRIER);
        ItemMeta meta = blocked.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy(reason));
        MetaUtil.setTag(meta, "btn", "blocked");
        blocked.setItemMeta(meta);
        inv.setItem(slot, blocked);
    }

    private static void addLore(Inventory inv, int slot, FileConfiguration cfg, String path, AmountHolder holder, int max) {
        ItemStack stack = inv.getItem(slot);
        if (stack == null || !stack.hasItemMeta()) return;

        ItemMeta meta = stack.getItemMeta();
        meta.setLore(panelLines(cfg, path, holder, max));
        stack.setItemMeta(meta);
    }

    private static List<String> panelLines(FileConfiguration cfg, String path, AmountHolder holder, int max) {
        return panelLines(cfg, path, holder, max, null);
    }

    private static List<String> panelLines(FileConfiguration cfg, String path, AmountHolder holder, int max, Player player) {

        ShopItem item = holder.getItem();
        int maxStack = item.getMaterial().getMaxStackSize();
        double unit = holder.isBuying() ? item.getBuy() : item.getSell();
        EconomyProvider eco = economy(item);

        // Con cupon activo, el total ya sale con el descuento
        double total = ShopInventory.money(unit * holder.getAmount());
        mp.quesito.qSMarketPlus.coupons.CouponManager.Quote quote = player != null && holder.isBuying()
                ? QSMarketPlus.getInstance().getCouponManager().quote(player, holder.getCategory(), total)
                : null;
        double shownTotal = quote != null ? quote.finalPrice() : total;

        List<String> out = new ArrayList<>();
        for (String line : cfg.getStringList(path)) {
            out.add(MessageUtil.toLegacy(line
                    .replace("<amount>", String.valueOf(holder.getAmount()))
                    .replace("<stacks>", MenuItems.stacksText(holder.getAmount(), maxStack))
                    .replace("<price>", MenuItems.formatPrice(eco, shownTotal))
                    .replace("<unit>", MenuItems.formatPrice(eco, unit))
                    .replace("<max>", String.valueOf(Math.max(0, max)))
                    .replace("<max_stacks>", max > 0 ? MenuItems.stacksText(max, maxStack) : "0")
                    .replace("<item>", item.getName())));
        }

        if (quote != null) {
            for (String line : cfg.getStringList("panel.coupon-lore")) {
                out.add(MessageUtil.toLegacy(line
                        .replace("<coupon>", quote.template().name())
                        .replace("<percent>", mp.quesito.qSMarketPlus.coupons.CouponManager.money(quote.template().percent()))
                        .replace("<saved>", mp.quesito.qSMarketPlus.coupons.CouponManager.money(quote.saved()))
                        .replace("<original>", MenuItems.formatPrice(eco, total))));
            }
        }
        return out;
    }

    private static ItemStack couponButton(FileConfiguration cfg, Player player, AmountHolder holder) {

        mp.quesito.qSMarketPlus.coupons.CouponManager coupons = QSMarketPlus.getInstance().getCouponManager();
        mp.quesito.qSMarketPlus.coupons.CouponManager.Coupon active = coupons.selected(player);
        boolean applies = active != null && coupons.template(active.template()).appliesTo(holder.getCategory());

        ConfigurationSection sec = cfg.getConfigurationSection("buttons.coupon");
        ItemStack button = sec != null ? MenuItems.buttonFromConfig(sec) : new ItemStack(Material.PAPER);

        ItemMeta meta = button.getItemMeta();
        MetaUtil.setTag(meta, "btn", "coupon");

        String path = applies ? "panel.coupon-button-active" : "panel.coupon-button";
        List<String> fallback = applies
                ? List.of("<green>Activo: <white><coupon>", "<gray>Clic: cambiar · Clic derecho: mis cupones")
                : List.of("<gray>Tienes <white><count></white> cupón(es) para esta categoría",
                "<gray>Clic: usar uno · Clic derecho: mis cupones");
        List<String> raw = cfg.isList(path) ? cfg.getStringList(path) : fallback;

        String count = String.valueOf(coupons.usableCount(player, holder.getCategory()));
        String name = applies ? coupons.template(active.template()).name() : "";
        List<String> lore = new ArrayList<>();
        for (String line : raw) {
            lore.add(MessageUtil.toLegacy(line.replace("<count>", count).replace("<coupon>", name)));
        }
        meta.setLore(lore);

        if (applies) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }
        button.setItemMeta(meta);
        return button;
    }

    // ===================================================
    //                   UTILIDADES
    // ===================================================
    private static EconomyProvider economy(ShopItem item) {
        return QSMarketPlus.getInstance().getEconomyManager().get(item.getEconomy());
    }

    private static List<String> lines(FileConfiguration cfg, String path, String fallback, Object... placeholders) {

        List<String> raw = cfg.getStringList(path);
        if (raw.isEmpty()) raw = List.of(fallback);

        List<String> out = new ArrayList<>();
        for (String line : raw) {
            for (int i = 0; i + 1 < placeholders.length; i += 2) {
                line = line.replace(String.valueOf(placeholders[i]), String.valueOf(placeholders[i + 1]));
            }
            out.add(MessageUtil.toLegacy(line));
        }
        return out;
    }

    private static ItemStack pane(FileConfiguration cfg, String path, Material fallback, String fallbackName, List<String> lore) {

        // AIR = sin relleno, para que se vea el fondo de Nexo
        Material material = Material.matchMaterial(cfg.getString(path + "-material", fallback.name()));
        if (material == Material.AIR) return null;

        ItemStack pane = NexoGui.item(cfg.getString(path + "-nexo", null));
        if (pane == null) pane = new ItemStack(material == null ? fallback : material);

        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy(cfg.getString(path + "-name", fallbackName)));
        if (lore != null) meta.setLore(lore);
        pane.setItemMeta(meta);
        return pane;
    }

    // ====================================================================
    //       CARGA UN BOTON DESDE CONFIG Y AÑADE SUS TAGS PERSONALIZADOS
    // ====================================================================
    private static void loadConfiguredButton(FileConfiguration cfg, Inventory inv, String path) {

        if (!cfg.isConfigurationSection(path)) return;
        ConfigurationSection sec = cfg.getConfigurationSection(path);

        if (!sec.getBoolean("enabled", true)) return;

        ItemStack button = MenuItems.buttonFromConfig(sec);
        if (button == null) return;

        ItemMeta meta = button.getItemMeta();

        if (sec.contains("tag")) {
            MetaUtil.setTag(meta, "btn", sec.getString("tag"));
        }

        if (sec.contains("value")) {
            String v = sec.getString("value");
            MetaUtil.setTag(meta, "value", v);

            if ("MIN".equalsIgnoreCase(v) || "MAX".equalsIgnoreCase(v)) {
                MetaUtil.setTag(meta, "special", v.toUpperCase());
            }
        }

        button.setItemMeta(meta);

        if (sec.contains("amount") && !sec.contains("nexo")) {
            button.setAmount(Math.max(1, Math.min(64, sec.getInt("amount", 1))));
        }

        int slot = sec.getInt("slot", -1);
        if (slot >= 0 && slot < inv.getSize()) inv.setItem(slot, button);
    }
}
