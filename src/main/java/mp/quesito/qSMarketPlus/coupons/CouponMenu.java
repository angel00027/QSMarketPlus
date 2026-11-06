package mp.quesito.qSMarketPlus.coupons;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.coupons.CouponManager.Coupon;
import mp.quesito.qSMarketPlus.coupons.CouponManager.Template;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import mp.quesito.qSMarketPlus.utils.MetaUtil;
import mp.quesito.qSMarketPlus.utils.NexoGui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** "Mis cupones": activar o desactivar cupones y canjear codigos. */
public final class CouponMenu {

    private static final int[] GRID = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public static class Holder implements InventoryHolder {
        private final int page;
        private Inventory inventory;

        public Holder(int page) {
            this.page = page;
        }

        public int getPage() { return page; }

        @Override
        public Inventory getInventory() { return inventory; }
    }

    private CouponMenu() {
    }

    public static void open(Player player, int page) {

        CouponManager manager = QSMarketPlus.getInstance().getCouponManager();
        YamlConfiguration cfg = manager.config();

        List<Coupon> wallet = manager.wallet(player.getUniqueId());
        Coupon active = manager.selected(player);

        int maxPage = Math.max(1, (int) Math.ceil(wallet.size() / (double) GRID.length));
        page = Math.max(1, Math.min(page, maxPage));

        Holder holder = new Holder(page);
        String title = cfg.getString("menu.title", "<dark_green>Mis cupones <gray>{page}/{max}")
                .replace("{page}", String.valueOf(page)).replace("{max}", String.valueOf(maxPage));
        Inventory inv = Bukkit.createInventory(holder, 54, MessageUtil.toLegacy(NexoGui.title(title)));
        holder.inventory = inv;

        int start = (page - 1) * GRID.length;
        for (int i = 0; i < GRID.length && start + i < wallet.size(); i++) {
            Coupon coupon = wallet.get(start + i);
            boolean isActive = active != null && active.code().equals(coupon.code());
            inv.setItem(GRID[i], couponIcon(manager, coupon, isActive));
        }

        if (wallet.isEmpty()) {
            inv.setItem(22, button(cfg, "empty", Material.PAPER, "<gray>Aún no tienes cupones", null,
                    List.of("<gray>Canjea un código con el botón de abajo", "<gray>o con <white>/cupon canjear <código>")));
        }

        if (page > 1) inv.setItem(45, button(cfg, "prev", Material.ARROW, "<yellow>Anterior", "prev", null));
        inv.setItem(47, button(cfg, "redeem", Material.NAME_TAG, "<green>Canjear código", "redeem",
                List.of("<gray>Se cierra el menú y escribes", "<gray>el código en el chat")));
        inv.setItem(49, button(cfg, "close", Material.BARRIER, "<red>Cerrar", "close", null));
        if (active != null) {
            inv.setItem(51, button(cfg, "deactivate", Material.GRAY_DYE, "<yellow>Quitar cupón activo", "deactivate", null));
        }
        if (page < maxPage) inv.setItem(53, button(cfg, "next", Material.ARROW, "<yellow>Siguiente", "next", null));

        Material filler = Material.matchMaterial(cfg.getString("menu.filler", "GRAY_STAINED_GLASS_PANE"));
        if (filler != null && !filler.isAir()) {
            ItemStack pane = new ItemStack(filler);
            ItemMeta meta = pane.getItemMeta();
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
            for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, pane);
        }

        player.openInventory(inv);
    }

    private static ItemStack couponIcon(CouponManager manager, Coupon coupon, boolean active) {

        Template t = manager.template(coupon.template());
        YamlConfiguration cfg = manager.config();

        ItemStack icon = NexoGui.item(active ? cfg.getString("menu.active-nexo", t.nexo()) : t.nexo());
        if (icon == null) {
            Material m = Material.matchMaterial(t.material());
            icon = new ItemStack(m != null ? m : Material.PAPER);
        }

        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy(t.name()));

        List<String> raw = cfg.isList("menu.coupon-lore") ? cfg.getStringList("menu.coupon-lore") : List.of(
                "<gray>Descuento: <green><percent>%",
                "<gray>Categorías: <white><categories>",
                "<gray>Usos: <white><uses>",
                "<gray>Caduca en: <white><time>",
                "<gray>Mínimo de compra: <white>$<minimum>",
                "<gray>Código: <dark_gray><code>");

        List<String> lore = new ArrayList<>();
        for (String line : raw) {
            if (line.contains("<minimum>") && t.minimum() <= 0) continue;
            if (line.contains("<max>") && t.maxDiscount() <= 0) continue;
            lore.add(MessageUtil.toLegacy(line
                    .replace("<percent>", CouponManager.money(t.percent()))
                    .replace("<categories>", manager.categoryNames(t))
                    .replace("<uses>", CouponManager.usesText(coupon.usesLeft()))
                    .replace("<time>", CouponManager.timeLeft(coupon.expires()))
                    .replace("<minimum>", CouponManager.money(t.minimum()))
                    .replace("<max>", CouponManager.money(t.maxDiscount()))
                    .replace("<code>", coupon.code())));
        }
        lore.add("");
        lore.add(MessageUtil.toLegacy(active
                ? cfg.getString("menu.active-line", "<green>✔ Activo: se aplica al comprar")
                : cfg.getString("menu.inactive-line", "<yellow>Clic para activarlo")));
        meta.setLore(lore);

        if (active) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        MetaUtil.setTag(meta, "coupon", coupon.code());
        icon.setItemMeta(meta);
        return icon;
    }

    private static ItemStack button(YamlConfiguration cfg, String key, Material material, String name, String btn, List<String> lore) {
        ConfigurationSection sec = cfg.getConfigurationSection("menu.buttons." + key);

        ItemStack item = sec != null ? NexoGui.item(sec.getString("nexo", null)) : null;
        if (item == null && sec != null && sec.contains("material")) {
            Material m = Material.matchMaterial(sec.getString("material"));
            if (m != null) item = new ItemStack(m);
        }
        if (item == null) item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy(sec != null ? sec.getString("name", name) : name));
        List<String> lines = sec != null && sec.isList("lore") ? sec.getStringList("lore") : lore;
        if (lines != null) meta.setLore(MessageUtil.loreToLegacy(lines));
        if (btn != null) MetaUtil.setTag(meta, "btn", btn);
        item.setItemMeta(meta);
        return item;
    }
}
