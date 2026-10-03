package com.github.fastpaths.manager;

import com.github.fastpaths.FastPathsPlugin;
import com.github.fastpaths.util.AttributeHelper;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PathManager {

    private final FastPathsPlugin plugin;
    private final NamespacedKey stepKey;

    // Vanilla player base step height is 0.6.
    // Adding 0.4 sets total step height to exactly 1.0, enabling stepping up between path blocks
    // (height difference of 1.0) without jumping, while preventing step-up onto full 1.0625 blocks.
    private static final double STEP_HEIGHT_BOOST = 0.4;
    private static final int POTION_DURATION_TICKS = 40; // 2 seconds
    private static final long GRACE_PERIOD_MILLIS = 1000L; // 1 second grace period on speed

    private final Set<UUID> playersOnPath = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> gracePeriodExpiry = new ConcurrentHashMap<>();
    private BukkitTask heartbeatTask;

    public PathManager(FastPathsPlugin plugin) {
        this.plugin = plugin;
        this.stepKey = new NamespacedKey(plugin, "path_step");
    }

    public void start() {
        stop();
        // Heartbeat task runs every 5 ticks (0.25 seconds) to handle grace period expirations
        // and maintain continuous speed effect for active path runners with zero flicker.
        this.heartbeatTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickHeartbeat, 5L, 5L);
    }

    public void stop() {
        if (heartbeatTask != null && !heartbeatTask.isCancelled()) {
            heartbeatTask.cancel();
            heartbeatTask = null;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            removeStepHeight(player);
            removeSpeedEffect(player);
        }
        playersOnPath.clear();
        gracePeriodExpiry.clear();
    }

    public void reload() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            removeStepHeight(player);
            removeSpeedEffect(player);
            if (isSteppingOnPath(player, player.getLocation())) {
                playersOnPath.add(player.getUniqueId());
                applyStepHeight(player);
                applySpeedEffect(player);
            } else {
                playersOnPath.remove(player.getUniqueId());
            }
        }
        gracePeriodExpiry.clear();
    }

    public void handlePlayerMove(Player player, Location to) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            UUID uuid = player.getUniqueId();
            if (playersOnPath.remove(uuid) || gracePeriodExpiry.remove(uuid) != null) {
                removeStepHeight(player);
                removeSpeedEffect(player);
            }
            return;
        }

        UUID uuid = player.getUniqueId();
        boolean wasOnPath = playersOnPath.contains(uuid);
        boolean onPath = isSteppingOnPath(player, to);

        if (onPath) {
            // Cancel any pending grace period expiration
            gracePeriodExpiry.remove(uuid);

            if (!wasOnPath) {
                playersOnPath.add(uuid);
                applyStepHeight(player);
                applySpeedEffect(player);
            }
        } else {
            if (wasOnPath) {
                playersOnPath.remove(uuid);
                // Step height is removed immediately so players cannot step up onto non-path blocks
                removeStepHeight(player);

                // Give 1-second grace period on speed so transitions / 1-block differences aren't choppy
                if (plugin.getPluginConfig().getPathSpeed() > 0) {
                    gracePeriodExpiry.put(uuid, System.currentTimeMillis() + GRACE_PERIOD_MILLIS);
                }
            }
        }
    }

    public void handlePlayerVehicle(Player player, boolean inside) {
        UUID uuid = player.getUniqueId();
        if (inside) {
            playersOnPath.remove(uuid);
            gracePeriodExpiry.remove(uuid);
            removeStepHeight(player);
            removeSpeedEffect(player);
        } else {
            handlePlayerMove(player, player.getLocation());
        }
    }

    public void handlePlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        playersOnPath.remove(uuid);
        gracePeriodExpiry.remove(uuid);
        removeStepHeight(player);
        removeSpeedEffect(player);
    }

    public void handlePlayerRespawn(Player player) {
        UUID uuid = player.getUniqueId();
        playersOnPath.remove(uuid);
        gracePeriodExpiry.remove(uuid);
        removeStepHeight(player);
        removeSpeedEffect(player);
    }

    public void handlePlayerTeleport(Player player, Location destination) {
        handlePlayerMove(player, destination);
    }

    public boolean isSteppingOnPath(Player player, Location loc) {
        if (loc == null || loc.getWorld() == null || player.isInsideVehicle()) return false;

        World world = loc.getWorld();
        int bx = loc.getBlockX();
        int bz = loc.getBlockZ();

        // 1. Direct block check at player feet (dirt path top surface is at Y + 0.9375):
        if (world.getBlockAt(bx, loc.getBlockY(), bz).getType() == Material.DIRT_PATH) {
            return true;
        }

        // 2. Supporting block check 0.5 below feet (matches Minecraft's getBlockStateOn / stepping_on):
        int belowY = (int) Math.floor(loc.getY() - 0.5);
        return world.getBlockAt(bx, belowY, bz).getType() == Material.DIRT_PATH;
    }

    private void applyStepHeight(Player player) {
        if (!plugin.getPluginConfig().isPathSteps()) return;

        Attribute stepAttr = AttributeHelper.getStepHeightAttribute();
        if (stepAttr != null) {
            AttributeInstance stepInstance = player.getAttribute(stepAttr);
            if (stepInstance != null) {
                AttributeModifier modifier = AttributeHelper.createModifier(
                        stepKey,
                        STEP_HEIGHT_BOOST,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                AttributeHelper.applyModifier(stepInstance, modifier, stepKey);
            }
        }
    }

    private void removeStepHeight(Player player) {
        Attribute stepAttr = AttributeHelper.getStepHeightAttribute();
        if (stepAttr != null) {
            AttributeInstance stepInstance = player.getAttribute(stepAttr);
            if (stepInstance != null) {
                AttributeHelper.removeModifier(stepInstance, stepKey);
            }
        }
    }

    private void applySpeedEffect(Player player) {
        int pathSpeed = plugin.getPluginConfig().getPathSpeed();
        if (pathSpeed <= 0) return;

        int amplifier = Math.clamp(pathSpeed - 1, 0, 255);

        PotionEffect currentEffect = player.getPotionEffect(PotionEffectType.SPEED);
        if (currentEffect != null) {
            // Preserve long-lasting brewed potions or beacons if equal/higher amplifier
            if (currentEffect.getDuration() > POTION_DURATION_TICKS + 10 && currentEffect.getAmplifier() >= amplifier) {
                return;
            }
        }

        PotionEffect effect = new PotionEffect(
                PotionEffectType.SPEED,
                POTION_DURATION_TICKS,
                amplifier,
                false,
                false,
                false
        );
        player.addPotionEffect(effect);
    }

    private void removeSpeedEffect(Player player) {
        PotionEffect currentEffect = player.getPotionEffect(PotionEffectType.SPEED);
        if (currentEffect != null) {
            // Only remove if this was our temporary short-lived path speed (duration <= POTION_DURATION_TICKS + 10).
            // This safely preserves legitimate player brewed potions or beacon effects.
            if (currentEffect.getDuration() <= POTION_DURATION_TICKS + 10) {
                player.removePotionEffect(PotionEffectType.SPEED);
            }
        }
    }

    private void tickHeartbeat() {
        long now = System.currentTimeMillis();
        int pathSpeed = plugin.getPluginConfig().getPathSpeed();

        // 1. Check players currently marked as on-path
        for (UUID uuid : playersOnPath) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                playersOnPath.remove(uuid);
                continue;
            }

            // Verify the player is still on a path block (e.g. block was broken or shoveled while standing still)
            if (!isSteppingOnPath(player, player.getLocation())) {
                playersOnPath.remove(uuid);
                removeStepHeight(player);
                if (pathSpeed > 0) {
                    gracePeriodExpiry.put(uuid, now + GRACE_PERIOD_MILLIS);
                }
                continue;
            }

            // Refresh the duration so the speed effect remains continuous with zero flicker
            if (pathSpeed > 0) {
                applySpeedEffect(player);
            }
        }

        // 2. Process grace period expirations
        if (!gracePeriodExpiry.isEmpty()) {
            gracePeriodExpiry.entrySet().removeIf(entry -> {
                if (now >= entry.getValue()) {
                    Player player = plugin.getServer().getPlayer(entry.getKey());
                    if (player != null && player.isOnline()) {
                        removeSpeedEffect(player);
                    }
                    return true;
                }
                return false;
            });
        }
    }

    public Set<UUID> getPlayersOnPath() {
        return Collections.unmodifiableSet(playersOnPath);
    }

    public Map<UUID, Long> getGracePeriodExpiry() {
        return Collections.unmodifiableMap(gracePeriodExpiry);
    }
}
