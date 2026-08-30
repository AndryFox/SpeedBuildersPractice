package it.andrea.speedbuilders;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private GameManager gameManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Inizializza il gestore della logica
        this.gameManager = new GameManager(this);

        // Registra i comandi
        Commands cmds = new Commands(this);
        getCommand("map").setExecutor(cmds);
        getCommand("practice").setExecutor(cmds);
        getCommand("lobby").setExecutor(cmds);

        // Registra gli eventi
        getServer().getPluginManager().registerEvents(new Listeners(this), this);
        getServer().getPluginManager().registerEvents(new BlockFixes(this), this);
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}