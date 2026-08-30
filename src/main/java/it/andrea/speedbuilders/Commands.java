package it.andrea.speedbuilders;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Commands implements CommandExecutor {

    private final Main plugin;

    public Commands(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        GameManager gm = plugin.getGameManager();
        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("practice") || cmdName.equals("p")) {

            if (args.length > 0) {
                String sub = args[0].toLowerCase();
                if (sub.equals("list")) {
                    gm.clearSearch(player); // Resetta eventuali vecchie ricerche
                    gm.openBuildMenu(player, 1);
                    return true;
                } else if (sub.equals("errors")) {
                    gm.showErrors(player);
                    return true;
                } else if (sub.equals("view")) {
                    gm.viewBuild(player);
                    return true;
                }
            }

            World practiceWorld = Bukkit.getWorld("practice");
            if (practiceWorld == null) {
                player.sendMessage("§cErrore: Il mondo 'practice' non esiste.");
                return true;
            }
            gm.resetPlayer(player);
            player.teleport(new Location(practiceWorld, 0.5, 101, 5.5, 180f, 35f));
            if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                player.setAllowFlight(true);
                player.setFlying(false);
            }
            player.sendMessage("§aArea pulita! Clicca sul bordo in quarzo per ricominciare.");
            return true;
        }

        if (cmdName.equals("lobby")) {
            if (plugin.getConfig().contains("locations.lobby")) {
                player.teleport((Location) plugin.getConfig().get("locations.lobby"));
                player.sendMessage("§aTeletrasportato alla Lobby!");
            } else {
                player.sendMessage("§cLa lobby non è stata impostata. Usa /map setlobby");
            }
            return true;
        }

        if (cmdName.equals("map")) {
            if (!player.hasPermission("speedbuilders.admin")) {
                player.sendMessage("§cNon hai i permessi.");
                return true;
            }
            if (args.length == 0) {
                player.sendMessage("§cUsa: /map <save|load|update|rename|delete|setup|setlobby>");
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "setlobby":
                    plugin.getConfig().set("locations.lobby", player.getLocation());
                    plugin.saveConfig();
                    player.sendMessage("§aPunto di spawn della §l/lobby§a impostato qui!");
                    break;
                case "setup":
                    gm.setupIsland(player);
                    break;
                case "confirm":
                    if (gm.hasPendingDelete(player)) {
                        int toDelete = gm.getPendingDelete(player);
                        plugin.getConfig().set("builds." + toDelete, null);
                        plugin.saveConfig();
                        player.sendMessage("§aBuild ID '" + toDelete + "' eliminata.");
                        gm.removePendingDelete(player);
                    } else player.sendMessage("§cNessuna eliminazione in sospeso.");
                    break;
                case "save":
                    if (args.length < 2) { player.sendMessage("§cUsa: /map save <Nome>"); break; }
                    StringBuilder nameBuilder = new StringBuilder();
                    for (int i = 1; i < args.length; i++) nameBuilder.append(args[i]).append(" ");
                    int nextId = 1;
                    if (plugin.getConfig().contains("builds")) {
                        for (String key : plugin.getConfig().getConfigurationSection("builds").getKeys(false)) {
                            try { if (Integer.parseInt(key) >= nextId) nextId = Integer.parseInt(key) + 1; } catch (Exception ignored) {}
                        }
                    }
                    gm.saveBuild(player, nextId, nameBuilder.toString().trim());
                    break;
                case "rename":
                    if (args.length < 3) { player.sendMessage("§cUsa: /map rename <id> <Nome>"); break; }
                    try {
                        int id = Integer.parseInt(args[1]);
                        if (!plugin.getConfig().contains("builds." + id)) { player.sendMessage("§cID non trovato."); break; }
                        StringBuilder renameBuilder = new StringBuilder();
                        for (int i = 2; i < args.length; i++) renameBuilder.append(args[i]).append(" ");
                        String newName = renameBuilder.toString().trim();
                        plugin.getConfig().set("builds." + id + ".name", newName);
                        plugin.saveConfig();
                        player.sendMessage("§aNome cambiato in '" + newName + "'!");
                    } catch (Exception e) { player.sendMessage("§cL'ID deve essere un numero!"); }
                    break;
                case "update":
                    if (args.length < 2) { player.sendMessage("§cUsa: /map update <id>"); break; }
                    try {
                        int id = Integer.parseInt(args[1]);
                        if (!plugin.getConfig().contains("builds." + id)) { player.sendMessage("§cID non trovato."); break; }
                        gm.saveBuild(player, id, plugin.getConfig().getString("builds." + id + ".name", "Sconosciuta"));
                        player.sendMessage("§eBlocchi aggiornati!");
                    } catch (Exception e) { player.sendMessage("§cL'ID deve essere un numero!"); }
                    break;
                case "load":
                    if (args.length < 2) { player.sendMessage("§cUsa: /map load <id>"); break; }
                    try { gm.loadBuild(player, Integer.parseInt(args[1])); } catch (Exception e) { player.sendMessage("§cID invalido!"); }
                    break;
                case "delete":
                    if (args.length < 2) { player.sendMessage("§cUsa: /map delete <id>"); break; }
                    try {
                        int id = Integer.parseInt(args[1]);
                        if (!plugin.getConfig().contains("builds." + id)) { player.sendMessage("§cID non trovato."); break; }
                        gm.setPendingDelete(player, id);
                        player.sendMessage("§cStai per eliminare l'ID " + id + ". Scrivi §l/map confirm");
                    } catch (Exception e) { player.sendMessage("§cID invalido!"); }
                    break;
                default:
                    player.sendMessage("§cUsa: /map <save|load|update|rename|delete|setup|setlobby>");
            }
        }
        return true;
    }
}