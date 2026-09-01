package it.andrea.speedbuilders;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.LinkedHashMap;
import java.util.Map;

public class HologramManager {
    private final Main plugin;
    private Hologram leaderboardHolo;

    public HologramManager(Main plugin) {
        this.plugin = plugin;
        startTask();
    }

    public void spawnOrUpdate() {
        if (!plugin.getConfig().contains("locations.hologram")) return;

        Location loc = (Location) plugin.getConfig().get("locations.hologram");

        // Esegue la query al database in modo asincrono (Zero Lag)
        new BukkitRunnable() {
            @Override
            public void run() {
                LinkedHashMap<String, Integer> top = plugin.getDatabase().getTopWRHolders(10);

                // Aggiorna l'ologramma nel thread principale
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (leaderboardHolo != null && !leaderboardHolo.isDeleted()) {
                        leaderboardHolo.delete();
                    }

                    leaderboardHolo = HologramsAPI.createHologram(plugin, loc);
                    leaderboardHolo.appendTextLine("§6§lFEAR GAMES WORLD RECORDS");
                    leaderboardHolo.appendTextLine("§eI 10 giocatori con più WR");
                    leaderboardHolo.appendTextLine("");

                    int pos = 1;
                    for (Map.Entry<String, Integer> entry : top.entrySet()) {
                        String color = (pos == 1) ? "§a" : (pos <= 3) ? "§b" : "§7";
                        leaderboardHolo.appendTextLine(color + "#" + pos + " §f" + entry.getKey() + " §8- §e" + entry.getValue() + " WR");
                        pos++;
                    }

                    if (top.isEmpty()) {
                        leaderboardHolo.appendTextLine("§cNessun record trovato.");
                    }

                    leaderboardHolo.appendTextLine("");
                    leaderboardHolo.appendTextLine("§8Aggiornamento in tempo reale...");
                });
            }
        }.runTaskAsynchronously(plugin);
    }

    private void startTask() {
        // Ripete l'aggiornamento ogni 3 minuti (3600 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                spawnOrUpdate();
            }
        }.runTaskTimer(plugin, 100L, 3600L);
    }

    public void remove() {
        if (leaderboardHolo != null && !leaderboardHolo.isDeleted()) {
            leaderboardHolo.delete();
        }
    }
}