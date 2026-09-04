package it.andrea.speedbuilders;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

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

        if (cmdName.equalsIgnoreCase("gmc")) {
            if (player.hasPermission("speedbuilders.admin") || player.isOp()) {
                player.setGameMode(org.bukkit.GameMode.CREATIVE);
                player.sendMessage("§aModalità Creativa attivata!");
            }
            return true;
        }
        if (cmdName.equalsIgnoreCase("gms")) {
            if (player.hasPermission("speedbuilders.admin") || player.isOp()) {
                player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                player.sendMessage("§eModalità Sopravvivenza attivata!");
            }
            return true;
        }

        if (cmdName.equals("fly")) {
            if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
                player.sendMessage("§cSei in Creativa, il volo è già forzato dal gioco!");
                return true;
            }

            if (player.isFlying()) {
                player.setAllowFlight(false);
                player.setFlying(false);
                player.sendMessage("§cVolo disattivato.");
            } else {
                player.setAllowFlight(true);
                player.setFlying(true);
                player.sendMessage("§aVolo attivato!");

                if (plugin.getConfig().getBoolean("players." + player.getUniqueId() + ".dj", false)) {
                    plugin.getConfig().set("players." + player.getUniqueId() + ".dj", false);
                    plugin.saveConfig();
                    player.sendMessage("§8§o(Double Jump disattivato in automatico)");
                }
            }
            return true;
        }

        if (cmdName.equals("dj")) {
            if (plugin.getGameManager().getState(player).equals("PLAYING")) {
                player.sendMessage("§cNon puoi usare il Double Jump mentre giochi!");
                return true;
            }

            boolean djState = !plugin.getConfig().getBoolean("players." + player.getUniqueId() + ".dj", false);
            plugin.getConfig().set("players." + player.getUniqueId() + ".dj", djState);
            plugin.saveConfig();
            player.sendMessage("§eDouble Jump " + (djState ? "§aattivato" : "§cdisattivato") + "§e.");

            if (djState) {
                player.setAllowFlight(true);
                if (player.isFlying()) {
                    player.setFlying(false);
                    player.sendMessage("§8§o(Volo disattivato in automatico)");
                }
            } else {
                if (!player.isFlying()) {
                    player.setAllowFlight(false);
                }
            }
            return true;
        }

        if (cmdName.equals("tpworld")) {
            if (args.length == 0) {
                player.sendMessage("§cUsa: /tpworld <nome>");
                return true;
            }
            World targetWorld = Bukkit.getWorld(args[0]);
            if (targetWorld != null) {
                player.teleport(targetWorld.getSpawnLocation());
                player.sendMessage("§aTeletrasportato nel mondo: " + targetWorld.getName());
            } else {
                player.sendMessage("§cIl mondo '" + args[0] + "' non è caricato sul server!");
            }
            return true;
        }

        if (cmdName.equals("practice") || cmdName.equals("p")) {
            if (args.length > 0) {
                String sub = args[0].toLowerCase();
                if (sub.equals("list")) {
                    gm.clearSearch(player);
                    gm.openCategoryMenu(player);
                    return true;
                } else if (sub.equals("errors")) {
                    gm.showErrors(player);
                    return true;
                } else if (sub.equals("view")) {
                    gm.viewBuild(player);
                    return true;
                } else if (sub.equals("leave")) {
                    gm.resetPlayer(player);
                    player.performCommand("lobby");
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

        if (cmdName.equals("category")) {
            if (!player.isOp() && !player.hasPermission("speedbuilders.admin")) {
                player.sendMessage("§cNon hai i permessi.");
                return true;
            }
            if (args.length == 0) {
                player.sendMessage("§cUsa: /category create <IP> <Nome Server>");
                player.sendMessage("§cUsa: /category seticon <Nome Server> §7(con l'oggetto in mano)");
                return true;
            }

            if (args[0].equalsIgnoreCase("create") && args.length >= 3) {
                String ip = args[1];
                StringBuilder nameBuilder = new StringBuilder();
                for (int i = 2; i < args.length; i++) nameBuilder.append(args[i]).append(" ");
                String name = nameBuilder.toString().trim();

                plugin.getConfig().set("custom_categories." + name + ".ip", ip);
                plugin.getConfig().set("custom_categories." + name + ".name", name);
                plugin.getConfig().set("custom_categories." + name + ".icon", "STAINED_CLAY;3"); // Azzurro di default
                plugin.saveConfig();

                plugin.getGameManager().getBuildConfig(name);
                player.sendMessage("§aCategoria §l" + name + " §acreata! IP: §f" + ip);
            }
            else if (args[0].equalsIgnoreCase("seticon") && args.length >= 2) {
                StringBuilder nameBuilder = new StringBuilder();
                for (int i = 1; i < args.length; i++) nameBuilder.append(args[i]).append(" ");
                String name = nameBuilder.toString().trim();

                // Consente di modificare FearGames, Mineplex o qualsiasi server custom
                if (!name.equalsIgnoreCase("FearGames") && !name.equalsIgnoreCase("Mineplex") && !plugin.getConfig().contains("custom_categories." + name)) {
                    player.sendMessage("§cLa categoria '" + name + "' non esiste!");
                    return true;
                }

                @SuppressWarnings("deprecation")
                org.bukkit.inventory.ItemStack inHand = player.getItemInHand();
                if (inHand == null || inHand.getType() == org.bukkit.Material.AIR) {
                    player.sendMessage("§cDevi avere un blocco o oggetto in mano per impostare l'icona!");
                    return true;
                }

                String iconData = inHand.getType().name() + ";" + inHand.getDurability();
                plugin.getConfig().set("custom_categories." + name + ".icon", iconData);

                // Salva il nome anche se stiamo modificando quelli base per la prima volta
                plugin.getConfig().set("custom_categories." + name + ".name", name);
                plugin.saveConfig();
                player.sendMessage("§aIcona di §l" + name + " §aaggiornata con successo!");
            } else {
                player.sendMessage("§cUsa: /category create <IP> <Nome> oppure /category seticon <Nome>");
            }
            return true;
        }

        if (cmdName.equals("map")) {
            if (args.length == 0) {
                player.sendMessage("§8§m--------------------------------");
                player.sendMessage("§6§lGestione Mappe - SpeedBuilders");
                player.sendMessage("§e/map setup §7- Genera l'arena base.");
                player.sendMessage("§e/map save <Categoria> <Nome> §7- Salva una nuova build.");
                player.sendMessage("§e/map update <Categoria> <id> §7- Aggiorna i blocchi.");
                player.sendMessage("§e/map rename <Categoria> <id> <Nome> §7- Rinomina build.");
                player.sendMessage("§e/map delete <Categoria> <id> §7- Elimina una build.");
                player.sendMessage("§e/map load <id> <Categoria> §7- Carica una build.");
                player.sendMessage("§8§m--------------------------------");
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
                case "save":
                    if (args.length < 3) { player.sendMessage("§cUsa: /map save <Categoria> <Nome>"); break; }
                    String catSave = args[1];
                    StringBuilder nameBuilder = new StringBuilder();
                    for (int i = 2; i < args.length; i++) nameBuilder.append(args[i]).append(" ");
                    int nextId = 1;
                    org.bukkit.configuration.file.FileConfiguration cfgSave = gm.getBuildConfig(catSave);
                    if (cfgSave.contains("builds")) {
                        for (String key : cfgSave.getConfigurationSection("builds").getKeys(false)) {
                            try { if (Integer.parseInt(key) >= nextId) nextId = Integer.parseInt(key) + 1; } catch (Exception ignored) {}
                        }
                    }
                    gm.saveBuild(player, nextId, nameBuilder.toString().trim(), catSave);
                    break;
                case "rename":
                    if (args.length < 4) { player.sendMessage("§cUsa: /map rename <Categoria> <id> <Nome>"); break; }
                    try {
                        String catRen = args[1];
                        int id = Integer.parseInt(args[2]);
                        StringBuilder renameBuilder = new StringBuilder();
                        for (int i = 3; i < args.length; i++) renameBuilder.append(args[i]).append(" ");
                        String newName = renameBuilder.toString().trim();

                        org.bukkit.configuration.file.FileConfiguration cfgRen = gm.getBuildConfig(catRen);
                        if (cfgRen.contains("builds." + id)) {
                            cfgRen.set("builds." + id + ".name", newName);
                            cfgRen.save(new java.io.File(plugin.getDataFolder(), catRen.toLowerCase() + "_builds.yml"));
                            player.sendMessage("§aNome cambiato in '" + newName + "'!");
                        } else {
                            player.sendMessage("§cID non trovato in " + catRen + ".");
                        }
                    } catch (Exception e) { player.sendMessage("§cL'ID deve essere un numero!"); }
                    break;
                case "update":
                    if (args.length < 3) { player.sendMessage("§cUsa: /map update <Categoria> <id>"); break; }
                    try {
                        String catUp = args[1];
                        int id = Integer.parseInt(args[2]);
                        org.bukkit.configuration.file.FileConfiguration cfgUp = gm.getBuildConfig(catUp);
                        if (cfgUp.contains("builds." + id)) {
                            gm.saveBuild(player, id, cfgUp.getString("builds." + id + ".name", "Sconosciuta"), catUp);
                            player.sendMessage("§eBlocchi aggiornati!");
                        } else {
                            player.sendMessage("§cID non trovato in " + catUp + ".");
                        }
                    } catch (Exception e) { player.sendMessage("§cL'ID deve essere un numero!"); }
                    break;
                case "delete":
                    if (args.length < 3) { player.sendMessage("§cUsa: /map delete <Categoria> <id>"); break; }
                    try {
                        String catDel = args[1];
                        int id = Integer.parseInt(args[2]);
                        org.bukkit.configuration.file.FileConfiguration cfgDel = gm.getBuildConfig(catDel);
                        if (!cfgDel.contains("builds." + id)) {
                            player.sendMessage("§cLa build con ID " + id + " non esiste in " + catDel + ".");
                            break;
                        }
                        cfgDel.set("builds." + id, null);
                        cfgDel.save(new java.io.File(plugin.getDataFolder(), catDel.toLowerCase() + "_builds.yml"));
                        player.sendMessage("§aBuild eliminata definitivamente da " + catDel + "!");
                    } catch (Exception e) { player.sendMessage("§cID non valido."); }
                    break;
                case "load":
                    if (args.length < 3) { player.sendMessage("§cUsa: /map load <id> <Categoria>"); break; }
                    try { gm.loadBuild(player, Integer.parseInt(args[1]), args[2]); } catch (Exception e) { player.sendMessage("§cID invalido!"); }
                    break;
                case "setholo":
                    plugin.getConfig().set("locations.hologram", player.getLocation().add(0, 2, 0));
                    plugin.saveConfig();
                    plugin.getHologramManager().spawnOrUpdate();
                    player.sendMessage("§aOlogramma della Top 10 posizionato in aria!");
                    break;
                case "setexit":
                    if (!player.isOp()) return true;
                    org.bukkit.entity.Villager npc = (org.bukkit.entity.Villager) player.getWorld().spawnEntity(player.getLocation(), org.bukkit.entity.EntityType.VILLAGER);
                    npc.setCustomName("§c§lExit");
                    npc.setCustomNameVisible(true);
                    npc.setAI(false);
                    npc.setInvulnerable(true);
                    npc.setCollidable(false);
                    player.sendMessage("§aNPC Exit creato alla tua posizione!");
                    break;
                default:
                    player.sendMessage("§cUsa: /map per vedere la lista dei comandi");
            }
        }
        return true;
    }
}