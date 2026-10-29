package com.example.autosell;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class AutoSellClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("autosell");

    private static final int MAIN_INVENTORY_SLOTS = 36; // hotbar + main inventory

    private final AutoSellConfig config = AutoSellConfig.load();
    private Map<String, Integer> lastCounts = null;
    private boolean pending = false;
    private int cooldown = 0;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) -> registerCommands(dispatcher));
    }

    // ------------------------------------------------------------------ tick logic

    private void onTick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (!config.enabled || player == null || mc.getConnection() == null) {
            lastCounts = null;
            pending = false;
            return;
        }

        Map<String, Integer> now = countItems(player);
        if (lastCounts != null) {
            for (Map.Entry<String, Integer> e : now.entrySet()) {
                int before = lastCounts.getOrDefault(e.getKey(), 0);
                if (e.getValue() > before && matches(e.getKey())) {
                    pending = true; // something we care about was picked up
                    break;
                }
            }
        }
        lastCounts = now;

        if (cooldown > 0) cooldown--;
        if (pending && cooldown <= 0) {
            mc.getConnection().sendCommand(config.command);
            pending = false;
            cooldown = config.delayTicks;
        }
    }

    private Map<String, Integer> countItems(LocalPlayer player) {
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < MAIN_INVENTORY_SLOTS; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            counts.merge(id, stack.getCount(), Integer::sum);
        }
        return counts;
    }

    private boolean matches(String id) {
        return config.items.isEmpty() || config.items.contains(id);
    }

    // ------------------------------------------------------------------ commands

    private void registerCommands(com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> d) {
        d.register(ClientCommandManager.literal("autosell")
            .executes(c -> {
                config.enabled = !config.enabled;
                config.save();
                return say(c.getSource(), "AutoSell " + (config.enabled ? "ENABLED" : "DISABLED"));
            })
            .then(ClientCommandManager.literal("status").executes(c -> say(c.getSource(),
                "enabled=" + config.enabled + ", command=/" + config.command
                    + ", delay=" + config.delayTicks + " ticks, items="
                    + (config.items.isEmpty() ? "ALL" : config.items))))
            .then(ClientCommandManager.literal("delay")
                .then(ClientCommandManager.argument("ticks", IntegerArgumentType.integer(0, 200))
                    .executes(c -> {
                        config.delayTicks = IntegerArgumentType.getInteger(c, "ticks");
                        config.save();
                        return say(c.getSource(), "Delay set to " + config.delayTicks + " ticks");
                    })))
            .then(ClientCommandManager.literal("command")
                .then(ClientCommandManager.argument("text", StringArgumentType.greedyString())
                    .executes(c -> {
                        String t = StringArgumentType.getString(c, "text").trim();
                        config.command = t.startsWith("/") ? t.substring(1) : t;
                        config.save();
                        return say(c.getSource(), "Sell command set to /" + config.command);
                    })))
            .then(ClientCommandManager.literal("add")
                .then(ClientCommandManager.argument("item", StringArgumentType.string())
                    .executes(c -> {
                        String id = normalize(StringArgumentType.getString(c, "item"));
                        if (!config.items.contains(id)) config.items.add(id);
                        config.save();
                        return say(c.getSource(), "Added " + id);
                    })))
            .then(ClientCommandManager.literal("remove")
                .then(ClientCommandManager.argument("item", StringArgumentType.string())
                    .executes(c -> {
                        String id = normalize(StringArgumentType.getString(c, "item"));
                        config.items.remove(id);
                        config.save();
                        return say(c.getSource(), "Removed " + id);
                    })))
            .then(ClientCommandManager.literal("clear").executes(c -> {
                config.items.clear();
                config.save();
                return say(c.getSource(), "Item filter cleared (triggers on ALL items)");
            })));
    }

    private static String normalize(String s) {
        s = s.toLowerCase().trim();
        return s.contains(":") ? s : "minecraft:" + s;
    }

    private static int say(FabricClientCommandSource src, String msg) {
        src.sendFeedback(Component.literal("[AutoSell] " + msg));
        return 1;
    }
}
