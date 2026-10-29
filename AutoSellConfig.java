package com.example.autosell;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AutoSellConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("autosell.json");

    public boolean enabled = false;
    /** Command sent WITHOUT the leading slash. */
    public String command = "sell";
    /** Ticks to wait between sell commands. 20 ticks = 1 second. 0 = as fast as possible (every tick). */
    public int delayTicks = 2;
    /** Item IDs to trigger on (e.g. "minecraft:cobblestone"). Empty list = every item. */
    public List<String> items = new ArrayList<>();

    public static AutoSellConfig load() {
        if (Files.exists(PATH)) {
            try (Reader r = Files.newBufferedReader(PATH)) {
                AutoSellConfig c = GSON.fromJson(r, AutoSellConfig.class);
                if (c != null) return c;
            } catch (IOException | RuntimeException e) {
                AutoSellClient.LOGGER.warn("Could not read config, using defaults", e);
            }
        }
        return new AutoSellConfig();
    }

    public void save() {
        try (Writer w = Files.newBufferedWriter(PATH)) {
            GSON.toJson(this, w);
        } catch (IOException e) {
            AutoSellClient.LOGGER.warn("Could not save config", e);
        }
    }
}
