package io.github.adytech99.healthindicators.mixin;
import io.github.adytech99.healthindicators.config.Config;
import io.github.adytech99.healthindicators.config.ModConfig;
import io.github.adytech99.healthindicators.enums.ArmorTypeEnum;
import io.github.adytech99.healthindicators.enums.HealthDisplayTypeEnum;
import io.github.adytech99.healthindicators.enums.HeartTypeEnum;
import io.github.adytech99.healthindicators.RenderTracker;
import io.github.adytech99.healthindicators.util.HeartJumpData;
import io.github.adytech99.healthindicators.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.DisplaySlot;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import static io.github.adytech99.healthindicators.enums.HeartTypeEnum.addHardcoreIcon;
import static io.github.adytech99.healthindicators.enums.HeartTypeEnum.addStatusIcon;


@Mixin(LivingEntityRenderer.class)
public abstract class EntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
        extends EntityRenderer<T, S>
        implements RenderLayerParent<S, M> {

    @Unique private final Minecraft client = Minecraft.getInstance();
    @Unique private static final Identifier ICONS_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/icons.png");
    @Unique private static final java.util.WeakHashMap<LivingEntityRenderState, LivingEntity> ENTITY_MAP = new java.util.WeakHashMap<>();

    protected EntityRendererMixin(EntityRendererProvider.Context ctx) {
        super(ctx);
    }
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"), remap = false)
    public void updateRenderState(T livingEntity, S livingEntityRenderState, float f, CallbackInfo ci){
        // Store the entity in a WeakHashMap keyed by the render state
        // This prevents the health sharing bug where entities of the same type show the same health
        // Using WeakHashMap ensures render states are garbage collected when no longer needed
        ENTITY_MAP.put(livingEntityRenderState, livingEntity);
    }
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("TAIL"), remap = false)
    public void render(S livingEntityRenderState, PoseStack matrixStack, SubmitNodeCollector orderedRenderCommandQueue, CameraRenderState cameraRenderState, CallbackInfo ci) {
        // Retrieve the entity from the map
        LivingEntity livingEntity = ENTITY_MAP.get(livingEntityRenderState);

        if (livingEntity != null && (RenderTracker.isInUUIDS(livingEntity) || (Config.getOverrideAllFiltersEnabled() && !RenderTracker.isInvalid(livingEntity)))) {
            if(Config.getHeartsRenderingEnabled() || Config.getOverrideAllFiltersEnabled()) {
                if (ModConfig.HANDLER.instance().indicator_type == HealthDisplayTypeEnum.HEARTS)
                    renderHearts(livingEntity, livingEntityRenderState.bodyRot, 0, matrixStack, orderedRenderCommandQueue);
                else if (ModConfig.HANDLER.instance().indicator_type == HealthDisplayTypeEnum.NUMBER)
                    renderNumber(livingEntity, livingEntityRenderState.bodyRot, 0, matrixStack, orderedRenderCommandQueue);
                else if (ModConfig.HANDLER.instance().indicator_type == HealthDisplayTypeEnum.DYNAMIC) {
                    if (livingEntity.getMaxHealth() > ModConfig.HANDLER.instance().dynamic_health_threshold)
                        renderNumber(livingEntity, livingEntityRenderState.bodyRot, 0, matrixStack, orderedRenderCommandQueue);
                    else renderHearts(livingEntity, livingEntityRenderState.bodyRot, 0, matrixStack, orderedRenderCommandQueue);
                }
            }
            if(Config.getArmorRenderingEnabled() || Config.getOverrideAllFiltersEnabled()) renderArmorPoints(livingEntity, livingEntityRenderState.bodyRot, 0, matrixStack, orderedRenderCommandQueue);
        }
    }
    @SuppressWarnings("unchecked")
    @Unique private void renderHearts(LivingEntity livingEntity, float yaw, float tickDelta, PoseStack matrixStack, SubmitNodeCollector orderedRenderCommandQueue){
        double d = this.entityRenderDispatcher.distanceToSqr(livingEntity);
        final T entAsT = (T) livingEntity; // retained for hasLabel calls
        int healthRed = Mth.ceil(livingEntity.getHealth());
        int maxHealth = Mth.ceil(livingEntity.getMaxHealth());
        int healthYellow = Mth.ceil(livingEntity.getAbsorptionAmount());
        if(ModConfig.HANDLER.instance().percentage_based_health) {
            healthRed = Mth.ceil(((float) healthRed /maxHealth) * ModConfig.HANDLER.instance().max_health);
            maxHealth = Mth.ceil(ModConfig.HANDLER.instance().max_health);
            healthYellow = Mth.ceil(livingEntity.getAbsorptionAmount());
        }
        int heartsRed = Mth.ceil(healthRed / 2.0F);
        boolean lastRedHalf = (healthRed & 1) == 1;
        int heartsNormal = Mth.ceil(maxHealth / 2.0F);
        int heartsYellow = Mth.ceil(healthYellow / 2.0F);
        boolean lastYellowHalf = (healthYellow & 1) == 1;
        int heartsTotal = heartsNormal + heartsYellow;
        int heartsPerRow = ModConfig.HANDLER.instance().icons_per_row;
        int pixelsTotal = Math.min(heartsTotal, heartsPerRow) * 8 + 1;
        float maxX = pixelsTotal / 2.0f;
        float scale = ModConfig.getSize();
        double heartDensity = 50F - (Math.max(4F - Math.ceil((double) heartsTotal / heartsPerRow), -3F) * 5F);
        double h = 0;
        // Check if entity is obstructed by blocks
        boolean shouldRenderThroughWalls = false;
        for (int isDrawingEmpty = 0; isDrawingEmpty < 2; isDrawingEmpty++) {
            //   Order 1: Empty hearts (background)
            //   Order 2: Filled hearts (foreground)
            OrderedSubmitNodeCollector targetQueue = shouldRenderThroughWalls ?
                orderedRenderCommandQueue.order(isDrawingEmpty) :
                orderedRenderCommandQueue;

            for (int heart = 0; heart < heartsTotal; heart++) {
                if (heart % heartsPerRow == 0) {
                    h = heart / heartDensity;
                }
                matrixStack.pushPose();
                matrixStack.translate(0, livingEntity.getBbHeight() + 0.5f + h, 0);
                if (livingEntity.hasEffect(MobEffects.REGENERATION) && ModConfig.HANDLER.instance().show_heart_effects) {
                    if(HeartJumpData.getWhichHeartJumping(livingEntity) == heart){
                        matrixStack.translate(0.0D, 1.15F * scale, 0.0D);
                    }
                }
        if ((this.shouldShowName(entAsT, d)
                        || (ModConfig.HANDLER.instance().force_higher_offset_for_players && livingEntity instanceof Player && livingEntity != client.player))
                        && d <= 4096.0) {
                    matrixStack.translate(0.0D, 9.0F * 1.15F * scale, 0.0D);
                    if (d < 100.0 && livingEntity instanceof Player && livingEntity.level().getScoreboard().getDisplayObjective(DisplaySlot.BELOW_NAME) != null) {
                        matrixStack.translate(0.0D, 9.0F * 1.15F * scale, 0.0D);
                    }
                }
                matrixStack.mulPose(this.entityRenderDispatcher.camera.rotation());
                matrixStack.scale(-scale, scale, scale);
                matrixStack.translate(0, ModConfig.getDisplayOffset(), 0);
                float x = maxX - (heart % heartsPerRow) * 8;
                if (isDrawingEmpty == 0) {
                    // Create heart texture identifier
                    String additionalIconEffects = "";
                    HeartTypeEnum type = HeartTypeEnum.EMPTY;
                    Identifier heartTextureId = ModConfig.HANDLER.instance().use_vanilla_textures ?
                            Identifier.fromNamespaceAndPath("healthindicators", "textures/gui/heart/" + additionalIconEffects + type.icon + ".png") :
                            Identifier.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/heart/" + additionalIconEffects + type.icon + ".png");
                    // Get vertex consumer for this specific texture with appropriate render type
                    RenderType renderLayer;
                    if (shouldRenderThroughWalls) {
                        // Use see-through render type
                        renderLayer = RenderTypes.textSeeThrough(heartTextureId);
                    } else {
                        // Use normal text render type
                        renderLayer = RenderTypes.text(heartTextureId);
                    }
                    final HeartTypeEnum renderType = type;
                    float opacity = ModConfig.HANDLER.instance().health_bar_opacity / 100.0F;
                        targetQueue.submitCustomGeometry(matrixStack, renderLayer, (matricesEntry, vertexConsumer) -> {
                            Matrix4f m = matricesEntry.pose();
                            RenderUtils.drawHeart(m, vertexConsumer, x, renderType, livingEntity, opacity, d, shouldRenderThroughWalls);
                        });
                } else {
                    HeartTypeEnum type;
                    if (heart < heartsRed) {
                        type = HeartTypeEnum.RED_FULL;
                        if (heart == heartsRed - 1 && lastRedHalf) {
                            type = HeartTypeEnum.RED_HALF;
                        }
                    } else if (heart < heartsNormal) {
                        type = HeartTypeEnum.EMPTY;
                    } else {
                        type = HeartTypeEnum.YELLOW_FULL;
                        if (heart == heartsTotal - 1 && lastYellowHalf) {
                            type = HeartTypeEnum.YELLOW_HALF;
                        }
                    }
                    if (type != HeartTypeEnum.EMPTY) {
                        // Create heart texture identifier with effects
                        String additionalIconEffects = "";
                        if(type != HeartTypeEnum.YELLOW_FULL && type != HeartTypeEnum.YELLOW_HALF && type != HeartTypeEnum.EMPTY && ModConfig.HANDLER.instance().show_heart_effects) {
                            additionalIconEffects = (addStatusIcon(livingEntity) + addHardcoreIcon(livingEntity));
                        }
                        Identifier heartTextureId = ModConfig.HANDLER.instance().use_vanilla_textures ?
                                Identifier.fromNamespaceAndPath("healthindicators", "textures/gui/heart/" + additionalIconEffects + type.icon + ".png") :
                                Identifier.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/heart/" + additionalIconEffects + type.icon + ".png");
                        RenderType renderLayer;
                        if (shouldRenderThroughWalls) {
                            renderLayer = RenderTypes.textSeeThrough(heartTextureId);
                        } else {
                            renderLayer = RenderTypes.text(heartTextureId);
                        }
                        final HeartTypeEnum renderType = type;
                        float opacity = ModConfig.HANDLER.instance().health_bar_opacity / 100.0F;
                        targetQueue.submitCustomGeometry(matrixStack, renderLayer, (matricesEntry, vertexConsumer) -> {
                            Matrix4f m = matricesEntry.pose();
                            RenderUtils.drawHeart(m, vertexConsumer, x, renderType, livingEntity, opacity, d, shouldRenderThroughWalls);
                        });
                    }
                }
                matrixStack.popPose();
            }
        }
    }
    @SuppressWarnings("unchecked")
    @Unique
    private void renderNumber(LivingEntity livingEntity, float yaw, float tickDelta, PoseStack matrixStack, SubmitNodeCollector orderedRenderCommandQueue){
        double d = this.entityRenderDispatcher.distanceToSqr(livingEntity);
        final T entAsT = (T) livingEntity;
        String healthText = RenderUtils.getHealthText(livingEntity);
        boolean shouldRenderThroughWalls = false;
        matrixStack.pushPose();
        float scale = ModConfig.getSize();
        matrixStack.translate(0, livingEntity.getBbHeight() + 0.5f, 0);
    if ((this.shouldShowName(entAsT, d)
                || (ModConfig.HANDLER.instance().force_higher_offset_for_players && livingEntity instanceof Player && livingEntity != client.player))
                && d <= 4096.0) {
            matrixStack.translate(0.0D, 9.0F * 1.15F * scale, 0.0D);
            if (d < 100.0 && livingEntity instanceof Player && livingEntity.level().getScoreboard().getDisplayObjective(DisplaySlot.BELOW_NAME) != null) {
                matrixStack.translate(0.0D, 9.0F * 1.15F * scale, 0.0D);
            }
        }
        matrixStack.mulPose(this.entityRenderDispatcher.camera.rotation());
        matrixStack.scale(scale, -scale, scale);
        matrixStack.translate(0, ModConfig.getDisplayOffset(), 0);
        Font textRenderer = Minecraft.getInstance().font;
        float x = -textRenderer.width(healthText) / 2.0f;
    Font.DisplayMode textLayerType = shouldRenderThroughWalls ?
        Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL;
    int backgroundColor = ModConfig.HANDLER.instance().render_number_display_background_color ?
        ModConfig.HANDLER.instance().number_display_background_color.getRGB() : 0;

    int textColor = ModConfig.HANDLER.instance().number_color.getRGB();
    int opacity = ModConfig.HANDLER.instance().health_bar_opacity;
    textColor = (textColor & 0x00FFFFFF) | ((opacity * 255 / 100) << 24);

    orderedRenderCommandQueue.submitText(
        matrixStack,
        x,
        0.0F,
        Component.literal(healthText).getVisualOrderText(),
        ModConfig.HANDLER.instance().render_number_display_shadow,
        textLayerType,
        15728880,
        textColor,
        backgroundColor,
        0
    );
        matrixStack.popPose();
    }
    @SuppressWarnings("unchecked")
    @Unique private void renderArmorPoints(LivingEntity livingEntity, float yaw, float tickDelta, PoseStack matrixStack, SubmitNodeCollector orderedRenderCommandQueue){
        double d = this.entityRenderDispatcher.distanceToSqr(livingEntity);
        final T entAsT = (T) livingEntity;
        int armor = Mth.ceil(livingEntity.getArmorValue());
        int maxArmor = Mth.ceil(livingEntity.getArmorValue());
        if(maxArmor == 0) return;
        int armorPoints = Mth.ceil(armor / 2.0F);
        boolean lastPointHalf = (armor & 1) == 1;
    int pointsTotal = 10;
        int pointsPerRow = ModConfig.HANDLER.instance().icons_per_row;
        int pixelsTotal = Math.min(pointsTotal, pointsPerRow) * 8 + 1;
        float maxX = pixelsTotal / 2.0f;
        float scale = ModConfig.getSize();
        // Check if entity is obstructed by blocks
        boolean shouldRenderThroughWalls = false;
    double h = 0;

        for (int isDrawingEmpty = 0; isDrawingEmpty < 2; isDrawingEmpty++) {
            // Again, switch to ordered queues for see-through mode
            OrderedSubmitNodeCollector targetQueue = shouldRenderThroughWalls ?
                orderedRenderCommandQueue.order(isDrawingEmpty + 2) :
                orderedRenderCommandQueue;

            for (int pointCount = 0; pointCount < pointsTotal; pointCount++) {
                if (pointCount % pointsPerRow == 0) {
                    h = (scale*10)*((pointCount/2 + pointsPerRow - 1) / pointsPerRow);
                }
                matrixStack.pushPose();
                int extraHeight = (int) (((livingEntity.getMaxHealth() + livingEntity.getAbsorptionAmount())/2 + pointsPerRow - 1) / pointsPerRow);
                matrixStack.translate(0, livingEntity.getBbHeight() + 0.75f + (scale*10)*(extraHeight-1) + h, 0);
        if ((this.shouldShowName(entAsT, d)
                        || (ModConfig.HANDLER.instance().force_higher_offset_for_players && livingEntity instanceof Player && livingEntity != client.player))
                        && d <= 4096.0) {
                    matrixStack.translate(0.0D, 9.0F * 1.15F * scale, 0.0D);
                    if (d < 100.0 && livingEntity instanceof Player && livingEntity.level().getScoreboard().getDisplayObjective(DisplaySlot.BELOW_NAME) != null) {
                        matrixStack.translate(0.0D, 9.0F * 1.15F * scale, 0.0D);
                    }
                }
                matrixStack.mulPose(this.entityRenderDispatcher.camera.rotation());
                matrixStack.scale(-scale, scale, scale);
                matrixStack.translate(0, ModConfig.getDisplayOffset(), 0);
                float x = maxX - (pointCount % pointsPerRow) * 8;
                ArmorTypeEnum type = (isDrawingEmpty == 0) ? ArmorTypeEnum.EMPTY : (pointCount < armorPoints ? ((pointCount == armorPoints - 1 && lastPointHalf) ? ArmorTypeEnum.HALF : ArmorTypeEnum.FULL) : null);
                if (type != null) {
                    Identifier armorTextureId = type.icon;

                    RenderType renderLayer;
                    if (shouldRenderThroughWalls) {
                        // Use see-through render type
                        renderLayer = RenderTypes.textSeeThrough(armorTextureId);
                    } else {
                        // Use normal text render type
                        renderLayer = RenderTypes.text(armorTextureId);
                    }
                    final ArmorTypeEnum renderType = type;
                    float opacity = ModConfig.HANDLER.instance().health_bar_opacity / 100.0F;
                    targetQueue.submitCustomGeometry(matrixStack, renderLayer, (matricesEntry, vertexConsumer) -> {
                        Matrix4f m = matricesEntry.pose();
                        RenderUtils.drawArmor(m, vertexConsumer, x, renderType, opacity, d, shouldRenderThroughWalls);
                    });
                }
                matrixStack.popPose();
            }
        }
    }
}
