package mp.quesito.qSMarketPlus.hooks.impl;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Hook con QsProteccion (softdepend).
 *
 * Usa reflexión para no generar dependencia
 * de compilación con QsProteccion.
 *
 * Verifica si un cofre está protegido por
 * el sistema de ChestProtect de QsProteccion
 * y si un jugador puede crear tiendas dentro
 * de una región de ProtectionStones.
 */
public final class QsProteccionHook {

    private static boolean available = false;
    private static Object chestProtectionManager;

    private static Method isProtectedMethod;
    private static Method getProtectionMethod;
    private static Method getOwnerMethod;

    private static Method getRegionMethod;
    private static Method regionIsOwnerMethod;
    private static Method regionIsMemberMethod;
    private static Method regionGetTenantMethod;

    private QsProteccionHook() {}

    /**
     * Inicializa el hook. Debe llamarse en onEnable.
     */
    public static void init() {
        available = false;
        chestProtectionManager = null;

        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("QsProteccion");
            if (plugin == null) {
                return;
            }

            Method getCpm =
                    plugin.getClass().getMethod("getChestProtectionManager");

            Object cpm = getCpm.invoke(plugin);

            if (cpm == null) {
                return;
            }

            chestProtectionManager = cpm;

            isProtectedMethod =
                    cpm.getClass().getMethod(
                            "isProtected",
                            Location.class
                    );

            getProtectionMethod =
                    cpm.getClass().getMethod(
                            "getProtection",
                            Location.class
                    );

            Class<?> protectionClass =
                    getProtectionMethod.getReturnType();

            if (protectionClass != null) {
                getOwnerMethod =
                        protectionClass.getMethod("getOwner");
            }

            // ==================================================
            // 🌎 REGIÓN DE PROTECTIONSTONES
            // ==================================================

            getRegionMethod =
                    cpm.getClass().getMethod(
                            "getRegion",
                            Location.class
                    );

            Class<?> regionClass =
                    getRegionMethod.getReturnType();

            if (regionClass != null) {
                regionIsOwnerMethod =
                        regionClass.getMethod(
                                "isOwner",
                                UUID.class
                        );

                regionIsMemberMethod =
                        regionClass.getMethod(
                                "isMember",
                                UUID.class
                        );

                regionGetTenantMethod =
                        regionClass.getMethod("getTenant");
            }

            available = true;

        } catch (Exception exception) {
            available = false;
            QSMarketPlus.getInstance().getLogger()
                    .warning("No se pudo conectar con QsProteccion: "
                            + exception.getMessage());
        }
    }

    // ==========================================================
    // 🔍 ESTADO
    // ==========================================================

    /**
     * Indica si QsProteccion está disponible y su API accesible.
     */
    public static boolean isAvailable() {
        return available;
    }

    /**
     * Reinicia el hook (se usa cuando QsProteccion se desactiva).
     */
    public static void reset() {
        available = false;
        chestProtectionManager = null;
        isProtectedMethod = null;
        getProtectionMethod = null;
        getOwnerMethod = null;
        getRegionMethod = null;
        regionIsOwnerMethod = null;
        regionIsMemberMethod = null;
        regionGetTenantMethod = null;
    }

    // ==========================================================
    // 🛡️ COFRE PROTEGIDO
    // ==========================================================

    /**
     * Comprueba si un cofre está protegido por QsProteccion.
     */
    public static boolean isProtected(Location location) {
        return getProtectedChestLocation(location) != null;
    }

    /**
     * Devuelve la ubicación real de la protección de un cofre.
     *
     * Soporta cofres dobles: si la protección está en la otra
     * mitad, devuelve esa mitad.
     *
     * null si el cofre (o su otra mitad) no está protegido.
     */
    public static Location getProtectedChestLocation(Location location) {
        if (!available || location == null) {
            return null;
        }

        if (isProtectedExact(location)) {
            return location;
        }

        Block block = location.getBlock();

        if (!(block.getState() instanceof Chest chest)) {
            return null;
        }

        if (chest.getInventory().getHolder()
                instanceof DoubleChest doubleChest) {

            Chest left = (Chest) doubleChest.getLeftSide();
            Chest right = (Chest) doubleChest.getRightSide();

            if (left != null && isProtectedExact(left.getLocation())) {
                return left.getLocation();
            }

            if (right != null && isProtectedExact(right.getLocation())) {
                return right.getLocation();
            }
        }

        return null;
    }

    private static boolean isProtectedExact(Location location) {
        if (!available || location == null) {
            return false;
        }

        try {
            return (Boolean) isProtectedMethod.invoke(
                    chestProtectionManager,
                    location
            );
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * Obtiene el dueño de la protección de un cofre.
     *
     * @return UUID del dueño, o null si no está protegido.
     */
    public static UUID getOwner(Location location) {
        if (!available || location == null) {
            return null;
        }

        try {
            Object protection =
                    getProtectionMethod.invoke(
                            chestProtectionManager,
                            location
                    );

            if (protection == null) {
                return null;
            }

            return (UUID) getOwnerMethod.invoke(protection);

        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * Comprueba si el jugador es dueño de la protección del cofre.
     */
    public static boolean isOwner(Location location, UUID uuid) {
        if (uuid == null) {
            return false;
        }

        UUID owner = getOwner(location);

        return owner != null && owner.equals(uuid);
    }

    // ==========================================================
    // 🪧 CARTEL DE PROTECCIÓN
    // ==========================================================

    /**
     * Comprueba si un cartel es el marcador de protección
     * de QsProteccion (un cartel de pared pegado a un cofre
     * protegido).
     */
    public static boolean isProtectionSign(Block block) {
        return getProtectedChestFromSign(block) != null;
    }

    /**
     * Si el bloque es un cartel de pared pegado a un cofre
     * protegido, devuelve el cofre. En caso contrario, null.
     */
    public static Block getProtectedChestFromSign(Block block) {
        if (!available || block == null) {
            return null;
        }

        if (!(block.getBlockData() instanceof WallSign wallSign)) {
            return null;
        }

        Block attached = block.getRelative(
                wallSign.getFacing().getOppositeFace()
        );

        if (!(attached.getState() instanceof org.bukkit.block.Chest)) {
            return null;
        }

        if (attached.getState() instanceof org.bukkit.block.Chest
                && isProtected(attached.getLocation())) {
            return attached;
        }

        return null;
    }

    // ==========================================================
    // 🌎 REGIÓN DE PROTECTIONSTONES
    // ==========================================================

    /**
     * Obtiene la región de ProtectionStones donde
     * se encuentra el bloque. null si no hay región
     * o el hook no está disponible.
     */
    public static Object getRegion(Location location) {
        if (!available || location == null) {
            return null;
        }

        try {
            return getRegionMethod.invoke(
                    chestProtectionManager,
                    location
            );
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * Comprueba si el jugador es dueño de la región
     * donde se encuentra el bloque.
     */
    public static boolean isRegionOwner(Location location, UUID uuid) {
        if (uuid == null) return false;

        Object region = getRegion(location);
        if (region == null) return false;

        try {
            return (Boolean) regionIsOwnerMethod.invoke(region, uuid);
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * Comprueba si el jugador es miembro de la región
     * donde se encuentra el bloque.
     */
    public static boolean isRegionMember(Location location, UUID uuid) {
        if (uuid == null) return false;

        Object region = getRegion(location);
        if (region == null) return false;

        try {
            return (Boolean) regionIsMemberMethod.invoke(region, uuid);
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * Obtiene el tenant/inquilino de la región.
     * null si no hay región o no tiene tenant.
     */
    public static UUID getRegionTenant(Location location) {
        Object region = getRegion(location);
        if (region == null) return null;

        try {
            return (UUID) regionGetTenantMethod.invoke(region);
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * Comprueba si el jugador puede crear una tienda
     * dentro de la región del cofre.
     *
     * REGLAS:
     *
     * - Sin región → permitido (true).
     * - Dueño o miembro de la región → permitido.
     * - Tenant o externo → denegado.
     */
    public static boolean canCreateInRegion(Location location, UUID uuid) {
        if (uuid == null) return false;

        Object region = getRegion(location);
        if (region == null) {
            return true;
        }

        if (isRegionOwner(location, uuid)) {
            return true;
        }

        return isRegionMember(location, uuid);
    }
}