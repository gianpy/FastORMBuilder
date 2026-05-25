package org.fastormbuilder.plugin.util;

import org.fastormbuilder.plugin.generator.annotation.PluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.*;

public class ConfigHelper {
    private static final Logger log = LoggerFactory.getLogger(ConfigHelper.class);
    private static final Map<Class<?>, Map<String, Field>> cache = new HashMap<>();

    public static <T> T fromProperties(Properties props, Class<T> type) {
        initFields(type);
        try {
            T config = type.newInstance();
            Map<String, Field> fields = cache.get(type);
            for (Field field : fields.values()) {
                PluginConfig ann = field.getAnnotation(PluginConfig.class);
                String val = props.getProperty(ann.configKey(), ann.defaultValue());
                field.set(config, convert(val, field.getType()));
            }
            return config;
        } catch (Exception e) {
            log.error("Failed to load config", e);
            return null;
        }
    }

    public static Object getFieldByKey(Object bean, String configKey) {
        initFields(bean.getClass());
        Field field = cache.get(bean.getClass()).get(configKey);
        if (field == null) return null;
        try {
            Object val = field.get(bean);
            if (val == null || (val instanceof String && TextUtils.isBlank(val.toString()))) {
                val = convert(field.getAnnotation(PluginConfig.class).defaultValue(), field.getType());
            }
            return val;
        } catch (IllegalAccessException e) {
            log.error("Failed to get field", e);
            return null;
        }
    }

    private static void initFields(Class<?> type) {
        if (!cache.containsKey(type)) {
            Map<String, Field> map = new HashMap<>();
            for (Field f : type.getFields()) {
                PluginConfig ann = f.getAnnotation(PluginConfig.class);
                if (Modifier.isStatic(f.getModifiers()) || ann == null) continue;
                map.put(ann.configKey(), f);
            }
            cache.put(type, map);
        }
    }

    private static Object convert(String val, Class<?> type) {
        if (type.equals(String.class)) return val;
        if (TextUtils.isBlank(val)) return null;
        if (Boolean.class.equals(type)) return Boolean.valueOf(val);
        if (Integer.class.equals(type)) return Integer.valueOf(val);
        if (BigDecimal.class.equals(type)) return new BigDecimal(val);
        return null;
    }
}
