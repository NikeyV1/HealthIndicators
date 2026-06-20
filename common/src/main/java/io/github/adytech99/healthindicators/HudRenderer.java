package io.github.adytech99.healthindicators;

import io.github.adytech99.healthindicators.util.RenderUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.awt.*;


public class HudRenderer {

    private static final RandomSource random = RandomSource.create();

    public static void onHudRender(GuiGraphics drawContext, DeltaTracker deltaTracker){
        drawNumberHealthGUIIndicator(RenderTracker.getTrackedEntity(), ModConfig.HANDLER.instance().number_color, 20, 20, ModConfig.HANDLER.instance().render_number_display_shadow, drawContext);
    }

    private static void drawHeart(GuiGraphics context, HudHeartType type, int x, int y, boolean hardcore, boolean blinking, boolean half) {
        // Intentionally empty (vanilla-HUD heart drawing is disabled in this fork).
    }

    public static void drawNumberHealthGUIIndicator(LivingEntity livingEntity, Color textColor, int x, int y, boolean shadow, GuiGraphics drawContext){
        String name = String.valueOf(livingEntity.getCustomName() != null ? livingEntity.getCustomName().getString() : livingEntity.getDisplayName().getString());
        drawContext.drawString(Minecraft.getInstance().font, name, x, y, textColor.getRGB(), shadow);
        drawContext.drawString(Minecraft.getInstance().font, RenderUtils.getHealthText(livingEntity), x, y+10, textColor.getRGB(), shadow);
    }

    enum HudHeartType {
        CONTAINER(ResourceLocation.withDefaultNamespace("hud/heart/container"), ResourceLocation.withDefaultNamespace("hud/heart/container_blinking"), ResourceLocation.withDefaultNamespace("hud/heart/container"), ResourceLocation.withDefaultNamespace("hud/heart/container_blinking"), ResourceLocation.withDefaultNamespace("hud/heart/container_hardcore"), ResourceLocation.withDefaultNamespace("hud/heart/container_hardcore_blinking"), ResourceLocation.withDefaultNamespace("hud/heart/container_hardcore"), ResourceLocation.withDefaultNamespace("hud/heart/container_hardcore_blinking")),
        NORMAL(ResourceLocation.withDefaultNamespace("hud/heart/full"), ResourceLocation.withDefaultNamespace("hud/heart/full_blinking"), ResourceLocation.withDefaultNamespace("hud/heart/half"), ResourceLocation.withDefaultNamespace("hud/heart/half_blinking"), ResourceLocation.withDefaultNamespace("hud/heart/hardcore_full"), ResourceLocation.withDefaultNamespace("hud/heart/hardcore_full_blinking"), ResourceLocation.withDefaultNamespace("hud/heart/hardcore_half"), ResourceLocation.withDefaultNamespace("hud/heart/hardcore_half_blinking")),
        POISONED(ResourceLocation.withDefaultNamespace("hud/heart/poisoned_full"), ResourceLocation.withDefaultNamespace("hud/heart/poisoned_full_blinking"), ResourceLocation.withDefaultNamespace("hud/heart/poisoned_half"), ResourceLocation.withDefaultNamespace("hud/heart/poisoned_half_blinking"), ResourceLocation.withDefaultNamespace("hud/heart/poisoned_hardcore_full"), ResourceLocation.withDefaultNamespace("hud/heart/poisoned_hardcore_full_blinking"), ResourceLocation.withDefaultNamespace("hud/heart/poisoned_hardcore_half"), ResourceLocation.withDefaultNamespace("hud/heart/poisoned_hardcore_half_blinking")),
        WITHERED(ResourceLocation.withDefaultNamespace("hud/heart/withered_full"), ResourceLocation.withDefaultNamespace("hud/heart/withered_full_blinking"), ResourceLocation.withDefaultNamespace("hud/heart/withered_half"), ResourceLocation.withDefaultNamespace("hud/heart/withered_half_blinking"), ResourceLocation.withDefaultNamespace("hud/heart/withered_hardcore_full"), ResourceLocation.withDefaultNamespace("hud/heart/withered_hardcore_full_blinking"), ResourceLocation.withDefaultNamespace("hud/heart/withered_hardcore_half"), ResourceLocation.withDefaultNamespace("hud/heart/withered_hardcore_half_blinking")),
        ABSORBING(ResourceLocation.withDefaultNamespace("hud/heart/absorbing_full"), ResourceLocation.withDefaultNamespace("hud/heart/absorbing_full_blinking"), ResourceLocation.withDefaultNamespace("hud/heart/absorbing_half"), ResourceLocation.withDefaultNamespace("hud/heart/absorbing_half_blinking"), ResourceLocation.withDefaultNamespace("hud/heart/absorbing_hardcore_full"), ResourceLocation.withDefaultNamespace("hud/heart/absorbing_hardcore_full_blinking"), ResourceLocation.withDefaultNamespace("hud/heart/absorbing_hardcore_half"), ResourceLocation.withDefaultNamespace("hud/heart/absorbing_hardcore_half_blinking")),
        FROZEN(ResourceLocation.withDefaultNamespace("hud/heart/frozen_full"), ResourceLocation.withDefaultNamespace("hud/heart/frozen_full_blinking"), ResourceLocation.withDefaultNamespace("hud/heart/frozen_half"), ResourceLocation.withDefaultNamespace("hud/heart/frozen_half_blinking"), ResourceLocation.withDefaultNamespace("hud/heart/frozen_hardcore_full"), ResourceLocation.withDefaultNamespace("hud/heart/frozen_hardcore_full_blinking"), ResourceLocation.withDefaultNamespace("hud/heart/frozen_hardcore_half"), ResourceLocation.withDefaultNamespace("hud/heart/frozen_hardcore_half_blinking"));

