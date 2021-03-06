package com.ai.cloud.skywalking.conf;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.ai.cloud.skywalking.conf.Config.SkyWalking.*;

public class ConfigInitializer {
    private static Logger logger = Logger.getLogger(ConfigInitializer.class.getName());

    public static void initialize() {
        InputStream inputStream = ConfigInitializer.class.getResourceAsStream("/sky-walking.auth");
        if (inputStream == null) {
            logger.log(Level.ALL, "No provider sky-walking certification documents, sky-walking api auto shutdown.");
        } else {
            try {
                Properties properties = new Properties();
                properties.load(inputStream);
                initNextLevel(properties, Config.class, new ConfigDesc());
                AuthDesc.isAuth = Boolean.valueOf(System.getenv(AUTH_SYSTEM_ENV_NAME));
                logger.log(Level.ALL, "sky-walking auth check : " + AuthDesc.isAuth);
                if(!AuthDesc.isAuth && AUTH_OVERRIDE){
                	AuthDesc.isAuth = AUTH_OVERRIDE;
                	logger.log(Level.ALL, "sky-walking auth override: " + AuthDesc.isAuth);
                }
            } catch (IllegalAccessException e) {
                logger.log(Level.ALL, "Parsing certification file failed, sky-walking api auto shutdown.");
            } catch (IOException e) {
                logger.log(Level.ALL, "Failed to read the certification file, sky-walking api auto shutdown.");
            }
        }
    }

    private static void initNextLevel(Properties properties, Class<?> recentConfigType, ConfigDesc parentDesc) throws NumberFormatException, IllegalArgumentException, IllegalAccessException {
        for (Field field : recentConfigType.getFields()) {
            if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
                String configKey = (parentDesc + "." +
                        field.getName()).toLowerCase();
                String value = properties.getProperty(configKey);
                if (value != null) {
                    if (field.getType().equals(int.class))
                        field.set(null, Integer.valueOf(value));
                    if (field.getType().equals(String.class))
                        field.set(null, value);
                    if (field.getType().equals(long.class))
                        field.set(null, Long.valueOf(value));
                    if (field.getType().equals(boolean.class))
                        field.set(null, Boolean.valueOf(value));
                }
            }
        }
        for (Class<?> innerConfiguration : recentConfigType.getClasses()) {
            parentDesc.append(innerConfiguration.getSimpleName());
            initNextLevel(properties, innerConfiguration, parentDesc);
            parentDesc.removeLastDesc();
        }
    }
}

class ConfigDesc {
    private LinkedList<String> descs = new LinkedList<String>();

    void append(String currentDesc) {
        descs.addLast(currentDesc);
    }

    void removeLastDesc() {
        descs.removeLast();
    }

    @Override
    public String toString() {
        if (descs.size() == 0) {
            return "";
        }
        StringBuilder ret = new StringBuilder(descs.getFirst());
        boolean first = true;
        for (String desc : descs) {
            if (first) {
                first = false;
                continue;
            }
            ret.append(".").append(desc);
        }
        return ret.toString();
    }
}
