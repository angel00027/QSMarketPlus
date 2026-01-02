package mp.quesito.qSMarketPlus.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.shop.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

public class MenuItems {

    // ============================================================
    //                      KEYS PDC GLOBALES
    // ============================================================

    public static final NamespacedKey KEY_AMOUNT_MOD =
            new NamespacedKey(QSMarketPlus.getInstance(), "amount_mod");

    public static final NamespacedKey KEY_SPECIAL =
            new NamespacedKey(QSMarketPlus.getInstance(), "special_btn");


    // ============================================================
    //               UTILIDAD: CABEZAS PERSONALIZADAS
    // ============================================================

    public static ItemStack customHead(String displayName, String textureInput) {

        // Crear cabeza base
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
                Bukkit.getLogger().warning("[QSMarketPlus] Textura inválida para cabeza: " + textureInput);
            }
        }

        head.setItemMeta(meta);
        return head;
    }

    private static String extractTextureUrl(String input) {

        // Si ya es URL → devolverla tal cual
        if (input.startsWith("http://") || input.startsWith("https://")) {
            return input;
        }

        // Intentar descodificar Base64
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

        // Ítem real con NBT + texturas
        ItemStack stack = item.getRealItem().clone();

        stack.setAmount(Math.min(amount, stack.getMaxStackSize()));

        ItemMeta meta = stack.getItemMeta();

        double unit = buying ? item.getBuy() : item.getSell();
        double total = unit * amount;

        // Si el ítem no tiene nombre custom, usar nombre del shop
        if (!meta.hasDisplayName()) {
            String title = buying ? "<green>Comprando: " : "<red>Vendiendo: ";
            meta.setDisplayName(MessageUtil.toLegacy(title + item.getName()));
        }

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        lore.add("");
        lore.add(MessageUtil.toLegacy("<gray>Cantidad: <yellow>" + amount));
        lore.add(MessageUtil.toLegacy("<gray>Precio unitario: <yellow>$" + unit));
        lore.add(MessageUtil.toLegacy("<gray>Total: <gold>$" + total));
        lore.add("");

        lore.add(MessageUtil.toLegacy(
                buying ? "<green>Click para confirmar compra." : "<red>Click para confirmar venta."
        ));

        meta.setLore(lore);
        stack.setItemMeta(meta);

        return stack;
    }



    // ============================================================
//      MiniMessage → LEGACY para usar en ItemMeta
// ============================================================
    public static String miniToLegacy(String text) {
        if (text == null) return "";
        return MessageUtil.toLegacy(text);
    }

    // ============================================================
