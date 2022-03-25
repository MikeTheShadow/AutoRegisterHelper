package com.miketheshadow.autoregister.annotations;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.*;

/**
 * For auto registration of commands without plugin.yml
 * <p>
 * Annotation targeting command Types.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface RegisterFullCommand {

    @NotNull String commandName();

    @NotNull String description();

    @NotNull String usageMessage();

    @NotNull String[] aliases();

    @Nullable String permission();

    @Nullable String permissionMessage();

}
