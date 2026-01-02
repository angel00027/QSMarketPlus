package mp.quesito.qSMarketPlus;

import mp.quesito.qSMarketPlus.auction.AuctionManager;
import mp.quesito.qSMarketPlus.commands.*;
import mp.quesito.qSMarketPlus.database.SQLManager;
import mp.quesito.qSMarketPlus.listeners.ItemsMenuListener;
import mp.quesito.qSMarketPlus.listeners.PlayerShopListener;
import mp.quesito.qSMarketPlus.listeners.ShopClickListener;
import mp.quesito.qSMarketPlus.listeners.SignShopListener;
import mp.quesito.qSMarketPlus.manager.*;
import mp.quesito.qSMarketPlus.shop.PlayerShop;
import mp.quesito.qSMarketPlus.shop.ShopCategory;
import mp.quesito.qSMarketPlus.utils.Lang;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.milkbowl.vault.economy.Economy;
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

    @Override
    public void onEnable() {

        instance = this;

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
        // 4) Vault Economy
        // ============================
        if (!setupEconomy()) {
            getLogger().severe("Vault o economía no disponible. Plugin desactivado.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // ============================
        // 5) AuctionManager
        // ============================
        auctionManager = new AuctionManager(this);

        // ============================
        // 6) Inicializar SignShopManager
        // ============================
        signShopManager = new SignShopManager();

        // ============================
        // 7) Registrar eventos
        // ============================
        getServer().getPluginManager().registerEvents(new ShopClickListener(categoryManager, itemManager), this);
        getServer().getPluginManager().registerEvents(new ItemsMenuListener(itemManager), this);
        getServer().getPluginManager().registerEvents(new PlayerShopListener(), this);
        getServer().getPluginManager().registerEvents(new SignShopListener(), this);

        // ============================
        // 8) Registrar comandos
        // ============================
        if (getCommand("shop") != null)
            getCommand("shop").setExecutor(new ShopCommand(this, categoryManager));

        getCommand("qsmarket").setExecutor(new QSMarketCommand(this));
        getCommand("qsmarket").setTabCompleter(new QSMarketTabCompleter(this));
        getCommand("sell").setExecutor(new SellCommand(this, categoryManager, itemManager));
        getCommand("sell").setTabCompleter(new SellTabCompleter());
        getCommand("ah").setExecutor(new AHCommand(this));
        getCommand("ah").setTabCompleter(new AHTab());

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

        if (sqlManager != null)
            sqlManager.shutdown();

        getLogger().info("QSMarketPlus ha sido desactivado.");
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


    // =====================================================
    //  GETTERS
    // =====================================================
    public BukkitAudiences adventure() { return adventure; }
    public CategoryManager getCategoryManager() { return categoryManager; }
    public ItemManager getItemManager() { return itemManager; }
    public AuctionManager getAuctionManager() { return auctionManager; }
    public SQLManager getSqlManager() { return sqlManager; }

    public UniquePurchaseManager getUniquePurchaseManager() {
        return uniquePurchaseManager;
    }
}
