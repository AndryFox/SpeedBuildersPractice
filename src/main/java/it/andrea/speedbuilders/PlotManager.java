package it.andrea.speedbuilders;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class PlotManager {

    private final Main plugin;
    private final HashMap<UUID, Integer> playerPlots = new HashMap<>();
    private int nextPlotId = 0;

    public PlotManager(Main plugin) {
        this.plugin = plugin;
    }

    public int getPlot(Player player) {
        UUID uuid = player.getUniqueId();
        if (!playerPlots.containsKey(uuid)) {
            playerPlots.put(uuid, nextPlotId);
            nextPlotId++;
        }
        return playerPlots.get(uuid);
    }

    public Location getPlotCenter(World world, int plotId) {
        // Nessun plot a 0, 100, 0! Il centro dell'arena è riservato.
        // Partiamo direttamente dal primo anello a raggio 50.
        int currentPlot = 0;
        int ring = 1;

        while (true) {
            int plotsInRing = (int) Math.round(2 * Math.PI * ring);

            if (plotId < currentPlot + plotsInRing) {
                int positionInRing = plotId - currentPlot;

                double angle = (2 * Math.PI / plotsInRing) * positionInRing;
                double radius = ring * 50.0;

                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);

                return new Location(world, Math.floor(x) + 0.5, 100, Math.floor(z) + 0.5);
            }
            currentPlot += plotsInRing;
            ring++;
        }
    }
}