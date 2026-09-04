package it.andrea.speedbuilders;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class HologramManager {
    private final Main plugin;
    private Hologram leaderboardHolo;
    private Hologram arenaHolo; // Aggiunto per la singola build

    public HologramManager(Main plugin) {
        this.plugin = plugin;
        startTask();
    }

    public void spawnOrUpdate() {
        if (!plugin.getConfig().contains("locations.hologram")) return;

        Location loc = (Location) plugin.getConfig().get("locations.hologram");

        new BukkitRunnable() {
            @Override
            public void run() {
                // Recupera dal DB (magari aumentalo a 20 o 30 nel DB se vuoi che raggruppi più persone nella top 10 effettiva)
                LinkedHashMap<String, Integer> top = plugin.getDatabase().getTopWRHolders(20);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (leaderboardHolo != null && !leaderboardHolo.isDeleted()) {
                        leaderboardHolo.delete();
                    }

                    leaderboardHolo = HologramsAPI.createHologram(plugin, loc);
                    leaderboardHolo.appendTextLine("§6§lFEAR GAMES WORLD RECORDS");
                    leaderboardHolo.appendTextLine("§eClassifica Globale WR");
                    leaderboardHolo.appendTextLine("");

                    if (top.isEmpty()) {
                        leaderboardHolo.appendTextLine("§cNessun record trovato.");
                    } else {
                        // Raggruppa i giocatori con lo stesso numero di WR (Ordine decrescente)
                        TreeMap<Integer, List<String>> grouped = new TreeMap<>(Collections.reverseOrder());
                        for (Map.Entry<String, Integer> entry : top.entrySet()) {
                            grouped.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
                        }

                        int position = 1;
                        for (Map.Entry<Integer, List<String>> entry : grouped.entrySet()) {
                            if (position > 10) break; // Mostra solo fino alla decima posizione

                            int wrCount = entry.getKey();
                            List<String> players = entry.getValue();

                            // Applica il colore del ruolo a ciascun nome
                            List<String> coloredNames = new ArrayList<>();
                            for (String pName : players) {
                                coloredNames.add(getPlayerRoleColor(pName, wrCount) + pName);
                            }

                            // Unisce i nomi con la barra spaziatrice
                            String namesJoined = String.join(" §8/ ", coloredNames);

                            String medal;
                            if (position == 1) medal = "§e§l1°";
                            else if (position == 2) medal = "§f§l2°";
                            else if (position == 3) medal = "§6§l3°";
                            else medal = "§7" + position + "°";

                            String line = medal + " §8| " + namesJoined + " §8- §a" + wrCount + " WR";
                            leaderboardHolo.appendTextLine(line);
                            position++;
                        }
                    }

                    leaderboardHolo.appendTextLine("");
                    leaderboardHolo.appendTextLine("§8Aggiornamento in tempo reale...");
                });
            }
        }.runTaskAsynchronously(plugin);
    }

    // Metodo per aggiornare la Top 10 specifica dell'arena (Tempi)
    public void updateArenaHologram(Location loc, int buildId, String category) {
        if (arenaHolo != null && !arenaHolo.isDeleted()) {
            arenaHolo.delete();
        }

        arenaHolo = HologramsAPI.createHologram(plugin, loc);
        String buildName = plugin.getGameManager().getBuildConfig(category).getString("builds." + buildId + ".name", "Sconosciuta");

        arenaHolo.appendTextLine("§b§l" + buildName + " §7[" + buildId + "]");
        arenaHolo.appendTextLine("§eTop 10 Tempi");
        arenaHolo.appendTextLine("");

        String recordKey = category + "_" + buildId;
        Map<String, Long> times = new LinkedHashMap<>();

        // Cerca i tempi di tutti i giocatori nel config.yml per questa specifica build
        if (plugin.getConfig().contains("records")) {
            for (String uuidStr : plugin.getConfig().getConfigurationSection("records").getKeys(false)) {
                if (plugin.getConfig().contains("records." + uuidStr + "." + recordKey)) {
                    long time = plugin.getConfig().getLong("records." + uuidStr + "." + recordKey);

                    String playerName = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr)).getName();
                    if (playerName == null) playerName = "Sconosciuto";

                    // Legge i tag dal config
                    String tags = plugin.getConfig().getString("records." + uuidStr + "." + recordKey + "_tags", "");

                    // Aggiunge il tag visivo accanto al nome (es: AndryFox_14 fly zen)
                    if (!tags.isEmpty()) {
                        playerName += " §8[§7" + tags + "§8]";
                    }

                    times.put(playerName, time);
                }
            }
        }

        if (times.isEmpty()) {
            arenaHolo.appendTextLine("§cNessun record stabilito.");
        } else {
            // Ordina dal tempo più basso (più veloce) al più alto
            List<Map.Entry<String, Long>> sortedTimes = new ArrayList<>(times.entrySet());
            sortedTimes.sort(Map.Entry.comparingByValue());

            int pos = 1;
            for (Map.Entry<String, Long> entry : sortedTimes) {
                if (pos > 10) break;
                double seconds = entry.getValue() / 1000.0;
                arenaHolo.appendTextLine("§e" + pos + ". §f" + entry.getKey() + " §8- §a" + seconds + "s");
                pos++;
            }
        }
    }

    private String getPlayerRoleColor(String playerName, int wrCount) {
        if (playerName.equalsIgnoreCase("AndryFox_14")) return "§b";

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
        return "§e";
    }

    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                spawnOrUpdate();
            }
        }.runTaskTimer(plugin, 100L, 3600L); // Ogni 3 minuti
    }

    public void remove() {
        if (leaderboardHolo != null && !leaderboardHolo.isDeleted()) leaderboardHolo.delete();
        if (arenaHolo != null && !arenaHolo.isDeleted()) arenaHolo.delete();
    }
}