package it.andrea.speedbuilders;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SaveMineplexCommand implements CommandExecutor {

    private final Main plugin;

    public SaveMineplexCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage("§cUsa: /savemineplex <id>");
            return true;
        }

        int buildId;
        try {
            buildId = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cL'ID deve essere un numero valido.");
            return true;
        }

        // 1. Identifica la spugna nella colonna del giocatore (altezza ~10)
        Block sponge = null;
        for (int y = player.getLocation().getBlockY() - 2; y <= player.getLocation().getBlockY() + 1; y++) {
            Block b = player.getWorld().getBlockAt(player.getLocation().getBlockX(), y, player.getLocation().getBlockZ());
            if (b.getType() == Material.SPONGE) {
                sponge = b;
                break;
            }
        }

        if (sponge == null) {
            player.sendMessage("§cSpugna non trovata! Assicurati di essere in piedi sul blocco centrale.");
            return true;
        }

        // 2. Cerca il cartello immediatamente sopra la spugna
        Sign sign = null;
        for (int i = 1; i <= 2; i++) {
            BlockState state = sponge.getRelative(0, i, 0).getState();
            if (state instanceof Sign) {
                sign = (Sign) state;
                break;
            }
        }

        if (sign == null) {
            player.sendMessage("§cCartello non trovato sopra la spugna!");
            return true;
        }

        // Legge e pulisce il nome dal cartello
        String buildName = (sign.getLine(0) + " " + sign.getLine(1) + " " + sign.getLine(2)).trim().replaceAll("§.", "");
        if (buildName.isEmpty()) {
            player.sendMessage("§cIl cartello è vuoto!");
            return true;
        }

        player.sendMessage("§eEsportazione di '" + buildName + "' in corso...");

        File file = new File(plugin.getDataFolder(), "mineplex_builds.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // 3. Calcola il pavimento e salva la build
        // Se la spugna è a Y=10 e l'altezza totale è 8 (1 floor + 7 build), il floor parte da Y = (10 - 8) = 2.
        // Uso un calcolo dinamico per salvare tutto ciò che sta sotto la spugna fino a 8 blocchi di profondità.
        int floorY = sponge.getY() - 8;

        saveBuildToConfig(config, buildId, buildName, sponge.getWorld(), sponge.getX(), floorY, sponge.getZ(), sponge.getY());

        try {
            config.save(file);
            player.sendMessage("§aBuild §f" + buildName + " §asalvata con successo nell'ID §e" + buildId + "§a!");
        } catch (IOException e) {
            player.sendMessage("§cErrore durante la scrittura del file mineplex_builds.yml!");
            e.printStackTrace();
        }

        return true;
    }

    private void saveBuildToConfig(FileConfiguration config, int id, String name, org.bukkit.World world, int centerX, int floorY, int centerZ, int spongeY) {
        String path = "builds." + id + ".";
        config.set(path + "name", name);

        List<String> blockList = new ArrayList<>();
        Set<String> uniqueMaterials = new LinkedHashSet<>();

        // Cicla l'area 7x7
        for (int x = -3; x <= 3; x++) {
            // y parte dal pavimento (floorY) e si ferma esattamente un blocco sotto la spugna
            for (int y = floorY; y < spongeY; y++) {
                for (int z = -3; z <= 3; z++) {
                    Block b = world.getBlockAt(centerX + x, y, centerZ + z);

                    if (b.getType() == Material.AIR || b.getType() == Material.SPONGE || b.getType() == Material.WALL_SIGN || b.getType() == Material.SIGN_POST) {
                        continue; // Ignora aria, la spugna stessa e i cartelli
                    }

                    String mat = b.getType().name();
                    byte data = b.getData();

                    // Converte l'altezza assoluta del mondo in altezza relativa (1 per il pavimento, 2-8 per la build)
                    int relativeY = y - floorY + 1;

                    blockList.add(x + ";" + relativeY + ";" + z + ";" + mat + ";" + data);

                    if (uniqueMaterials.size() < 9) {
                        uniqueMaterials.add(mat + ";" + data);
                    }
                }
            }
        }

        // Crea la Hotbar
        List<String> hotbarList = new ArrayList<>(uniqueMaterials);
        while (hotbarList.size() < 9) {
            hotbarList.add("AIR;0");
        }

        config.set(path + "blocks", blockList);
        config.set(path + "hotbar", hotbarList);
    }
}