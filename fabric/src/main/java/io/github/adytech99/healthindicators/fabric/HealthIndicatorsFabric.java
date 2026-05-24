package io.github.adytech99.healthindicators.fabric;

import io.github.adytech99.healthindicators.HealthIndicatorsCommon;
import io.github.adytech99.healthindicators.PingPayload;
import io.github.adytech99.healthindicators.RenderTracker;
import io.github.adytech99.healthindicators.config.Config;
import io.github.adytech99.healthindicators.config.ModConfig;
import io.github.adytech99.healthindicators.fabric.commands.ModCommands;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.util.Identifier;

import static io.github.adytech99.healthindicators.HealthIndicatorsCommon.HEALTH_INDICATORS_CATEGORY;

@Environment(EnvType.CLIENT)
public class HealthIndicatorsFabric implements ClientModInitializer {

    public static final String MOD_ID = HealthIndicatorsCommon.MOD_ID;

    public static final KeyBinding HEARTS_RENDERING_ENABLED = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key." + MOD_ID + ".renderingEnabled",
            InputUtil.GLFW_KEY_LEFT,
            HEALTH_INDICATORS_CATEGORY
    ));

    public static final KeyBinding ARMOR_RENDERING_ENABLED = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key." + MOD_ID + ".armorRenderingEnabled",
            InputUtil.GLFW_KEY_RIGHT_SHIFT,
            HEALTH_INDICATORS_CATEGORY
    ));

    public static final KeyBinding OVERRIDE_ALL_FILTERS = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key." + MOD_ID + ".overrideAllFilters",
            InputUtil.GLFW_KEY_RIGHT,
            HEALTH_INDICATORS_CATEGORY
    ));

    public static final KeyBinding INCREASE_HEART_OFFSET = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key." + MOD_ID + ".increaseHeartOffset",
            InputUtil.GLFW_KEY_UP,
            HEALTH_INDICATORS_CATEGORY
    ));

    public static final KeyBinding DECREASE_HEART_OFFSET = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key." + MOD_ID + ".decreaseHeartOffset",
            InputUtil.GLFW_KEY_DOWN,
            HEALTH_INDICATORS_CATEGORY
    ));

    public static final KeyBinding OPEN_CONFIG_SCREEN = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key." + MOD_ID + ".openModMenuConfig",
            InputUtil.GLFW_KEY_I,
            HEALTH_INDICATORS_CATEGORY
    ));

    @Override
    public void onInitializeClient() {
        HealthIndicatorsCommon.init();

        // ── Channel-Setup ────────────────────────────────────────────────────
        // Fixer Handshake-Channel statt versioned ID.
        // Vorteil: Server kennt immer "healthindicators:handshake" unabhängig
        // von der Client-Version. Kein String-Matching auf "v1_0_0" etc. nötig.
        PingPayload.VERSIONED_ID = HealthIndicatorsCommon.HANDSHAKE_CHANNEL;

        PayloadTypeRegistry.playC2S().register(PingPayload.VERSIONED_ID, PingPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PingPayload.VERSIONED_ID, PingPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(PingPayload.VERSIONED_ID, (payload, context) -> {});

        // ── Handshake beim Server-Join ───────────────────────────────────────
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            try {
                // Channel über Typ finden, nicht über String-Name (mapping-safe)
                io.netty.channel.Channel ch = findFieldByType(
                        findFieldByType(handler, net.minecraft.network.ClientConnection.class),
                        io.netty.channel.Channel.class);

                if (ch == null) throw new Exception("Channel nicht gefunden");

                // opsec_filter Context holen → writeAndFlush darauf = geht durch encoder, überspringt opsec
                io.netty.channel.ChannelHandlerContext opsecCtx = ch.pipeline().context("opsec_filter");
                if (opsecCtx != null) {
                    opsecCtx.writeAndFlush(new CustomPayloadC2SPacket(new PingPayload()));
                    HealthIndicatorsCommon.LOGGER.info("[HealthIndicators] Bypass via opsec_filter context");
                } else {
                    ch.writeAndFlush(new CustomPayloadC2SPacket(new PingPayload()));
                    HealthIndicatorsCommon.LOGGER.info("[HealthIndicators] Kein opsec_filter gefunden, direkt gesendet");
                }
            } catch (Exception e) {
                HealthIndicatorsCommon.LOGGER.warn("[HealthIndicators] Handshake fehlgeschlagen: " + e.getMessage());
                sender.sendPacket(new PingPayload());
            }
        });

        // ── Tick / Keybinds ──────────────────────────────────────────────────
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            HealthIndicatorsCommon.tick();

            while (HEARTS_RENDERING_ENABLED.wasPressed()) {
                HealthIndicatorsCommon.enableHeartsRendering();
            }
            while (ARMOR_RENDERING_ENABLED.wasPressed()) {
                HealthIndicatorsCommon.enableArmorRendering();
            }
            while (INCREASE_HEART_OFFSET.wasPressed()) {
                HealthIndicatorsCommon.increaseOffset();
            }
            while (DECREASE_HEART_OFFSET.wasPressed()) {
                HealthIndicatorsCommon.decreaseOffset();
            }
            if (OVERRIDE_ALL_FILTERS.isPressed()) {
                HealthIndicatorsCommon.overrideFilters();
            } else if (Config.getOverrideAllFiltersEnabled()) {
                HealthIndicatorsCommon.disableOverrideFilters();
            }
            if (OPEN_CONFIG_SCREEN.wasPressed()) {
                HealthIndicatorsCommon.openConfigScreen();
            }
        });

        // ── Entity / Lifecycle ───────────────────────────────────────────────
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            RenderTracker.removeFromUUIDS(entity);
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            ModConfig.HANDLER.save();
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T findFieldByType(Object obj, Class<T> type) throws Exception {
        if (obj == null) return null;
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
                if (type.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return (T) f.get(obj);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }
}