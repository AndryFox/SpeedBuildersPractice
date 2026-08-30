package it.andrea.speedbuilders;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Main extends JavaPlugin implements CommandExecutor {

    private HashMap<Player, String> pendingDeletes = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getCommand("map").setExecutor(this);
        getCommand("practice").setExecutor(this);
        getCommand("lobby").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        String cmdName = command.getName().toLowerCase();

        // Comando: /practice (o /p)
        if (cmdName.equals("practice") || cmdName.equals("p")) {
            if (getConfig().contains("locations.practice")) {
                Location loc = (Location) getConfig().get("locations.practice");
                player.teleport(loc);
                player.sendMessage("§aTeletrasportato all'area SpeedBuilders!");
            } else {
                player.sendMessage("§cIl punto di allenamento non è stato impostato. Usa /map setpractice");
            }
            return true;
        }

        // Comando: /lobby
        if (cmdName.equals("lobby")) {
            if (getConfig().contains("locations.lobby")) {
                Location loc = (Location) getConfig().get("locations.lobby");
                player.teleport(loc);
                player.sendMessage("§aTeletrasportato alla Lobby!");
            } else {
                player.sendMessage("§cLa lobby non è stata impostata. Usa /map setlobby");
            }
            return true;
        }

        // Comando principale: /map
        if (cmdName.equals("map")) {
            if (args.length == 0) {
                player.sendMessage("§cUso: /map <create|load|edit|rename|delete|confirm|setup|setlobby|setpractice>");
                return true;
            }

            String subCommand = args[0].toLowerCase();

            // Salva la posizione attuale come punto di spawn per la lobby
            if (subCommand.equals("setlobby")) {
                getConfig().set("locations.lobby", player.getLocation());
                saveConfig();
                player.sendMessage("§aPunto di spawn della §l/lobby§a impostato qui!");
                return true;
            }

            // Salva la posizione attuale come punto di spawn per l'allenamento
            if (subCommand.equals("setpractice")) {
                getConfig().set("locations.practice", player.getLocation());
                saveConfig();
                player.sendMessage("§aPunto di spawn di §l/practice§a impostato qui!");
                return true;
            }

            if (subCommand.equals("setup")) {
                setupIsland(player);
                return true;
            }

            if (subCommand.equals("confirm")) {
                if (pendingDeletes.containsKey(player)) {
                    String toDelete = pendingDeletes.get(player);
                    getConfig().set("builds." + toDelete, null);
                    saveConfig();
                    player.sendMessage("§aBuild '" + toDelete + "' eliminata definitivamente.");
                    pendingDeletes.remove(player);
                } else {
                    player.sendMessage("§cNessuna eliminazione in sospeso. Usa prima /map delete <nome>");
                }
                return true;
            }

            if (args.length >= 2) {
                String buildName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

                switch (subCommand) {
                    case "create":
                        if (getConfig().contains("builds." + buildName)) {
                            player.sendMessage("§cLa build esiste già! Usa '/map edit " + buildName + "'.");
                        } else {
                            saveBuild(player, buildName);
                            player.sendMessage("§aBuild '" + buildName + "' creata con successo!");
                        }
                        return true;

                    case "edit":
                        if (!getConfig().contains("builds." + buildName)) {
                            player.sendMessage("§cQuesta build non esiste. Usa /map create.");
                            return true;
                        }
                        saveBuild(player, buildName);
                        player.sendMessage("§eBuild '" + buildName + "' sovrascritta con successo!");
                        return true;

                    case "load":
                        loadBuild(player, buildName);
                        return true;

                    case "delete":
                        if (!getConfig().contains("builds." + buildName)) {
                            player.sendMessage("§cErrore: Nessuna build trovata.");
                            return true;
                        }
                        pendingDeletes.put(player, buildName);
                        player.sendMessage("§cStai per eliminare la build '" + buildName + "'. Scrivi §l/map confirm§c per confermare.");
                        return true;

                    case "rename":
                        String[] parts = buildName.split(" to ");
                        if (parts.length != 2) {
                            player.sendMessage("§cUso: /map rename <Nome Vecchio> to <Nome Nuovo>");
                            return true;
                        }
                        String oldName = parts[0].trim();
                        String newName = parts[1].trim();

                        if (!getConfig().contains("builds." + oldName)) {
                            player.sendMessage("§cErrore: Build '" + oldName + "' non trovata.");
                            return true;
                        }

                        List<String> data = getConfig().getStringList("builds." + oldName);
                        getConfig().set("builds." + newName, data);
                        getConfig().set("builds." + oldName, null);
                        saveConfig();
                        player.sendMessage("§aBuild rinominata da '" + oldName + "' a '" + newName + "'!");
                        return true;
                }
            }
        }
        return true;
    }

    private void setupIsland(Player player) {
        // Cerca il mondo creato con Multiverse
        World practiceWorld = Bukkit.getWorld("practice");
        if (practiceWorld == null) {
            player.sendMessage("§cErrore: Il mondo 'practice' non esiste. Crealo prima con /mv create practice normal -t FLAT");
            return;
        }

        // Coordinate fisse per l'isola nel mondo vuoto
        int centerX = 0;
        int centerY = 100;
        int centerZ = 0;

        // Raggio massimo dell'isola (più largo del plot 7x7)
        int maxRadius = 9;

        // Genera la forma dell'isola, restringendo il raggio di 1 blocco per ogni livello che si scende
        for (int yOffset = 0; yOffset >= -6; yOffset--) {
            int currentRadius = maxRadius + yOffset;

            for (int x = -currentRadius; x <= currentRadius; x++) {
                for (int z = -currentRadius; z <= currentRadius; z++) {
                    // Formula matematica del cerchio per non fare un quadrato
                    if (x * x + z * z <= currentRadius * currentRadius) {
                        Block b = practiceWorld.getBlockAt(centerX + x, centerY + yOffset, centerZ + z);
                        if (yOffset == 0) {
                            b.setType(Material.GRASS);
                        } else if (yOffset >= -3) {
                            b.setType(Material.DIRT);
                        } else {
                            b.setType(Material.STONE);
                        }
                    }
                }
            }
        }

        // Sostituisce il centro esatto dell'erba con il plot 7x7 in quarzo
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                Block b = practiceWorld.getBlockAt(centerX + x, centerY, centerZ + z);
                b.setType(Material.QUARTZ_BLOCK);

                // Pulisce l'aria in altezza (32 blocchi) nel caso ci fosse spazzatura generata dal mondo
                for (int y = 1; y <= 32; y++) {
                    practiceWorld.getBlockAt(centerX + x, centerY + y, centerZ + z).setType(Material.AIR);
                }
            }
        }

        // Teletrasporta il giocatore al centro e salva la posizione automaticamente
        Location spawnIsland = new Location(practiceWorld, 0.5, 101, 0.5);
        player.teleport(spawnIsland);

        getConfig().set("locations.practice", spawnIsland);
        saveConfig();

        player.sendMessage("§bIsola di allenamento generata con successo a X:0 Z:0!");
        player.sendMessage("§aIl punto di /practice è stato impostato automaticamente qui.");
    }

    private void saveBuild(Player player, String buildName) {
        Location center = player.getLocation();
        List<String> blocksData = new ArrayList<>();
        for (int x = -3; x <= 3; x++) {
            for (int y = 0; y < 32; y++) {
                for (int z = -3; z <= 3; z++) {
                    Block block = center.getWorld().getBlockAt(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z);
                    if (block.getType() != Material.AIR) {
                        byte data = block.getData();
                        blocksData.add(x + ";" + y + ";" + z + ";" + block.getType().name() + ";" + data);
                    }
                }
            }
        }
        getConfig().set("builds." + buildName, blocksData);
        saveConfig();
    }

    private void loadBuild(Player player, String buildName) {
        if (!getConfig().contains("builds." + buildName)) return;
        Location center = player.getLocation();
        for (int x = -3; x <= 3; x++) {
            for (int y = 0; y < 32; y++) {
                for (int z = -3; z <= 3; z++) {
                    Block block = center.getWorld().getBlockAt(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z);
                    block.setType(Material.AIR);
                }
            }
        }
        List<String> blocksData = getConfig().getStringList("builds." + buildName);
        for (String dataString : blocksData) {
            String[] parts = dataString.split(";");
            if (parts.length == 5) {
                try {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    int z = Integer.parseInt(parts[2]);
                    Material material = Material.valueOf(parts[3]);
                    byte data = Byte.parseByte(parts[4]);
                    Block block = center.getWorld().getBlockAt(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z);
                    block.setType(material);
                    block.setData(data);
                } catch (Exception ignored) {}
            }
        }
        player.sendMessage("§aBuild '" + buildName + "' caricata!");
    }
}