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
    private final HashMap<Player, String> playerStates = new HashMap<>();
    private final HashMap<Player, Integer> currentBuild = new HashMap<>();

    private final HashMap<Player, Boolean> awaitingSearch = new HashMap<>();
    private final HashMap<Player, String> activeSearch = new HashMap<>();
    private final HashMap<Player, String> currentCategory = new HashMap<>();
    private final HashMap<Player, Boolean> continuousRandom = new HashMap<>();
    private final HashMap<Player, Boolean> awaitingCategory = new HashMap<>();
    private final HashMap<String, FileConfiguration> dynamicConfigs = new HashMap<>();
    private final HashMap<Player, String> timerModes = new HashMap<>();
    private final HashMap<Player, Boolean> usedFly = new HashMap<>();

    public void clearCurrentCategory(Player player) { currentCategory.remove(player); }
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
    private final org.bukkit.configuration.file.FileConfiguration memoryConfig = new org.bukkit.configuration.file.YamlConfiguration();

    public GameManager(Main plugin) {
        this.plugin = plugin;
    }

    public String getActiveSearch(Player p) { return activeSearch.get(p); }
    public String getTimerMode(Player p) { return timerModes.getOrDefault(p, "FIRST_BLOCK"); }
    public void setTimerMode(Player p, String mode) { timerModes.put(p, mode); }
    public void setUsedFly(Player p, boolean val) { if (val) usedFly.put(p, true); else usedFly.remove(p); }
    public boolean hasUsedFly(Player p) { return usedFly.getOrDefault(p, false); }
    public String getState(Player player) { return playerStates.getOrDefault(player, "IDLE"); }
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

    public void setState(Player player, String state) {
        playerStates.put(player, state);
        updateScoreboard(player);
    }

    public void updateScoreboard(Player player) {
        plugin.getUIManager().updateScoreboard(player);
    }

    public void forceReset(Player player) {
        plugin.getMatchManager().cancelTasks(player);
        usedFly.remove(player);
        player.getInventory().clear();
        clearPlot(player);
    }

    public void setupIsland(Player player) { plugin.getArenaManager().setupIsland(player); }
    public void clearIsland(Player player) { plugin.getArenaManager().clearIsland(player); }
    public void generateFloor(Player player, int buildId, String category) { plugin.getArenaManager().generateFloor(player, buildId, category); }
    public void saveAndApplyCustomFloor(Player player) { plugin.getArenaManager().saveAndApplyCustomFloor(player); }
    public void resetCustomFloor(Player player) { plugin.getArenaManager().resetCustomFloor(player); }
    public void openCategoryMenu(Player player) { plugin.getMenuManager().openCategoryMenu(player); }
    public void openBuildMenu(Player player, int page, String category) { plugin.getMenuManager().openBuildMenu(player, page, category); }
    public void viewBuild(Player player) { plugin.getMatchManager().viewBuild(player); }
    public void giveBuildItems(Player player, int buildId) { plugin.getMatchManager().giveBuildItems(player, buildId); }
    public boolean checkBuildPerfect(Player player) { return plugin.getMatchManager().checkBuildPerfect(player); }
    public void showErrors(Player player) { plugin.getMatchManager().showErrors(player); }
    public void readyBuild(Player player) { plugin.getMatchManager().readyBuild(player); }
    public void startActualReady(Player player) { plugin.getMatchManager().startActualReady(player); }
    public void startTimer(Player player) { plugin.getMatchManager().startTimer(player); }
    public void handlePerfect(Player player) { plugin.getMatchManager().handlePerfect(player); }

    public void resetPlayer(Player player) {
        forceReset(player);
        playerStates.put(player, "IDLE");
        continuousRandom.remove(player);
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
    public void saveBuild(Player player, int id, String buildName, String category) {
        World world = Bukkit.getWorld("practice");
        if (world == null) return;

        int plotId = plugin.getPlotManager().getPlot(player);
        Location centerLoc = plugin.getPlotManager().getPlotCenter(world, plotId);
        int cX = centerLoc.getBlockX(), cZ = centerLoc.getBlockZ();

        List<String> blocksData = new ArrayList<>();
        for (int x = -3; x <= 3; x++) {
            for (int y = 1; y <= 32; y++) {
                for (int z = -3; z <= 3; z++) {
                    Block block = world.getBlockAt(cX + x, 100 + y, cZ + z);
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

        String oldCat = currentCategory.get(player);
        currentCategory.put(player, category);

        if (oldCat == null || !oldCat.equalsIgnoreCase(category)) {
            if (category.equalsIgnoreCase("Hypixel")) {
                plugin.getConfig().set("players." + player.getUniqueId() + ".dj", false);
                player.setAllowFlight(true);
                player.setFlying(true);
                player.sendMessage("§8§o(Volo attivato in automatico per Hypixel)");
            } else {
                plugin.getConfig().set("players." + player.getUniqueId() + ".dj", true);
                player.setAllowFlight(true);
                player.setFlying(false);
                player.sendMessage("§8§o(Double Jump attivato in automatico)");
            }
            plugin.saveConfig();
        }

        FileConfiguration config = getBuildConfig(category);

        if (!config.contains("builds." + id)) {
            player.sendMessage("§cNessuna build trovata con l'ID " + id);
            return;
        }

        clearPlot(player);
        generateFloor(player, id, category);

        int plotId = plugin.getPlotManager().getPlot(player);
        Location centerLoc = plugin.getPlotManager().getPlotCenter(world, plotId);
        int cX = centerLoc.getBlockX(), cZ = centerLoc.getBlockZ();

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
                        Location loc = new Location(world, cX + x + 0.5, 100 + y, cZ + z + 0.5, 180f, 0f);
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

                    Block block = world.getBlockAt(cX + x, 100 + y, cZ + z);
                    block.setType(material); block.setData(data);

                    if (material == Material.SKULL && expectedSkullType >= 0 && expectedSkullType < org.bukkit.SkullType.values().length) {
                        org.bukkit.block.Skull skull = (org.bukkit.block.Skull) block.getState();
                        skull.setSkullType(org.bukkit.SkullType.values()[expectedSkullType]);
                        skull.update();
                    }
                } catch (Exception ignored) {}
            }
        }

        plugin.getHologramManager().updateArenaHologram(new Location(world, cX - 5.5, 105.0, cZ - 5.5), id, category);
        currentBuild.put(player, id);

        updateScoreboard(player);
    }

    public void clearPlot(Player player) {
        World world = player.getWorld();
        if (!world.getName().equals("practice")) return;

        int plotId = plugin.getPlotManager().getPlot(player);
        Location centerLoc = plugin.getPlotManager().getPlotCenter(world, plotId);
        int cX = centerLoc.getBlockX(), cZ = centerLoc.getBlockZ();

        // Pulisce i blocchi del plot (zona di costruzione 7x7)
        for (int x = -3; x <= 3; x++) {
            for (int y = 1; y <= 32; y++) {
                for (int z = -3; z <= 3; z++) {
                    world.getBlockAt(cX + x, 100 + y, cZ + z).setType(Material.AIR);
                }
            }
        }

        // Pulisce i mob del plot
        for (org.bukkit.entity.Entity ent : world.getEntities()) {
            if (ent.hasMetadata("SpeedBuildersMob")) {
                if (Math.abs(ent.getLocation().getBlockX() - cX) <= 3 && Math.abs(ent.getLocation().getBlockZ() - cZ) <= 3) {
                    ent.remove();
                }
            }
        }
    }

    public FileConfiguration getBuildConfig(String category) {
        if (category == null) return plugin.getFearConfig();

        if (category.equalsIgnoreCase("TempTest")) return memoryConfig;

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

}