package ninjaphenix.container_library.wrappers;

import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import ninjaphenix.container_library.CommonMain;
import ninjaphenix.container_library.Utils;
import ninjaphenix.container_library.config.Config;
import ninjaphenix.container_library.config.ConfigV0;
import ninjaphenix.container_library.config.Converter;
import ninjaphenix.container_library.config.LegacyFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

final class ConfigWrapperImpl implements ConfigWrapper {
    private static ConfigWrapperImpl INSTANCE;
    private Path configPath;
    private ConfigV0 config;

    public static ConfigWrapper getInstance() {
        if (ConfigWrapperImpl.INSTANCE == null) {
            ConfigWrapperImpl.INSTANCE = new ConfigWrapperImpl();
        }
        return ConfigWrapperImpl.INSTANCE;
    }

    public void initialise() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve(Utils.CONFIG_PATH);
        config = this.getConfig();
    }

    public boolean isScrollingUnrestricted() {
        return !config.isScrollingRestricted();
    }

    public void setScrollingRestricted(boolean value) {
        if (config.isScrollingRestricted() == value) {
            config.setScrollingRestricted(!value);
            this.saveConfig(config);
        }
    }

    public ResourceLocation getPreferredScreenType() {
        return config.getScreenType();
    }

    public boolean setPreferredScreenType(ResourceLocation screenType) {
        if ((Utils.UNSET_SCREEN_TYPE.equals(screenType) || Utils.PAGE_SCREEN_TYPE.equals(screenType)
                || Utils.SCROLL_SCREEN_TYPE.equals(screenType) || Utils.SINGLE_SCREEN_TYPE.equals(screenType))
                && screenType != config.getScreenType()) {
            config.setScreenType(screenType);
            this.saveConfig(config);
            return true;
        }
        return false;
    }

    private <T extends Config> void saveConfig(T config) {
        try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
            Map<String, Object> configValues = config.getConverter().toSource(config);
            configValues.put("config_version", config.getVersion());
            Utils.GSON.toJson(configValues, Utils.MAP_TYPE, Utils.GSON.newJsonWriter(writer));
        } catch (IOException e) {
            CommonMain.LOGGER.warn("Failed to save Expanded Storage's config.", e);
        }
    }

    private ConfigV0 getConfig() {
        Path oldPath = FabricLoader.getInstance().getConfigDir().resolve(Utils.FABRIC_LEGACY_CONFIG_PATH);
        boolean triedLoadingOldConfig = false;
        Path newPath = configPath;
        boolean triedLoadingNewConfig = false;
        ConfigV0 config = null;
        if (Files.exists(newPath)) {
            triedLoadingNewConfig = true;
            config = this.loadConfig(newPath, ConfigV0.Factory.INSTANCE, false);
        }
        if (Files.exists(oldPath)) {
            triedLoadingOldConfig = true;
            try (BufferedReader reader = Files.newBufferedReader(oldPath)) {
                String configLines = reader.lines().collect(Collectors.joining());
                if (config == null) {
                    ConfigV0 oldConfig = this.convertToConfig(configLines, LegacyFactory.INSTANCE, oldPath);
                    if (oldConfig != null) {
                        config = oldConfig;
                        this.saveConfig(config);
                    }
                }
                this.backupFile(oldPath, String.format("Failed to backup legacy Expanded Storage config, '%s'.", oldPath.getFileName().toString()), configLines);
            } catch (IOException e) {
                if (config == null) {
                    CommonMain.LOGGER.warn("Failed to load legacy Expanded Storage Config, new default config will be used.", e);
                }
            }
        }
        if (config == null) {
            if (triedLoadingOldConfig || triedLoadingNewConfig) {
                CommonMain.LOGGER.warn("Could not load an existing config, Expanded Storage is using it's default config.");
            }
            config = new ConfigV0();
            this.saveConfig(config);
        }
        return config;
    }

    // Tries to load a config file, returns null if loading fails.
    // Will need to be reworked to allow converting between ConfigV0 and ConfigV1
    // essentially converter will need to be decided in this method based on the value of "config_version"
    private <T extends Config> T convertToConfig(String lines, Converter<Map<String, Object>, T> converter, Path configPath) {
        try {
            Map<String, Object> configMap = Utils.GSON.fromJson(lines, Utils.MAP_TYPE);
            // Do not edit, gson returns a double, we want an int.
            // This is so fucking cursed.
            int configVersion = Mth.floor((Double) configMap.getOrDefault("config_version", (double) -1));
            if (configVersion == converter.getSourceVersion()) {
                T returnValue = converter.fromSource(configMap);
                if (returnValue.getVersion() == converter.getTargetVersion()) {
                    return returnValue;
                } else {
                    throw new IllegalStateException(String.format("CODE ERROR: Converter converted to an invalid config, expected version %s, got %s.", converter.getTargetVersion(), returnValue.getVersion()));
                }
            } else {
                throw new IllegalStateException(String.format("CODE ERROR: Converter converted to an invalid config, expected version %s, got %s.", converter.getSourceVersion(), configVersion));
            }
        } catch (JsonParseException e) {
            String configFileName = configPath.getFileName().toString();
            CommonMain.warnThrowableMessage("Failed to convert config, backing config '{}'.", e, configFileName);
            this.backupFile(configPath, String.format("Failed to backup expanded storage config which failed to read, '%s'.%n", configFileName), lines);
            return null;
        }
    }

    private void backupFile(Path path, String failureMessage, String contents) {
        try {
            Path backupPath = path.resolveSibling(path.getFileName() + new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss").format(new Date()) + ".backup");
            Files.move(path, backupPath);
        } catch (IOException e2) {
            CommonMain.LOGGER.warn(failureMessage, e2);
            if (contents != null) {
                CommonMain.LOGGER.warn(contents);
            }
        }
    }

    private <T extends Config> T loadConfig(Path configPath, Converter<Map<String, Object>, T> converter, boolean isLegacy) {
        try (BufferedReader reader = Files.newBufferedReader(configPath)) {
            String configLines = reader.lines().collect(Collectors.joining());
            return this.convertToConfig(configLines, converter, configPath);
        } catch (IOException e) {
            String configFileName = configPath.getFileName().toString();
            CommonMain.warnThrowableMessage("Failed to read {}Expanded Storage config, '{}'.", e, isLegacy ? "legacy " : "", configFileName);
            e.printStackTrace();
            this.backupFile(configPath, String.format("Failed to backup %sExpanded Storage config, '%s'.%n", isLegacy ? "legacy " : "", configFileName), null);
        }
        return null;
    }
}
