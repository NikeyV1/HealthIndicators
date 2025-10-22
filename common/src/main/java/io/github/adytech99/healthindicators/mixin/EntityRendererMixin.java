package io.github.adytech99.healthindicators.mixin;
import io.github.adytech99.healthindicators.config.Config;
import io.github.adytech99.healthindicators.config.ModConfig;
import io.github.adytech99.healthindicators.enums.ArmorTypeEnum;
import io.github.adytech99.healthindicators.enums.HealthDisplayTypeEnum;
import io.github.adytech99.healthindicators.enums.HeartTypeEnum;
import io.github.adytech99.healthindicators.RenderTracker;
import io.github.adytech99.healthindicators.util.HeartJumpData;
import io.github.adytech99.healthindicators.util.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
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
        implements FeatureRendererContext<S, M> {
            
    @Unique private final MinecraftClient client = MinecraftClient.getInstance();
    @Unique private static final Identifier ICONS_TEXTURE = Identifier.of("minecraft", "textures/gui/icons.png");
    @Unique private static final java.util.WeakHashMap<LivingEntityRenderState, LivingEntity> ENTITY_MAP = new java.util.WeakHashMap<>();
    
    protected EntityRendererMixin(EntityRendererFactory.Context ctx) {
        super(ctx);
    }
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    public void updateRenderState(T livingEntity, S livingEntityRenderState, float f, CallbackInfo ci){
        // Store the entity in a WeakHashMap keyed by the render state
        // This prevents the health sharing bug where entities of the same type show the same health
        // Using WeakHashMap ensures render states are garbage collected when no longer needed
        ENTITY_MAP.put(livingEntityRenderState, livingEntity);
    }
    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("TAIL"))
    public void render(S livingEntityRenderState, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState cameraRenderState, CallbackInfo ci) {
        // Retrieve the entity from the map
        LivingEntity livingEntity = ENTITY_MAP.get(livingEntityRenderState);
        
        if (livingEntity != null && (RenderTracker.isInUUIDS(livingEntity) || (Config.getOverrideAllFiltersEnabled() && !RenderTracker.isInvalid(livingEntity)))) {
            if(Config.getHeartsRenderingEnabled() || Config.getOverrideAllFiltersEnabled()) {
                if (ModConfig.HANDLER.instance().indicator_type == HealthDisplayTypeEnum.HEARTS)
                    renderHearts(livingEntity, livingEntityRenderState.bodyYaw, 0, matrixStack, orderedRenderCommandQueue);
                else if (ModConfig.HANDLER.instance().indicator_type == HealthDisplayTypeEnum.NUMBER)
                    renderNumber(livingEntity, livingEntityRenderState.bodyYaw, 0, matrixStack, orderedRenderCommandQueue);
                else if (ModConfig.HANDLER.instance().indicator_type == HealthDisplayTypeEnum.DYNAMIC) {
                    if (livingEntity.getMaxHealth() > 100)
                        renderNumber(livingEntity, livingEntityRenderState.bodyYaw, 0, matrixStack, orderedRenderCommandQueue);
                    else renderHearts(livingEntity, livingEntityRenderState.bodyYaw, 0, matrixStack, orderedRenderCommandQueue);
                }
            }
            if(Config.getArmorRenderingEnabled() || Config.getOverrideAllFiltersEnabled()) renderArmorPoints(livingEntity, livingEntityRenderState.bodyYaw, 0, matrixStack, orderedRenderCommandQueue);
        }
    }
    @SuppressWarnings("unchecked")
    @Unique private void renderHearts(LivingEntity livingEntity, float yaw, float tickDelta, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue){
        double d = this.dispatcher.getSquaredDistanceToCamera(livingEntity);
        final T entAsT = (T) livingEntity; // retained for hasLabel calls
        int healthRed = MathHelper.ceil(livingEntity.getHealth());
        int maxHealth = MathHelper.ceil(livingEntity.getMaxHealth());
        int healthYellow = MathHelper.ceil(livingEntity.getAbsorptionAmount());
        if(ModConfig.HANDLER.instance().percentage_based_health) {
            healthRed = MathHelper.ceil(((float) healthRed /maxHealth) * ModConfig.HANDLER.instance().max_health);
            maxHealth = MathHelper.ceil(ModConfig.HANDLER.instance().max_health);
            healthYellow = MathHelper.ceil(livingEntity.getAbsorptionAmount());
        }
        int heartsRed = MathHelper.ceil(healthRed / 2.0F);
        boolean lastRedHalf = (healthRed & 1) == 1;
        int heartsNormal = MathHelper.ceil(maxHealth / 2.0F);
        int heartsYellow = MathHelper.ceil(healthYellow / 2.0F);
        boolean lastYellowHalf = (healthYellow & 1) == 1;
        int heartsTotal = heartsNormal + heartsYellow;
        int heartsPerRow = ModConfig.HANDLER.instance().icons_per_row;
        int pixelsTotal = Math.min(heartsTotal, heartsPerRow) * 8 + 1;
        float maxX = pixelsTotal / 2.0f;
        float scale = ModConfig.HANDLER.instance().size;
        double heartDensity = 50F - (Math.max(4F - Math.ceil((double) heartsTotal / heartsPerRow), -3F) * 5F);
        double h = 0;
        // Check if entity is obstructed by blocks
        boolean isObstructed = ModConfig.HANDLER.instance().show_through_walls; // && isEntityObstructedByBlocks(livingEntity);
        for (int isDrawingEmpty = 0; isDrawingEmpty < 2; isDrawingEmpty++) {
            //   Order 1: Empty hearts (background)
            //   Order 2: Filled hearts (foreground)
            RenderCommandQueue targetQueue = isObstructed ? 
                orderedRenderCommandQueue.getBatchingQueue(isDrawingEmpty) : 
                orderedRenderCommandQueue;
            
            for (int heart = 0; heart < heartsTotal; heart++) {
                if (heart % heartsPerRow == 0) {
                    h = heart / heartDensity;
                }
                matrixStack.push();
                matrixStack.translate(0, livingEntity.getHeight() + 0.5f + h, 0);
                if (livingEntity.hasStatusEffect(StatusEffects.REGENERATION) && ModConfig.HANDLER.instance().show_heart_effects) {
                    if(HeartJumpData.getWhichHeartJumping(livingEntity) == heart){
                        matrixStack.translate(0.0D, 1.15F * scale, 0.0D);
                    }
                }
        if ((this.hasLabel(entAsT, d)
                        || (ModConfig.HANDLER.instance().force_higher_offset_for_players && livingEntity instanceof PlayerEntity && livingEntity != client.player))
                        && d <= 4096.0) {
                    matrixStack.translate(0.0D, 9.0F * 1.15F * scale, 0.0D);
                    if (d < 100.0 && livingEntity instanceof PlayerEntity && livingEntity.getEntityWorld().getScoreboard().getObjectiveForSlot(net.minecraft.scoreboard.ScoreboardDisplaySlot.BELOW_NAME) != null) {
                        matrixStack.translate(0.0D, 9.0F * 1.15F * scale, 0.0D);
                    }
                }
                matrixStack.multiply(this.dispatcher.camera.getRotation());
                matrixStack.scale(-scale, scale, scale);
                matrixStack.translate(0, ModConfig.HANDLER.instance().display_offset, 0);
                float x = maxX - (heart % heartsPerRow) * 8;
                if (isDrawingEmpty == 0) {
                    // Create heart texture identifier
                    String additionalIconEffects = "";
                    HeartTypeEnum type = HeartTypeEnum.EMPTY;
                    Identifier heartTextureId = ModConfig.HANDLER.instance().use_vanilla_textures ?
                            Identifier.of("healthindicators", "textures/gui/heart/" + additionalIconEffects + type.icon + ".png") :
                            Identifier.of("minecraft", "textures/gui/sprites/hud/heart/" + additionalIconEffects + type.icon + ".png");
                    // Get vertex consumer for this specific texture with appropriate render layer
                    RenderLayer renderLayer;
                    if (isObstructed) {
                        // Use see-through render layer
                        renderLayer = RenderLayer.getTextSeeThrough(heartTextureId);
                    } else {
                        // Use normal text render layer
                        renderLayer = RenderLayer.getText(heartTextureId);
                    }
                    final HeartTypeEnum renderType = type;
                    float opacity = ModConfig.HANDLER.instance().health_bar_opacity / 100.0F;
                        targetQueue.submitCustom(matrixStack, renderLayer, (matricesEntry, vertexConsumer) -> {
                            Matrix4f m = matricesEntry.getPositionMatrix();
                            RenderUtils.drawHeart(m, vertexConsumer, x, renderType, livingEntity, opacity, d, isObstructed);
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
                                Identifier.of("healthindicators", "textures/gui/heart/" + additionalIconEffects + type.icon + ".png") :
                                Identifier.of("minecraft", "textures/gui/sprites/hud/heart/" + additionalIconEffects + type.icon + ".png");
                        RenderLayer renderLayer;
                        if (isObstructed) {
                            renderLayer = RenderLayer.getTextSeeThrough(heartTextureId);
                        } else {
                            renderLayer = RenderLayer.getText(heartTextureId);
                        }
                        final HeartTypeEnum renderType = type;
                        float opacity = ModConfig.HANDLER.instance().health_bar_opacity / 100.0F;
                        targetQueue.submitCustom(matrixStack, renderLayer, (matricesEntry, vertexConsumer) -> {
                            Matrix4f m = matricesEntry.getPositionMatrix();
                            RenderUtils.drawHeart(m, vertexConsumer, x, renderType, livingEntity, opacity, d, isObstructed);
                        });
                    }
                }
                matrixStack.pop();
            }
        }
    }
    @SuppressWarnings("unchecked")
    @Unique
    private void renderNumber(LivingEntity livingEntity, float yaw, float tickDelta, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue){
        double d = this.dispatcher.getSquaredDistanceToCamera(livingEntity);
        final T entAsT = (T) livingEntity;
        String healthText = RenderUtils.getHealthText(livingEntity);
        boolean isObstructed = ModConfig.HANDLER.instance().show_through_walls;
        matrixStack.push();
        float scale = ModConfig.HANDLER.instance().size;
        matrixStack.translate(0, livingEntity.getHeight() + 0.5f, 0);
    if ((this.hasLabel(entAsT, d)
                || (ModConfig.HANDLER.instance().force_higher_offset_for_players && livingEntity instanceof PlayerEntity && livingEntity != client.player))
                && d <= 4096.0) {
            matrixStack.translate(0.0D, 9.0F * 1.15F * scale, 0.0D);
            if (d < 100.0 && livingEntity instanceof PlayerEntity && livingEntity.getEntityWorld().getScoreboard().getObjectiveForSlot(net.minecraft.scoreboard.ScoreboardDisplaySlot.BELOW_NAME) != null) {
                matrixStack.translate(0.0D, 9.0F * 1.15F * scale, 0.0D);
            }
        }
        matrixStack.multiply(this.dispatcher.camera.getRotation());
        matrixStack.scale(scale, -scale, scale);
        matrixStack.translate(0, -ModConfig.HANDLER.instance().display_offset, 0);
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        float x = -textRenderer.getWidth(healthText) / 2.0f;
    TextRenderer.TextLayerType textLayerType = isObstructed ?
        TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL;
    int backgroundColor = ModConfig.HANDLER.instance().render_number_display_background_color ?
        ModConfig.HANDLER.instance().number_display_background_color.getRGB() : 0;

    int textColor = ModConfig.HANDLER.instance().number_color.getRGB();
    int opacity = ModConfig.HANDLER.instance().health_bar_opacity;
    textColor = (textColor & 0x00FFFFFF) | ((opacity * 255 / 100) << 24);

    orderedRenderCommandQueue.submitText(
        matrixStack,
        x,
        0.0F,
        Text.literal(healthText).asOrderedText(),
        ModConfig.HANDLER.instance().render_number_display_shadow,
        textLayerType,
        15728880,
        textColor,
        backgroundColor,
        0
    );
        matrixStack.pop();
    }
    @SuppressWarnings("unchecked")
    @Unique private void renderArmorPoints(LivingEntity livingEntity, float yaw, float tickDelta, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue){
        double d = this.dispatcher.getSquaredDistanceToCamera(livingEntity);
        final T entAsT = (T) livingEntity;
        int armor = MathHelper.ceil(livingEntity.getArmor());
        int maxArmor = MathHelper.ceil(livingEntity.getArmor());
        if(maxArmor == 0) return;
        int armorPoints = MathHelper.ceil(armor / 2.0F);
        boolean lastPointHalf = (armor & 1) == 1;
    int pointsTotal = 10;
        int pointsPerRow = ModConfig.HANDLER.instance().icons_per_row;
        int pixelsTotal = Math.min(pointsTotal, pointsPerRow) * 8 + 1;
        float maxX = pixelsTotal / 2.0f;
        float scale = ModConfig.HANDLER.instance().size;
        // Check if entity is obstructed by blocks
        boolean isObstructed = ModConfig.HANDLER.instance().show_through_walls;
    double h = 0;
        
        for (int isDrawingEmpty = 0; isDrawingEmpty < 2; isDrawingEmpty++) {
            // Again, switch to batching queues for see-through mode
            RenderCommandQueue targetQueue = isObstructed ? 
                orderedRenderCommandQueue.getBatchingQueue(isDrawingEmpty + 2) : 
                orderedRenderCommandQueue;
            
            for (int pointCount = 0; pointCount < pointsTotal; pointCount++) {
                if (pointCount % pointsPerRow == 0) {
                    h = (scale*10)*((pointCount/2 + pointsPerRow - 1) / pointsPerRow);
                }
                matrixStack.push();
                int extraHeight = (int) (((livingEntity.getMaxHealth() + livingEntity.getAbsorptionAmount())/2 + pointsPerRow - 1) / pointsPerRow);
                matrixStack.translate(0, livingEntity.getHeight() + 0.75f + (scale*10)*(extraHeight-1) + h, 0);
        if ((this.hasLabel(entAsT, d)
                        || (ModConfig.HANDLER.instance().force_higher_offset_for_players && livingEntity instanceof PlayerEntity && livingEntity != client.player))
                        && d <= 4096.0) {
                    matrixStack.translate(0.0D, 9.0F * 1.15F * scale, 0.0D);
                    if (d < 100.0 && livingEntity instanceof PlayerEntity && livingEntity.getEntityWorld().getScoreboard().getObjectiveForSlot(net.minecraft.scoreboard.ScoreboardDisplaySlot.BELOW_NAME) != null) {
                        matrixStack.translate(0.0D, 9.0F * 1.15F * scale, 0.0D);
                    }
                }
                matrixStack.multiply(this.dispatcher.camera.getRotation());
                matrixStack.scale(-scale, scale, scale);
                matrixStack.translate(0, ModConfig.HANDLER.instance().display_offset, 0);
                float x = maxX - (pointCount % pointsPerRow) * 8;
                ArmorTypeEnum type = (isDrawingEmpty == 0) ? ArmorTypeEnum.EMPTY : (pointCount < armorPoints ? ((pointCount == armorPoints - 1 && lastPointHalf) ? ArmorTypeEnum.HALF : ArmorTypeEnum.FULL) : null);
                if (type != null) {
                    Identifier armorTextureId = type.icon;

                    RenderLayer renderLayer;
                    if (isObstructed) {
                        // Use see-through render layer
                        renderLayer = RenderLayer.getTextSeeThrough(armorTextureId);
                    } else {
                        // Use normal text render layer
                        renderLayer = RenderLayer.getText(armorTextureId);
                    }
                    final ArmorTypeEnum renderType = type;
                    float opacity = ModConfig.HANDLER.instance().health_bar_opacity / 100.0F;
                    targetQueue.submitCustom(matrixStack, renderLayer, (matricesEntry, vertexConsumer) -> {
                        Matrix4f m = matricesEntry.getPositionMatrix();
                        RenderUtils.drawArmor(m, vertexConsumer, x, renderType, opacity, d, isObstructed);
                    });
                }
                matrixStack.pop();
            }
        }
    }
}