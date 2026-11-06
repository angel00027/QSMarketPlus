package mp.quesito.qSMarketPlus.coupons;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.database.SQLManager;
import mp.quesito.qSMarketPlus.shop.ShopCategory;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

/**
 * Cupones de descuento para la tienda.
 *
 * Plantillas (cupones.yml): porcentaje, categorias, usos, duracion, minimo y tope.
 * Codigos: se generan desde una plantilla para un jugador (aparece directo en
 * "Mis cupones") o sueltos (el primero que lo canjea se lo queda). Los codigos
 * publicos de cupones.yml los puede canjear cualquiera, con limite total y por jugador.
 *
 * Cada jugador elige un cupon activo; se aplica solo a compras de sus categorias.
 */
public class CouponManager {

    public record Template(String id, String name, double percent, Set<String> categories, int uses,
                           long durationMs, double minimum, double maxDiscount, String material, String nexo) {

        public boolean appliesTo(ShopCategory category) {
            return categories.contains("*") || (category != null && categories.contains(category.getId().toLowerCase()));
        }
    }

    public record Coupon(String code, String template, UUID owner, int usesLeft, long expires) {
        public boolean expired() {
            return expires > 0 && System.currentTimeMillis() > expires;
        }

        public boolean unlimited() {
            return usesLeft == UNLIMITED;
        }

        /** Le quedan usos (los ilimitados siempre). */
        public boolean usable() {
            return !expired() && (unlimited() || usesLeft > 0);
        }
    }

    /** Lo que ahorra una compra con el cupon activo. */
    public record Quote(Coupon coupon, Template template, double original, double saved) {
        public double finalPrice() {
            return Math.max(0, Math.round((original - saved) * 100.0) / 100.0);
        }
    }

