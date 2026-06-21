package io.github.adytech99.healthindicators;

import dev.architectury.event.events.client.ClientGuiEvent;
import io.github.adytech99.healthindicators.config.Config;
import io.github.adytech99.healthindicators.config.ModConfig;
import io.github.adytech99.healthindicators.util.ConfigUtils;
import io.github.adytech99.healthindicators.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HealthIndicatorsCommon {
    public static final String MOD_ID = "healthindicators";
    public static Minecraft client = Minecraft.getInstance();
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final KeyMapping.Category HEALTH_INDICATORS_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    public static final CustomPacketPayload.Type<PingPayload> HANDSHAKE_CHANNEL =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "handshake"));

    private static boolean changed = false;
    private static boolean openConfig = false;


    public static void init() {
        ModConfig.HANDLER.load();
        Config.load();
        ClientGuiEvent.RENDER_HUD.register(HealthIndicatorsCommon::onHudRender);
        client = Minecraft.getInstance();
        LOGGER.info("Never be heartless!");
    }

    public static void tick(){
        if(openConfig){
            Screen configScreen = ModConfig.createScreen(null);
            client.setScreenAndShow(configScreen);
            openConfig = false;
        }
        if(client == null || client.level == null){
            client = Minecraft.getInstance();
            return;
        }
        if(changed && client.level != null && client.level.getGameTime() % 200 == 0){
            ModConfig.HANDLER.save();
            changed = false;
        }
        RenderTracker.tick(client);
        DamageDirectionIndicatorRenderer.tick();
    }

    public static void onHudRender(GuiGraphicsExtractor drawContext1, DeltaTracker renderTickCounter1) {
        if(RenderTracker.getTrackedEntity() != null) HudRenderer.onHudRender(drawContext1, renderTickCounter1);
        if(ModConfig.HANDLER.instance().enable_damage_direction_indicators && client.hasSingleplayerServer()) DamageDirectionIndicatorRenderer.render(drawContext1, renderTickCounter1.getGameTimeDeltaTicks());
    }

    public static void openConfig(){
        try {
            openConfig = client.level != null;
        } catch (NullPointerException e) {
            openConfig = false;
        }
    }

    public static void enableHeartsRendering(){
        Config.setHeartsRenderingEnabled(!Config.getHeartsRenderingEnabled());
        if (client != null && client.player != null) {
            ChatFormatting formatting;
            if(ModConfig.HANDLER.instance().colored_messages) formatting = Config.getHeartsRenderingEnabled() ? ChatFormatting.GREEN : ChatFormatting.RED;
            else formatting = ChatFormatting.WHITE;
            ConfigUtils.sendMessage(client.player, Component.literal((Config.getHeartsRenderingEnabled() ? "Enabled" : "Disabled") + " Health Indicators").withStyle(formatting));
        }
    }

    public static void enableArmorRendering(){
        Config.setArmorRenderingEnabled(!Config.getArmorRenderingEnabled());
        if (client != null && client.player != null) {
            ChatFormatting formatting;
            if(ModConfig.HANDLER.instance().colored_messages) formatting = Config.getArmorRenderingEnabled() ? ChatFormatting.GREEN : ChatFormatting.RED;
            else formatting = ChatFormatting.WHITE;
            ConfigUtils.sendMessage(client.player, Component.literal((Config.getArmorRenderingEnabled() ? "Enabled" : "Disabled") + " Armor Indicators").withStyle(formatting));
        }
    }

    public static void increaseOffset(){
        ModConfig.HANDLER.instance().display_offset = Math.clamp(ModConfig.HANDLER.instance().display_offset + ModConfig.HANDLER.instance().offset_step_size, -5.0, 5.0);
        changed = true;
        if (client != null && client.player != null) {
            ConfigUtils.sendMessage(client.player, Component.literal("Set heart offset to " + Util.truncate(ModConfig.HANDLER.instance().display_offset,2)));
        }
    }
    public static void decreaseOffset(){
        ModConfig.HANDLER.instance().display_offset = Math.clamp(
                ModConfig.HANDLER.instance().display_offset - ModConfig.HANDLER.instance().offset_step_size,
                -5.0, 5.0);
        changed = true;
        if (client != null && client.player != null) {
            ConfigUtils.sendMessage(client.player, Component.literal("Set heart offset to " + Util.truncate(ModConfig.HANDLER.instance().display_offset,2)));
        }
    }

    public static void overrideFilters(){
        Config.setOverrideAllFiltersEnabled(true);
        if (client != null && client.player != null) {
            ConfigUtils.sendOverlayMessage(client.player, Component.literal( " Config Criteria " + (Config.getOverrideAllFiltersEnabled() ? "Temporarily Overridden" : "Re-implemented")));
        }
    }

    public static void disableOverrideFilters(){
        Config.setOverrideAllFiltersEnabled(false);
        if (client != null && client.player != null) client.player.sendOverlayMessage(Component.literal(""));
    }

    public static void openConfigScreen(){
        openConfig();
    }
}
