package it.andrea.speedbuilders;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public class ArenaManager {

    private final Main plugin;

    public ArenaManager(Main plugin) {
        this.plugin = plugin;
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

        int plotId = plugin.getPlotManager().getPlot(player);
        Location centerLoc = plugin.getPlotManager().getPlotCenter(practiceWorld, plotId);
        int cX = centerLoc.getBlockX(), centerY = 100, cZ = centerLoc.getBlockZ(), maxRadius = 13;

        // 1. Genera la base galleggiante
        for (int yOffset = 0; yOffset >= -14; yOffset--) {
            double currentRadius = maxRadius * (1.0 - Math.pow((double) Math.abs(yOffset) / 14.0, 1.5));
            for (int x = -maxRadius; x <= maxRadius; x++) {
                for (int z = -maxRadius; z <= maxRadius; z++) {
                    double distance = Math.sqrt(x * x + z * z);
                    double noise = (Math.random() * 2.5) - 1.25;

                    if (distance + noise <= currentRadius) {
                        Block b = practiceWorld.getBlockAt(cX + x, centerY + yOffset, cZ + z);
                        double rand = Math.random();
                        if (rand > 0.7) { b.setType(Material.STAINED_CLAY); b.setData((byte) 1); }
                        else if (rand > 0.4) { b.setType(Material.CONCRETE); b.setData((byte) 1); }
                        else if (rand > 0.15) { b.setType(Material.RED_SANDSTONE); }
                        else { b.setType(Material.WOOD); b.setData((byte) 1); }
                    }
                }
            }
        }

        // 2. Genera il pavimento e i bordi di Quarzo
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                Block topBlock = practiceWorld.getBlockAt(cX + x, centerY, cZ + z);
                for (int y = 1; y <= 32; y++) {
                    practiceWorld.getBlockAt(cX + x, centerY + y, cZ + z).setType(Material.AIR);
                }

                if (x >= -3 && x <= 3 && z >= -3 && z <= 3) {
                    topBlock.setType(Material.GRASS); topBlock.setData((byte) 0);
                    Block underBlock = practiceWorld.getBlockAt(cX + x, centerY - 1, cZ + z);
                    underBlock.setType(Material.WOOD); underBlock.setData((byte) 1);
                } else {
                    topBlock.setType(Material.QUARTZ_BLOCK);
                }
            }
        }

        // 3. Cartello Replay (Centro in basso, Y=101)
        Block replayBlock = practiceWorld.getBlockAt(cX, 101, cZ - 5);
        replayBlock.setType(Material.WALL_SIGN);
        replayBlock.setData((byte) 3);
        org.bukkit.block.Sign replaySign = (org.bukkit.block.Sign) replayBlock.getState();
        replaySign.setLine(1, "§b§lReplay");
        replaySign.update();

        // 1. Floor
        Block floorBlock = practiceWorld.getBlockAt(cX - 2, 102, cZ - 5);
        floorBlock.setType(Material.WALL_SIGN);
        floorBlock.setData((byte) 3);
        org.bukkit.block.Sign floorSign = (org.bukkit.block.Sign) floorBlock.getState();
        floorSign.setLine(1, "§9§lFloor");
        floorSign.setLine(3, "§lRaymano");
        floorSign.update();

        // 2. Building
        Block buildingBlock = practiceWorld.getBlockAt(cX - 1, 102, cZ - 5);
        buildingBlock.setType(Material.WALL_SIGN);
        buildingBlock.setData((byte) 3);
        org.bukkit.block.Sign buildingSign = (org.bukkit.block.Sign) buildingBlock.getState();
        buildingSign.setLine(1, "§e§lBuilding");
        buildingSign.setLine(3, "§lNever Dies");
        buildingSign.update();

        // 3. Timer
        Block timerBlock = practiceWorld.getBlockAt(cX + 1, 102, cZ - 5);
        timerBlock.setType(Material.WALL_SIGN);
        timerBlock.setData((byte) 3);
        org.bukkit.block.Sign timerSign = (org.bukkit.block.Sign) timerBlock.getState();
        timerSign.setLine(1, "§c§lTimer");
        timerSign.update();

        // 4. Modalità
        Block modeBlock = practiceWorld.getBlockAt(cX + 2, 102, cZ - 5);
        modeBlock.setType(Material.WALL_SIGN);
        modeBlock.setData((byte) 3);
        org.bukkit.block.Sign modeSign = (org.bukkit.block.Sign) modeBlock.getState();
        modeSign.setLine(1, "§a§lModalità");
        modeSign.update();

        // 4. SPAWN AUTOMATICO DEI 4 NPC CON CITIZENS
        net.citizensnpcs.api.npc.NPCRegistry registry = net.citizensnpcs.api.CitizensAPI.getNPCRegistry();
        Object[][] npcs = {
                {cX - 6 + 0.5, 101.0, cZ + 6 + 0.5, -135f, "§c§l/leave", "namsar"},
                {cX - 8 + 0.5, 101.0, cZ + 3 + 0.5, -90f, "§e§lLista Build", "ShinoKage007"},
                {cX - 8 + 0.5, 101.0, cZ + 0.5, -90f, "§c§lTrova Errori", "Shiro100"},
                {cX - 8 + 0.5, 101.0, cZ - 3 + 0.5, -90f, "§b§lGuarda Build", "Lorenz223"}
        };

        for (Object[] npcData : npcs) {
            double nx = (double) npcData[0];
            double ny = (double) npcData[1];
            double nz = (double) npcData[2];
            float nyaw = (float) npcData[3];
            String nName = (String) npcData[4];
            String skinName = (String) npcData[5];

            org.bukkit.Location npcLoc = new org.bukkit.Location(practiceWorld, nx, ny, nz, nyaw, 0f);

            boolean exists = false;
            for (net.citizensnpcs.api.npc.NPC existing : registry) {
                if (existing.isSpawned() && existing.getStoredLocation().getWorld().equals(practiceWorld)) {
                    if (existing.getStoredLocation().distanceSquared(npcLoc) < 1.0) {
                        exists = true;
                        break;
                    }
                }
            }

            if (!exists) {
                net.citizensnpcs.api.npc.NPC npc = registry.createNPC(org.bukkit.entity.EntityType.PLAYER, nName);
                npc.data().set("player-skin-name", skinName);
                npc.spawn(npcLoc);
            }
        }

        org.bukkit.Location spawnIsland = new org.bukkit.Location(practiceWorld, cX + 0.5, 101, cZ + 7.5, 180f, 0f);
        player.teleport(spawnIsland);
        player.sendMessage("§bIsola §e(Plot ID: " + plotId + ") §bgenerata con successo!");
    }

    public void clearIsland(Player player) {
        int plotId = plugin.getPlotManager().getPlot(player);
        World practiceWorld = Bukkit.getWorld("practice");
        if (practiceWorld == null) return;

        Location center = plugin.getPlotManager().getPlotCenter(practiceWorld, plotId);
        int cX = center.getBlockX();
        int cZ = center.getBlockZ();

        for (int x = -15; x <= 15; x++) {
            for (int y = 85; y <= 125; y++) {
                for (int z = -15; z <= 15; z++) {
                    practiceWorld.getBlockAt(cX + x, y, cZ + z).setType(Material.AIR);
                }
            }
        }

        // 2. Rimozione NPC blindata
        try {
            net.citizensnpcs.api.npc.NPCRegistry registry = net.citizensnpcs.api.CitizensAPI.getNPCRegistry();
            java.util.List<net.citizensnpcs.api.npc.NPC> toDestroy = new java.util.ArrayList<>();

            for (net.citizensnpcs.api.npc.NPC npc : registry) {
                if (npc.isSpawned() && npc.getStoredLocation().getWorld().equals(practiceWorld)) {
                    if (npc.getStoredLocation().distanceSquared(center) < 400) {
                        toDestroy.add(npc);
                    }
                }
            }

            for (net.citizensnpcs.api.npc.NPC npc : toDestroy) {
                npc.destroy();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Errore minore con gli NPC, ma il plot verra' liberato lo stesso!");
        } finally {
            // 3. Il blocco 'finally' assicura che il plot venga SEMPRE restituito alla coda!
            plugin.getPlotManager().removePlot(player);
        }
    }

    @SuppressWarnings("deprecation")
    public void generateFloor(Player player, int buildId, String category) {
        World world = player.getWorld();
        FileConfiguration config = plugin.getGameManager().getBuildConfig(category);
        List<String> blocksData = config.getStringList("builds." + buildId + ".blocks");

        int plotId = plugin.getPlotManager().getPlot(player);
        Location centerLoc = plugin.getPlotManager().getPlotCenter(world, plotId);
        int cX = centerLoc.getBlockX(), cZ = centerLoc.getBlockZ();

        boolean useCustom = plugin.getConfig().getBoolean("players." + player.getUniqueId() + ".use_custom_floor", false);

        if (useCustom && plugin.getConfig().contains("players." + player.getUniqueId() + ".custom_floor_data")) {
            List<String> customData = plugin.getConfig().getStringList("players." + player.getUniqueId() + ".custom_floor_data");
            for (String data : customData) {
                String[] parts = data.split(";");
                if (parts.length == 4) {
                    world.getBlockAt(cX + Integer.parseInt(parts[0]), 100, cZ + Integer.parseInt(parts[1]))
                            .setTypeIdAndData(Material.valueOf(parts[2]).getId(), Byte.parseByte(parts[3]), false);
                }
            }
        } else {
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    Block floorBlock = world.getBlockAt(cX + x, 100, cZ + z);
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
                            floorBlock.setData((byte) 15);
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
                            floorBlock.setType(Material.GRASS);
                            floorBlock.setData((byte) 0);
                        }
                    } else {
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
                            floorBlock.setType(Material.GRASS);
                            floorBlock.setData((byte) 0);
                        }
                    }
                }
            }
        }
    }

    public void saveAndApplyCustomFloor(Player player) {
        World w = player.getWorld();
        java.util.List<String> floorBlocks = new java.util.ArrayList<>();

        int plotId = plugin.getPlotManager().getPlot(player);
        Location centerLoc = plugin.getPlotManager().getPlotCenter(w, plotId);
        int cX = centerLoc.getBlockX(), cZ = centerLoc.getBlockZ();

        plugin.getConfig().set("players." + player.getUniqueId() + ".use_custom_floor", true);

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                Block b101 = w.getBlockAt(cX + x, 101, cZ + z);
                Block b100 = w.getBlockAt(cX + x, 100, cZ + z);

                if (b101.getType() != Material.AIR) {
                    floorBlocks.add(x + ";" + z + ";" + b101.getType().name() + ";" + b101.getData());
                    b100.setType(b101.getType());
                    b100.setData(b101.getData());
                } else {
                    floorBlocks.add(x + ";" + z + ";GRASS;0");
                    b100.setType(Material.GRASS);
                }
            }
        }

        plugin.getConfig().set("players." + player.getUniqueId() + ".custom_floor_data", floorBlocks);
        plugin.saveConfig();
    }

    public void resetCustomFloor(Player player) {
        plugin.getConfig().set("players." + player.getUniqueId() + ".use_custom_floor", false);
        plugin.saveConfig();

        int buildId = plugin.getGameManager().getCurrentBuild(player);
        if (buildId != -1) {
            generateFloor(player, buildId, plugin.getGameManager().getCurrentCategory(player));
        } else {
            World w = player.getWorld();
            int plotId = plugin.getPlotManager().getPlot(player);
            Location centerLoc = plugin.getPlotManager().getPlotCenter(w, plotId);
            int cX = centerLoc.getBlockX(), cZ = centerLoc.getBlockZ();

            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    Block b = w.getBlockAt(cX + x, 100, cZ + z);
                    b.setType(Material.GRASS);
                    b.setData((byte) 0);
                }
            }
        }
    }
}