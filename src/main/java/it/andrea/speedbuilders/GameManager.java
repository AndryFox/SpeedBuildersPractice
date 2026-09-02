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

    public void setAwaitingSearch(Player p, boolean val) { if (val) awaitingSearch.put(p, true); else awaitingSearch.remove(p); }
    public boolean isAwaitingSearch(Player p) { return awaitingSearch.containsKey(p); }
    public void setActiveSearch(Player p, String search) { activeSearch.put(p, search); }
    public void clearSearch(Player p) { activeSearch.remove(p); }
    public String getCurrentCategory(Player player) { return currentCategory.getOrDefault(player, "FearGames"); }

    public GameManager(Main plugin) {
        this.plugin = plugin;
    }

    public Material getInventoryItemMaterial(Material blockMat) {
        switch (blockMat.name()) {
            case "WOODEN_DOOR": return Material.WOOD_DOOR;
            case "IRON_DOOR_BLOCK": return Material.IRON_DOOR;
            case "SPRUCE_DOOR": return Material.SPRUCE_DOOR_ITEM;
            case "BIRCH_DOOR": return Material.BIRCH_DOOR_ITEM;
            case "JUNGLE_DOOR": return Material.JUNGLE_DOOR_ITEM;
            case "ACACIA_DOOR": return Material.ACACIA_DOOR_ITEM;
            case "DARK_OAK_DOOR": return Material.DARK_OAK_DOOR_ITEM;
            case "FLOWER_POT": return Material.FLOWER_POT_ITEM;
            case "BREWING_STAND": return Material.BREWING_STAND_ITEM;
            case "CAULDRON": return Material.CAULDRON_ITEM;
            case "BED_BLOCK": return Material.BED;
            case "DIODE_BLOCK_OFF":
            case "DIODE_BLOCK_ON": return Material.DIODE;
            case "REDSTONE_COMPARATOR_OFF":
            case "REDSTONE_COMPARATOR_ON": return Material.REDSTONE_COMPARATOR;
            case "SUGAR_CANE_BLOCK": return Material.SUGAR_CANE;
            case "REDSTONE_WIRE": return Material.REDSTONE;
            case "SIGN_POST":
            case "WALL_SIGN": return Material.SIGN;
            default: return blockMat;
        }
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
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(""));
    }

    @SuppressWarnings("deprecation")
    public void setupIsland(Player player) {
        World practiceWorld = Bukkit.getWorld("practice");
        if (practiceWorld == null) {
            player.sendMessage("§cErrore: Il mondo 'practice' non esiste.");
            return;
        }

        practiceWorld.setDifficulty(Difficulty.PEACEFUL);
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

    @SuppressWarnings("deprecation")
    public void saveBuild(Player player, int id, String buildName) {
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

        FileConfiguration config = plugin.getFearConfig();
        config.set("builds." + id + ".name", buildName);
        config.set("builds." + id + ".blocks", blocksData);
        config.set("builds." + id + ".hotbar", hotbar);
        try {
            config.save(new java.io.File(plugin.getDataFolder(), "feargames_builds.yml"));
        } catch (Exception e) { e.printStackTrace(); }

        player.sendMessage("§aBuild '" + buildName + "' salvata con il tuo ordine dell'inventario (ID: " + id + ")!");
    }

    @SuppressWarnings("deprecation")
    public void loadBuild(Player player, int id, String category) {
        World world = Bukkit.getWorld("practice");
        if (world == null) return;

        currentCategory.put(player, category);
        FileConfiguration config = getBuildConfig(category);

        if (!config.contains("builds." + id)) {
            player.sendMessage("§cNessuna build trovata con l'ID " + id);
            return;
        }

        clearPlot(world);

        List<String> blocksData = config.getStringList("builds." + id + ".blocks");
        for (String dataString : blocksData) {
            String[] parts = dataString.split(";");
            if (parts.length == 5) {
                try {
                    int x = Integer.parseInt(parts[0]); int y = Integer.parseInt(parts[1]); int z = Integer.parseInt(parts[2]);
                    Material material = Material.valueOf(parts[3]); byte data = Byte.parseByte(parts[4]);
                    Block block = world.getBlockAt(x, 100 + y, z);
                    block.setType(material); block.setData(data);
                } catch (Exception ignored) {}
            }
        }

        String name = config.getString("builds." + id + ".name", "Sconosciuta");
        player.sendMessage("§aBuild '" + name + "' (" + category + ") caricata! Clicca sul quarzo per iniziare.");
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
        final long finalBest = buildId != -1 ? plugin.getConfig().getLong("records." + player.getUniqueId().toString() + "." + buildId, 0) : 0;

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

    @SuppressWarnings("deprecation")
    public void giveBuildItems(Player player, int buildId) {
        player.getInventory().clear();
        String cat = getCurrentCategory(player);
        List<String> blocksData = getBuildConfig(cat).getStringList("builds." + buildId + ".blocks");
        HashMap<String, Integer> blockCounts = new HashMap<>();

        for (String dataString : blocksData) {
            String[] parts = dataString.split(";");
            if (parts.length == 5) {
                Material material = Material.valueOf(parts[3]);
                byte data = Byte.parseByte(parts[4]);

                if ((material.name().contains("DOOR") || material == Material.BED_BLOCK) && data >= 8) {
                    continue;
                }

                material = getInventoryItemMaterial(material);

                if (material == Material.LOG || material == Material.LOG_2) {
                    data = (byte) (data % 4);
                } else if (material.name().contains("STEP") || material.name().contains("SLAB")) {
                    data = (byte) (data % 8);
                } else if (material.name().contains("DOOR") || material == Material.BED) {
                    data = 0;
                }

                String matData = material.name() + ";" + data;
                blockCounts.put(matData, blockCounts.getOrDefault(matData, 0) + 1);
            }
        }

        List<String> hotbar = getBuildConfig(cat).getStringList("builds." + buildId + ".hotbar");
        if (hotbar != null && !hotbar.isEmpty()) {
            for (int i = 0; i < hotbar.size() && i < 9; i++) {
                String[] matData = hotbar.get(i).split(";");
                if (!matData[0].equals("AIR")) {
                    String key = matData[0] + ";" + matData[1];

                    if (blockCounts.containsKey(key)) {
                        Material material = Material.valueOf(matData[0]);
                        byte data = Byte.parseByte(matData[1]);

                        int totalNeeded = blockCounts.get(key);
                        int toPutInSlot = Math.min(totalNeeded, 64);
                        int leftOver = totalNeeded - toPutInSlot;

                        player.getInventory().setItem(i, new ItemStack(material, toPutInSlot, data));

                        if (leftOver > 0) {
                            blockCounts.put(key, leftOver);
                        } else {
                            blockCounts.remove(key);
                        }
                    }
                }
            }
        }

        for (Map.Entry<String, Integer> entry : blockCounts.entrySet()) {
            String[] matData = entry.getKey().split(";");
            Material material = Material.valueOf(matData[0]);
            byte data = Byte.parseByte(matData[1]);

            player.getInventory().addItem(new ItemStack(material, entry.getValue(), data));
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
        if (blocksInArena != blocksData.size()) return false;

        for (String dataString : blocksData) {
            String[] parts = dataString.split(";");
            if (parts.length == 5) {
                try {
                    Block block = world.getBlockAt(Integer.parseInt(parts[0]), 100 + Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                    Material savedMat = Material.valueOf(parts[3]);
                    byte savedData = Byte.parseByte(parts[4]);

                    boolean ignoreData = (savedMat == Material.PUMPKIN || savedMat == Material.JACK_O_LANTERN);

                    if (block.getType() != savedMat || (!ignoreData && block.getData() != savedData)) {
                        return false;
                    }
                } catch (Exception e) { return false; }
            }
        }
        return true;
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
        if (buildId != -1) {
            String recordPath = "records." + player.getUniqueId().toString() + "." + buildId;
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
                        loadBuild(player, buildId, getCurrentCategory(player));
                        startCountdown(player, 3);
                    }
                }
            }.runTaskLater(plugin, 50L);
        } else {
            playerStates.put(player, "IDLE");
        }
    }

    public FileConfiguration getBuildConfig(String category) {
        return (category != null && category.equals("Mineplex")) ? plugin.getMineplexConfig() : plugin.getFearConfig();
    }

    public void openCategoryMenu(Player player) {
        org.bukkit.inventory.Inventory inv = Bukkit.createInventory(null, 27, "§8Seleziona Server");

        ItemStack fear = new ItemStack(Material.STAINED_CLAY, 1, (byte) 14);
        org.bukkit.inventory.meta.ItemMeta fearMeta = fear.getItemMeta();
        fearMeta.setDisplayName("§c§lmc.feargames.eu");
        fearMeta.setLore(java.util.Arrays.asList("§7Clicca per sfogliare le", "§7build originali di FearGames."));
        fear.setItemMeta(fearMeta);

        ItemStack mineplex = new ItemStack(Material.STAINED_CLAY, 1, (byte) 5);
        org.bukkit.inventory.meta.ItemMeta mineplexMeta = mineplex.getItemMeta();
        mineplexMeta.setDisplayName("§a§lplay.mineplex.com");
        mineplexMeta.setLore(java.util.Arrays.asList("§7Clicca per sfogliare le", "§7build originali di Mineplex."));
        mineplex.setItemMeta(mineplexMeta);

        inv.setItem(11, fear);
        inv.setItem(15, mineplex);

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

            Material iconMat = Material.PAPER;
            byte iconData = 0;
            List<String> hotbar = config.getStringList("builds." + id + ".hotbar");
            if (hotbar != null && !hotbar.isEmpty()) {
                for(String h : hotbar) {
                    if(!h.startsWith("AIR")) {
                        String[] p = h.split(";");
                        iconMat = Material.valueOf(p[0]);
                        iconData = Byte.parseByte(p[1]);
                        break;
                    }
                }
            }

            ItemStack item = new ItemStack(iconMat, 1, iconData);
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

        ItemStack searchBtn = new ItemStack(Material.NAME_TAG);
        org.bukkit.inventory.meta.ItemMeta searchMeta = searchBtn.getItemMeta();
        searchMeta.setDisplayName("§e§lCerca Build");
        if (filter == null) {
            searchMeta.setLore(java.util.Arrays.asList("§7Clicca qui per cercare", "§7una build scrivendo il nome."));
        } else {
            searchMeta.setLore(java.util.Arrays.asList("§7Filtro attivo: §f" + filter, "", "§eClicca per cercare altro."));
        }
        searchBtn.setItemMeta(searchMeta);
        inv.setItem(49, searchBtn);

        player.openInventory(inv);
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
                expected.put(parts[0] + ";" + parts[1] + ";" + parts[2], parts[3] + ";" + parts[4]);
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
                            String[] p = exp.split(";");
                            Material eMat = Material.valueOf(p[0]);
                            byte eData = Byte.parseByte(p[1]);
                            boolean ignoreData = (eMat == Material.PUMPKIN || eMat == Material.JACK_O_LANTERN);

                            if (b.getType() != eMat || (!ignoreData && b.getData() != eData)) {
                                player.sendBlockChange(b.getLocation(), Material.STAINED_GLASS, (byte) 14);
                                errorsCount++;
                            }
                            expected.remove(locKey);
                        }
                    }
                }
            }
        }

        for (Map.Entry<String, String> entry : expected.entrySet()) {
            String[] loc = entry.getKey().split(";");
            String[] matData = entry.getValue().split(";");

            org.bukkit.Location bLoc = new org.bukkit.Location(world, Integer.parseInt(loc[0]), 100 + Integer.parseInt(loc[1]), Integer.parseInt(loc[2]));
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

    public void viewBuild(Player player) {
        int buildId = getCurrentBuild(player);
        if (buildId == -1) { player.sendMessage("§cDevi prima caricare una build con /map load <id>!"); return; }
        forceReset(player);
        loadBuild(player, buildId, getCurrentCategory(player));
        setState(player, "IDLE");
        player.sendMessage("§aBuild in modalità esplorazione. Clicca il quarzo per far partire il timer!");
    }

}