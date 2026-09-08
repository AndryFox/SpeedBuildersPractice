package it.andrea.speedbuilders;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MenuManager {

    private final Main plugin;

    public MenuManager(Main plugin) {
        this.plugin = plugin;
    }

    public void openCategoryMenu(Player player) {
        org.bukkit.configuration.ConfigurationSection section = plugin.getConfig().getConfigurationSection("custom_categories");

        List<String> allServers = new ArrayList<>();
        allServers.add("FearGames");
        allServers.add("Mineplex");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                if (!key.equalsIgnoreCase("FearGames") && !key.equalsIgnoreCase("Mineplex") && !key.equalsIgnoreCase("Custom")) {
                    allServers.add(key);
                }
            }
        }

        allServers.sort(String.CASE_INSENSITIVE_ORDER);

        Inventory inv = Bukkit.createInventory(null, 54, "§8Seleziona Server");

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
        ItemMeta customMeta = customItem.getItemMeta();
        customMeta.setDisplayName("§e§l" + customName);

        FileConfiguration cfgCustom = plugin.getGameManager().getBuildConfig("Custom");
        int customBuildCount = cfgCustom.contains("builds") ? cfgCustom.getConfigurationSection("builds").getKeys(false).size() : 0;

        customMeta.setLore(Arrays.asList(
                "§7IP: §f" + customIp,
                "§7Mappe totali: §e" + customBuildCount,
                "",
                "§7Clicca per sfogliare le",
                "§7build originali di " + customName + "."
        ));
        customItem.setItemMeta(customMeta);
        inv.setItem(4, customItem);

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
            ItemMeta meta = item.getItemMeta();

            if (serverKey.equalsIgnoreCase("FearGames")) meta.setDisplayName("§c§l" + name);
            else if (serverKey.equalsIgnoreCase("Mineplex")) meta.setDisplayName("§a§l" + name);
            else meta.setDisplayName("§b§l" + name);

            FileConfiguration cfg = plugin.getGameManager().getBuildConfig(serverKey);
            int buildCount = cfg.contains("builds") ? cfg.getConfigurationSection("builds").getKeys(false).size() : 0;

            meta.setLore(Arrays.asList(
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

        ItemStack filler = new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 15);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        int[] borderSlots = {
                0, 1, 2, 3, 5, 6, 7, 8,
                9, 10, 11, 12, 13, 14, 15, 16, 17,
                18, 26,
                27, 35,
                36, 44,
                45, 46, 47, 48, 49, 50, 51, 52, 53
        };

        for (int i : borderSlots) {
            inv.setItem(i, filler);
        }

        ItemStack searchBtn = new ItemStack(Material.NAME_TAG);
        ItemMeta searchMeta = searchBtn.getItemMeta();
        searchMeta.setDisplayName("§e§lCerca Build Globale");
        searchMeta.setLore(Arrays.asList("§7Clicca per cercare una", "§7build in tutti i server."));
        searchBtn.setItemMeta(searchMeta);
        inv.setItem(49, searchBtn);

        player.openInventory(inv);
    }

    public void openBuildMenu(Player player, int page, String category) {
        String filter = plugin.getGameManager().getActiveSearch(player);
        String title = filter == null ? "§8" + category + " - P. " + page : "§8Ricerca - P. " + page;
        if (title.length() > 32) title = title.substring(0, 32);

        Inventory inv = Bukkit.createInventory(null, 54, title);

        class BuildData {
            int id; String name; String cat;
            BuildData(int id, String name, String cat) { this.id = id; this.name = name; this.cat = cat; }
        }
        List<BuildData> buildList = new ArrayList<>();

        List<String> categoriesToSearch = new ArrayList<>();
        if (category.equals("Global")) {
            categoriesToSearch.add("FearGames");
            categoriesToSearch.add("Mineplex");
            categoriesToSearch.add("Custom");
            org.bukkit.configuration.ConfigurationSection section = plugin.getConfig().getConfigurationSection("custom_categories");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    if (!categoriesToSearch.contains(key)) categoriesToSearch.add(key);
                }
            }
        } else {
            categoriesToSearch.add(category);
        }

        for (String searchCat : categoriesToSearch) {
            FileConfiguration config = plugin.getGameManager().getBuildConfig(searchCat);
            if (config.contains("builds")) {
                for (String key : config.getConfigurationSection("builds").getKeys(false)) {
                    String name = config.getString("builds." + key + ".name", "Sconosciuta");
                    if (filter == null || name.toLowerCase().contains(filter.toLowerCase())) {
                        try { buildList.add(new BuildData(Integer.parseInt(key), name, searchCat)); } catch (Exception ignored) {}
                    }
                }
            }
        }

        buildList.sort((b1, b2) -> b1.name.compareToIgnoreCase(b2.name));

        int maxItemsPerPage = 45;
        int startIndex = (page - 1) * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, buildList.size());

        for (int i = startIndex; i < endIndex; i++) {
            BuildData bd = buildList.get(i);
            FileConfiguration config = plugin.getGameManager().getBuildConfig(bd.cat);

            ItemStack item = new ItemStack(Material.PAPER);
            List<String> hotbar = config.getStringList("builds." + bd.id + ".hotbar");
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
                                ItemStack normalized = ItemUtils.normalizeItem(rawMat, rawData, bd.cat);
                                if (normalized != null) item = new ItemStack(normalized.getType(), 1, normalized.getDurability());
                                else item = new ItemStack(rawMat, 1, rawData);
                            } catch (Exception ignored) {}
                        }
                        break;
                    }
                }
            }

            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§a" + bd.name);
            meta.setLore(Arrays.asList("§7ID: " + bd.id, "§7Server: §f" + bd.cat, "", "§eClicca per giocare!"));
            item.setItemMeta(meta);
            inv.setItem(i - startIndex, item);
        }

        if (page > 1) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta meta = prev.getItemMeta();
            meta.setDisplayName("§cPagina Precedente");
            prev.setItemMeta(meta);
            inv.setItem(45, prev);
        }

        if (endIndex < buildList.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta meta = next.getItemMeta();
            meta.setDisplayName("§aPagina Successiva");
            next.setItemMeta(meta);
            inv.setItem(53, next);
        }

        ItemStack backBtn = new ItemStack(Material.DARK_OAK_DOOR_ITEM);
        ItemMeta backMeta = backBtn.getItemMeta();
        backMeta.setDisplayName("§c§lTorna ai Server");
        backBtn.setItemMeta(backMeta);
        inv.setItem(48, backBtn);

        ItemStack searchBtn = new ItemStack(Material.NAME_TAG);
        ItemMeta searchMeta = searchBtn.getItemMeta();
        searchMeta.setDisplayName("§e§lCerca Build");
        if (filter == null) searchMeta.setLore(Arrays.asList("§7Tasto Sinistro: §fCerca una build"));
        else searchMeta.setLore(Arrays.asList("§7Filtro attivo: §f" + filter, "", "§7Tasto Sinistro: §fNuova ricerca", "§7Tasto Destro: §cRimuovi filtro"));
        searchBtn.setItemMeta(searchMeta);
        inv.setItem(48, searchBtn);

        ItemStack randomBtn = new ItemStack(Material.ENDER_PEARL);
        ItemMeta randomMeta = randomBtn.getItemMeta();
        randomMeta.setDisplayName("§d§lBuild Casuale");
        randomMeta.setLore(Arrays.asList("§7Tasto Sinistro (SX): §aRandom Continua", "§7Tasto Destro (DX): §eRandom Singola"));
        randomBtn.setItemMeta(randomMeta);
        inv.setItem(50, randomBtn);

        player.openInventory(inv);
    }

}