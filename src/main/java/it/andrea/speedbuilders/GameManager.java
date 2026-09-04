package it.andrea.speedbuilders;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameManager {

    private final Main plugin;

    private final HashMap<Player, Integer> pendingDeletes = new HashMap<>();
    private final HashMap<Player, Long> activeTimers = new HashMap<>();
    private final HashMap<Player, BukkitTask> actionBars = new HashMap<>();
    private final HashMap<Player, BukkitTask> countdownTasks = new HashMap<>();
    private final HashMap<Player, String> playerStates = new HashMap<>();
    private final HashMap<Player, Integer> currentBuild = new HashMap<>();

    private final HashMap<Player, Boolean> awaitingSearch = new HashMap<>();
    private final HashMap<Player, String> activeSearch = new HashMap<>();
    private final HashMap<Player, String> currentCategory = new HashMap<>();
    private final HashMap<Player, Boolean> continuousRandom = new HashMap<>();
    private final HashMap<Player, Boolean> awaitingCategory = new HashMap<>();
    private final HashMap<String, FileConfiguration> dynamicConfigs = new HashMap<>();

    public void setAwaitingCategory(Player p, boolean val) { if (val) awaitingCategory.put(p, true); else awaitingCategory.remove(p); }
    public boolean isAwaitingCategory(Player p) { return awaitingCategory.containsKey(p); }
    public void setAwaitingSearch(Player p, boolean val) { if (val) awaitingSearch.put(p, true); else awaitingSearch.remove(p); }
    public boolean isAwaitingSearch(Player p) { return awaitingSearch.containsKey(p); }
    public void setActiveSearch(Player p, String search) { activeSearch.put(p, search); }
    public void clearSearch(Player p) { activeSearch.remove(p); }
    public String getCurrentCategory(Player player) { return currentCategory.getOrDefault(player, "FearGames"); }
    public void setContinuousRandom(Player p, boolean val) { if (val) continuousRandom.put(p, true); else continuousRandom.remove(p); }
    public boolean isContinuousRandom(Player p) { return continuousRandom.getOrDefault(p, false); }
    public boolean hasActiveSearch(Player p) { return activeSearch.containsKey(p); }
    // Configurazione fantasma per i test, non viene mai scritta su file
    private final org.bukkit.configuration.file.FileConfiguration memoryConfig = new org.bukkit.configuration.file.YamlConfiguration();

    public GameManager(Main plugin) {
        this.plugin = plugin;
    }

    public String getState(Player player) { return playerStates.getOrDefault(player, "IDLE"); }
    public void setState(Player player, String state) { playerStates.put(player, state); }
    public int getCurrentBuild(Player player) { return currentBuild.getOrDefault(player, -1); }
    public boolean hasPendingDelete(Player player) { return pendingDeletes.containsKey(player); }
    public int getPendingDelete(Player player) { return pendingDeletes.get(player); }
    public void removePendingDelete(Player player) { pendingDeletes.remove(player); }
    public void setPendingDelete(Player player, int id) { pendingDeletes.put(player, id); }

    public boolean isLobbyWorld(World world) {
        if (plugin.getConfig().contains("locations.lobby")) {
            Location loc = (Location) plugin.getConfig().get("locations.lobby");
            return world.equals(loc.getWorld());
        }
        return false;
    }

    public void forceReset(Player player) {
        if (activeTimers.containsKey(player)) activeTimers.remove(player);
        if (actionBars.containsKey(player)) { actionBars.get(player).cancel(); actionBars.remove(player); }
        if (countdownTasks.containsKey(player)) { countdownTasks.get(player).cancel(); countdownTasks.remove(player); }
        player.getInventory().clear();
        clearPlot(player.getWorld());
    }

    public void resetPlayer(Player player) {
        forceReset(player);
        playerStates.put(player, "IDLE");
        continuousRandom.remove(player); // <- Aggiunto questo
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(""));
    }

    public int getRandomBuildId(String category) {
        FileConfiguration config = getBuildConfig(category);
        if (config.contains("builds")) {
            List<String> keys = new java.util.ArrayList<>(config.getConfigurationSection("builds").getKeys(false));
            if (!keys.isEmpty()) {
                String randomKey = keys.get(new java.util.Random().nextInt(keys.size()));
                try { return Integer.parseInt(randomKey); } catch (Exception ignored) {}
            }
        }
        return -1;
    }

    @SuppressWarnings("deprecation")
    public void setupIsland(Player player) {
        World practiceWorld = Bukkit.getWorld("practice");
        if (practiceWorld == null) {
            player.sendMessage("§cErrore: Il mondo 'practice' non esiste.");
            return;
        }

        practiceWorld.setDifficulty(Difficulty.NORMAL);
        practiceWorld.setGameRuleValue("doMobSpawning", "false");
        practiceWorld.setGameRuleValue("doDaylightCycle", "false");
        practiceWorld.setGameRuleValue("announceAdvancements", "false");
        practiceWorld.setGameRuleValue("randomTickSpeed", "0");
        practiceWorld.setTime(6000);

        int centerX = 0, centerY = 100, centerZ = 0, maxRadius = 13;

        for (int yOffset = 0; yOffset >= -14; yOffset--) {
            double currentRadius = maxRadius * (1.0 - Math.pow((double) Math.abs(yOffset) / 14.0, 1.5));
            for (int x = -maxRadius; x <= maxRadius; x++) {
                for (int z = -maxRadius; z <= maxRadius; z++) {
                    double distance = Math.sqrt(x * x + z * z);
                    double noise = (Math.random() * 2.5) - 1.25;

                    if (distance + noise <= currentRadius) {
                        Block b = practiceWorld.getBlockAt(centerX + x, centerY + yOffset, centerZ + z);
                        double rand = Math.random();
                        if (rand > 0.7) { b.setType(Material.STAINED_CLAY); b.setData((byte) 1); }
                        else if (rand > 0.4) { b.setType(Material.CONCRETE); b.setData((byte) 1); }
                        else if (rand > 0.15) { b.setType(Material.RED_SANDSTONE); }
                        else { b.setType(Material.WOOD); b.setData((byte) 1); }
                    }
                }
            }
        }

        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                Block topBlock = practiceWorld.getBlockAt(centerX + x, centerY, centerZ + z);
                for (int y = 1; y <= 32; y++) {
                    practiceWorld.getBlockAt(centerX + x, centerY + y, centerZ + z).setType(Material.AIR);
                }

                if (x >= -3 && x <= 3 && z >= -3 && z <= 3) {
                    topBlock.setType(Material.STAINED_GLASS); topBlock.setData((byte) 15);
                    Block underBlock = practiceWorld.getBlockAt(centerX + x, centerY - 1, centerZ + z);
                    underBlock.setType(Material.WOOD); underBlock.setData((byte) 1);
                } else {
                    topBlock.setType(Material.QUARTZ_BLOCK);
                }
            }
        }

        Location spawnIsland = new Location(practiceWorld, 0.5, 101, 5.5, 180f, 35f);
        player.teleport(spawnIsland);
        player.sendMessage("§bNuova isola di allenamento generata con successo!");
    }

    public void saveAndApplyCustomFloor(Player player) {
        org.bukkit.World w = player.getWorld();
        java.util.List<String> floorBlocks = new java.util.ArrayList<>();

        plugin.getConfig().set("players." + player.getUniqueId() + ".use_custom_floor", true);

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                org.bukkit.block.Block b101 = w.getBlockAt(x, 101, z);
                org.bukkit.block.Block b100 = w.getBlockAt(x, 100, z);

                if (b101.getType() != org.bukkit.Material.AIR) {
                    floorBlocks.add(x + ";" + z + ";" + b101.getType().name() + ";" + b101.getData());
                    b100.setType(b101.getType());
                    b100.setData(b101.getData());
                } else {
                    floorBlocks.add(x + ";" + z + ";GRASS;0");
                    b100.setType(org.bukkit.Material.GRASS);
                }
            }
        }

        plugin.getConfig().set("players." + player.getUniqueId() + ".custom_floor_data", floorBlocks);
        plugin.saveConfig();
        // Nessun messaggio e nessun suono.
    }

    @SuppressWarnings("deprecation")
    public void generateFloor(Player player, int buildId, String category) {
        World world = player.getWorld();
        FileConfiguration config = getBuildConfig(category);
        List<String> blocksData = config.getStringList("builds." + buildId + ".blocks");

        boolean useCustom = plugin.getConfig().getBoolean("players." + player.getUniqueId() + ".use_custom_floor", false);

        if (useCustom && plugin.getConfig().contains("players." + player.getUniqueId() + ".custom_floor_data")) {
            // Modalità Custom Floor (attivata solo tramite il cartello)
            List<String> customData = plugin.getConfig().getStringList("players." + player.getUniqueId() + ".custom_floor_data");
            for (String data : customData) {
                String[] parts = data.split(";");
                if (parts.length == 4) {
                    world.getBlockAt(Integer.parseInt(parts[0]), 100, Integer.parseInt(parts[1]))
                            .setTypeIdAndData(Material.valueOf(parts[2]).getId(), Byte.parseByte(parts[3]), false);
                }
            }
        } else {
            // Modalità Pavimento della Mappa
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    Block floorBlock = world.getBlockAt(x, 100, z);
                    boolean found = false;

                    if (category.equals("FearGames")) {
                        for (String dataString : blocksData) {
                            String[] parts = dataString.split(";");
                            if (parts.length == 5 && Integer.parseInt(parts[0]) == x && Integer.parseInt(parts[1]) == 0 && Integer.parseInt(parts[2]) == z) {
                                floorBlock.setType(Material.valueOf(parts[3]));
                                floorBlock.setData(Byte.parseByte(parts[4]));
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            floorBlock.setType(Material.STAINED_GLASS);
                            floorBlock.setData((byte) 15); // Vetro nero per FearGames
                        }
                    } else if (category.equals("Mineplex")) {
                        for (String dataString : blocksData) {
                            String[] parts = dataString.split(";");
                            if (parts.length == 5 && Integer.parseInt(parts[0]) == x && Integer.parseInt(parts[1]) == 1 && Integer.parseInt(parts[2]) == z) {
                                Material m = Material.valueOf(parts[3]);
                                if (!m.isSolid() || m.name().contains("FENCE") || m.name().contains("DOOR") || m.name().contains("SKULL") || m.name().contains("STEP")) {
                                    floorBlock.setType(Material.DIRT);
                                } else {
                                    floorBlock.setType(m);
                                    floorBlock.setData(Byte.parseByte(parts[4]));
                                }
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            floorBlock.setType(Material.GRASS); // Erba di default per Mineplex
                            floorBlock.setData((byte) 0);
                        }
                    } else {
                        // Per le build Custom, TempTest o qualsiasi altra categoria
                        for (String dataString : blocksData) {
                            String[] parts = dataString.split(";");
                            if (parts.length == 5 && Integer.parseInt(parts[0]) == x && Integer.parseInt(parts[1]) == 0 && Integer.parseInt(parts[2]) == z) {
                                floorBlock.setType(Material.valueOf(parts[3]));
                                floorBlock.setData(Byte.parseByte(parts[4]));
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            floorBlock.setType(Material.GRASS); // Erba di default assoluto
                            floorBlock.setData((byte) 0);
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void saveBuild(Player player, int id, String buildName, String category) {
        World world = Bukkit.getWorld("practice");
        if (world == null) return;

        List<String> blocksData = new ArrayList<>();
        for (int x = -3; x <= 3; x++) {
            for (int y = 1; y <= 32; y++) {
                for (int z = -3; z <= 3; z++) {
                    Block block = world.getBlockAt(x, 100 + y, z);
                    if (block.getType() != Material.AIR) {
                        blocksData.add(x + ";" + y + ";" + z + ";" + block.getType().name() + ";" + block.getData());
                    }
                }
            }
        }

        List<String> hotbar = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                hotbar.add(item.getType().name() + ";" + item.getData().getData());
            } else {
                hotbar.add("AIR;0");
            }
        }

        FileConfiguration config = getBuildConfig(category);
        config.set("builds." + id + ".name", buildName);
        config.set("builds." + id + ".blocks", blocksData);
        config.set("builds." + id + ".hotbar", hotbar);

        // Salva su file SOLO se non è un test temporaneo
        if (!category.equalsIgnoreCase("TempTest")) {
            try {
                config.save(new java.io.File(plugin.getDataFolder(), category.toLowerCase() + "_builds.yml"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            player.sendMessage("§aBuild '" + buildName + "' salvata in " + category + " (ID: " + id + ")!");
        } else {
            player.sendMessage("§eBuild memorizzata in RAM per il test!");
        }
    }

    @SuppressWarnings("deprecation")
    public void loadBuild(Player player, int id, String category) {
        World world = Bukkit.getWorld("practice");
        if (world == null) return;

        // Spegne il Custom Floor per caricare il vero pavimento della mappa
        plugin.getConfig().set("players." + player.getUniqueId() + ".use_custom_floor", false);
        plugin.saveConfig();

        currentCategory.put(player, category);
        FileConfiguration config = getBuildConfig(category);

        if (!config.contains("builds." + id)) {
            player.sendMessage("§cNessuna build trovata con l'ID " + id);
            return;
        }

        clearPlot(world);
        generateFloor(player, id, category);

        List<String> hotbar = config.getStringList("builds." + id + ".hotbar");
        byte expectedSkullType = 0;
        if (hotbar != null) {
            for (String h : hotbar) {
                if (h.startsWith("SKULL_ITEM;")) {
                    expectedSkullType = Byte.parseByte(h.split(";")[1]);
                    break;
                }
            }
        }

        List<String> blocksData = config.getStringList("builds." + id + ".blocks");
        for (String dataString : blocksData) {
            String[] parts = dataString.split(";");
            if (parts.length == 5) {
                try {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    int z = Integer.parseInt(parts[2]);

                    if (category.equals("Mineplex")) {
                        if (y == 1) continue;
                        y = y - 1;
                    }

                    if (category.equals("FearGames") && y == 0) continue;

                    if (parts[3].startsWith("MOB_")) {
                        org.bukkit.entity.EntityType type = plugin.getMobManager().getEntityType(parts[3].substring(4));
                        Location loc = new Location(world, x + 0.5, 100 + y, z + 0.5, 180f, 0f);
                        org.bukkit.entity.Entity ent = world.spawnEntity(loc, type);

                        ent.setMetadata("SpeedBuildersMob", new org.bukkit.metadata.FixedMetadataValue(plugin, true));

                        if (ent instanceof org.bukkit.entity.LivingEntity) {
                            org.bukkit.entity.LivingEntity le = (org.bukkit.entity.LivingEntity) ent;
                            le.setAI(false);
                            le.setSilent(true);
                            le.setCollidable(false);
                            le.setRemoveWhenFarAway(false);

                            if (ent instanceof org.bukkit.entity.Zombie) {
                                ((org.bukkit.entity.Zombie) ent).setBaby(false);
                            }
                        }
                        continue;
                    }

                    Material material = Material.valueOf(parts[3]);
                    byte data = Byte.parseByte(parts[4]);

                    Block block = world.getBlockAt(x, 100 + y, z);
                    block.setType(material); block.setData(data);

                    if (material == Material.SKULL && expectedSkullType >= 0 && expectedSkullType < org.bukkit.SkullType.values().length) {
                        org.bukkit.block.Skull skull = (org.bukkit.block.Skull) block.getState();
                        skull.setSkullType(org.bukkit.SkullType.values()[expectedSkullType]);
                        skull.update();
                    }
                } catch (Exception ignored) {}
            }
        }

        String name = config.getString("builds." + id + ".name", "Sconosciuta");
        player.sendMessage("§aBuild '" + name + "' (" + category + ") §e[ID: " + id + "]§a caricata! Clicca sul quarzo per iniziare.");
        plugin.getHologramManager().updateArenaHologram(new Location(world, -5.5, 105.0, -5.5), id, category);
        currentBuild.put(player, id);
    }

    public void clearPlot(World world) {
        for (int x = -3; x <= 3; x++) {
            for (int y = 1; y <= 32; y++) {
                for (int z = -3; z <= 3; z++) {
                    world.getBlockAt(x, 100 + y, z).setType(Material.AIR);
                }
            }
        }
        plugin.getMobManager().clearMobs(world);
    }

    public void startCountdown(Player player, int countdown) {
        playerStates.put(player, "COUNTDOWN");
        int buildId = currentBuild.getOrDefault(player, -1);
        String cat = getCurrentCategory(player);
        String configName = buildId != -1 ? getBuildConfig(cat).getString("builds." + buildId + ".name", "Build Libera") : "Build Libera";
        final String buildName = configName;

        BukkitTask task = new BukkitRunnable() {
            int count = countdown;
            float[] scale = {0.5f, 0.5f, 0.63f, 0.79f, 1.0f, 1.26f};

            @Override
            public void run() {
                if (!player.isOnline() || !playerStates.getOrDefault(player, "").equals("COUNTDOWN")) { this.cancel(); return; }

                if (count > 3) {
                    player.sendTitle("", "§6" + buildName, 5, 25, 0);
                    count--;
                } else if (count > 0) {
                    player.sendTitle("", "§a" + count, 0, 25, 0);
                    int pitchIndex = Math.max(0, Math.min(5, 6 - count));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 1f, scale[pitchIndex]);
                    count--;
                } else {
                    player.sendTitle("", "§cTempo esaurito!", 0, 20, 10);
                    player.playSound(player.getLocation(), Sound.BLOCK_WOOD_BREAK, 1.5f, 1f);
                    clearPlot(player.getWorld());
                    if (buildId != -1) giveBuildItems(player, buildId);
                    playerStates.put(player, "PLAYING");
                    startTimer(player);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        countdownTasks.put(player, task);
    }

    private void startTimer(Player player) {
        activeTimers.put(player, System.currentTimeMillis());
        int buildId = currentBuild.getOrDefault(player, -1);
        String cat = getCurrentCategory(player);

        String recordKey = cat + "_" + buildId;
        String basePath = "records." + player.getUniqueId().toString() + ".";

        // Recupero automatico dei vecchi record
        if (buildId != -1 && cat.equals("FearGames")) {
            if (!plugin.getConfig().contains(basePath + recordKey) && plugin.getConfig().contains(basePath + buildId)) {
                long oldRecord = plugin.getConfig().getLong(basePath + buildId);
                plugin.getConfig().set(basePath + recordKey, oldRecord);
                plugin.getConfig().set(basePath + buildId, null); // Elimina il vecchio formato
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
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(timeText));
            }
        }.runTaskTimer(plugin, 0L, 1L);
        actionBars.put(player, task);
    }

    public void handlePerfect(Player player) {
        if (!playerStates.getOrDefault(player, "").equals("PLAYING") || !activeTimers.containsKey(player)) return;

        playerStates.put(player, "WAITING");
        player.getInventory().clear();
        if (actionBars.containsKey(player)) { actionBars.get(player).cancel(); actionBars.remove(player); }

        long elapsed = System.currentTimeMillis() - activeTimers.getOrDefault(player, System.currentTimeMillis());
        activeTimers.remove(player);
        double seconds = elapsed / 1000.0;

        player.sendTitle("", "§aCostruzione perfetta! §8| §fTempo: §e" + seconds + "s", 5, 40, 10);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

        int buildId = currentBuild.getOrDefault(player, -1);
        String cat = getCurrentCategory(player); // Prende la categoria attuale

        if (buildId != -1) {
            // Salva il record usando Categoria_ID (es. FearGames_1)
            String recordKey = cat + "_" + buildId;
            String recordPath = "records." + player.getUniqueId().toString() + "." + recordKey;
            long currentRecord = plugin.getConfig().getLong(recordPath, 0);

            if (currentRecord == 0 || elapsed < currentRecord) {
                plugin.getConfig().set(recordPath, elapsed);
                plugin.saveConfig();
                player.sendMessage(currentRecord == 0 ? "§b§lRecord Personale: §f" + seconds + "s" : "§b§lRecord Personale: §f" + seconds + "s §7(" + (currentRecord / 1000.0) + "s -> " + seconds + "s)");
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && playerStates.getOrDefault(player, "").equals("WAITING")) {
                        int nextBuildId = buildId;
                        if (isContinuousRandom(player)) {
                            int randomId = getRandomBuildId(cat);
                            if (randomId != -1) nextBuildId = randomId;
                        }
                        loadBuild(player, nextBuildId, cat);
                        startCountdown(player, 3);
                    }
                }
            }.runTaskLater(plugin, 50L);
        } else {
            playerStates.put(player, "IDLE");
        }
    }

    public FileConfiguration getBuildConfig(String category) {
        if (category == null) return plugin.getFearConfig();

        // <-- INIZIO MODIFICA: Se è il test, restituisce la RAM
        if (category.equalsIgnoreCase("TempTest")) return memoryConfig;
        // <-- FINE MODIFICA

        if (category.equalsIgnoreCase("Mineplex")) return plugin.getMineplexConfig();
        if (category.equalsIgnoreCase("FearGames")) return plugin.getFearConfig();

        if (dynamicConfigs.containsKey(category)) return dynamicConfigs.get(category);

        java.io.File file = new java.io.File(plugin.getDataFolder(), category.toLowerCase() + "_builds.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (Exception ignored) {}
        }
        FileConfiguration cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        dynamicConfigs.put(category, cfg);
        return cfg;
    }

    public void openCategoryMenu(Player player) {
        org.bukkit.configuration.ConfigurationSection section = plugin.getConfig().getConfigurationSection("custom_categories");

        java.util.List<String> allServers = new java.util.ArrayList<>();
        allServers.add("FearGames");
        allServers.add("Mineplex");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                // Esclude Custom dalla lista normale e impedisce i doppioni
                if (!key.equalsIgnoreCase("FearGames") && !key.equalsIgnoreCase("Mineplex") && !key.equalsIgnoreCase("Custom")) {
                    allServers.add(key);
                }
            }
        }

        allServers.sort(String.CASE_INSENSITIVE_ORDER);

        // GUI da 54 slot per fare spazio alla categoria Custom isolata
        org.bukkit.inventory.Inventory inv = Bukkit.createInventory(null, 54, "§8Seleziona Server");

        // --- 1. IL TRONO: CATEGORIA "CUSTOM" (Slot 4 - Al centro in alto) ---
        String customName = section != null ? section.getString("Custom.name", "Custom") : "Custom";
        String customIp = section != null ? section.getString("Custom.ip", "Locale") : "Locale";
        String customIcon = section != null ? section.getString("Custom.icon", "WORKBENCH;0") : "WORKBENCH;0";

        Material cMat = Material.WORKBENCH;
        short cData = 0;
        try {
            String[] parts = customIcon.split(";");
            cMat = Material.valueOf(parts[0]);
            cData = Short.parseShort(parts[1]);
        } catch (Exception ignored) {}

        ItemStack customItem = new ItemStack(cMat, 1, cData);
        org.bukkit.inventory.meta.ItemMeta customMeta = customItem.getItemMeta();
        customMeta.setDisplayName("§e§l" + customName);

        org.bukkit.configuration.file.FileConfiguration cfgCustom = getBuildConfig("Custom");
        int customBuildCount = cfgCustom.contains("builds") ? cfgCustom.getConfigurationSection("builds").getKeys(false).size() : 0;

        customMeta.setLore(java.util.Arrays.asList(
                "§7IP: §f" + customIp,
                "§7Mappe totali: §e" + customBuildCount,
                "",
                "§7Clicca per sfogliare le",
                "§7build originali di " + customName + "."
        ));
        customItem.setItemMeta(customMeta);
        inv.setItem(4, customItem);

        // --- 2. GLI ALTRI SERVER (A partire dalla riga 3) ---
        int[] slots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        int slotIndex = 0;

        for (String serverKey : allServers) {
            String defaultName = serverKey;
            String defaultIp = "Sconosciuto";
            String defaultIcon = "STAINED_CLAY;3";

            if (serverKey.equalsIgnoreCase("FearGames")) {
                defaultIp = "mc.feargames.eu";
                defaultIcon = "STAINED_CLAY;14";
            } else if (serverKey.equalsIgnoreCase("Mineplex")) {
                defaultIp = "play.mineplex.com";
                defaultIcon = "STAINED_CLAY;5";
            }

            String name = section != null ? section.getString(serverKey + ".name", defaultName) : defaultName;
            String ip = section != null ? section.getString(serverKey + ".ip", defaultIp) : defaultIp;
            String iconStr = section != null ? section.getString(serverKey + ".icon", defaultIcon) : defaultIcon;

            Material mat = Material.STAINED_CLAY;
            short data = 3;
            try {
                String[] parts = iconStr.split(";");
                mat = Material.valueOf(parts[0]);
                data = Short.parseShort(parts[1]);
            } catch (Exception ignored) {}

            ItemStack item = new ItemStack(mat, 1, data);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

            if (serverKey.equalsIgnoreCase("FearGames")) meta.setDisplayName("§c§l" + name);
            else if (serverKey.equalsIgnoreCase("Mineplex")) meta.setDisplayName("§a§l" + name);
            else meta.setDisplayName("§b§l" + name);

            org.bukkit.configuration.file.FileConfiguration cfg = getBuildConfig(serverKey);
            int buildCount = cfg.contains("builds") ? cfg.getConfigurationSection("builds").getKeys(false).size() : 0;

            meta.setLore(java.util.Arrays.asList(
                    "§7IP: §f" + ip,
                    "§7Mappe totali: §e" + buildCount,
                    "",
                    "§7Clicca per sfogliare le",
                    "§7build originali di " + name + "."
            ));
            item.setItemMeta(meta);

            if (slotIndex < slots.length) {
                inv.setItem(slots[slotIndex], item);
                slotIndex++;
            } else {
                inv.addItem(item);
            }
        }

        // --- 3. CONTORNO IN VETRO NERO (SOLO BORDI E PRIME DUE RIGHE) ---
        ItemStack filler = new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 15);
        org.bukkit.inventory.meta.ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        // Specifica esattamente in quali slot mettere il vetro nero per creare la cornice
        int[] borderSlots = {
                0, 1, 2, 3, /* Il 4 è lasciato libero per la categoria Custom */ 5, 6, 7, 8,
                9, 10, 11, 12, 13, 14, 15, 16, 17,
                18, 26,
                27, 35,
                36, 44,
                45, 46, 47, 48, 49, 50, 51, 52, 53
        };

        for (int i : borderSlots) {
            inv.setItem(i, filler);
        }

        player.openInventory(inv);
    }

    public void openBuildMenu(Player player, int page, String category) {
        String filter = activeSearch.get(player);
        String title = filter == null ? "§8" + category + " - P. " + page : "§8Cerca (" + category + ") - P. " + page;
        if (title.length() > 32) title = title.substring(0, 32);

        org.bukkit.inventory.Inventory inv = Bukkit.createInventory(null, 54, title);
        FileConfiguration config = getBuildConfig(category);

        List<Integer> buildIds = new ArrayList<>();
        if (config.contains("builds")) {
            for (String key : config.getConfigurationSection("builds").getKeys(false)) {
                String name = config.getString("builds." + key + ".name", "Sconosciuta");
                if (filter == null || name.toLowerCase().contains(filter.toLowerCase())) {
                    try { buildIds.add(Integer.parseInt(key)); } catch (Exception ignored) {}
                }
            }
        }

        int maxItemsPerPage = 45;
        int startIndex = (page - 1) * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, buildIds.size());

        for (int i = startIndex; i < endIndex; i++) {
            int id = buildIds.get(i);
            String name = config.getString("builds." + id + ".name", "Sconosciuta");

            ItemStack item = new ItemStack(Material.PAPER);
            List<String> hotbar = config.getStringList("builds." + id + ".hotbar");
            if (hotbar != null && !hotbar.isEmpty()) {
                for (String h : hotbar) {
                    if (!h.startsWith("AIR")) {
                        String[] p = h.split(";");
                        if (p[0].startsWith("MOB_")) {
                            item = plugin.getMobManager().getMobEgg(p[0].substring(4));
                        } else {
                            try {
                                Material rawMat = Material.valueOf(p[0]);
                                byte rawData = Byte.parseByte(p[1]);
                                ItemStack normalized = ItemUtils.normalizeItem(rawMat, rawData, category);
                                if (normalized != null) {
                                    item = new ItemStack(normalized.getType(), 1, normalized.getDurability());
                                } else {
                                    item = new ItemStack(rawMat, 1, rawData);
                                }
                            } catch (Exception ignored) {}
                        }
                        break;
                    }
                }
            }

            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§a" + name);
            meta.setLore(java.util.Arrays.asList("§7ID: " + id, "", "§eClicca per giocare!"));
            item.setItemMeta(meta);

            inv.setItem(i - startIndex, item);
        }

        if (page > 1) {
            ItemStack prev = new ItemStack(Material.ARROW);
            org.bukkit.inventory.meta.ItemMeta meta = prev.getItemMeta();
            meta.setDisplayName("§cPagina Precedente");
            prev.setItemMeta(meta);
            inv.setItem(45, prev);
        }

        if (endIndex < buildIds.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            org.bukkit.inventory.meta.ItemMeta meta = next.getItemMeta();
            meta.setDisplayName("§aPagina Successiva");
            next.setItemMeta(meta);
            inv.setItem(53, next);
        }

        // Tasto per tornare alla lista dei Server (Slot 48, a sinistra del tasto Cerca)
        ItemStack backBtn = new ItemStack(Material.DARK_OAK_DOOR_ITEM);
        org.bukkit.inventory.meta.ItemMeta backMeta = backBtn.getItemMeta();
        backMeta.setDisplayName("§c§lTorna ai Server");
        backMeta.setLore(java.util.Arrays.asList("§7Clicca per tornare alla", "§7selezione del server."));
        backBtn.setItemMeta(backMeta);
        inv.setItem(48, backBtn);

        ItemStack searchBtn = new ItemStack(Material.NAME_TAG);
        org.bukkit.inventory.meta.ItemMeta searchMeta = searchBtn.getItemMeta();
        searchMeta.setDisplayName("§e§lCerca Build");
        if (filter == null) {
            searchMeta.setLore(java.util.Arrays.asList("§7Tasto Sinistro: §fCerca una build"));
        } else {
            searchMeta.setLore(java.util.Arrays.asList("§7Filtro attivo: §f" + filter, "", "§7Tasto Sinistro: §fNuova ricerca", "§7Tasto Destro: §cRimuovi filtro"));
        }
        searchBtn.setItemMeta(searchMeta);
        inv.setItem(48, searchBtn); // Spostato a sinistra

        ItemStack randomBtn = new ItemStack(Material.ENDER_PEARL);
        org.bukkit.inventory.meta.ItemMeta randomMeta = randomBtn.getItemMeta();
        randomMeta.setDisplayName("§d§lBuild Casuale");
        randomMeta.setLore(java.util.Arrays.asList("§7Tasto Sinistro (SX): §aRandom Continua", "§8(Cambia build ad ogni completamento)", "", "§7Tasto Destro (DX): §eRandom Singola", "§8(Sceglie una build e la ripete all'infinito)"));
        randomBtn.setItemMeta(randomMeta);
        inv.setItem(50, randomBtn); // Posizionato a destra

        player.openInventory(inv);
    }

    public void viewBuild(Player player) {
        int buildId = getCurrentBuild(player);
        if (buildId == -1) { player.sendMessage("§cDevi prima caricare una build con /map load <id>!"); return; }
        forceReset(player);
        loadBuild(player, buildId, getCurrentCategory(player));
        setState(player, "IDLE");
        player.sendMessage("§aBuild in modalità esplorazione. Clicca il quarzo per far partire il timer!");
    }

    @SuppressWarnings("deprecation")
    public void giveBuildItems(Player player, int buildId) {
        player.getInventory().clear();
        String cat = getCurrentCategory(player);
        List<String> blocksData = getBuildConfig(cat).getStringList("builds." + buildId + ".blocks");
        List<String> hotbar = getBuildConfig(cat).getStringList("builds." + buildId + ".hotbar");
        HashMap<String, Integer> blockCounts = new HashMap<>();

        // Identifica in anticipo quale teschio serve
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
                    // Impone il tipo Wither (o altro) al conteggio dell'inventario
                    if (normalized.getType() == Material.SKULL_ITEM) {
                        normalized.setDurability(expectedSkullType);
                    }
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

                            // Piazza l'oggetto ESATTAMENTE nello slot 'i' in cui è stato salvato
                            player.getInventory().setItem(i, egg);

                            int leftOver = totalNeeded - egg.getAmount();
                            if (leftOver > 0) blockCounts.put(rawMat, leftOver);
                            else blockCounts.remove(rawMat);
                        }
                        continue;
                    }

                    byte rawData = Byte.parseByte(matDataRaw[1]);
                    ItemStack normalized = ItemUtils.normalizeItem(Material.valueOf(rawMat), rawData, cat);
                    if (normalized != null) {
                        if (normalized.getType() == Material.SKULL_ITEM) {
                            normalized.setDurability(expectedSkullType);
                        }
                        String key = normalized.getType().name() + ";" + normalized.getDurability();

                        if (processedHotbar.contains(key)) continue;
                        processedHotbar.add(key);

                        if (blockCounts.containsKey(key)) {
                            int totalNeeded = blockCounts.get(key);
                            int toPutInSlot = Math.min(totalNeeded, 64);

                            // Piazza l'oggetto ESATTAMENTE nello slot 'i' in cui è stato salvato
                            player.getInventory().setItem(i, new ItemStack(normalized.getType(), toPutInSlot, normalized.getDurability()));

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

    @SuppressWarnings("deprecation")
    public boolean checkBuildPerfect(Player player) {
        int buildId = currentBuild.getOrDefault(player, -1);
        if (buildId == -1) return false;

        String cat = getCurrentCategory(player);
        List<String> blocksData = getBuildConfig(cat).getStringList("builds." + buildId + ".blocks");
        World world = player.getWorld();

        int blocksInArena = 0;
        for (int x = -3; x <= 3; x++) {
            for (int y = 1; y <= 32; y++) {
                for (int z = -3; z <= 3; z++) {
                    if (world.getBlockAt(x, 100 + y, z).getType() != Material.AIR) blocksInArena++;
                }
            }
        }

        for (org.bukkit.entity.Entity ent : world.getEntities()) {
            if (ent.hasMetadata("SpeedBuildersMob")) blocksInArena++;
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
                        Location checkLoc = new Location(world, Integer.parseInt(parts[0]) + 0.5, 100 + savedY + 0.5, Integer.parseInt(parts[2]) + 0.5);
                        for (org.bukkit.entity.Entity ent : world.getNearbyEntities(checkLoc, 0.5, 0.5, 0.5)) {
                            if (ent.hasMetadata("SpeedBuildersMob") && ent.getType() == expectedType) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) return false;
                        continue;
                    }

                    Block block = world.getBlockAt(Integer.parseInt(parts[0]), 100 + savedY, Integer.parseInt(parts[2]));
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
                    if (savedMat == Material.ENDER_PORTAL_FRAME) ignoreData = true; // <- FIX END PORTAL
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
        int buildId = getCurrentBuild(player);
        if (buildId == -1) { player.sendMessage("§cNessuna build caricata!"); return; }
        if (!getState(player).equals("PLAYING")) { player.sendMessage("§cDevi essere in partita per vedere gli errori!"); return; }

        String cat = getCurrentCategory(player);
        List<String> blocksData = getBuildConfig(cat).getStringList("builds." + buildId + ".blocks");
        World world = player.getWorld();

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
                    Block b = world.getBlockAt(x, 100 + y, z);
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
                            if (eMat == Material.ENDER_PORTAL_FRAME) ignoreData = true; // <- FIX END PORTAL
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
                int eX = ent.getLocation().getBlockX();
                int eY = ent.getLocation().getBlockY() - 100;
                int eZ = ent.getLocation().getBlockZ();
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
            org.bukkit.Location bLoc = new org.bukkit.Location(world, Integer.parseInt(loc[0]), 100 + Integer.parseInt(loc[1]), Integer.parseInt(loc[2]));

            if (matData[0].startsWith("MOB_")) {
                // Spawna particelle rosse invece del vetro
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
            player.sendMessage("§aNessun errore! Devi solo premere il pulsante mancante (se c'è).");
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
                                Block b = world.getBlockAt(x, 100 + y, z);
                                player.sendBlockChange(b.getLocation(), b.getType(), b.getData());
                            }
                        }
                    }
                }
            }.runTaskLater(plugin, 100L);
        }
    }

    public void openGamemodeMenu(org.bukkit.entity.Player player) {
        org.bukkit.inventory.Inventory inv = Bukkit.createInventory(null, 27, "§8Scegli la Modalità");

        org.bukkit.inventory.ItemStack creative = new org.bukkit.inventory.ItemStack(Material.DIAMOND_PICKAXE);
        org.bukkit.inventory.meta.ItemMeta cMeta = creative.getItemMeta();
        cMeta.setDisplayName("§b§lCostruttore (Creativa)");
        cMeta.setLore(java.util.Arrays.asList("§7Entra nell'arena in Creativa", "§7per creare mappe o pavimenti."));
        creative.setItemMeta(cMeta);

        org.bukkit.inventory.ItemStack survival = new org.bukkit.inventory.ItemStack(Material.APPLE);
        org.bukkit.inventory.meta.ItemMeta sMeta = survival.getItemMeta();
        sMeta.setDisplayName("§a§lGiocatore (Sopravvivenza)");
        sMeta.setLore(java.util.Arrays.asList("§7Entra nell'arena in Sopravvivenza", "§7per giocare e testare le mappe."));
        survival.setItemMeta(sMeta);

        inv.setItem(11, creative);
        inv.setItem(15, survival);

        // Contorno
        org.bukkit.inventory.ItemStack filler = new org.bukkit.inventory.ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 15);
        org.bukkit.inventory.meta.ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                inv.setItem(i, filler);
            }
        }

        player.openInventory(inv);
    }

    public void resetCustomFloor(Player player) {
        // Disattiva la modalità custom floor nel config
        plugin.getConfig().set("players." + player.getUniqueId() + ".use_custom_floor", false);
        plugin.saveConfig();

        int buildId = getCurrentBuild(player);
        if (buildId != -1) {
            // Se c'è una build caricata, rigenera il pavimento originale di quella specifica mappa
            generateFloor(player, buildId, getCurrentCategory(player));
        } else {
            // Se l'arena è vuota, rimette l'erba standard su tutto il quadrato 7x7
            org.bukkit.World w = player.getWorld();
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    org.bukkit.block.Block b = w.getBlockAt(x, 100, z);
                    b.setType(Material.GRASS);
                    b.setData((byte) 0);
                }
            }
        }
    }

}