package com.github.fastpaths;

import com.github.fastpaths.command.FastPathsCommand;
import com.github.fastpaths.config.FastPathsConfig;
import com.github.fastpaths.listener.PlayerMoveListener;
import com.github.fastpaths.manager.PathManager;
import com.github.fastpaths.util.AttributeHelper;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class FastPathsPlugin extends JavaPlugin {

    private FastPathsConfig pluginConfig;
    private PathManager pathManager;

    @Override
    public void onEnable() {
        // 1. Load configuration
        this.pluginConfig = new FastPathsConfig(this);
        this.pluginConfig.load();

        // 2. Validate step height attribute support
        if (AttributeHelper.getStepHeightAttribute() == null) {
            getLogger().warning("Step height attribute is not available on this server version. 'path-steps' will be disabled.");
        }

        // 3. Initialize path manager
        this.pathManager = new PathManager(this);
        this.pathManager.start();

        // 4. Register listeners
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this, pathManager), this);

        // 5. Register command natively for Paper plugins (JavaPlugin#registerCommand)
        registerCommand(
                "fastpaths",
                "FastPaths plugin management command",
                List.of("fasterpaths", "fp"),
                new FastPathsCommand(this)
        );

        getLogger().info("FastPaths v" + getPluginMeta().getVersion() + " successfully enabled!");
        getLogger().info("path-speed: " + pluginConfig.getPathSpeed() + ", path-steps: " + pluginConfig.isPathSteps());
    }

    @Override
    public void onDisable() {
        if (pathManager != null) {
            pathManager.stop();
        }
        getLogger().info("FastPaths successfully disabled and all modifiers cleaned up.");
    }

    public FastPathsConfig getPluginConfig() {
        return pluginConfig;
    }

    public PathManager getPathManager() {
        return pathManager;
    }
}
