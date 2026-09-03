package it.andrea.speedbuilders;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ScanAllMineplexCommand implements CommandExecutor {

    private final Main plugin;

    public ScanAllMineplexCommand(Main plugin) {
        this.plugin = plugin;
    }

    private static class ScannedBuild {
        String name;
        org.bukkit.World world;
        int centerX, baseY, centerZ;

        public ScannedBuild(String name, org.bukkit.World world, int centerX, int baseY, int centerZ) {
            this.name = name;
            this.world = world;
            this.centerX = centerX;
            this.baseY = baseY;
            this.centerZ = centerZ;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        int minX = -475;
        int maxX = 110;
        int minZ = -10;
        int maxZ = 515;

        player.sendMessage("§eAvvio scansione ultrarapida nel rettangolo: X(" + minX + " -> " + maxX + ") Z(" + minZ + " -> " + maxZ + ")...");
        FileConfiguration config = plugin.getMineplexConfig();

        Set<String> existingNames = new HashSet<>();
        List<ScannedBuild> foundBuilds = new ArrayList<>();
        org.bukkit.World world = player.getWorld();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Block sponge = world.getBlockAt(x, 15, z);

                if (sponge.getType() == Material.SPONGE) {
                    BlockState state = sponge.getRelative(0, 1, 0).getState();
                    if (state instanceof Sign) {
                        Sign sign = (Sign) state;
                        String buildName = (sign.getLine(0) + " " + sign.getLine(1) + " " + sign.getLine(2)).trim().replaceAll("§.", "");

                        if (!buildName.isEmpty() && !existingNames.contains(buildName.toLowerCase())) {

                            int baseY = 4; // Pavimento custom

                            foundBuilds.add(new ScannedBuild(buildName, world, sponge.getX(), baseY, sponge.getZ()));
                            existingNames.add(buildName.toLowerCase());

                            if (foundBuilds.size() % 100 == 0) {
                                player.sendMessage("§aTrovate in memoria " + foundBuilds.size() + " build...");
                            }
                        }
                    }
                }
            }
        }

        player.sendMessage("§eOrdinamento alfabetico in corso...");
        foundBuilds.sort((b1, b2) -> b1.name.compareToIgnoreCase(b2.name));

        config.set("builds", null);

        int currentId = 1;
        for (ScannedBuild b : foundBuilds) {
            saveBuildToConfig(config, currentId, b.name, b.world, b.centerX, b.baseY, b.centerZ);
            currentId++;
        }

        try {
            config.save(new File(plugin.getDataFolder(), "mineplex_builds.yml"));
            player.sendMessage("§a§lSCANSIONE COMPLETATA! §eSalvate §l" + foundBuilds.size() + "§e build alfabeticamente.");
        } catch (IOException e) { player.sendMessage("§cErrore durante il salvataggio del file!"); }

        return true;
    }

    private void saveBuildToConfig(FileConfiguration config, int id, String name, org.bukkit.World world, int centerX, int baseY, int centerZ) {
        String path = "builds." + id + ".";
        config.set(path + "name", name);
        List<String> blockList = new ArrayList<>();

        List<String> uniqueBlocks = new ArrayList<>();
        List<String> uniqueMobs = new ArrayList<>();

        // ORDINE RICHIESTO: Prima asse X (West->East), poi Y (Basso->Alto)
        for (int x = -3; x <= 3; x++) {
            for (int y = baseY; y < 15; y++) {
                for (int z = -3; z <= 3; z++) {
                    Block b = world.getBlockAt(centerX + x, y, centerZ + z);

                    if (b.getType() == Material.AIR) continue;

                    String mat = b.getType().name();
                    byte data = b.getData();

                    if (b.getType() == Material.WALL_SIGN || b.getType() == Material.SIGN_POST) {
                        Sign signState = (Sign) b.getState();
                        String text = (signState.getLine(0) + "_" + signState.getLine(1) + "_" + signState.getLine(2))
                                .replace(" ", "_").replaceAll("_+", "_").replaceAll("§.", "").trim().toUpperCase();

                        if (text.startsWith("_")) text = text.substring(1);
                        if (text.endsWith("_")) text = text.substring(0, text.length() - 1);
                        if (text.isEmpty()) text = "PIG";

                        mat = "MOB_" + text;
                        data = 0;
                    }

                    blockList.add(x + ";" + (y - baseY + 1) + ";" + z + ";" + mat + ";" + data);

                    if (y > baseY) {
                        if (mat.startsWith("MOB_")) {
                            if (!uniqueMobs.contains(mat + ";" + data)) uniqueMobs.add(mat + ";" + data);
                        } else {
                            String normMat = mat;
                            byte normData = data;
                            if (mat.equals("DAYLIGHT_DETECTOR_INVERTED")) normMat = "DAYLIGHT_DETECTOR";
                            if (mat.equals("GLOWING_REDSTONE_ORE")) normMat = "REDSTONE_ORE";
                            // Estrae il vero tipo di teschio e lo salva in SKULL_ITEM per la hotbar
                            if (mat.equals("SKULL")) {
                                normMat = "SKULL_ITEM";
                                org.bukkit.block.Skull skull = (org.bukkit.block.Skull) b.getState();
                                normData = (byte) skull.getSkullType().ordinal();
                            }
                            String key = normMat + ";" + normData;
                            if (!uniqueBlocks.contains(key)) uniqueBlocks.add(key);
                        }
                    }
                }
            }
        }

        List<String> hotbarList = new ArrayList<>();
        for(String b : uniqueBlocks) { if(hotbarList.size() < 9) hotbarList.add(b); }
        for(String m : uniqueMobs) { if(hotbarList.size() < 9) hotbarList.add(m); }
        while (hotbarList.size() < 9) hotbarList.add("AIR;0");

        config.set(path + "blocks", blockList);
        config.set(path + "hotbar", hotbarList);
    }
}