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
        int centerX, floorY, centerZ, spongeY;

        public ScannedBuild(String name, org.bukkit.World world, int centerX, int floorY, int centerZ, int spongeY) {
            this.name = name;
            this.world = world;
            this.centerX = centerX;
            this.floorY = floorY;
            this.centerZ = centerZ;
            this.spongeY = spongeY;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        // Limiti estratti dai waypoint del Lunar Client (con un po' di margine)
        int minX = -475;
        int maxX = 110;
        int minZ = -10;
        int maxZ = 515;

        player.sendMessage("§eAvvio scansione nel rettangolo: X(" + minX + " -> " + maxX + ") Z(" + minZ + " -> " + maxZ + ")...");
        FileConfiguration config = plugin.getMineplexConfig();

        Set<String> existingNames = new HashSet<>();
        List<ScannedBuild> foundBuilds = new ArrayList<>();

        org.bukkit.World world = player.getWorld();

        // 1. Scansiona il rettangolo esatto calcolato dai tuoi waypoint
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = 9; y <= 11; y++) {
                    Block sponge = world.getBlockAt(x, y, z);
                    if (sponge.getType() == Material.SPONGE) {
                        Sign sign = null;
                        for (int i = 1; i <= 2; i++) {
                            BlockState state = sponge.getRelative(0, i, 0).getState();
                            if (state instanceof Sign) { sign = (Sign) state; break; }
                        }

                        if (sign != null) {
                            String buildName = (sign.getLine(0) + " " + sign.getLine(1) + " " + sign.getLine(2)).trim().replaceAll("§.", "");
                            if (!buildName.isEmpty() && !existingNames.contains(buildName.toLowerCase())) {
                                int floorY = sponge.getY() - 8;

                                foundBuilds.add(new ScannedBuild(buildName, world, sponge.getX(), floorY, sponge.getZ(), sponge.getY()));
                                existingNames.add(buildName.toLowerCase());

                                if (foundBuilds.size() % 100 == 0) {
                                    player.sendMessage("§aTrovate in memoria " + foundBuilds.size() + " build...");
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Ordina alfabeticamente
        player.sendMessage("§eOrdinamento alfabetico in corso...");
        foundBuilds.sort((b1, b2) -> b1.name.compareToIgnoreCase(b2.name));

        config.set("builds", null);

        // 3. Salva sul file assegnando gli ID
        int currentId = 1;
        for (ScannedBuild b : foundBuilds) {
            saveBuildToConfig(config, currentId, b.name, b.world, b.centerX, b.floorY, b.centerZ, b.spongeY);
            currentId++;
        }

        try {
            config.save(new File(plugin.getDataFolder(), "mineplex_builds.yml"));
            player.sendMessage("§a§lSCANSIONE COMPLETATA! §eSalvate §l" + foundBuilds.size() + "§e build ordinate alfabeticamente.");
        } catch (IOException e) { player.sendMessage("§cErrore durante il salvataggio del file!"); }

        return true;
    }

    private void saveBuildToConfig(FileConfiguration config, int id, String name, org.bukkit.World world, int centerX, int floorY, int centerZ, int spongeY) {
        String path = "builds." + id + ".";
        config.set(path + "name", name);
        List<String> blockList = new ArrayList<>();
        Set<String> uniqueMaterials = new LinkedHashSet<>();

        for (int x = -3; x <= 3; x++) {
            for (int y = floorY; y < spongeY; y++) {
                for (int z = -3; z <= 3; z++) {
                    Block b = world.getBlockAt(centerX + x, y, centerZ + z);
                    if (b.getType() == Material.AIR || b.getType() == Material.SPONGE || b.getType() == Material.WALL_SIGN || b.getType() == Material.SIGN_POST) continue;

                    String mat = b.getType().name();
                    byte data = b.getData();
                    blockList.add(x + ";" + (y - floorY + 1) + ";" + z + ";" + mat + ";" + data);
                    if (uniqueMaterials.size() < 9) uniqueMaterials.add(mat + ";" + data);
                }
            }
        }

        List<String> hotbarList = new ArrayList<>(uniqueMaterials);
        while (hotbarList.size() < 9) hotbarList.add("AIR;0");
        config.set(path + "blocks", blockList);
        config.set(path + "hotbar", hotbarList);
    }
}