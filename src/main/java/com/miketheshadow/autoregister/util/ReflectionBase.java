package com.miketheshadow.autoregister.util;

import com.miketheshadow.autoregister.annotations.InjectPlugin;
import com.miketheshadow.autoregister.api.AutoRegister;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * The base for all reflection utilities
 * If you want to have access to every file in your plugin you can extend this and use it
 * to create your own reflection tool
 */
public class ReflectionBase {

    protected String packageName;
    protected Set<Class<?>> classes;
    protected Plugin plugin;
    protected boolean force = false;
    protected boolean debugLogging = false;

    protected boolean initialPackageFound = false;

    /**
     * This opens the jar file to basically read the contents.
     * I can't imagine the jar is ever unreadable, but you never know these days.
     * To get all the current packages use {@link AutoRegister#getClasses}
     *
     * @return All the classes within the package.
     */
    protected Set<Class<?>> collectAllClasses() {
        String searchName = packageName.replaceAll("[.]", "/");
        ClassLoader classLoader = plugin.getClass().getClassLoader();
        Set<Class<?>> classes = new HashSet<>();
        try {
            File currentFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            JarInputStream jarInputStream = new JarInputStream(new FileInputStream(currentFile));
            while (true) {
                ZipEntry entry = jarInputStream.getNextEntry();
                if (entry == null) break;
                String name = entry.getName();
                if (!force) {
                    String compare;
                    if (name.length() >= searchName.length()) {
                        compare = name.substring(0, searchName.length() - 1);
                        initialPackageFound = true;
                    } else {
                        compare = name;
                    }
                    if (!searchName.contains(compare) && !initialPackageFound) break;
                }

                if (name.contains(searchName) && name.endsWith(".class")) {
                    Class<?> clazz = classLoader.loadClass(name.replace(".class", "")
                            .replaceAll("/", "."));
                    try {
                        for (Field field : clazz.getDeclaredFields()) {
                            field.setAccessible(true);
                            if (field.getAnnotation(InjectPlugin.class) == null) continue;
                            field.set(clazz, plugin);
                        }
                    } catch (IllegalAccessException | IllegalArgumentException ignored) {
                    }

                    classes.add(clazz);
                }
            }
            return classes;
        } catch (Exception e) {
            e.printStackTrace();
        }
        debugLog("Total classes loaded: " + classes.size());
        return classes;
    }

    /**
     * Gets any class in the package annotated with annotation
     *
     * @param <A>        Specifies the class is an annotation.
     * @param annotation The annotation to filter the classes by.
     * @return All classes annotated with the parameter.
     */
    public <A extends Annotation> Set<Class<?>> getClassesAnnotatedWith(Class<A> annotation) {
        if (!annotation.isAnnotation()) {
            throw new IllegalStateException("Class " + annotation.getName() + " is not an annotation!");
        }
        Set<Class<?>> annotated = classes.stream().filter(clazz -> clazz.getAnnotation(annotation) != null).collect(Collectors.toSet());
        debugLog("Found: " + annotated.size() + " classes annotated with " + annotation.getName());
        return annotated;
    }

    public void debugLog(String message) {
        if (debugLogging) {
            plugin.getLogger().info(message);
        }
    }

    public void error(String message) {
        plugin.getLogger().severe(message);
    }


    /**
     * Enable debug messages.
     * Currently, the names of everything registered and the sizes/amounts of classes loaded are all that
     * is logged.
     *
     * @return The instance of {@link AutoRegister}.
     */
    public ReflectionBase enableDebugMessages() {
        this.debugLogging = true;
        return this;
    }

    /**
     * Enables the force loading of all classes.
     * In some circumstances it's possible for things to not work as intended.
     * force loading all classes will create a more significant delay on the
     * speed of loading and should only be used if listeners are not registering properly.
     *
     * @return The instance of {@link AutoRegister}.
     */
    public ReflectionBase forceLoadAllClasses() {
        this.force = true;
        return this;
    }

    /**
     * @return a set of every class in your project.
     */
    public Set<Class<?>> getClasses() {
        return classes;
    }
}
