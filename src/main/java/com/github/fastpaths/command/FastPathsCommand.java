package com.github.fastpaths.command;

import com.github.fastpaths.FastPathsPlugin;
import com.github.fastpaths.config.FastPathsConfig;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FastPathsCommand implements BasicCommand {

    private final FastPathsPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public FastPathsCommand(FastPathsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (!sender.hasPermission("fastpaths.admin")) {
            sender.sendMessage(miniMessage.deserialize("<red>You do not have permission to use this command.</red>"));
            return;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.getPluginConfig().load();
            plugin.getPathManager().reload();
            sender.sendMessage(miniMessage.deserialize("<green>[FastPaths]</green> <white>Configuration reloaded successfully!</white>"));
            return;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
            FastPathsConfig config = plugin.getPluginConfig();
            sender.sendMessage(miniMessage.deserialize("<gold>=== FastPaths Status ===</gold>"));
            sender.sendMessage(miniMessage.deserialize("<yellow>Path Speed:</yellow> <white>" + config.getPathSpeed() + "</white>"));
            sender.sendMessage(miniMessage.deserialize("<yellow>Path Steps:</yellow> <white>" + config.isPathSteps() + "</white>"));
            sender.sendMessage(miniMessage.deserialize("<yellow>Players currently on paths:</yellow> <white>" + plugin.getPathManager().getPlayersOnPath().size() + "</white>"));
            return;
        }

        sender.sendMessage(miniMessage.deserialize("<gold>=== FastPaths Help ===</gold>"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/fastpaths reload</yellow> - <white>Reload configuration</white>"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/fastpaths info</yellow> - <white>View current configuration & active stats</white>"));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (!source.getSender().hasPermission("fastpaths.admin")) {
            return Collections.emptyList();
        }

        if (args.length <= 1) {
            String input = args.length == 0 ? "" : args[0].toLowerCase();
            List<String> completions = new ArrayList<>();
            for (String sub : List.of("reload", "info", "help")) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
            return completions;
        }

        return Collections.emptyList();
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return sender.hasPermission("fastpaths.admin");
    }

    @Override
    public String permission() {
        return "fastpaths.admin";
    }
}
