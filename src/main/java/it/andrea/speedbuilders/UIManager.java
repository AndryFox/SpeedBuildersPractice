package it.andrea.speedbuilders;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

public class UIManager {

    private final Main plugin;

    public UIManager(Main plugin) {
        this.plugin = plugin;
    }

    public void updateScoreboard(Player player) {
        GameManager gm = plugin.getGameManager();

        if (!player.getWorld().getName().equals("practice")) {
            updateLobbyScoreboard(player);
            return;
        }

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();
        Objective obj = board.registerNewObjective("speedbuilders", "dummy");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        obj.setDisplayName("§aIn Gioco");

        int buildId = gm.getCurrentBuild(player);
        String cat = gm.getCurrentCategory(player);
        String bName = buildId != -1 ? gm.getBuildConfig(cat).getString("builds." + buildId + ".name", "Nessuna") : "Nessuna";
        String timerMode = gm.getTimerMode(player).equals("COUNTDOWN") ? "Zen" : "Classica";
        String state = gm.getState(player);

        String stateFormat = "In Attesa";
        if (state.equals("PLAYING")) stateFormat = "In Gioco";
        else if (state.equals("COUNTDOWN")) stateFormat = "Osservazione";
        else if (state.equals("SHOWING_NAME")) stateFormat = "Memorizzazione";
        else if (state.equals("WAITING_FIRST_BLOCK")) stateFormat = "Attesa blocco";

        String prStr = "Nessuno";
        String wrStr = "Nessuno";

        if (buildId != -1) {
            String recordKey = cat + "_" + buildId;
            long pr = -1;
            long wr = -1;

            if (plugin.getConfig().contains("records")) {
                for (String uuidStr : plugin.getConfig().getConfigurationSection("records").getKeys(false)) {
                    ConfigurationSection userSec = plugin.getConfig().getConfigurationSection("records." + uuidStr);
                    if (userSec == null) continue;

                    for (String key : userSec.getKeys(false)) {
                        if (key.equals(recordKey) || key.startsWith(recordKey + "_")) {
                            if (key.endsWith("_tags")) continue;
                            long time = userSec.getLong(key);

                            long timeForWr = time;
                            String mode = key.startsWith(recordKey + "_") ? key.substring(recordKey.length() + 1) : "normal";
                            if (mode.contains("fly") && !cat.equalsIgnoreCase("Hypixel")) timeForWr += 3000;

                            if (uuidStr.equals(player.getUniqueId().toString())) {
                                if (pr == -1 || time < pr) pr = time;
                            }
                            if (wr == -1 || timeForWr < wr) wr = timeForWr;
                        }
                    }
                }
            }
            if (pr != -1) prStr = (pr / 1000.0) + "s";
            if (wr != -1) wrStr = (wr / 1000.0) + "s";
        }

        // --- NUOVO ORDINE DELLA SCOREBOARD ---

        // 1. Stato e Modalità in cima
        obj.getScore("§1").setScore(12);
        obj.getScore("§6Stato: §6" + stateFormat).setScore(11);
        obj.getScore("§6Modalità: §d" + timerMode).setScore(10);

        obj.getScore("§2").setScore(9);

        // Legge la modalità attuale del giocatore
        String currentGamemode = player.getGameMode() == org.bukkit.GameMode.CREATIVE ? "§bCreativa" : "§aSopravvivenza";

        // 2. Gamemode, Build, Server e Record Personale (attaccato)
        obj.getScore("§6Gamemode: " + currentGamemode).setScore(8);
        obj.getScore("§6Build: §a" + bName).setScore(7);
        obj.getScore("§6Server: §e" + cat).setScore(6);
        obj.getScore("§6Record Personale: §a" + prStr).setScore(5);

        obj.getScore("§3").setScore(4);

        // 3. World Record in fondo
        obj.getScore("§6Record WR: §a" + wrStr).setScore(3);

        obj.getScore("§4").setScore(2);
        obj.getScore("§6sbpractice.falix.gg").setScore(1);

        player.setScoreboard(board);
    }

    public void updateLobbyScoreboard(Player player) {
        // Se per sbaglio viene chiamato in arena, ignora
        if (player.getWorld().getName().equals("practice")) return;

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();
        Objective obj = board.registerNewObjective("lobby_board", "dummy");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        obj.setDisplayName("§e§lPRACTICE");

        // Conta i giocatori online
        int online = Bukkit.getOnlinePlayers().size();

        // Recupera i WR per mostrare le statistiche (se la query è asincrona, andrebbe gestita,
        // ma per ora la prendiamo diretta come fai in Listeners)
        int wrCount = plugin.getDatabase().getPlayerWRCount(player.getName());

        // Calcola il rango al volo (potresti estrarre questa logica in un metodo a parte per non ripeterla)
        String rankColor = "§e";
        String rankName = "Newbie";
        String tag = "Newbie";

        if (wrCount >= 100) { rankColor = "§6"; rankName = "Greatest of All Time"; tag = "G.O.A.T."; }
        else if (wrCount >= 90) { rankColor = "§e"; rankName = "Legend"; tag = "Legend"; }
        else if (wrCount >= 80) { rankColor = "§6"; rankName = "Grandmaster"; tag = "G-Master"; }
        else if (wrCount >= 70) { rankColor = "§c"; rankName = "Master"; tag = "Master"; }
        else if (wrCount >= 60) { rankColor = "§4"; rankName = "Expert"; tag = "Expert"; }
        else if (wrCount >= 50) { rankColor = "§c"; rankName = "Imperial"; tag = "Imperial"; }
        else if (wrCount >= 45) { rankColor = "§d"; rankName = "Professional"; tag = "Pro"; }
        else if (wrCount >= 40) { rankColor = "§5"; rankName = "Talented"; tag = "Talented"; }
        else if (wrCount >= 35) { rankColor = "§9"; rankName = "Skilled"; tag = "Skilled"; }
        else if (wrCount >= 30) { rankColor = "§1"; rankName = "Seasoned"; tag = "Seasoned"; }
        else if (wrCount >= 25) { rankColor = "§3"; rankName = "Experienced"; tag = "Experienced"; }
        else if (wrCount >= 20) { rankColor = "§2"; rankName = "Trained"; tag = "Trained"; }
        else if (wrCount >= 15) { rankColor = "§a"; rankName = "Apprentice"; tag = "Apprentice"; }
        else if (wrCount >= 10) { rankColor = "§1"; rankName = "Amateur"; tag = "Amateur"; }
        else if (wrCount >= 6) { rankColor = "§8"; rankName = "Rookie"; tag = "Rookie"; }
        else if (wrCount >= 3) { rankColor = "§7"; rankName = "Novice"; tag = "Novice"; }
        else if (wrCount >= 1) { rankColor = "§f"; rankName = "Prospect"; tag = "Prospect"; }

        if (player.getName().equalsIgnoreCase("AndryFox_14")) {
            rankColor = "§b"; rankName = "Elite Fox";
        }

        obj.getScore("§1").setScore(10);
        obj.getScore("§fGiocatore: §a" + player.getName()).setScore(9);
        obj.getScore("§fRango: " + rankColor + rankName).setScore(8);
        obj.getScore("§fWR Totali: §e" + wrCount).setScore(7);

        obj.getScore("§2").setScore(6);
        obj.getScore("§fOnline: §a" + online).setScore(5);

        obj.getScore("§3").setScore(4);
        obj.getScore("§7Usa §b/p §7per giocare").setScore(3);

        obj.getScore("§4").setScore(2);
        obj.getScore("§6sbpractice.falix.gg").setScore(1);

        player.setScoreboard(board);
    }

}