package com.miketheshadow.autoregister.api;

import com.miketheshadow.autoregister.annotations.InjectPlugin;
import com.miketheshadow.autoregister.annotations.RegisterCommand;
import com.miketheshadow.autoregister.annotations.RegisterFullCommand;
import com.miketheshadow.autoregister.annotations.RegisterTabComplete;
import com.miketheshadow.autoregister.util.BukkitReflectionUtil;
import com.miketheshadow.autoregister.util.ReflectionBase;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The core class of this tool. Automatically reads the jar file and
 * attempts to register any listener and command available.
 * Commands need to be annotated with {@link RegisterCommand}
 * Listeners need to extend Spigots Listener class.
 */
public final class AutoRegister extends ReflectionBase {

    /**
     * @param plugin      Your plugin. Do not pass in another plugin but your own into this method, or
     *                    you will end up with weird/bad side effects
     * @param packageName A package path using . as the separator. Example: com.miketheshadow.autoregister
     *                    Where autoregister is the base package directory.
     *                    Do not pass in something like com. or com.name as this could have the unintended
     *                    effect of registering any library's listeners as well.
     */
    public AutoRegister(Plugin plugin, String packageName,boolean forceRegister) {
        this.plugin = plugin;
        this.packageName = packageName;
        this.force = forceRegister;
    }

    public AutoRegister(Plugin plugin, String packageName) {
        this.plugin = plugin;
        this.packageName = packageName;
    }

    public void start() {
        this.classes = collectAllClasses();
    }

    /**
     * This is what should be used by default.
     * Unless you have your own custom auto-registry this
     * method will handle the registering of commands and listeners for you.
     */
    public void defaultSetup() {
        try {
            this.classes = collectAllClasses();
            debugLog("Registering listeners...");
            registerListeners();
            debugLog("Registering commands...");
            registerCommands();
            debugLog("Registering full commands...");
            registerFullCommands();
            debugLog("Setup complete!");
        } catch (Exception e) {
            plugin.getLogger().severe("Unable to register events with message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Registers all listener classes
     */
    public void registerListeners() {
        Set<Class<?>> clazzes = getListeners();
        PluginManager manager = Bukkit.getServer().getPluginManager();
        for (Class<?> clazz : clazzes) {
            debugLog("Registering listener: " + clazz.getName());
            try {
                Listener listener = (Listener) clazz.getDeclaredConstructor().newInstance();
                manager.registerEvents(listener, plugin);
                try {
                    for (Field field : clazz.getDeclaredFields()) {
                        field.setAccessible(true);
                        if (field.getAnnotation(InjectPlugin.class) == null) continue;
                        field.set(listener, plugin);
                    }
                } catch (IllegalAccessException | IllegalArgumentException ignored) {
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                plugin.getLogger().severe("Unable to register listener: " + clazz.getName() + "!");
                e.printStackTrace();
            }
        }
    }

    /**
     * Registers all commands
     */
    public void registerCommands() {
        registerBasicCommands();
        registerFullCommands();
    }

    public void registerFullCommands() {

        Set<Class<?>> annotatedWithFullRegisterCommandSet = getClassesAnnotatedWith(RegisterFullCommand.class);

        List<Command> commandList = new ArrayList<>();

        for (Class<?> clazz : annotatedWithFullRegisterCommandSet) {
            RegisterFullCommand commandAnnotation = clazz.getAnnotation(RegisterFullCommand.class);
            if (commandAnnotation == null) continue;

            String name = commandAnnotation.commandName();
            String description = commandAnnotation.description();
            String usage = commandAnnotation.usageMessage();
            List<String> alias = List.of(commandAnnotation.aliases());

            // Nullable fields
            String permission = commandAnnotation.permission();
            String permissionMessage = commandAnnotation.permissionMessage();

            try {
                Constructor<?> constructor = clazz.getDeclaredConstructor(String.class, String.class, String.class, List.class);
                Command command = (Command) constructor.newInstance(name, description, usage, alias);
                command.setPermission(permission);
                command.setPermissionMessage(permissionMessage);
                commandList.add(command);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                error("Unable to register command " + clazz.getName() + "!");
                e.printStackTrace();
            }
        }
        CommandMap commandMap = BukkitReflectionUtil.getCommandMap();
        // Get command map throws an error therefore we just need to return here
        if (commandMap == null || commandList.isEmpty()) {
            return;
        }

        // Copying how bukkit handles command registration
        commandMap.registerAll(plugin.getDescription().getName(), commandList);
    }

    private void registerBasicCommands() {

        Set<Class<?>> annotatedWithRegisterCommand = getClassesAnnotatedWith(RegisterCommand.class);

        Set<Class<?>> annotatedWithTabComplete = getClassesAnnotatedWith(RegisterTabComplete.class);

        for (Class<?> clazz : annotatedWithRegisterCommand) {
            RegisterCommand commandAnnotation = clazz.getAnnotation(RegisterCommand.class);
            if (commandAnnotation == null) continue;
            String commandName = commandAnnotation.commandName();
            PluginCommand command = Bukkit.getServer().getPluginCommand(commandName);
            debugLog("Registering command: " + commandName + " from class " + clazz.getName());
            if (command == null) {
                throw new NotImplementedException("Missing plugin.yml registration for command: " + commandName);
            }
            CommandExecutor commandExecutor;
            try {
                // Register command
                commandExecutor = (CommandExecutor) clazz.getDeclaredConstructor().newInstance();
                command.setExecutor(commandExecutor);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                plugin.getLogger().severe("Unable to register command: " + commandName + "!");
                e.printStackTrace();
                continue;
            }
            //If there is auto complete register it
            TabCompleter tabCompleter = getTabCompleterForCommand(annotatedWithTabComplete, commandName);
            if (tabCompleter != null) command.setTabCompleter(tabCompleter);

            // Attempt to inject plugin into any instanced variables within said class
            try {
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    if (field.getAnnotation(InjectPlugin.class) == null) continue;
                    field.set(commandExecutor, plugin);
                }
            } catch (IllegalAccessException | IllegalArgumentException ignored) {
            }
        }
    }

    private TabCompleter getTabCompleterForCommand(Set<Class<?>> annotatedWithTabComplete, String command) {
        try {
            for (Class<?> clazz : annotatedWithTabComplete) {
                RegisterTabComplete registerTabComplete = clazz.getAnnotation(RegisterTabComplete.class);
                if (registerTabComplete == null || !registerTabComplete.commandName().equals(command)) continue;
                return (TabCompleter) clazz.getDeclaredConstructor().newInstance();
            }

        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * @return returns a Set of classes that implement Spigot's Listener class
     */
    public Set<Class<?>> getListeners() {
        Set<Class<?>> listeners = getClasses().stream().filter(Listener.class::isAssignableFrom).collect(Collectors.toSet());
        debugLog("Found: " + listeners.size() + " listeners.");
        return listeners;
    }

}
