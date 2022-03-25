package com.miketheshadow.autoregister.annotations;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 * For auto registration of TabCompleter
 * <p>
 * Annotation targeting command Types.
 * Set the {@link RegisterTabComplete#commandName()} argument to the command name
 * IE: if the command is /help -> @CommandLoader(commandName = "help").
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RegisterTabComplete {

    /**
     * This is a required field.
     *
     * @return a string representing the commands name.
     */
    @NotNull
    String commandName();

}