    /** uses_left de un cupon que nunca se gasta. */
    public static final int UNLIMITED = -1;

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));

    private final QSMarketPlus plugin;
    private final SQLManager sql;
    private final SecureRandom random = new SecureRandom();
    private final Map<UUID, String> selected = new HashMap<>();

    private final Map<String, Template> templates = new LinkedHashMap<>();
    private YamlConfiguration config;

    public CouponManager(QSMarketPlus plugin) {
        this.plugin = plugin;
        this.sql = plugin.getSqlManager();

        sql.update("""
                CREATE TABLE IF NOT EXISTS coupons (
                    code VARCHAR(32) PRIMARY KEY,
                    template VARCHAR(64) NOT NULL,
                    owner VARCHAR(36),
                    uses_left INT NOT NULL,
                    expires BIGINT NOT NULL,
                    created BIGINT NOT NULL,
                    created_by VARCHAR(36)
                )""");
        sql.update("""
                CREATE TABLE IF NOT EXISTS coupon_public_uses (
                    code VARCHAR(64) NOT NULL,
                    uuid VARCHAR(36) NOT NULL,
                    uses INT NOT NULL,
                    PRIMARY KEY (code, uuid)
                )""");

        reload();
    }

    // =====================================================
    //                    CONFIG
    // =====================================================
    public void reload() {
        File file = new File(plugin.getDataFolder(), "cupones.yml");
        if (!file.exists()) plugin.saveResource("cupones.yml", false);
        config = YamlConfiguration.loadConfiguration(file);

        templates.clear();
        ConfigurationSection sec = config.getConfigurationSection("plantillas");
        if (sec == null) return;

        for (String id : sec.getKeys(false)) {
            ConfigurationSection t = sec.getConfigurationSection(id);
            if (t == null) continue;

            Set<String> cats = new HashSet<>();
            for (String c : t.getStringList("categorias")) cats.add(c.toLowerCase());
            if (cats.isEmpty()) cats.add("*");

            templates.put(id.toLowerCase(), new Template(
                    id.toLowerCase(),
                    t.getString("nombre", id),
                    Math.max(0, Math.min(100, t.getDouble("descuento", 10))),
                    cats,
                    parseUses(t.getString("usos", "1")),
                    parseDuration(t.getString("duracion", "0")),
                    Math.max(0, t.getDouble("minimo", 0)),
                    Math.max(0, t.getDouble("maximo", 0)),
                    t.getString("material", "PAPER"),
                    t.getString("nexo", null)));
        }
    }

    public YamlConfiguration config() {
        return config;
    }

    public Collection<Template> templates() {
        return templates.values();
    }

    public Template template(String id) {
        return id == null ? null : templates.get(id.toLowerCase());
    }

    // =====================================================
    //                  CREAR Y CANJEAR
    // =====================================================

    /** owner null = codigo suelto: lo canjea el primero que lo escriba. */
    public Coupon generate(Template template, UUID owner, UUID creator) {
        String code;
        do {
            code = randomCode();
        } while (find(code) != null);

        long expires = template.durationMs() > 0 ? System.currentTimeMillis() + template.durationMs() : 0;
        sql.update("INSERT INTO coupons (code, template, owner, uses_left, expires, created, created_by) VALUES (?,?,?,?,?,?,?)",
                code, template.id(), owner == null ? null : owner.toString(), template.uses(), expires,
                System.currentTimeMillis(), creator == null ? null : creator.toString());
        return new Coupon(code, template.id(), owner, template.uses(), expires);
    }

    public enum RedeemResult { OK, NOT_FOUND, TAKEN, EXPIRED, LIMIT, ALREADY }

    public record Redeem(RedeemResult result, Coupon coupon) {
    }

    public Redeem redeem(Player player, String rawCode) {
        String code = rawCode.trim().toUpperCase();
        UUID uuid = player.getUniqueId();

        // 1) Codigo publico de cupones.yml
        ConfigurationSection pub = publicCode(code);
        if (pub != null) {
            Template template = template(pub.getString("plantilla"));
            if (template == null) return new Redeem(RedeemResult.NOT_FOUND, null);

            int perPlayer = Math.max(1, pub.getInt("usos-por-jugador", 1));
            int total = pub.getInt("usos-totales", 0);

            if (publicUses(code, uuid) >= perPlayer) return new Redeem(RedeemResult.ALREADY, null);
            if (total > 0 && publicUsesTotal(code) >= total) return new Redeem(RedeemResult.LIMIT, null);

            if (sql.update("UPDATE coupon_public_uses SET uses = uses + 1 WHERE code=? AND uuid=?", code, uuid.toString()) == 0) {
                sql.update("INSERT INTO coupon_public_uses (code, uuid, uses) VALUES (?,?,1)", code, uuid.toString());
            }
            return new Redeem(RedeemResult.OK, generate(template, uuid, null));
        }

        // 2) Codigo generado
        Coupon coupon = find(code);
        if (coupon == null) return new Redeem(RedeemResult.NOT_FOUND, null);
        if (!coupon.usable()) return new Redeem(RedeemResult.EXPIRED, null);
        if (coupon.owner() != null) {
            return new Redeem(coupon.owner().equals(uuid) ? RedeemResult.ALREADY : RedeemResult.TAKEN, null);
        }

        // Solo se lo queda si nadie lo reclamo antes (evita dos canjes a la vez)
        if (sql.update("UPDATE coupons SET owner=? WHERE code=? AND owner IS NULL", uuid.toString(), code) == 0) {
            return new Redeem(RedeemResult.TAKEN, null);
        }
        return new Redeem(RedeemResult.OK, new Coupon(code, coupon.template(), uuid, coupon.usesLeft(), coupon.expires()));
    }

    public boolean delete(String code) {
        selected.values().removeIf(c -> c.equalsIgnoreCase(code));
        return sql.update("DELETE FROM coupons WHERE code=?", code.toUpperCase()) > 0;
    }

    // =====================================================
    //                    CONSULTAS
    // =====================================================
    public Coupon find(String code) {
        List<Coupon> out = new ArrayList<>();
        sql.query("SELECT * FROM coupons WHERE code=?", rs -> read(rs, out), code.toUpperCase());
        return out.isEmpty() ? null : out.get(0);
    }

    /** Cupones utilizables del jugador (con usos y sin caducar), los que caducan antes primero. */
    public List<Coupon> wallet(UUID uuid) {
        List<Coupon> out = new ArrayList<>();
        sql.query("SELECT * FROM coupons WHERE owner=? AND (uses_left > 0 OR uses_left = ?)", rs -> read(rs, out),
                uuid.toString(), UNLIMITED);
        out.removeIf(c -> !c.usable() || template(c.template()) == null);
        out.sort(Comparator.comparingLong(c -> c.expires() == 0 ? Long.MAX_VALUE : c.expires()));
        return out;
    }

    private void read(java.sql.ResultSet rs, List<Coupon> out) {
        try {
            while (rs.next()) {
                String owner = rs.getString("owner");
                out.add(new Coupon(rs.getString("code"), rs.getString("template"),
                        owner == null ? null : UUID.fromString(owner), rs.getInt("uses_left"), rs.getLong("expires")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ConfigurationSection publicCode(String code) {
        ConfigurationSection sec = config.getConfigurationSection("codigos-publicos");
        if (sec == null) return null;
        for (String key : sec.getKeys(false)) {
            if (key.equalsIgnoreCase(code)) return sec.getConfigurationSection(key);
        }
        return null;
    }

    private int publicUses(String code, UUID uuid) {
        int[] n = {0};
        sql.query("SELECT uses FROM coupon_public_uses WHERE code=? AND uuid=?", rs -> {
            try { if (rs.next()) n[0] = rs.getInt(1); } catch (Exception ignored) { }
        }, code, uuid.toString());
        return n[0];
    }

    private int publicUsesTotal(String code) {
        int[] n = {0};
        sql.query("SELECT COALESCE(SUM(uses), 0) FROM coupon_public_uses WHERE code=?", rs -> {
            try { if (rs.next()) n[0] = rs.getInt(1); } catch (Exception ignored) { }
        }, code);
        return n[0];
    }

    // =====================================================
    //              CUPON ACTIVO Y DESCUENTO
    // =====================================================
    public Coupon selected(Player player) {
        String code = selected.get(player.getUniqueId());
        if (code == null) return null;

        Coupon coupon = find(code);
        if (coupon == null || !coupon.usable() || !player.getUniqueId().equals(coupon.owner())) {
            selected.remove(player.getUniqueId());
            return null;
        }
        return coupon;
    }

    public void select(Player player, String code) {
        if (code == null) selected.remove(player.getUniqueId());
        else selected.put(player.getUniqueId(), code.toUpperCase());
    }

    /** Pasa al siguiente cupon que sirve para esta categoria; tras el ultimo, ninguno. */
    public Coupon cycle(Player player, ShopCategory category) {
        List<Coupon> usable = new ArrayList<>();
        for (Coupon c : wallet(player.getUniqueId())) {
            if (template(c.template()).appliesTo(category)) usable.add(c);
        }

        Coupon current = selected(player);
        int index = current == null ? -1 : indexOf(usable, current.code());
        Coupon next = index + 1 < usable.size() ? usable.get(index + 1) : null;
        select(player, next == null ? null : next.code());
        return next;
    }

    private int indexOf(List<Coupon> list, String code) {
        for (int i = 0; i < list.size(); i++) if (list.get(i).code().equals(code)) return i;
        return -1;
    }

    public int usableCount(Player player, ShopCategory category) {
        int n = 0;
        for (Coupon c : wallet(player.getUniqueId())) if (template(c.template()).appliesTo(category)) n++;
        return n;
    }

    /** Descuento del cupon activo para una compra; null si no aplica. */
    public Quote quote(Player player, ShopCategory category, double price) {
        Coupon coupon = selected(player);
        if (coupon == null) return null;

        Template template = template(coupon.template());
        if (template == null || !template.appliesTo(category) || price < template.minimum() || price <= 0) return null;

        double saved = price * template.percent() / 100.0;
        if (template.maxDiscount() > 0) saved = Math.min(saved, template.maxDiscount());
        saved = Math.round(saved * 100.0) / 100.0;

        return saved > 0 ? new Quote(coupon, template, price, saved) : null;
    }

    /** Gasta un uso; si se acaba, deja de estar activo. */
    public void consume(Player player, Coupon coupon) {
        if (coupon.unlimited()) return;
        sql.update("UPDATE coupons SET uses_left = uses_left - 1 WHERE code=? AND uses_left > 0", coupon.code());
        if (coupon.usesLeft() <= 1) selected.remove(player.getUniqueId());
    }

    // =====================================================
    //                   UTILIDADES
    // =====================================================
    private String randomCode() {
        StringBuilder sb = new StringBuilder(config.getString("prefijo", "QSM")).append('-');
        for (int i = 0; i < 8; i++) {
            if (i == 4) sb.append('-');
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString().toUpperCase();
    }

    /** "infinito", "ilimitado" o -1 = nunca se gasta. */
    public static int parseUses(String raw) {
        if (raw == null) return 1;
        String s = raw.trim().toLowerCase();
        if (s.equals("-1") || s.startsWith("infinit") || s.startsWith("ilimitad")) return UNLIMITED;
        try {
            return Math.max(1, Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public static String usesText(int uses) {
        return uses == UNLIMITED ? "ilimitados" : String.valueOf(uses);
    }

    public static long parseDuration(String raw) {
        if (raw == null) return 0;
        String s = raw.trim().toLowerCase();
        if (s.isEmpty() || s.equals("0") || s.equals("nunca")) return 0;

        long total = 0;
        StringBuilder num = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (Character.isDigit(ch)) {
                num.append(ch);
                continue;
            }
            if (num.isEmpty()) continue;
            long n = Long.parseLong(num.toString());
            num.setLength(0);
            total += switch (ch) {
                case 'd' -> n * 86_400_000L;
                case 'h' -> n * 3_600_000L;
                case 'm' -> n * 60_000L;
                case 's' -> n * 1_000L;
                default -> 0;
            };
        }
        if (!num.isEmpty()) total += Long.parseLong(num.toString()) * 86_400_000L; // solo numero = dias
        return total;
    }

    public static String timeLeft(long expires) {
        if (expires <= 0) return "nunca";
        long ms = expires - System.currentTimeMillis();
        if (ms <= 60_000L) return "menos de 1m";
        long minutes = ms / 60_000L;
        long days = minutes / 1440, hours = (minutes % 1440) / 60, mins = minutes % 60;
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + mins + "m";
        return mins + "m";
    }

    public static String money(double value) {
        return MONEY.format(value);
    }

    /** Nombres legibles de las categorias de una plantilla. */
    public String categoryNames(Template template) {
        if (template.categories().contains("*")) return text("todas-las-categorias", "todas");
        List<String> names = new ArrayList<>();
        for (String id : template.categories()) {
            ShopCategory cat = plugin.getCategoryManager().getCategory(id);
            names.add(cat != null ? MessageUtil.stripLegacy(MessageUtil.toLegacy(cat.getName())).trim() : id);
        }
        return String.join(", ", names);
    }

    public String text(String key, String fallback) {
        return config.getString("mensajes." + key, fallback);
    }

    public void send(org.bukkit.command.CommandSender to, String key, String fallback, String... vars) {
        String raw = text(key, fallback);
        for (int i = 0; i + 1 < vars.length; i += 2) raw = raw.replace("<" + vars[i] + ">", vars[i + 1]);
        to.sendMessage(MessageUtil.toLegacy(raw));
    }
}
