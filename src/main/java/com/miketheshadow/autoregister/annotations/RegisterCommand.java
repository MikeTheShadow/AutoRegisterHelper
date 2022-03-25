package com.miketheshadow.autoregister.annotations;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 * For auto registration of commands with plugin.yml configs
 * <p>
 * Annotation targeting command Types.
 * Set the {@link RegisterCommand#commandName()} argument to the command name
 * IE: if the command is /help -> @CommandLoader(commandName = "help").
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface RegisterCommand {

    /**
     * This is a required field.
     *
     * @return a string representing the commands name.
     */
    @NotNull
    String commandName();

}