        private final ResourceLocation fullTexture;
        private final ResourceLocation fullBlinkingTexture;
        private final ResourceLocation halfTexture;
        private final ResourceLocation halfBlinkingTexture;
        private final ResourceLocation hardcoreFullTexture;
        private final ResourceLocation hardcoreFullBlinkingTexture;
        private final ResourceLocation hardcoreHalfTexture;
        private final ResourceLocation hardcoreHalfBlinkingTexture;

        HudHeartType(final ResourceLocation fullTexture, final ResourceLocation fullBlinkingTexture, final ResourceLocation halfTexture, final ResourceLocation halfBlinkingTexture, final ResourceLocation hardcoreFullTexture, final ResourceLocation hardcoreFullBlinkingTexture, final ResourceLocation hardcoreHalfTexture, final ResourceLocation hardcoreHalfBlinkingTexture) {
            this.fullTexture = fullTexture;
            this.fullBlinkingTexture = fullBlinkingTexture;
            this.halfTexture = halfTexture;
            this.halfBlinkingTexture = halfBlinkingTexture;
            this.hardcoreFullTexture = hardcoreFullTexture;
            this.hardcoreFullBlinkingTexture = hardcoreFullBlinkingTexture;
            this.hardcoreHalfTexture = hardcoreHalfTexture;
            this.hardcoreHalfBlinkingTexture = hardcoreHalfBlinkingTexture;
        }

        public ResourceLocation getTexture(boolean hardcore, boolean half, boolean blinking) {
            if (!hardcore) {
                if (half) {
                    return blinking ? this.halfBlinkingTexture : this.halfTexture;
                } else {
                    return blinking ? this.fullBlinkingTexture : this.fullTexture;
                }
            } else if (half) {
                return blinking ? this.hardcoreHalfBlinkingTexture : this.hardcoreHalfTexture;
            } else {
                return blinking ? this.hardcoreFullBlinkingTexture : this.hardcoreFullTexture;
            }
        }

        static HudHeartType fromEntityState(LivingEntity livingEntity) {
            HudHeartType heartType;
            if (livingEntity.hasEffect(MobEffects.POISON)) {
                heartType = POISONED;
            } else if (livingEntity.hasEffect(MobEffects.WITHER)) {
                heartType = WITHERED;
            } else if (livingEntity.isFullyFrozen()) {
                heartType = FROZEN;
            } else {
                heartType = NORMAL;
            }

            return heartType;
        }
    }

}
