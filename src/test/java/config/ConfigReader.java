package config;

import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {
    private static final Properties properties = new Properties();

    static {
        String configFile = System.getenv("CI") != null ? "config-ci.properties" : "config.properties";
        try (InputStream input = ConfigReader.class.getClassLoader().getResourceAsStream(configFile)) {
            if (input == null) {
                throw new RuntimeException("Config file not found: " + configFile);
            }
            properties.load(input);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config: " + e.getMessage());
        }
    }

    public static String get(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Config key not found: " + key);
        }
        return value;
    }

    public static String getBaseUrl() {
        return get("base.url");
    }

    public static String getBrowser() {
        return get("browser");
    }

    public static boolean isHeadless() {
        return Boolean.parseBoolean(get("headless"));
    }

    public static int getTimeout() {
        return Integer.parseInt(get("timeout"));
    }

    public static int getSlowMo() {
        return Integer.parseInt(get("slowmo"));
    }
}
