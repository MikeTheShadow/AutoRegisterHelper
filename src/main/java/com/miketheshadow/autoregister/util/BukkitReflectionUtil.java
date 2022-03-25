package com.miketheshadow.autoregister.util;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.logging.Logger;

public class BukkitReflectionUtil {

    static Logger logger = Bukkit.getLogger();

    /**
     *
     * @return CommandMap from Bukkit's SimplePluginManager
     */
    @Nullable
    public static CommandMap getCommandMap() {
        try {
            PluginManager pluginManager = Bukkit.getPluginManager();
            if (pluginManager instanceof SimplePluginManager) {
                Field field = pluginManager.getClass().getDeclaredField("commandMap");
                field.setAccessible(true);
                return (CommandMap) field.get(Bukkit.getPluginManager());
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.severe("Unable to get command map!");
            e.printStackTrace();
        }
        return null;
    }

}
