package mp.quesito.qSMarketPlus.utils;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.economia.EconomyProvider;
import mp.quesito.qSMarketPlus.shop.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import java.net.URL;
import java.util.*;

public class MenuItems {

    public static final NamespacedKey KEY_AMOUNT_MOD =
            new NamespacedKey(QSMarketPlus.getInstance(), "amount_mod");

    public static final NamespacedKey KEY_SPECIAL =
            new NamespacedKey(QSMarketPlus.getInstance(), "special_btn");


    public static String formatPrice(EconomyProvider eco, double price) {

        String symbol = eco != null ? eco.getSymbol() : "$";
        String currency = eco != null ? eco.getName() : "Money";

        String format = QSMarketPlus.getInstance()
                .getConfig()
                .getString("economies.price-format", "<symbol><price> <currency>");

        return format
                .replace("<price>", String.valueOf(ShopInventory.money(price)))
                .replace("<symbol>", symbol)
                .replace("<currency>", currency);
    }


    public static ItemStack customHead(String displayName, String textureInput) {

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        meta.setDisplayName(MessageUtil.toLegacy(displayName));

        if (textureInput != null && !textureInput.isEmpty()) {
            try {

                String textureUrl = extractTextureUrl(textureInput);

                PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
                profile.getTextures().setSkin(new URL(textureUrl));
                meta.setOwnerProfile(profile);

            } catch (Exception ex) {
                Bukkit.getLogger().warning("[QSMarketPlus] Textura inválida: " + textureInput);
            }
        }

        head.setItemMeta(meta);
        return head;
    }


    private static String extractTextureUrl(String input) {

        if (input.startsWith("http://") || input.startsWith("https://")) {
            return input;
        }

        try {

            String decoded = new String(Base64.getDecoder().decode(input));

            int start = decoded.indexOf("http");
            int end = decoded.indexOf("\"", start);

            if (start == -1 || end == -1) {
                throw new IllegalArgumentException("Base64 inválido");
            }

            return decoded.substring(start, end);

        } catch (Exception e) {
            throw new IllegalArgumentException("No es URL ni Base64 válida.");
        }
    }


    public static ItemStack confirmInfoItem(ShopItem item, int amount, boolean buying) {

        ItemStack stack = item.getRealItem().clone();
        stack.setAmount(Math.min(amount, stack.getMaxStackSize()));

        ItemMeta meta = stack.getItemMeta();

        double price = buying ? item.getBuy() * amount : item.getSell() * amount;

        EconomyProvider eco = QSMarketPlus.getInstance()
                .getEconomyManager()
                .get(item.getEconomy());

        String priceFormatted = formatPrice(eco, price);

        String stacksText = stacksText(amount, stack.getMaxStackSize());

        List<String> rawLore = QSMarketPlus.getInstance()
                .getAmountMenuConfig()
                .getConfig()
                .getStringList(buying ? "info-slot.lore-buy" : "info-slot.lore-sell");

        List<String> lore = new ArrayList<>();

        for (String line : rawLore) {

            line = line
                    .replace("<amount>", String.valueOf(amount))
                    .replace("<stacks>", stacksText)
                    .replace("<price>", priceFormatted)
                    .replace("<item>", item.getName());

            lore.add(MessageUtil.toLegacy(line));
        }

        meta.setLore(lore);
        stack.setItemMeta(meta);

        return stack;
    }

    public static String miniToLegacy(String text) {
        if (text == null) return "";
        return MessageUtil.toLegacy(text);
    }

    // Botón Confirmar del menú de cantidad: muestra cantidad, pilas y total en su lore
    public static ItemStack amountConfirmItem(ShopItem item, int amount, boolean buying) {

        FileConfiguration cfg = QSMarketPlus.getInstance()
                .getAmountMenuConfig()
                .getConfig();

        ConfigurationSection sec = cfg.getConfigurationSection("buttons.confirm");
        ItemStack btn = sec != null ? buttonFromConfig(sec) : null;
        if (btn == null) {
            btn = new ItemStack(Material.LIME_DYE);
        }

        ItemMeta meta = btn.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(btn.getType());
        }

        MetaUtil.setTag(meta, "btn", "confirm");

        double price = buying ? item.getBuy() * amount : item.getSell() * amount;

