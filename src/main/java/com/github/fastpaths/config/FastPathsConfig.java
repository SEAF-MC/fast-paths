package com.github.fastpaths.config;

import com.github.fastpaths.FastPathsPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class FastPathsConfig {

    private final FastPathsPlugin plugin;

    private int pathSpeed;
    private boolean pathSteps;

    public FastPathsConfig(FastPathsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.pathSpeed = Math.clamp(config.getInt("path-speed", 2), 0, 255);
        this.pathSteps = config.getBoolean("path-steps", true);
    }

    public int getPathSpeed() {
        return pathSpeed;
    }

    public boolean isPathSteps() {
        return pathSteps;
    }
}
