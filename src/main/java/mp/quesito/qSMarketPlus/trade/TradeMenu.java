package mp.quesito.qSMarketPlus.trade;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import mp.quesito.qSMarketPlus.utils.MetaUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TradeMenu {

    public static final int SIZE = 54;

    // Cabezas de jugador
    public static final int A_HEAD = 9;
    public static final int B_HEAD = 17;

    // Zonas de oferta: cada jugador ocupa la mitad del GUI (12 slots) - estilo AXTrade
    public static final int[] A_OFFER = {
            10, 11, 12,
            19, 20, 21,
            28, 29, 30,
            37, 38, 39
    };

    public static final int[] B_OFFER = {
            14, 15, 16,
            23, 24, 25,
            32, 33, 34,
            41, 42, 43
    };

    // Acciones jugador A (columna izquierda)
    public static final int A_MONEY = 18;
    public static final int A_XP = 27;
    public static final int A_READY = 36;

    // Acciones jugador B (columna derecha)
    public static final int B_MONEY = 26;
    public static final int B_XP = 35;
    public static final int B_READY = 44;

    // Centro
    public static final int STATUS = 13;
    public static final int HELP = 22;
    public static final int CANCEL = 40;

    private static final Set<Integer> OFFER_SET = new HashSet<>();

    static {
        for (int s : A_OFFER) OFFER_SET.add(s);
        for (int s : B_OFFER) OFFER_SET.add(s);
    }

    // =====================================================
    //                ABRIR EL TRADEO (AMBOS)
    // =====================================================
    public static void open(TradeSession session) {
        Player a = Bukkit.getPlayer(session.getPlayerA());
        Player b = Bukkit.getPlayer(session.getPlayerB());
        if (a == null || b == null) return;

        TradeHolder holder = new TradeHolder(session);

        String title = MessageUtil.toLegacy(
                "<dark_gray>Trade » <aqua>" + a.getName() +
                        "</aqua> <dark_gray>⇄</dark_gray> <aqua>" + b.getName() + "</aqua>"
        );

        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        session.setInventory(inv);

        render(inv, session);

        a.openInventory(inv);
        b.openInventory(inv);
    }

    // =====================================================
    //                RE-DIBUJAR GUI
    // =====================================================
    public static void render(Inventory inv, TradeSession session) {
        if (inv == null || session == null) return;

        ItemStack glass = glass();

        for (int i = 0; i < SIZE; i++) {
            if (OFFER_SET.contains(i)) continue;
            inv.setItem(i, glass);
        }

        inv.setItem(A_HEAD, headLabel(session.getPlayerA()));
        inv.setItem(B_HEAD, headLabel(session.getPlayerB()));

        inv.setItem(A_MONEY, moneyButton(session.getPlayerA(), session.getMoneyA()));
        inv.setItem(A_XP, xpButton(session.getPlayerA(), session.getXpA()));
        inv.setItem(B_MONEY, moneyButton(session.getPlayerB(), session.getMoneyB()));
        inv.setItem(B_XP, xpButton(session.getPlayerB(), session.getXpB()));

        inv.setItem(A_READY, readyButton(session.isReadyA(), session.getCountdown()));
        inv.setItem(B_READY, readyButton(session.isReadyB(), session.getCountdown()));

        inv.setItem(STATUS, statusPane(inv, session));
        inv.setItem(HELP, helpPane());

        inv.setItem(CANCEL, cancelButton());
    }

    // =====================================================
    //         LEER / LIMPIAR OFERTAS DEL INVENTARIO
    // =====================================================
    public static List<ItemStack> readOffers(Inventory inv, int[] slots) {
        List<ItemStack> list = new ArrayList<>();
        if (inv == null) return list;

        for (int s : slots) {
            ItemStack it = inv.getItem(s);
            if (it != null && !it.getType().isAir()) list.add(it);
        }
        return list;
    }

    public static int countOffered(Inventory inv, int[] slots) {
        int count = 0;
        if (inv == null) return 0;

        for (int s : slots) {
            ItemStack it = inv.getItem(s);
            if (it != null && !it.getType().isAir()) count += it.getAmount();
        }
        return count;
    }

    public static void clearOffers(Inventory inv, int[] slots) {
        if (inv == null) return;
        for (int s : slots) inv.setItem(s, null);
    }

    // =====================================================
    //                     ITEMS DEL GUI
    // =====================================================
    private static ItemStack glass() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack headLabel(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        String name = op.getName() != null ? op.getName() : "?";

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(op);
        meta.setDisplayName(MessageUtil.toLegacy("<yellow>" + name));

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.toLegacy("<gray>Click en tus ítems del inventario"));
        lore.add(MessageUtil.toLegacy("<gray>(o <yellow>Shift+Click</yellow>) para colocarlos"));
        lore.add(MessageUtil.toLegacy("<gray>en tu mitad."));
        meta.setLore(lore);

        head.setItemMeta(meta);
        return head;
    }

    private static ItemStack readyButton(boolean ready, int countdown) {
        ItemStack item;
        List<String> lore = new ArrayList<>();

        if (countdown > 0) {
            // Ambos listos -> el botón muestra un stack de "countdown" lana naranja (5, 4, 3, ...)
            item = new ItemStack(Material.ORANGE_WOOL, Math.max(1, countdown));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(MessageUtil.toLegacy("<yellow>" + countdown));
            lore.add(MessageUtil.toLegacy("<gray>Completando el tradeo..."));
            lore.add(MessageUtil.toLegacy("<gray>Click para cancelar y"));
            lore.add(MessageUtil.toLegacy("<gray>añadir algo más."));
            meta.setLore(lore);
            MetaUtil.setTag(meta, "btn", "ready");
            item.setItemMeta(meta);
            return item;
        }

        Material mat = ready ? Material.LIME_WOOL : Material.RED_WOOL;
        item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if (ready) {
            meta.setDisplayName(MessageUtil.toLegacy("<green>✔ Listo</green>"));
            lore.add(MessageUtil.toLegacy("<gray>Estás listo para completar el tradeo."));
            lore.add(MessageUtil.toLegacy("<gray>Click para desmarcar."));
        } else {
            meta.setDisplayName(MessageUtil.toLegacy("<gray>Listo</gray>"));
            lore.add(MessageUtil.toLegacy("<gray>Click para confirmar tu oferta."));
            lore.add(MessageUtil.toLegacy("<gray>Con <yellow>ambos</yellow> listos empieza"));
            lore.add(MessageUtil.toLegacy("<gray>una <yellow>cuenta atrás de " + countdownSeconds() + "s</yellow>."));
            lore.add(MessageUtil.toLegacy("<gray>Click de nuevo para cancelar."));
        }

        meta.setLore(lore);
        MetaUtil.setTag(meta, "btn", "ready");

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack moneyButton(UUID uuid, double offered) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(MessageUtil.toLegacy("<yellow>Dinero</yellow> <white>(" + name(uuid) + ")</white>"));

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.toLegacy("<gray>Ofrecido: <green>$" + formatDouble(offered)));
        lore.add("");
        lore.add(MessageUtil.toLegacy("<green>Click izq:</green> <white>se cierra el menú"));
        lore.add(MessageUtil.toLegacy("<white>  y escribes la cantidad en el chat"));
        lore.add(MessageUtil.toLegacy("<red>Click der:</red> <white>quitar la oferta"));
        meta.setLore(lore);

        MetaUtil.setTag(meta, "aid", "money");

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack xpButton(UUID uuid, int offeredLevels) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(MessageUtil.toLegacy("<aqua>Niveles de XP</aqua> <white>(" + name(uuid) + ")</white>"));

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.toLegacy("<gray>Ofrecido: <aqua>" + offeredLevels + " niveles"));
        lore.add("");
        lore.add(MessageUtil.toLegacy("<green>Click izq:</green> <white>se cierra el menú"));
        lore.add(MessageUtil.toLegacy("<white>  y escribes la cantidad en el chat"));
        lore.add(MessageUtil.toLegacy("<red>Click der:</red> <white>quitar la oferta"));
        meta.setLore(lore);

        MetaUtil.setTag(meta, "aid", "xp");

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack helpPane() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(MessageUtil.toLegacy("<yellow>¿Cómo ofrecer?</yellow>"));

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.toLegacy("<gray>1. Pon tus ítems en tu mitad (izquierda o derecha)."));
        lore.add(MessageUtil.toLegacy("<gray>2. Click en el botón de <yellow>Dinero</yellow> o <aqua>Niveles</aqua>"));
        lore.add(MessageUtil.toLegacy("<gray>   y escribe la cantidad en el chat."));
        lore.add(MessageUtil.toLegacy("<gray>3. Cuando ambos estén <green>Listos</green>,"));
        lore.add(MessageUtil.toLegacy("<gray>   empieza una cuenta atrás y se completa."));
        lore.add(MessageUtil.toLegacy("<gray>   Click en <green>Listo</green> para cancelar."));
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack statusPane(Inventory inv, TradeSession session) {
        ItemStack item = new ItemStack(Material.SUNFLOWER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(MessageUtil.toLegacy("<yellow>Estado del Tradeo"));

        List<String> lore = new ArrayList<>();

        lore.add(MessageUtil.toLegacy("<gray>» <white>" + name(session.getPlayerA()) +
                "</white>: " + (session.isReadyA()
                ? "<green>✔ Listo</green>"
                : "<red>✘ No listo</red>")
                + " <dark_gray>(" + countOffered(inv, A_OFFER) + ")" +
                " <green>$" + formatDouble(session.getMoneyA()) + "</green>" +
                " <aqua>" + session.getXpA() + " niv</aqua>"));
        lore.add(MessageUtil.toLegacy("<gray>» <white>" + name(session.getPlayerB()) +
                "</white>: " + (session.isReadyB()
                ? "<green>✔ Listo</green>"
                : "<red>✘ No listo</red>")
                + " <dark_gray>(" + countOffered(inv, B_OFFER) + ")" +
                " <green>$" + formatDouble(session.getMoneyB()) + "</green>" +
                " <aqua>" + session.getXpB() + " niv</aqua>"));
        lore.add("");
        lore.add(MessageUtil.toLegacy("<gray>Ambos deben estar <green>Listos</green>"));

        if (session.getCountdown() > 0) {
            lore.add(MessageUtil.toLegacy("<yellow>Completando en <green>" + session.getCountdown()
                    + "</green> seg..."));
            lore.add(MessageUtil.toLegacy("<gray>Click en tu botón <green>Listo</green>"));
            lore.add(MessageUtil.toLegacy("<gray>para cancelar y añadir algo más."));
        } else {
            lore.add(MessageUtil.toLegacy("<gray>para completar el intercambio."));
            lore.add(MessageUtil.toLegacy(""));
            lore.add(MessageUtil.toLegacy("<gray>Con ambos listos, hay <yellow>" + countdownSeconds() + " seg</yellow>"));
            lore.add(MessageUtil.toLegacy("<gray>para cancelar antes de completar."));
        }

        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack cancelButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(MessageUtil.toLegacy("<red>✘ Cancelar tradeo"));

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.toLegacy("<gray>Los ítems ofrecidos serán devueltos."));
        meta.setLore(lore);

        MetaUtil.setTag(meta, "btn", "cancel");

        item.setItemMeta(meta);
        return item;
    }

    // =====================================================
    //                      HELPERS
    // =====================================================
    private static String name(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() != null ? op.getName() : "?";
    }

    private static int countdownSeconds() {
        return Math.max(1, QSMarketPlus.getInstance().getConfig().getInt("trade.countdown-seconds", 5));
    }

    private static String formatDouble(double amount) {
        return amount == Math.floor(amount)
                ? String.valueOf((long) amount)
                : String.format("%.2f", amount);
    }
}