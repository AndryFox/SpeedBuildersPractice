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
    private Hologram arenaHolo;

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
                        TreeMap<Integer, List<String>> grouped = new TreeMap<>(Collections.reverseOrder());
                        for (Map.Entry<String, Integer> entry : top.entrySet()) {
                            grouped.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
                        }

                        int position = 1;
                        for (Map.Entry<Integer, List<String>> entry : grouped.entrySet()) {
                            if (position > 10) break;

                            int wrCount = entry.getKey();
                            List<String> players = entry.getValue();

                            List<String> coloredNames = new ArrayList<>();
                            for (String pName : players) {
                                coloredNames.add(getPlayerRoleColor(pName, wrCount) + pName);
                            }

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

        if (plugin.getConfig().contains("records")) {
            for (String uuidStr : plugin.getConfig().getConfigurationSection("records").getKeys(false)) {
                org.bukkit.configuration.ConfigurationSection userSec = plugin.getConfig().getConfigurationSection("records." + uuidStr);
                if (userSec == null) continue;

                for (String key : userSec.getKeys(false)) {
                    if (key.equals(recordKey) || key.startsWith(recordKey + "_")) {
                        if (key.endsWith("_tags")) continue;

                        long rawTime = userSec.getLong(key);
                        String mode = "normal";

                        if (key.startsWith(recordKey + "_")) {
                            mode = key.substring(recordKey.length() + 1);
                        } else {
                            String oldTags = userSec.getString(recordKey + "_tags", "");
                            if (oldTags.contains("fly") && oldTags.contains("zen")) mode = "fly_zen";
                            else if (oldTags.contains("fly")) mode = "fly";
                            else if (oldTags.contains("zen")) mode = "zen";
                        }

                        long sortingTime = rawTime;
                        if ((mode.contains("fly") || mode.contains("fly_zen")) && !category.equalsIgnoreCase("Hypixel")) {
                            sortingTime += 3000;
                        }

                        String playerName = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr)).getName();
                        if (playerName == null) playerName = "Sconosciuto";

                        String displayTags = "";
                        if (mode.equals("fly")) displayTags = "fly";
                        else if (mode.equals("zen")) displayTags = "zen";
                        else if (mode.equals("fly_zen")) displayTags = "zen fly";

                        String entryName = playerName;
                        if (!displayTags.isEmpty()) entryName += " §8[§7" + displayTags + "§8]";

                        times.put(entryName + ";;" + rawTime, sortingTime);
                    }
                }
            }
        }

        if (times.isEmpty()) {
            arenaHolo.appendTextLine("§cNessun record stabilito.");
        } else {
            List<Map.Entry<String, Long>> sortedTimes = new ArrayList<>(times.entrySet());
            sortedTimes.sort(Map.Entry.comparingByValue());

            int pos = 1;
            for (Map.Entry<String, Long> entry : sortedTimes) {
                if (pos > 10) break;

                String[] parts = entry.getKey().split(";;");
                String displayName = parts[0];
                double rawSeconds = Long.parseLong(parts[1]) / 1000.0;

                arenaHolo.appendTextLine("§e" + pos + ". §f" + displayName + " §8- §a" + rawSeconds + "s");
                pos++;
            }
        }
    }

    public void deleteArenaHologram() {
        if (arenaHolo != null && !arenaHolo.isDeleted()) {
            arenaHolo.delete();
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
        }.runTaskTimer(plugin, 100L, 3600L);
    }

    public void remove() {
        if (leaderboardHolo != null && !leaderboardHolo.isDeleted()) leaderboardHolo.delete();
        deleteArenaHologram();
    }
}