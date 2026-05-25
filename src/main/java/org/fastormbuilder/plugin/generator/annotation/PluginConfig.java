package org.fastormbuilder.plugin.generator.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PluginConfig {
    String configKey();
    String defaultValue() default "";
}
