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
        if (!plugin.getBuildsConfig().contains("locations.hologram")) return;

        Location loc = (Location) plugin.getBuildsConfig().get("locations.hologram");

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

                    if (top.isEmpty()) {
                        leaderboardHolo.appendTextLine("§cNessun record trovato.");
                    } else {
                        // Ciclo per la generazione della Top 10
                        int position = 1;
                        for (Map.Entry<String, Integer> entry : top.entrySet()) { // Corretto: top.entrySet()
                            String playerName = entry.getKey();
                            int wrCount = entry.getValue();

                            // Calcola la medaglia (Posizione)
                            String medal;
                            if (position == 1) {
                                medal = "§e§l1°"; // Oro
                            } else if (position == 2) {
                                medal = "§f§l2°"; // Argento
                            } else if (position == 3) {
                                medal = "§6§l3°"; // Bronzo
                            } else {
                                medal = "§7" + position + "°"; // Dal 4° in poi, grigio normale
                            }

                            // Calcola il colore del nome in base al Ruolo
                            String roleColor = getPlayerRoleColor(playerName, wrCount);

                            // Costruisci la riga finale
                            String line = medal + " §8| " + roleColor + playerName + " §8- §a" + wrCount + " WR";

                            // Aggiungi la riga all'ologramma
                            leaderboardHolo.appendTextLine(line); // Corretto: leaderboardHolo.appendTextLine
                            position++;
                        }
                    }

                    leaderboardHolo.appendTextLine("");
                    leaderboardHolo.appendTextLine("§8Aggiornamento in tempo reale...");
                });
            }
        }.runTaskAsynchronously(plugin);
    }

    private String getPlayerRoleColor(String playerName, int wrCount) {
        if (playerName.equalsIgnoreCase("AndryFox_14")) return "§b"; // Il tuo ruolo Elite Fox

        if (wrCount >= 100) return "§6";
        if (wrCount >= 90) return "§e";
        if (wrCount >= 80) return "§6";
        if (wrCount >= 70) return "§c";
        if (wrCount >= 60) return "§4";
        if (wrCount >= 50) return "§c";
        if (wrCount >= 45) return "§d";
        if (wrCount >= 40) return "§5";
        if (wrCount >= 35) return "§9";
        if (wrCount >= 30) return "§1";
        if (wrCount >= 25) return "§3";
        if (wrCount >= 20) return "§2";
        if (wrCount >= 15) return "§a";
        if (wrCount >= 10) return "§1";
        if (wrCount >= 6) return "§8";
        if (wrCount >= 3) return "§7";
        if (wrCount >= 1) return "§f";
        return "§e"; // Newbie
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