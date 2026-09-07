package mp.quesito.qSMarketPlus;

import mp.quesito.qSMarketPlus.auction.AuctionManager;
import mp.quesito.qSMarketPlus.commands.*;
import mp.quesito.qSMarketPlus.database.SQLManager;
import mp.quesito.qSMarketPlus.economia.*;
import mp.quesito.qSMarketPlus.hooks.HookManager;
import mp.quesito.qSMarketPlus.hooks.impl.QsProteccionHook;
import mp.quesito.qSMarketPlus.listeners.*;
import mp.quesito.qSMarketPlus.manager.*;
import mp.quesito.qSMarketPlus.shop.PlayerShop;
import mp.quesito.qSMarketPlus.shop.ShopCategory;
import mp.quesito.qSMarketPlus.trade.TradeManager;
import mp.quesito.qSMarketPlus.utils.Lang;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class QSMarketPlus extends JavaPlugin {

    private static QSMarketPlus instance;
    public static QSMarketPlus getInstance() { return instance; }

    private BukkitAudiences adventure;
    private PlayerShopManager shopManager;

    // Configuraciones de menús
    public AmountMenuConfig amountMenuConfig;
    public ConfirmMenuConfig confirmMenuConfig;
    public ActionMenuConfig actionMenuConfig;
    private UniquePurchaseManager uniquePurchaseManager;
    // Managers
    private CategoryManager categoryManager;
    private ItemManager itemManager;
    private AuctionManager auctionManager;
    private SQLManager sqlManager;
    private SignShopManager signShopManager;
    public static Economy economy;
    private boolean qstextursHook;
    private HookManager hookManager;
    private EconomyManager economyManager;
    private TradeManager tradeManager;


    @Override
    public void onEnable() {

        instance = this;
        qstextursHook = getServer().getPluginManager().isPluginEnabled("QSTexturs");

        // ============================
        // Hook con QsProteccion (softdepend)
        // ============================
        QsProteccionHook.init();

        if (QsProteccionHook.isAvailable()) {
            getLogger().info("Hooked into QsProteccion ✔");
        } else {
            getLogger().info("QsProteccion no encontrado. Soporte de protes desactivado.");
        }

        // ============================
        // Hook dinámico con QsProteccion:
        // si QsProteccion se activa DESPUÉS de QSMarketPlus
        // (posible por softdepend), lo detectamos y reinicializamos.
        // ============================
        getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onPluginEnable(org.bukkit.event.server.PluginEnableEvent event) {
                if (event.getPlugin().getName().equals("QsProteccion")) {
                    QsProteccionHook.init();
                    getLogger().info("Hooked into QsProteccion ✔ (activado después de QSMarketPlus)");
                }
            }

            @org.bukkit.event.EventHandler
            public void onPluginDisable(org.bukkit.event.server.PluginDisableEvent event) {
                if (event.getPlugin().getName().equals("QsProteccion")) {
                    QsProteccionHook.reset();
                }
            }
        }, this);

        // ============================
        // 4) Vault Economy
        // ============================
        if (!setupEconomy()) {
            getLogger().severe("Vault o economía no disponible. Plugin desactivado.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        if (qstextursHook) {
            getLogger().info("Hooked into QSTexturs ✔");
        } else {
            getLogger().info("QSTexturs no encontrado. Soporte desactivado.");
        }

        economyManager = new EconomyManager();

        // PlayerPoints
        if (Bukkit.getPluginManager().isPluginEnabled("PlayerPoints")) {

            PlayerPointsAPI api = PlayerPoints.getInstance().getAPI();

            economyManager.register(
                    "points",
                    new PlayerPointsProvider(api, getConfig())
            );

            getLogger().info("PlayerPoints economy loaded.");
        }

        // Vault
        economyManager.register(
                "vault",
                new VaultEconomyProvider(QSMarketPlus.economy, getConfig())
        );

        // XP
        economyManager.register(
                "xp",
                new XPEconomyProvider(getConfig())
        );

        // Levels
        economyManager.register(
                "levels",
                new LevelEconomyProvider(getConfig())
        );


        // Cargar config
        saveDefaultConfig();

        // ============================
        //  1) Inicializar SQL
        // ============================
        sqlManager = new SQLManager(this);
        sqlManager.init();

        // ============================
        // 2) Managers base
        // ============================
        shopManager = new PlayerShopManager(this);
        hookManager = new HookManager();
        categoryManager = new CategoryManager(this);
        itemManager = new ItemManager(this);

        amountMenuConfig = new AmountMenuConfig(this);
        confirmMenuConfig = new ConfirmMenuConfig(this);
        actionMenuConfig = new ActionMenuConfig(this);
        uniquePurchaseManager = new UniquePurchaseManager(this);
        categoryManager.loadCategories();
        // ==========================
        // Cargar items de cada categoría
        // ==========================
        for (ShopCategory category : categoryManager.getCategories().values()) {
            itemManager.loadCategoryItems(category);
        }



        // ============================
        // 3) Adventure + mensajes
        // ============================
        adventure = BukkitAudiences.create(this);
        MessageUtil.init(this);
        Lang.init(this);
        AHConfig.load(this);

        // ============================
        // 5) AuctionManager
        // ============================
        auctionManager = new AuctionManager(this);

        // ============================
        // 5.1) TradeManager
        // ============================
        tradeManager = new TradeManager(this);

        // ============================
        // 6) Inicializar SignShopManager
        // ============================
        signShopManager = new SignShopManager();
        ShopSessionManager sessionManager = new ShopSessionManager();
        // ============================
        // 7) Registrar eventos
        // ============================
        getServer().getPluginManager().registerEvents(new ShopClickListener(categoryManager, itemManager), this);
        getServer().getPluginManager().registerEvents(new ItemsMenuListener(itemManager), this);
        getServer().getPluginManager().registerEvents(new PlayerShopListener(), this);
        getServer().getPluginManager().registerEvents(new SignShopListener(), this);
        getServer().getPluginManager().registerEvents(new SellStickListener(this), this);
        getServer().getPluginManager().registerEvents(new TradeListener(), this);
        // ============================
        // 8) Registrar comandos
        // ============================
        if (getCommand("shop") != null)
            getCommand("shop").setExecutor(new ShopCommand(this, categoryManager));

        getCommand("qsmarket").setExecutor(new QSMarketCommand(this));
        getCommand("qsmarket").setTabCompleter(new QSMarketTabCompleter(this));
        // Registrar el ejecutor del comando
        this.getCommand("myshop").setExecutor(new PlayerShopCommand(this));

        // ✨ REGISTRO DEL TAB COMPLETER
        this.getCommand("myshop").setTabCompleter(new PlayerShopTabCompleter());
        getCommand("sell").setExecutor(new SellCommand(this, categoryManager, itemManager));
        getCommand("sell").setTabCompleter(new SellTabCompleter());
        getCommand("ah").setExecutor(new AHCommand(this));
        getCommand("ah").setTabCompleter(new AHTab());

        // ============================
        // Tradeo entre jugadores
        // ============================
        if (getCommand("trade") != null) {
            getCommand("trade").setExecutor(new TradeCommand(this));
            getCommand("trade").setTabCompleter(new TradeTabCompleter());
        }

        // ============================
        // 9) Cargar SignShops (después de que los mundos estén listos)
        // ============================

        Bukkit.getScheduler().runTaskLater(this, () -> {
            signShopManager.loadAllShops();
            shopManager.loadAllShops();
            getLogger().info("Todos los SignShops y PlayerShops han sido cargados.");
        }, 20L);


        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (PlayerShop shop : shopManager.getAllShops()) {
                shop.updateHologramVisibility();
            }
        }, 0L, 20L); // cada 20 ticks = 1 segundo


        getLogger().info("QSMarketPlus activado correctamente ✔");
    }

    @Override
    public void onDisable() {

        if (adventure != null)
            adventure.close();

        if (shopManager != null)
            shopManager.removeAllHolograms();

        // Cancelar tradeos activos para no perder ítems
        if (tradeManager != null)
            tradeManager.cancelAll();

        if (sqlManager != null)
            sqlManager.shutdown();

        getLogger().info("QSMarketPlus ha sido desactivado.");
    }

    public Economy getEconomy() {
        return economy;
    }

    public SignShopManager getSignShopManager() { return signShopManager; }

    // =====================================================
    //  CONFIGURACIÓN DE ECONOMÍA (Vault)
    // =====================================================
    private boolean setupEconomy() {

        if (getServer().getPluginManager().getPlugin("Vault") == null)
            return false;

        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null) return false;

        economy = rsp.getProvider();
        return (economy != null);
    }
    public PlayerShopManager getShopManager() {
        return shopManager;
    }
    public boolean hasQSTexturs() {
        return qstextursHook;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    // =====================================================
    //  GETTERS
    // =====================================================
    public BukkitAudiences adventure() { return adventure; }
    public CategoryManager getCategoryManager() { return categoryManager; }
    public ItemManager getItemManager() { return itemManager; }
    public AuctionManager getAuctionManager() { return auctionManager; }
    public SQLManager getSqlManager() { return sqlManager; }
    public TradeManager getTradeManager() { return tradeManager; }
    public HookManager getHookManager() {
        return hookManager;
    }
    public UniquePurchaseManager getUniquePurchaseManager() {
        return uniquePurchaseManager;
    }

    public AmountMenuConfig getAmountMenuConfig() {
        return amountMenuConfig;
    }

    public ConfirmMenuConfig getConfirmMenuConfig() {
        return confirmMenuConfig;
    }

    public ActionMenuConfig getActionMenuConfig() {
        return actionMenuConfig;
    }
}
