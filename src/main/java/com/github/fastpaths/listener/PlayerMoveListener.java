package com.github.fastpaths.listener;

import com.github.fastpaths.FastPathsPlugin;
import com.github.fastpaths.manager.PathManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class PlayerMoveListener implements Listener {

    private final FastPathsPlugin plugin;
    private final PathManager pathManager;

    public PlayerMoveListener(FastPathsPlugin plugin, PathManager pathManager) {
        this.plugin = plugin;
        this.pathManager = pathManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Optimization: ignore head rotations and micro movements within the same block.
        // This eliminates 90%+ of redundant checks with zero CPU overhead.
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        pathManager.handlePlayerMove(event.getPlayer(), to);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Location to = event.getTo();
        if (to == null) return;
        pathManager.handlePlayerTeleport(event.getPlayer(), to);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        pathManager.handlePlayerRespawn(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        pathManager.handlePlayerQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        pathManager.handlePlayerMove(event.getPlayer(), event.getPlayer().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player player) {
            pathManager.handlePlayerVehicle(player, true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onVehicleExit(VehicleExitEvent event) {
        if (event.getExited() instanceof Player player) {
            pathManager.handlePlayerVehicle(player, false);
        }
    }
}
