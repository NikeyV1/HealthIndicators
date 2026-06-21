package io.github.adytech99.healthindicators.fabric;

import io.github.adytech99.healthindicators.HealthIndicatorsCommon;
import io.github.adytech99.healthindicators.PingPayload;
import io.github.adytech99.healthindicators.RenderTracker;
import io.github.adytech99.healthindicators.ServerPermissionPayload;
import io.github.adytech99.healthindicators.ServerPermissions;
import io.github.adytech99.healthindicators.config.Config;
import io.github.adytech99.healthindicators.config.ModConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import static io.github.adytech99.healthindicators.HealthIndicatorsCommon.HEALTH_INDICATORS_CATEGORY;

@Environment(EnvType.CLIENT)
public class HealthIndicatorsFabric implements ClientModInitializer {

    public static final String MOD_ID = HealthIndicatorsCommon.MOD_ID;

    public static final KeyMapping HEARTS_RENDERING_ENABLED = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key." + MOD_ID + ".renderingEnabled",
            GLFW.GLFW_KEY_LEFT,
            HEALTH_INDICATORS_CATEGORY
    ));

    public static final KeyMapping ARMOR_RENDERING_ENABLED = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key." + MOD_ID + ".armorRenderingEnabled",
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            HEALTH_INDICATORS_CATEGORY
    ));

    public static final KeyMapping OVERRIDE_ALL_FILTERS = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key." + MOD_ID + ".overrideAllFilters",
            GLFW.GLFW_KEY_RIGHT,
            HEALTH_INDICATORS_CATEGORY
    ));

    public static final KeyMapping INCREASE_HEART_OFFSET = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key." + MOD_ID + ".increaseHeartOffset",
            GLFW.GLFW_KEY_UP,
            HEALTH_INDICATORS_CATEGORY
    ));

    public static final KeyMapping DECREASE_HEART_OFFSET = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key." + MOD_ID + ".decreaseHeartOffset",
            GLFW.GLFW_KEY_DOWN,
            HEALTH_INDICATORS_CATEGORY
    ));

    public static final KeyMapping OPEN_CONFIG_SCREEN = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key." + MOD_ID + ".openModMenuConfig",
            GLFW.GLFW_KEY_I,
            HEALTH_INDICATORS_CATEGORY
    ));

    @Override
    public void onInitializeClient() {
        HealthIndicatorsCommon.init();

        // ── Channel-Setup ────────────────────────────────────────────────────
        String version = net.fabricmc.loader.api.FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");

        CustomPacketPayload.Type<PingPayload> versionedId = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath("healthindicators", "v" + version.replace(".", "_"))
        );
        PayloadTypeRegistry.serverboundPlay().register(versionedId, PingPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(versionedId, PingPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(versionedId, (payload, context) -> {});

        PingPayload.VERSIONED_ID = HealthIndicatorsCommon.HANDSHAKE_CHANNEL;
        PayloadTypeRegistry.serverboundPlay().register(PingPayload.VERSIONED_ID, PingPayload.CODEC);

        PayloadTypeRegistry.clientboundPlay().register(ServerPermissionPayload.ID, ServerPermissionPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ServerPermissionPayload.ID, (payload, context) -> {
            ServerPermissions.setAllowInvisiblePlayers(payload.allowInvisiblePlayers());
            HealthIndicatorsCommon.LOGGER.info("[HealthIndicators] Server permissions received: allowInvisible=" + payload.allowInvisiblePlayers());
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ServerPermissions.reset();
            try {
                io.netty.channel.Channel ch = findFieldByType(
                        findFieldByType(handler, net.minecraft.network.Connection.class),
                        io.netty.channel.Channel.class);

                if (ch == null) throw new Exception("Channel not found");

                io.netty.channel.ChannelHandlerContext opsecCtx = ch.pipeline().context("opsec_filter");
                if (opsecCtx != null) {
                    opsecCtx.writeAndFlush(new ServerboundCustomPayloadPacket(new PingPayload()));
                    HealthIndicatorsCommon.LOGGER.info("[HealthIndicators] Handshake sent (v" + version + ")");
                } else {
                    ch.writeAndFlush(new ServerboundCustomPayloadPacket(new PingPayload()));
                    HealthIndicatorsCommon.LOGGER.info("[HealthIndicators] Handshake sent (v" + version + ")");
                }
            } catch (Exception e) {
                HealthIndicatorsCommon.LOGGER.warn("[HealthIndicators] Handshake failed: " + e.getMessage());
                sender.sendPacket(new PingPayload());
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ServerPermissions.reset();
        });

        // ── Tick / Keybinds ──────────────────────────────────────────────────
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            HealthIndicatorsCommon.tick();

            while (HEARTS_RENDERING_ENABLED.consumeClick()) {
                HealthIndicatorsCommon.enableHeartsRendering();
            }
            while (ARMOR_RENDERING_ENABLED.consumeClick()) {
                HealthIndicatorsCommon.enableArmorRendering();
            }
            while (INCREASE_HEART_OFFSET.consumeClick()) {
                HealthIndicatorsCommon.increaseOffset();
            }
            while (DECREASE_HEART_OFFSET.consumeClick()) {
                HealthIndicatorsCommon.decreaseOffset();
            }
            if (OVERRIDE_ALL_FILTERS.isDown()) {
                HealthIndicatorsCommon.overrideFilters();
            } else if (Config.getOverrideAllFiltersEnabled()) {
                HealthIndicatorsCommon.disableOverrideFilters();
            }
            if (OPEN_CONFIG_SCREEN.consumeClick()) {
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
