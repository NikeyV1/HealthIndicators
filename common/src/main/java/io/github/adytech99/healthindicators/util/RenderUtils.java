package io.github.adytech99.healthindicators.util;

import io.github.adytech99.healthindicators.config.ModConfig;
import io.github.adytech99.healthindicators.enums.ArmorTypeEnum;
import io.github.adytech99.healthindicators.enums.HeartTypeEnum;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.entity.LivingEntity;
import org.joml.Matrix4f;

public class RenderUtils {
    public static void drawHeart(Matrix4f model, VertexConsumer vertexConsumer, float x, HeartTypeEnum type, LivingEntity livingEntity) {
        drawHeart(model, vertexConsumer, x, type, livingEntity, 1.0F, 0.0);
    }

    public static void drawHeart(Matrix4f model, VertexConsumer vertexConsumer, float x, HeartTypeEnum type, LivingEntity livingEntity, float opacity) {
        drawHeart(model, vertexConsumer, x, type, livingEntity, opacity, 0.0);
    }

    public static void drawHeart(Matrix4f model, VertexConsumer vertexConsumer, float x, HeartTypeEnum type, LivingEntity livingEntity, float opacity, double squaredDistance) {
        drawHeart(model, vertexConsumer, x, type, livingEntity, opacity, squaredDistance, false);
    }
    
    public static void drawHeart(Matrix4f model, VertexConsumer vertexConsumer, float x, HeartTypeEnum type, LivingEntity livingEntity, float opacity, double squaredDistance, boolean isObstructed) {
        float minU = 0F;
        float maxU = 1F;
        float minV = 0F;
        float maxV = 1F;

        float heartSize = 9F;
        
        // Z offset only when non-see through, might change later.
        float z = 0F;
        if (!isObstructed) {
            float baseOffset = 0.01F;
            float distanceScale = 1.0F + (float)(Math.sqrt(squaredDistance) * 0.001F);
            float scaledOffset = baseOffset * distanceScale;
            z = (type == HeartTypeEnum.EMPTY) ? -scaledOffset : scaledOffset;
        }

        vertexConsumer.vertex(model, x, 0F - heartSize, z).texture(minU, maxV).light(15728880).color(1.0F, 1.0F, 1.0F, opacity);
        vertexConsumer.vertex(model, x - heartSize, 0F - heartSize, z).texture(maxU, maxV).light(15728880).color(1.0F, 1.0F, 1.0F, opacity);
        vertexConsumer.vertex(model, x - heartSize, 0F, z).texture(maxU, minV).light(15728880).color(1.0F, 1.0F, 1.0F, opacity);
        vertexConsumer.vertex(model, x, 0F, z).texture(minU, minV).light(15728880).color(1.0F, 1.0F, 1.0F, opacity);
    }

    public static void drawArmor(Matrix4f model, VertexConsumer vertexConsumer, float x, ArmorTypeEnum type) {
        drawArmor(model, vertexConsumer, x, type, 1.0F, 0.0);
    }

    public static void drawArmor(Matrix4f model, VertexConsumer vertexConsumer, float x, ArmorTypeEnum type, float opacity) {
        drawArmor(model, vertexConsumer, x, type, opacity, 0.0);
    }

    public static void drawArmor(Matrix4f model, VertexConsumer vertexConsumer, float x, ArmorTypeEnum type, float opacity, double squaredDistance) {
        drawArmor(model, vertexConsumer, x, type, opacity, squaredDistance, false);
    }
    
    public static void drawArmor(Matrix4f model, VertexConsumer vertexConsumer, float x, ArmorTypeEnum type, float opacity, double squaredDistance, boolean isObstructed) {
        float minU = 0F;
        float maxU = 1F;
        float minV = 0F;
        float maxV = 1F;

        float armorSize = 9F;
        
        float z = 0F;
        if (!isObstructed) {
            float baseOffset = 0.01F;
            float distanceScale = 1.0F + (float)(Math.sqrt(squaredDistance) * 0.001F);
            float scaledOffset = baseOffset * distanceScale;
            z = (type == ArmorTypeEnum.EMPTY) ? -scaledOffset : scaledOffset;
        }

        vertexConsumer.vertex(model, x, 0F - armorSize, z).texture(minU, maxV).light(15728880).color(1.0F, 1.0F, 1.0F, opacity);
        vertexConsumer.vertex(model, x - armorSize, 0F - armorSize, z).texture(maxU, maxV).light(15728880).color(1.0F, 1.0F, 1.0F, opacity);
        vertexConsumer.vertex(model, x - armorSize, 0F, z).texture(maxU, minV).light(15728880).color(1.0F, 1.0F, 1.0F, opacity);
        vertexConsumer.vertex(model, x, 0F, z).texture(minU, minV).light(15728880).color(1.0F, 1.0F, 1.0F, opacity);
    }

    public static String getHealthText(LivingEntity livingEntity) {
        int decimalPlaces = ModConfig.HANDLER.instance().decimal_places;
        float health = livingEntity.getHealth();
        float maxHealth = livingEntity.getMaxHealth();
        float absorption = livingEntity.getAbsorptionAmount();

        String healthStr = formatToDecimalPlaces(health + absorption, decimalPlaces);
        String maxHealthStr = formatToDecimalPlaces(maxHealth, decimalPlaces);

        if (ModConfig.HANDLER.instance().percentage_based_health) {
            float percentage = ((health + absorption) / maxHealth) * 100;
            String percentageStr = formatToDecimalPlaces(percentage, decimalPlaces);
            return percentageStr + " %";
        } else {
            return healthStr + " / " + maxHealthStr;
        }
    }

    private static String formatToDecimalPlaces(float value, int decimalPlaces) {
        if (decimalPlaces == 0) {
            return String.format("%.0f", value);
        } else {
            String decimalPlaces2 = String.valueOf(decimalPlaces);
            return String.format("%." + decimalPlaces2 + "f", value);
        }
    }
}