        EconomyProvider eco = QSMarketPlus.getInstance()
                .getEconomyManager()
                .get(item.getEconomy());

        String priceFormatted = formatPrice(eco, price);
        String stacksText = stacksText(amount, item.getMaterial().getMaxStackSize());

        List<String> lore = new ArrayList<>();
        for (String line : cfg.getStringList(buying ? "info-slot.lore-buy" : "info-slot.lore-sell")) {
            line = line
                    .replace("<amount>", String.valueOf(amount))
                    .replace("<stacks>", stacksText)
                    .replace("<price>", priceFormatted)
                    .replace("<item>", item.getName());
            lore.add(MessageUtil.toLegacy(line));
        }
        meta.setLore(lore);

        btn.setItemMeta(meta);
        return btn;
    }

    public static String stacksText(int amount, int maxStack) {
        if (maxStack <= 1) return String.valueOf(amount);
        int stacks = amount / maxStack;
        int rest = amount % maxStack;
        String stacksPart = stacks > 0
                ? stacks + (stacks == 1 ? " Pila" : " Pilas")
                : "";
        String restPart = rest > 0 ? String.valueOf(rest) : "";
        if (stacks > 0 && rest > 0) return stacksPart + " + " + rest;
        return stacks > 0 ? stacksPart : restPart;
    }


    public static ItemStack next(String action) {

        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        if (action == null) {

            meta.setDisplayName(MessageUtil.toLegacy("<gray>Sin más páginas</gray>"));
            item.setItemMeta(meta);
            return item;

        }

        String name = action.equals("next")
                ? "<green>Siguiente →</green>"
                : "<green>← Anterior</green>";

        meta.setDisplayName(MessageUtil.toLegacy(name));
        MetaUtil.setTag(meta, "btn", action);

        item.setItemMeta(meta);
        return item;
    }


    public static ItemStack green(String action, String name) {

        ItemStack item = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(MessageUtil.toLegacy(name));
        MetaUtil.setTag(meta, "btn", action);

        item.setItemMeta(meta);
        return item;
    }


    public static ItemStack red(String action, String name) {

        ItemStack item = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(MessageUtil.toLegacy(name));
        MetaUtil.setTag(meta, "btn", action);

        item.setItemMeta(meta);
        return item;
    }


    public static ItemStack buttonFromConfig(ConfigurationSection sec) {

        String name = sec.getString("name", "<white>Button</white>");
        String head = sec.getString("head", null);
        String materialName = sec.getString("material", "STONE");

        ItemStack item = NexoGui.item(sec.getString("nexo", null));

        if (item != null) {
            // icono de Nexo
        } else if (head != null && !head.isEmpty()) {
            item = customHead(name, head);
        } else {
            Material material = Material.matchMaterial(materialName);
            item = new ItemStack(material != null ? material : Material.STONE);
        }

        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(MessageUtil.toLegacy(name));
        meta.setLore(MessageUtil.loreToLegacy(sec.getStringList("lore")));

        if (sec.contains("custom-model-data")) {
            meta.setCustomModelData(sec.getInt("custom-model-data"));
        }

        item.setItemMeta(meta);

        return item;
    }


    public static ItemStack glass() {

        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(" ");

        item.setItemMeta(meta);
        return item;
    }


    public static ItemStack backButton() {

        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(MessageUtil.toLegacy("<yellow>Volver"));
        MetaUtil.setTag(meta, "btn", "back");

        item.setItemMeta(meta);

        return item;
    }


    public static ItemStack cancelButton() {

        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(MessageUtil.toLegacy("<red>Cancelar"));
        MetaUtil.setTag(meta, "btn", "cancel");

        item.setItemMeta(meta);
        return item;
    }


    public static ItemStack confirmButton() {

        ItemStack item = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(MessageUtil.toLegacy("<green>Confirmar"));
        MetaUtil.setTag(meta, "btn", "confirm");

        item.setItemMeta(meta);

        return item;
    }


    public static ItemStack infoAmountItem(ShopItem item, int amount, boolean buying) {

        ItemStack stack = item.getRealItem().clone();
        stack.setAmount(Math.min(amount, stack.getMaxStackSize()));

        ItemMeta meta = stack.getItemMeta();

        if (!meta.hasDisplayName()) {
            meta.setDisplayName(MessageUtil.toLegacy("<aqua>" + item.getName()));
        }

        double price = buying ? item.getBuy() * amount : item.getSell() * amount;

        EconomyProvider eco = QSMarketPlus.getInstance()
                .getEconomyManager()
                .get(item.getEconomy());

        String priceFormatted = formatPrice(eco, price);

        // Leer lore desde YAML
        List<String> rawLore = QSMarketPlus.getInstance()
                .getAmountMenuConfig()
                .getConfig()
                .getStringList(buying ? "info-slot.lore-buy" : "info-slot.lore-sell");

        List<String> lore = new ArrayList<>();

        for (String line : rawLore) {

            line = line
                    .replace("<amount>", String.valueOf(amount))
                    .replace("<stacks>", stacksText(amount, stack.getMaxStackSize()))
                    .replace("<price>", priceFormatted)
                    .replace("<item>", item.getName());

            lore.add(MessageUtil.toLegacy(line));
        }

        meta.setLore(lore);
        stack.setItemMeta(meta);

        return stack;
    }

    public static boolean alreadyOwned(Player player, ShopItem item) {

        if (!item.isOnlyOnce()) return false;

        String perm = item.getPermission();

        if (perm != null && !perm.isEmpty()) {

            if (player.hasPermission(perm)) {
                return true;
            }

        }

        return false;
    }


    public static ItemStack buyButton(Player player, ShopItem item) {

        ItemStack stack = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = stack.getItemMeta();

        if (item.isOnlyOnce() && alreadyOwned(player, item)) {

            meta.setDisplayName(MessageUtil.toLegacy("<red>Ya adquirido"));

            List<String> lore = new ArrayList<>();

            lore.add(MessageUtil.toLegacy("<gray>Este artículo solo puede"));
            lore.add(MessageUtil.toLegacy("<gray>comprarse una vez."));
            lore.add("");

            if (item.getPermission() != null) {
                lore.add(MessageUtil.toLegacy("<gray>Permiso: <yellow>" + item.getPermission()));
            }

            meta.setLore(lore);

            MetaUtil.setTag(meta, "btn", "disabled");

            stack.setType(Material.BARRIER);
            stack.setItemMeta(meta);

            return stack;
        }

        meta.setDisplayName(MessageUtil.toLegacy("<green>Comprar"));

        List<String> lore = new ArrayList<>();

        lore.add(MessageUtil.toLegacy("<gray>Comprar: <white>" + item.getName()));

        EconomyProvider eco = QSMarketPlus.getInstance()
                .getEconomyManager()
                .get(item.getEconomy());

        lore.add(MessageUtil.toLegacy("<gray>Precio: <white>" + formatPrice(eco, item.getBuy())));

        meta.setLore(lore);

        MetaUtil.setTag(meta, "btn", "buy");

        stack.setItemMeta(meta);

        return stack;
    }


    public static ItemStack sellButton(ShopItem item) {

        ItemStack stack = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = stack.getItemMeta();

        meta.setDisplayName(MessageUtil.toLegacy("<red>Vender"));

        List<String> lore = new ArrayList<>();

        lore.add(MessageUtil.toLegacy("<gray>Vender: <white>" + item.getName()));

        EconomyProvider eco = QSMarketPlus.getInstance()
                .getEconomyManager()
                .get(item.getEconomy());

        lore.add(MessageUtil.toLegacy("<gray>Precio: <white>" + formatPrice(eco, item.getSell())));

        meta.setLore(lore);

        MetaUtil.setTag(meta, "btn", "sell");

        stack.setItemMeta(meta);

        return stack;
    }


    public static ItemStack getButton(ConfigurationSection root, String id) {

        ConfigurationSection sec = root.getConfigurationSection("buttons." + id);

        if (sec == null) return null;

        ItemStack item = buttonFromConfig(sec);

        ItemMeta meta = item.getItemMeta();

        MetaUtil.setTag(meta, "btn", id);

        item.setItemMeta(meta);

        return item;
    }


    public static int getButtonSlot(ConfigurationSection root, String id) {

        ConfigurationSection sec = root.getConfigurationSection("buttons." + id);

        if (sec == null) return -1;

        return sec.getInt("slot", -1);
    }

}