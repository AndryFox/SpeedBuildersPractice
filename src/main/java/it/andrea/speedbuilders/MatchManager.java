package it.andrea.speedbuilders;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchManager {

    private final Main plugin;

    // Variabili di stato spostate qui dal GameManager
    private final HashMap<Player, Long> activeTimers = new HashMap<>();
    private final HashMap<Player, BukkitTask> actionBars = new HashMap<>();
    private final HashMap<Player, BukkitTask> countdownTasks = new HashMap<>();

    public MatchManager(Main plugin) {
        this.plugin = plugin;
    }

    public void cancelTasks(Player player) {
        if (activeTimers.containsKey(player)) activeTimers.remove(player);
        if (actionBars.containsKey(player)) { actionBars.get(player).cancel(); actionBars.remove(player); }
        if (countdownTasks.containsKey(player)) { countdownTasks.get(player).cancel(); countdownTasks.remove(player); }
    }

    public boolean hasActiveTimer(Player player) {
        return activeTimers.containsKey(player);
    }

    public void viewBuild(Player player) {
        GameManager gm = plugin.getGameManager();
        int buildId = gm.getCurrentBuild(player);
        if (buildId == -1) { player.sendMessage("§cDevi prima caricare una build con /map load <id>!"); return; }
        gm.forceReset(player);
        gm.loadBuild(player, buildId, gm.getCurrentCategory(player));
        gm.setState(player, "IDLE");
        player.sendMessage("§aBuild in modalità esplorazione. Clicca il cartello Replay per giocare!");
    }

    @SuppressWarnings("deprecation")
    public void giveBuildItems(Player player, int buildId) {
        player.getInventory().clear();
        GameManager gm = plugin.getGameManager();
        String cat = gm.getCurrentCategory(player);
        List<String> blocksData = gm.getBuildConfig(cat).getStringList("builds." + buildId + ".blocks");
        List<String> hotbar = gm.getBuildConfig(cat).getStringList("builds." + buildId + ".hotbar");
        HashMap<String, Integer> blockCounts = new HashMap<>();

        byte expectedSkullType = 0;
        if (hotbar != null) {
            for (String h : hotbar) {
                if (h.startsWith("SKULL_ITEM;")) {
                    expectedSkullType = Byte.parseByte(h.split(";")[1]);
                    break;
                }
            }
        }

        for (String dataString : blocksData) {
            String[] parts = dataString.split(";");
            if (parts.length == 5) {
                if (cat.equals("Mineplex") && Integer.parseInt(parts[1]) == 1) continue;

                String rawMat = parts[3];
                byte data = Byte.parseByte(parts[4]);

                if (rawMat.startsWith("MOB_")) {
                    blockCounts.put(rawMat, blockCounts.getOrDefault(rawMat, 0) + 1);
                    continue;
                }

                Material material = Material.valueOf(rawMat);
                if ((material.name().contains("DOOR") || material == Material.BED_BLOCK || material == Material.DOUBLE_PLANT) && data >= 8) continue;

                ItemStack normalized = ItemUtils.normalizeItem(material, data, cat);
                if (normalized != null) {
                    if (normalized.getType() == Material.SKULL_ITEM) normalized.setDurability(expectedSkullType);

                    String matName = normalized.getType().name();
                    short dura = normalized.getDurability();
                    if (matName.contains("STAIRS") || matName.contains("PISTON") || matName.contains("TRAPDOOR") || matName.contains("GATE") || matName.contains("TORCH") || matName.contains("LADDER")) {
                        dura = 0;
                    } else if (matName.contains("STEP") || matName.contains("SLAB")) {
                        dura = (short) (dura % 8);
                    } else if (matName.contains("LOG")) {
                        dura = (short) (dura % 4);
                    }
                    normalized.setDurability(dura);

                    String matData = normalized.getType().name() + ";" + normalized.getDurability();
                    blockCounts.put(matData, blockCounts.getOrDefault(matData, 0) + normalized.getAmount());
                }
            }
        }

        int slotIndex = 0;
        java.util.Set<String> processedHotbar = new java.util.HashSet<>();

        if (hotbar != null && !hotbar.isEmpty()) {
            for (int i = 0; i < 9 && i < hotbar.size(); i++) {
                String h = hotbar.get(i);
                if (!h.equals("AIR;0")) {
                    String[] matDataRaw = h.split(";");
                    String rawMat = matDataRaw[0];

                    if (rawMat.startsWith("MOB_")) {
                        if (processedHotbar.contains(rawMat)) continue;
                        processedHotbar.add(rawMat);
                        if (blockCounts.containsKey(rawMat)) {
                            ItemStack egg = plugin.getMobManager().getMobEgg(rawMat.substring(4));
                            int totalNeeded = blockCounts.get(rawMat);
                            egg.setAmount(Math.min(totalNeeded, 64));
                            player.getInventory().setItem(slotIndex, egg);
                            slotIndex++;
                            int leftOver = totalNeeded - egg.getAmount();
                            if (leftOver > 0) blockCounts.put(rawMat, leftOver);
                            else blockCounts.remove(rawMat);
                        }
                        continue;
                    }

                    byte rawData = Byte.parseByte(matDataRaw[1]);
                    ItemStack normalized = ItemUtils.normalizeItem(Material.valueOf(rawMat), rawData, cat);
                    if (normalized != null) {
                        if (normalized.getType() == Material.SKULL_ITEM) normalized.setDurability(expectedSkullType);
                        String matName = normalized.getType().name();
                        short dura = normalized.getDurability();
                        if (matName.contains("STAIRS") || matName.contains("PISTON") || matName.contains("TRAPDOOR") || matName.contains("GATE") || matName.contains("TORCH") || matName.contains("LADDER")) dura = 0;
                        else if (matName.contains("STEP") || matName.contains("SLAB")) dura = (short) (dura % 8);
                        else if (matName.contains("LOG")) dura = (short) (dura % 4);
                        normalized.setDurability(dura);

                        String key = normalized.getType().name() + ";" + normalized.getDurability();
                        if (processedHotbar.contains(key)) continue;
                        processedHotbar.add(key);

                        if (blockCounts.containsKey(key)) {
                            int totalNeeded = blockCounts.get(key);
                            int toPutInSlot = Math.min(totalNeeded, 64);
                            player.getInventory().setItem(slotIndex, new ItemStack(normalized.getType(), toPutInSlot, normalized.getDurability()));
                            slotIndex++;
                            int leftOver = totalNeeded - toPutInSlot;
                            if (leftOver > 0) blockCounts.put(key, leftOver);
                            else blockCounts.remove(key);
                        }
                    }
                }
            }
        }

        for (Map.Entry<String, Integer> entry : blockCounts.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("MOB_")) {
                ItemStack egg = plugin.getMobManager().getMobEgg(key.substring(4));
                egg.setAmount(entry.getValue());
                player.getInventory().addItem(egg);
            } else {
                String[] matData = key.split(";");
                player.getInventory().addItem(new ItemStack(Material.valueOf(matData[0]), entry.getValue(), Short.parseShort(matData[1])));
            }
        }
    }

    public void readyBuild(Player player) {
        GameManager gm = plugin.getGameManager();
        gm.setState(player, "SHOWING_NAME");

        if (countdownTasks.containsKey(player)) {
            countdownTasks.get(player).cancel();
        }

        int buildId = gm.getCurrentBuild(player);
        String cat = gm.getCurrentCategory(player);
        String bName = buildId != -1 ? gm.getBuildConfig(cat).getString("builds." + buildId + ".name", "Build Libera") : "Build Libera";

        BukkitTask task = new BukkitRunnable() {
            int count = 3;
            @Override
            public void run() {
                if (!player.isOnline() || !gm.getState(player).equals("SHOWING_NAME")) {
                    this.cancel();
                    return;
                }
                if (count > 0) {
                    player.sendTitle("", "§6" + bName, 5, 25, 0);
                    count--;
                } else {
                    startActualReady(player);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        countdownTasks.put(player, task);
    }

    public void startActualReady(Player player) {
        GameManager gm = plugin.getGameManager();
        String mode = gm.getTimerMode(player);
        int buildId = gm.getCurrentBuild(player);

        if (countdownTasks.containsKey(player)) {
            countdownTasks.get(player).cancel();
        }

        // Se è in modalità Classica (FIRST_BLOCK) E in Creativa, aspetta il primo blocco.
        // Altrimenti (se è Zen), va dritto al countdown ignorando la gamemode!
        if (mode.equals("FIRST_BLOCK") && player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            gm.clearPlot(player);
            if (buildId != -1) giveBuildItems(player, buildId);
            gm.setState(player, "WAITING_FIRST_BLOCK");
            player.sendTitle("", "§7(Creativa: Piazza per avviare)", 0, 40, 10);
            return;
        }

        if (mode.equals("COUNTDOWN")) {
            gm.setState(player, "COUNTDOWN");
            BukkitTask task = new BukkitRunnable() {
                int count = 3;
                float[] scale = {0.5f, 0.5f, 0.63f, 0.79f, 1.0f, 1.26f};
                @Override
                public void run() {
                    if (!player.isOnline() || !gm.getState(player).equals("COUNTDOWN")) {
                        this.cancel();
                        return;
                    }
                    if (count > 0) {
                        int pitchIndex = Math.max(0, Math.min(5, 6 - count));
                        broadcastToPlot(player, null, "", "§a" + count, 0, 25, 0, Sound.BLOCK_NOTE_PLING, scale[pitchIndex]);
                        count--;
                    } else {
                        broadcastToPlot(player, null, "", "§cTempo esaurito!", 0, 20, 10, Sound.BLOCK_WOOD_BREAK, 1f);
                        gm.clearPlot(player);
                        if (buildId != -1) giveBuildItems(player, buildId);
                        gm.setState(player, "PLAYING");
                        startTimer(player);
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L);
            countdownTasks.put(player, task);
        } else {
            gm.clearPlot(player);
            if (buildId != -1) giveBuildItems(player, buildId);
            gm.setState(player, "WAITING_FIRST_BLOCK");
            broadcastToPlot(player, null, "", "§7(Il timer parte al primo blocco)", 0, 40, 10, Sound.BLOCK_WOOD_BREAK, 1f);
        }
    }

    public void startTimer(Player player) {
        GameManager gm = plugin.getGameManager();
        activeTimers.put(player, System.currentTimeMillis());

        boolean isActuallyFlying = player.getAllowFlight() && !plugin.getConfig().getBoolean("players." + player.getUniqueId() + ".dj", false);
        if (isActuallyFlying) gm.setUsedFly(player, true);

        int buildId = gm.getCurrentBuild(player);
        String cat = gm.getCurrentCategory(player);

        boolean isZen = gm.getTimerMode(player).equals("COUNTDOWN");
        boolean flyUsed = isActuallyFlying;

        String modeKey = "normal";
        if (flyUsed && isZen) modeKey = "fly_zen";
        else if (flyUsed) modeKey = "fly";
        else if (isZen) modeKey = "zen";

        String basePath = "records." + player.getUniqueId().toString() + ".";
        String recordKey = cat + "_" + buildId + "_" + modeKey;

        if (buildId != -1 && cat.equals("FearGames")) {
            if (!plugin.getConfig().contains(basePath + recordKey) && plugin.getConfig().contains(basePath + cat + "_" + buildId)) {
                long oldRecord = plugin.getConfig().getLong(basePath + cat + "_" + buildId);
                plugin.getConfig().set(basePath + recordKey, oldRecord);
                plugin.saveConfig();
            }
        }

        final long finalBest = buildId != -1 ? plugin.getConfig().getLong(basePath + recordKey, 0) : 0;

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !activeTimers.containsKey(player)) { this.cancel(); return; }
                long elapsed = System.currentTimeMillis() - activeTimers.get(player);
                String recordText = finalBest > 0 ? String.format("§6§lRecord: §f%.3f s", finalBest / 1000.0) : "§6§lRecord: §7Nessuno";
                String timeText = String.format("§e§lTempo: §f%.3f s §8| %s", elapsed / 1000.0, recordText);
                broadcastToPlot(player, timeText, null, null, 0, 0, 0, null, 1f);
            }
        }.runTaskTimer(plugin, 0L, 1L);
        actionBars.put(player, task);
    }

    public void handlePerfect(Player player) {
        GameManager gm = plugin.getGameManager();
        if (!gm.getState(player).equals("PLAYING") || !activeTimers.containsKey(player)) return;

        gm.setState(player, "WAITING");
        player.getInventory().clear();
        if (actionBars.containsKey(player)) { actionBars.get(player).cancel(); actionBars.remove(player); }

        long elapsed = System.currentTimeMillis() - activeTimers.getOrDefault(player, System.currentTimeMillis());
        activeTimers.remove(player);

        String cat = gm.getCurrentCategory(player);
        boolean flyUsed = gm.hasUsedFly(player);
        boolean isZen = gm.getTimerMode(player).equals("COUNTDOWN");

        String modeKey = "normal";
        if (flyUsed && isZen) modeKey = "fly_zen";
        else if (flyUsed) modeKey = "fly";
        else if (isZen) modeKey = "zen";

        double seconds = elapsed / 1000.0;

        // Leggiamo subito l'ID e il nome della mappa per metterli nel sottotitolo
        int buildId = gm.getCurrentBuild(player);
        String bName = buildId != -1 ? gm.getBuildConfig(cat).getString("builds." + buildId + ".name", "Build Libera") : "Build Libera";

        // Titolo con il Tempo in verde (§a), Sottotitolo con Nome | Build in grigio (§7)
        player.sendTitle("§a" + seconds + "s", "§7" + player.getName() + " | " + bName, 10, 40, 10);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

        // Titolo e suono trasmessi a tutti i presenti!
        broadcastToPlot(player, null, "§a" + seconds + "s", "§7" + player.getName() + " | " + bName, 10, 40, 10, Sound.ENTITY_PLAYER_LEVELUP, 1f);

        // Messaggio in chat visibile SOLO per te in grigio e grigio chiaro
        player.sendMessage("§8Hai completato la build §7" + bName + " §8(§7" + buildId + "§8) in §7" + seconds + "s");

        if (buildId != -1) {
            long currentWR = -1;
            String recordKeyBase = cat + "_" + buildId;
            if (plugin.getConfig().contains("records")) {
                for (String uuidStr : plugin.getConfig().getConfigurationSection("records").getKeys(false)) {
                    org.bukkit.configuration.ConfigurationSection userSec = plugin.getConfig().getConfigurationSection("records." + uuidStr);
                    if (userSec != null) {
                        for (String k : userSec.getKeys(false)) {
                            if (k.equals(recordKeyBase) || k.startsWith(recordKeyBase + "_")) {
                                if (k.endsWith("_tags")) continue;
                                long t = userSec.getLong(k);
                                String m = k.startsWith(recordKeyBase + "_") ? k.substring(recordKeyBase.length() + 1) : "normal";
                                if (m.contains("fly") && !cat.equalsIgnoreCase("Hypixel")) t += 3000;
                                if (currentWR == -1 || t < currentWR) currentWR = t;
                            }
                        }
                    }
                }
            }

            long timeForWR = elapsed;
            if (modeKey.contains("fly") && !cat.equalsIgnoreCase("Hypixel")) timeForWR += 3000;

            String recordKey = cat + "_" + buildId + "_" + modeKey;
            String recordPath = "records." + player.getUniqueId().toString() + "." + recordKey;
            long currentRecord = plugin.getConfig().getLong(recordPath, 0);

            boolean isFirstTime = (currentRecord == 0);
            boolean isWorldRecord = (currentWR == -1 || timeForWR < currentWR);

            if (isFirstTime || elapsed < currentRecord) {
                plugin.getConfig().set(recordPath, elapsed);
                plugin.saveConfig();
            }

            if (isWorldRecord) {
                String diffText;
                if (currentWR == -1) {
                    diffText = "§8(§aPrimo record assoluto!§8)";
                } else {
                    double diff = (currentWR - timeForWR) / 1000.0;
                    diffText = String.format(java.util.Locale.US, "§8(§c%.3fs §8-> §a%.3fs §8| §e-%.3fs§8)", (currentWR / 1000.0), (timeForWR / 1000.0), diff);
                }
                TextComponent msg = new TextComponent("§8[§bPractice§8] §e" + player.getName() + " §7ha stabilito il nuovo §6§lWorld Record §7su §a" + bName + "§7! " + diffText);

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.spigot().sendMessage(msg);
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERDRAGON_GROWL, 0.5f, 1.5f);
                }
            } else if (!isFirstTime && elapsed < currentRecord) {
                player.sendMessage("§a§lNuovo record personale! §7(Precedente: " + (currentRecord / 1000.0) + "s)");
            }

            gm.updateScoreboard(player);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && gm.getState(player).equals("WAITING")) {
                        int nextBuildId = buildId;
                        if (gm.isContinuousRandom(player)) {
                            int randomId = gm.getRandomBuildId(cat);
                            if (randomId != -1) nextBuildId = randomId;
                            gm.loadBuild(player, nextBuildId, cat);
                            readyBuild(player); // Modalità Random: Mostra di nuovo il nome!
                        } else {
                            gm.loadBuild(player, nextBuildId, cat);
                            startActualReady(player); // Ripetizione: Salta il nome e va dritto al countdown!
                        }
                    }
                }
            }.runTaskLater(plugin, 50L);
        } else {
            gm.setState(player, "IDLE");
        }
    }

    @SuppressWarnings("deprecation")
    public boolean checkBuildPerfect(Player player) {
        GameManager gm = plugin.getGameManager();
        int buildId = gm.getCurrentBuild(player);
        if (buildId == -1) return false;

        String cat = gm.getCurrentCategory(player);
        List<String> blocksData = gm.getBuildConfig(cat).getStringList("builds." + buildId + ".blocks");
        World world = player.getWorld();

        int plotId = plugin.getPlotManager().getPlot(player);
        Location centerLoc = plugin.getPlotManager().getPlotCenter(world, plotId);
        int cX = centerLoc.getBlockX(), cZ = centerLoc.getBlockZ();

        int blocksInArena = 0;
        for (int x = -3; x <= 3; x++) {
            for (int y = 1; y <= 32; y++) {
                for (int z = -3; z <= 3; z++) {
                    if (world.getBlockAt(cX + x, 100 + y, cZ + z).getType() != Material.AIR) blocksInArena++;
                }
            }
        }

        for (org.bukkit.entity.Entity ent : world.getEntities()) {
            if (ent.hasMetadata("SpeedBuildersMob")) {
                if (Math.abs(ent.getLocation().getBlockX() - cX) <= 3 && Math.abs(ent.getLocation().getBlockZ() - cZ) <= 3) {
                    blocksInArena++;
                }
            }
        }

        int expectedBlocks = 0;
        for (String dataString : blocksData) {
            String[] parts = dataString.split(";");
            if (parts.length == 5) {
                if (cat.equals("Mineplex") && Integer.parseInt(parts[1]) == 1) continue;
                if (cat.equals("FearGames") && Integer.parseInt(parts[1]) == 0) continue;
                expectedBlocks++;
            }
        }

        if (blocksInArena != expectedBlocks) return false;

        for (String dataString : blocksData) {
            String[] parts = dataString.split(";");
            if (parts.length == 5) {
                try {
                    int savedY = Integer.parseInt(parts[1]);
                    if (cat.equals("Mineplex")) {
                        if (savedY == 1) continue;
                        savedY = savedY - 1;
                    }
                    if (cat.equals("FearGames") && savedY == 0) continue;

                    String savedMatStr = parts[3];

                    if (savedMatStr.startsWith("MOB_")) {
                        org.bukkit.entity.EntityType expectedType = plugin.getMobManager().getEntityType(savedMatStr.substring(4));
                        boolean found = false;
                        Location checkLoc = new Location(world, cX + Integer.parseInt(parts[0]) + 0.5, 100 + savedY + 0.5, cZ + Integer.parseInt(parts[2]) + 0.5);
                        for (org.bukkit.entity.Entity ent : world.getNearbyEntities(checkLoc, 0.5, 0.5, 0.5)) {
                            if (ent.hasMetadata("SpeedBuildersMob") && ent.getType() == expectedType) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) return false;
                        continue;
                    }

                    Block block = world.getBlockAt(cX + Integer.parseInt(parts[0]), 100 + savedY, cZ + Integer.parseInt(parts[2]));
                    Material savedMat = Material.valueOf(savedMatStr);
                    byte savedData = Byte.parseByte(parts[4]);
                    Material blockMat = block.getType();

                    if (savedMat == Material.GLOWING_REDSTONE_ORE && blockMat == Material.REDSTONE_ORE) savedMat = Material.REDSTONE_ORE;
                    if (savedMat == Material.REDSTONE_ORE && blockMat == Material.GLOWING_REDSTONE_ORE) blockMat = Material.REDSTONE_ORE;
                    if (savedMat == Material.DAYLIGHT_DETECTOR_INVERTED && blockMat == Material.DAYLIGHT_DETECTOR) savedMat = Material.DAYLIGHT_DETECTOR;
                    if (savedMat == Material.DAYLIGHT_DETECTOR && blockMat == Material.DAYLIGHT_DETECTOR_INVERTED) blockMat = Material.DAYLIGHT_DETECTOR;

                    if (blockMat != savedMat) return false;

                    boolean ignoreData = false;
                    if (savedMat.name().contains("BANNER") || savedMat.name().contains("SKULL")) ignoreData = true;
                    if (savedMat == Material.SKULL || savedMat == Material.SKULL_ITEM) ignoreData = true;
                    if (savedMat.name().contains("PLATE")) ignoreData = true;
                    if (savedMat == Material.DAYLIGHT_DETECTOR || savedMat == Material.DAYLIGHT_DETECTOR_INVERTED) ignoreData = true;
                    if (savedMat == Material.ENDER_PORTAL_FRAME) ignoreData = true;
                    if (cat.equals("FearGames") && (savedMat == Material.PUMPKIN || savedMat == Material.JACK_O_LANTERN)) ignoreData = true;

                    if (savedMat == Material.LEAVES || savedMat == Material.LEAVES_2) {
                        if ((block.getData() % 4) != (savedData % 4)) return false;
                        ignoreData = true;
                    }

                    if (!ignoreData && block.getData() != savedData) {
                        return false;
                    }
                } catch (Exception e) { return false; }
            }
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    public void showErrors(Player player) {
        GameManager gm = plugin.getGameManager();
        int buildId = gm.getCurrentBuild(player);
        if (buildId == -1) { player.sendMessage("§cNessuna build caricata!"); return; }
        if (!gm.getState(player).equals("PLAYING")) { player.sendMessage("§cDevi essere in partita per vedere gli errori!"); return; }

        String cat = gm.getCurrentCategory(player);
        List<String> blocksData = gm.getBuildConfig(cat).getStringList("builds." + buildId + ".blocks");
        World world = player.getWorld();

        int plotId = plugin.getPlotManager().getPlot(player);
        Location centerLoc = plugin.getPlotManager().getPlotCenter(world, plotId);
        int cX = centerLoc.getBlockX(), cZ = centerLoc.getBlockZ();

        HashMap<String, String> expected = new HashMap<>();
        for (String dataString : blocksData) {
            String[] parts = dataString.split(";");
            if (parts.length == 5) {
                int savedY = Integer.parseInt(parts[1]);
                if (cat.equals("Mineplex")) {
                    if (savedY == 1) continue;
                    savedY = savedY - 1;
                }
                if (cat.equals("FearGames") && savedY == 0) continue;

                String rawMat = parts[3];
                if (rawMat.startsWith("MOB_")) {
                    expected.put(parts[0] + ";" + savedY + ";" + parts[2], rawMat + ";" + parts[4]);
                    continue;
                }

                Material savedMat = Material.valueOf(rawMat);
                if (savedMat == Material.GLOWING_REDSTONE_ORE) savedMat = Material.REDSTONE_ORE;
                if (savedMat == Material.DAYLIGHT_DETECTOR_INVERTED) savedMat = Material.DAYLIGHT_DETECTOR;
                expected.put(parts[0] + ";" + savedY + ";" + parts[2], savedMat.name() + ";" + parts[4]);
            }
        }

        int errorsCount = 0;

        for (int x = -3; x <= 3; x++) {
            for (int y = 1; y <= 32; y++) {
                for (int z = -3; z <= 3; z++) {
                    String locKey = x + ";" + y + ";" + z;
                    Block b = world.getBlockAt(cX + x, 100 + y, cZ + z);
                    String exp = expected.get(locKey);

                    if (b.getType() != Material.AIR) {
                        if (exp == null) {
                            player.sendBlockChange(b.getLocation(), Material.STAINED_GLASS, (byte) 14);
                            errorsCount++;
                        } else {
                            if (exp.startsWith("MOB_")) {
                                player.sendBlockChange(b.getLocation(), Material.STAINED_GLASS, (byte) 14);
                                errorsCount++;
                                expected.remove(locKey);
                                continue;
                            }

                            String[] p = exp.split(";");
                            Material eMat = Material.valueOf(p[0]);
                            byte eData = Byte.parseByte(p[1]);
                            Material blockMat = b.getType();

                            if (blockMat == Material.GLOWING_REDSTONE_ORE) blockMat = Material.REDSTONE_ORE;
                            if (blockMat == Material.DAYLIGHT_DETECTOR_INVERTED) blockMat = Material.DAYLIGHT_DETECTOR;

                            boolean ignoreData = false;
                            if (eMat.name().contains("BANNER") || eMat.name().contains("SKULL")) ignoreData = true;
                            if (eMat.name().contains("PLATE")) ignoreData = true;
                            if (eMat == Material.DAYLIGHT_DETECTOR || eMat == Material.DAYLIGHT_DETECTOR_INVERTED) ignoreData = true;
                            if (eMat == Material.ENDER_PORTAL_FRAME) ignoreData = true;
                            if (cat.equals("FearGames") && (eMat == Material.PUMPKIN || eMat == Material.JACK_O_LANTERN)) ignoreData = true;

                            boolean error = false;
                            if (blockMat != eMat) {
                                error = true;
                            } else {
                                if (eMat == Material.LEAVES || eMat == Material.LEAVES_2) {
                                    if ((b.getData() % 4) != (eData % 4)) error = true;
                                    ignoreData = true;
                                }
                                if (!ignoreData && b.getData() != eData) {
                                    error = true;
                                }
                            }

                            if (error) {
                                player.sendBlockChange(b.getLocation(), Material.STAINED_GLASS, (byte) 14);
                                errorsCount++;
                            }
                            expected.remove(locKey);
                        }
                    }
                }
            }
        }

        for (org.bukkit.entity.Entity ent : world.getEntities()) {
            if (ent.hasMetadata("SpeedBuildersMob")) {
                if (Math.abs(ent.getLocation().getBlockX() - cX) > 3 || Math.abs(ent.getLocation().getBlockZ() - cZ) > 3) continue;
                int eX = ent.getLocation().getBlockX() - cX;
                int eY = ent.getLocation().getBlockY() - 100;
                int eZ = ent.getLocation().getBlockZ() - cZ;
                String locKey = eX + ";" + eY + ";" + eZ;
                String exp = expected.get(locKey);

                if (exp == null || !exp.startsWith("MOB_") || plugin.getMobManager().getEntityType(exp.substring(4)) != ent.getType()) {
                    player.sendBlockChange(ent.getLocation(), Material.STAINED_GLASS, (byte) 14);
                    errorsCount++;
                }
                if (exp != null && exp.startsWith("MOB_") && plugin.getMobManager().getEntityType(exp.substring(4)) == ent.getType()) {
                    expected.remove(locKey);
                }
            }
        }

        for (Map.Entry<String, String> entry : expected.entrySet()) {
            String[] loc = entry.getKey().split(";");
            String[] matData = entry.getValue().split(";");
            org.bukkit.Location bLoc = new org.bukkit.Location(world, cX + Integer.parseInt(loc[0]), 100 + Integer.parseInt(loc[1]), cZ + Integer.parseInt(loc[2]));

            if (matData[0].startsWith("MOB_")) {
                player.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_ANGRY, bLoc.add(0.5, 0.5, 0.5), 5);
                errorsCount++;
                continue;
            }

            Material eMat = Material.valueOf(matData[0]);
            byte eData = Byte.parseByte(matData[1]);
            player.sendBlockChange(bLoc, eMat, eData);
            errorsCount++;
        }

        if (errorsCount == 0) {
            player.sendMessage("§aNessun errore.");
        } else {
            player.sendMessage("§cMostrando " + errorsCount + " errori per 5 secondi!");
            player.sendMessage("§8- §cBlocchi rossi§7: Sono da rimuovere o sostituire.");
            player.sendMessage("§8- §aBlocchi perfetti apparsi§7: Li avevi dimenticati.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;
                    for (int x = -3; x <= 3; x++) {
                        for (int y = 1; y <= 32; y++) {
                            for (int z = -3; z <= 3; z++) {
                                Block b = world.getBlockAt(cX + x, 100 + y, cZ + z);
                                player.sendBlockChange(b.getLocation(), b.getType(), b.getData());
                            }
                        }
                    }
                }
            }.runTaskLater(plugin, 100L);
        }
    }

    // NUOVO: Trasmette titoli, actionbar e suoni a tutti i giocatori nel plot
    public void broadcastToPlot(Player host, String actionbar, String title, String sub, int in, int stay, int out, Sound sound, float pitch) {
        int targetPlot = plugin.getPlotManager().getPlot(host);
        for (Player p : host.getWorld().getPlayers()) {
            if (plugin.getPlotManager().getPlot(p) == targetPlot) {
                if (actionbar != null) p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(actionbar));
                if (title != null || sub != null) p.sendTitle(title != null ? title : "", sub != null ? sub : "", in, stay, out);
                if (sound != null) p.playSound(p.getLocation(), sound, 1f, pitch);
            }
        }
    }

}