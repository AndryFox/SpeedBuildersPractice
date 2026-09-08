package it.andrea.speedbuilders;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
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
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
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

        obj.getScore("§1").setScore(11);
        obj.getScore("§6Mappa: §a" + bName).setScore(10);
        obj.getScore("§6Server: §e" + cat).setScore(9);

        obj.getScore("§2").setScore(8);
        obj.getScore("§6Stato: §6" + stateFormat).setScore(7);
        obj.getScore("§6Modalità: §d" + timerMode).setScore(6);

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

        obj.getScore("§3").setScore(5);
        obj.getScore("§6Record Tuo: §a" + prStr).setScore(4);
        obj.getScore("§6Record WR: §a" + wrStr).setScore(3);

        obj.getScore("§4").setScore(2);
        obj.getScore("§6sbpractice.falix.gg").setScore(1);

        player.setScoreboard(board);
    }
}