//   BOTONES GENERALES (Paginación / Verde / Rojo) MiniMessage
// ============================================================

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

        // name viene sin MiniMessage → permitirlo también
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

        ItemStack item;

        // Si tiene textura
        if (head != null && !head.isEmpty()) {
            item = customHead(name, head);
        } else {
            item = new ItemStack(Material.matchMaterial(materialName));
        }

        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(MessageUtil.toLegacy(name));
        meta.setLore(MessageUtil.loreToLegacy(sec.getStringList("lore")));

        item.setItemMeta(meta);
        return item;
    }

    // ============================================================
    //                         DECORACIÓN
    // ============================================================

    public static ItemStack glass() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }


    // ============================================================
    //                        VOLVER / CANCELAR
    // ============================================================

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


    // ============================================================
    //               CONFIRMAR COMPRA / VENTA
    // ============================================================

    public static ItemStack confirmButton() {
        ItemStack item = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(MessageUtil.toLegacy("<green>Confirmar"));
        MetaUtil.setTag(meta, "btn", "confirm");

        item.setItemMeta(meta);
        return item;
    }


    // ============================================================
    //               ITEM INFORMATIVO PARA CANTIDAD
    // ============================================================

    public static ItemStack infoAmountItem(ShopItem item, int amount, boolean buying) {

        // 1️⃣ Ítem real incluyendo NBT, encantamientos, IA/Oraxen, etc.
        ItemStack stack = item.getRealItem().clone();

        // Respetar límite de stack
        stack.setAmount(Math.min(amount, stack.getMaxStackSize()));

        ItemMeta meta = stack.getItemMeta();

        double price = buying ? item.getBuy() * amount : item.getSell() * amount;

        // 2️⃣ Si no tiene título personalizado, usar el del shop
        if (!meta.hasDisplayName()) {
            meta.setDisplayName(MessageUtil.toLegacy("<aqua>" + item.getName() + "</aqua>"));
        }

        // 3️⃣ Conservar lore ORIGINAL (con conversión segura)
        List<String> lore = new ArrayList<>();

        if (meta.hasLore() && meta.getLore() != null) {
            for (String line : meta.getLore()) {
                lore.add(MessageUtil.toLegacy(line)); // 🔥 FIX: convertir y conservar
            }
        }

        // 4️⃣ agregar espacio
        lore.add("");

        // 5️⃣ agregar lore de cantidad / precio
        lore.add(MessageUtil.toLegacy("<gray>Cantidad: <white>" + amount));
        lore.add(MessageUtil.toLegacy("<gray>Total: <white>$" + price));

        lore.add("");

        lore.add(MessageUtil.toLegacy(
                buying ? "<green>Comprando..." : "<red>Vendiendo..."
        ));

        meta.setLore(lore);
        stack.setItemMeta(meta);

        return stack;
    }



    public static boolean alreadyOwned(Player player, ShopItem item) {

        // No se usa one-time → no bloquear
        if (!item.isOnlyOnce()) return false;

        // Verificar permiso configurado en shop.yml
        String perm = item.getPermission();
        if (perm != null && !perm.isEmpty()) {
            if (player.hasPermission(perm)) {
                return true; // ya lo tiene
            }
        }

        return false;
    }



    // ============================================================
    //                         BOTÓN COMPRAR
    // ============================================================

    public static ItemStack buyButton(Player player, ShopItem item) {

        ItemStack stack = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = stack.getItemMeta();

        // ========================================================
        //        ✔️ Si el jugador ya lo compró → BLOQUEAR
        // ========================================================
        if (item.isOnlyOnce() && alreadyOwned(player, item)) {

            meta.setDisplayName(MessageUtil.toLegacy("<red>Ya adquirido</red>"));

            List<String> lore = new ArrayList<>();
            lore.add(MessageUtil.toLegacy("<gray>Este artículo solo puede"));
            lore.add(MessageUtil.toLegacy("<gray>comprarse una vez."));
            lore.add("");

            if (item.getPermission() != null)
                lore.add(MessageUtil.toLegacy("<gray>Permiso detectado: <yellow>" + item.getPermission()));

            meta.setLore(lore);

            // etiqueta para bloquear clics
            MetaUtil.setTag(meta, "btn", "disabled");

            stack.setType(Material.BARRIER); // opcional: ícono bloqueado
            stack.setItemMeta(meta);
            return stack;
        }

        // ========================================================
        //              ✔️ BOTÓN NORMAL DE COMPRA
        // ========================================================
        meta.setDisplayName(MessageUtil.toLegacy("<green>Comprar</green>"));

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.toLegacy("<gray>Comprar: <white>" + item.getName()));
        lore.add(MessageUtil.toLegacy("<gray>Precio: <white>$" + item.getBuy()));

        meta.setLore(lore);

        MetaUtil.setTag(meta, "btn", "buy");

        stack.setItemMeta(meta);
        return stack;
    }


    // ============================================================
    //                         BOTÓN VENDER
    // ============================================================

    public static ItemStack sellButton(ShopItem item) {
        ItemStack stack = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = stack.getItemMeta();

        meta.setDisplayName(MessageUtil.toLegacy("<red>Vender</red>"));

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.toLegacy("<gray>Vender: <white>" + item.getName()));
        lore.add(MessageUtil.toLegacy("<gray>Precio: <white>$" + item.getBuy()));

        meta.setLore(lore);

        MetaUtil.setTag(meta, "btn", "sell");

        stack.setItemMeta(meta);
        return stack;
    }
}
