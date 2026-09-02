package it.andrea.speedbuilders;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Main extends JavaPlugin {

    private GameManager gameManager;
    private Database database;
    private HologramManager hologramManager;

    // Dichiarazione dei file custom
    private FileConfiguration fearConfig;
    private FileConfiguration mineplexConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Caricamento sicuro dei file custom
        try {
            File fearFile = new File(getDataFolder(), "feargames_builds.yml");
            if (!fearFile.exists()) {
                fearFile.getParentFile().mkdirs();
                fearFile.createNewFile();
            }
            fearConfig = YamlConfiguration.loadConfiguration(fearFile);

            File mineplexFile = new File(getDataFolder(), "mineplex_builds.yml");
            if (!mineplexFile.exists()) {
                mineplexFile.getParentFile().mkdirs();
                mineplexFile.createNewFile();
            }
            mineplexConfig = YamlConfiguration.loadConfiguration(mineplexFile);
        } catch (Exception e) {
            getLogger().severe("Errore durante la creazione dei file delle build!");
            e.printStackTrace();
        }

        // Setup configurazione Database
        if (!getConfig().contains("database.host")) {
            getConfig().set("database.host", "aws-0-eu-central-1.pooler.supabase.com");
            getConfig().set("database.port", 6543);
            getConfig().set("database.name", "postgres");
            getConfig().set("database.user", "postgres.tuouser");
            getConfig().set("database.password", "tuapassword");
            saveConfig();
        }

        // Connessione a Supabase
        this.database = new Database(
                getConfig().getString("database.host"),
                getConfig().getInt("database.port"),
                getConfig().getString("database.name"),
                getConfig().getString("database.user"),
                getConfig().getString("database.password")
        );

        if (database.connect()) {
            getLogger().info("Connesso con successo al database Supabase!");
        }

        if (!getServer().getPluginManager().isPluginEnabled("HolographicDisplays")) {
            getLogger().severe("*** HolographicDisplays non trovato! Installa il plugin. ***");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.hologramManager = new HologramManager(this);

        // Inizializza il gestore della logica
        this.gameManager = new GameManager(this);

        // Registra i comandi
        Commands cmds = new Commands(this);
        getCommand("map").setExecutor(cmds);
        getCommand("practice").setExecutor(cmds);
        getCommand("p").setExecutor(cmds);
        getCommand("lobby").setExecutor(cmds);
        getCommand("fly").setExecutor(cmds);
        getCommand("dj").setExecutor(cmds);

        // Comando per salvare le build di Mineplex
        getCommand("savemineplex").setExecutor(new SaveMineplexCommand(this));
        getCommand("scanall").setExecutor(new ScanAllMineplexCommand(this));

        // Registra gli eventi
        getServer().getPluginManager().registerEvents(new Listeners(this), this);
        getServer().getPluginManager().registerEvents(new BlockFixes(this), this);
    }

    @Override
    public void onDisable() {
        // Disconnette il database per non lasciare connessioni appese
        if (database != null) {
            database.disconnect();
        }

        // Rimuove l'ologramma per evitare cloni fantasma al reload
        if (hologramManager != null) {
            hologramManager.remove();
        }

        getLogger().info("SpeedBuilders disattivato correttamente!");
    }

    // Metodi Getter per accedere ovunque a queste classi e configurazioni
    public GameManager getGameManager() { return gameManager; }
    public Database getDatabase() { return database; }
    public HologramManager getHologramManager() { return hologramManager; }

    public FileConfiguration getFearConfig() { return fearConfig; }
    public FileConfiguration getMineplexConfig() { return mineplexConfig; }
}