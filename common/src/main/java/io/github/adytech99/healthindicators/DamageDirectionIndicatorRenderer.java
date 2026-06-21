package io.github.adytech99.healthindicators;

import io.github.adytech99.healthindicators.config.ModConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.awt.*;

public class DamageDirectionIndicatorRenderer {
    private static Player player = HealthIndicatorsCommon.client.player;
    private static int timeSinceLastDamage = Integer.MAX_VALUE;
    private static LivingEntity attacker;

    public static void markDamageToPlayer(LivingEntity livingEntity){
        timeSinceLastDamage = 0;
        attacker = livingEntity;
    }

    public static void tick(){
        player = HealthIndicatorsCommon.client.player;
        if(timeSinceLastDamage != Integer.MAX_VALUE) timeSinceLastDamage++;
        if (attacker == null || attacker.isDeadOrDying() || attacker.isRemoved()){
            timeSinceLastDamage = Integer.MAX_VALUE;
            attacker = null;
        }
        if(timeSinceLastDamage == Integer.MAX_VALUE) attacker = null;
    }

    public static void render(GuiGraphicsExtractor drawContext, float tickDelta) {
        if (player == null) return;
        if (timeSinceLastDamage <= ModConfig.HANDLER.instance().damage_direction_indicators_visibility_time * 20 && attacker != null) {
            // Get positions and calculate direction
            Vec3 playerPos = player.position();
            Vec3 attackerPos = attacker.position();
            double deltaX = attackerPos.x - playerPos.x;
            double deltaZ = attackerPos.z - playerPos.z;

            // Calculate yaw to attacker and delta from player's current view
            float yawToAttacker = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0f;
            yawToAttacker = Mth.wrapDegrees(yawToAttacker);
            float deltaYaw = Mth.wrapDegrees(yawToAttacker - player.getYRot());

            // Get screen center and setup parameters
            int centerX = drawContext.guiWidth() / 2;
            int centerY = drawContext.guiHeight() / 2;
            float angle = (float) Math.toRadians(deltaYaw);

            // Calculate fade-out alpha (0-255)
            int alpha = 255;
            if (ModConfig.HANDLER.instance().damage_direction_indicators_fade_out) {
                int fadeDelay = (ModConfig.HANDLER.instance().damage_direction_indicators_visibility_time - ModConfig.HANDLER.instance().damage_direction_indicators_fade_out_time) * 20;
                if (timeSinceLastDamage >= fadeDelay) {
                    float progress = Math.min((timeSinceLastDamage - fadeDelay) / (float) (ModConfig.HANDLER.instance().damage_direction_indicators_fade_out_time * 20), 1.0f);
                    alpha = 255 - (int) (255 * progress);
                }
            }

            float scale = ModConfig.HANDLER.instance().damage_direction_indicators_scale;
            Color color = ModConfig.HANDLER.instance().damage_direction_indicators_color;

            // Draw the curved arc indicator
            drawArcIndicator(drawContext, centerX, centerY, angle, scale, color, alpha);
        }
    }

    private static void drawArcIndicator(GuiGraphicsExtractor context, int centerX, int centerY, float directionAngle, float scale, Color color, int alpha) {
        // Parameters for the curved arc indicator
        // radians
        float radius = 28.0f * scale; // Distance from center
        float arcSpan = 50.0f; // Arc width in degrees
        float thickness = 1.6f * scale; // Thickness of the arc
        int segments = 20; // Number of segments for smooth curve

        // Create color with alpha for main arc
        int mainColor = (alpha << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();

        // Create slightly darker color for outer edge (subtle depth effect)
        int darkerAlpha = (int)(alpha * 0.25f);
        int darkerColor = (darkerAlpha << 24) | ((color.getRed() * 2 / 3) << 16) | ((color.getGreen() * 2 / 3) << 8) | (color.getBlue() * 2 / 3);

        // Draw the main arc as a series of connected quads
        for (int i = 0; i < segments; i++) {
            float t1 = (float)i / segments;
            float t2 = (float)(i + 1) / segments;

            // Calculate angles for this segment (centered on direction angle)
            float angle1 = directionAngle - (float)Math.toRadians(arcSpan / 2.0f) + (float)Math.toRadians(arcSpan * t1);
            float angle2 = directionAngle - (float)Math.toRadians(arcSpan / 2.0f) + (float)Math.toRadians(arcSpan * t2);

            // Inner arc points
            float x1Inner = centerX + radius * Mth.sin(angle1);
            float y1Inner = centerY - radius * Mth.cos(angle1);
            float x2Inner = centerX + radius * Mth.sin(angle2);
            float y2Inner = centerY - radius * Mth.cos(angle2);

            // Outer arc points
            float x1Outer = centerX + (radius + thickness) * Mth.sin(angle1);
            float y1Outer = centerY - (radius + thickness) * Mth.cos(angle1);
            float x2Outer = centerX + (radius + thickness) * Mth.sin(angle2);
            float y2Outer = centerY - (radius + thickness) * Mth.cos(angle2);

            // Draw quad for this segment
            drawQuad(context, x1Inner, y1Inner, x2Inner, y2Inner, x2Outer, y2Outer, x1Outer, y1Outer, mainColor);
        }

        // Draw subtle glow/shadow on outer edge for depth
        for (int i = 0; i < segments; i++) {
            float t1 = (float)i / segments;
            float t2 = (float)(i + 1) / segments;

            float angle1 = directionAngle - (float)Math.toRadians(arcSpan / 2.0f) + (float)Math.toRadians(arcSpan * t1);
            float angle2 = directionAngle - (float)Math.toRadians(arcSpan / 2.0f) + (float)Math.toRadians(arcSpan * t2);

            float x1 = centerX + (radius + thickness) * Mth.sin(angle1);
            float y1 = centerY - (radius + thickness) * Mth.cos(angle1);
            float x2 = centerX + (radius + thickness) * Mth.sin(angle2);
            float y2 = centerY - (radius + thickness) * Mth.cos(angle2);

            float x1Outer = centerX + (radius + thickness + 1.0f * scale) * Mth.sin(angle1);
            float y1Outer = centerY - (radius + thickness + 1.0f * scale) * Mth.cos(angle1);
            float x2Outer = centerX + (radius + thickness + 1.0f * scale) * Mth.sin(angle2);
            float y2Outer = centerY - (radius + thickness + 1.0f * scale) * Mth.cos(angle2);

            drawQuad(context, x1, y1, x2, y2, x2Outer, y2Outer, x1Outer, y1Outer, darkerColor);
        }

        // Draw arrow pointer at the center of the arc pointing inward
        drawArrowPointer(context, centerX, centerY, directionAngle, radius, scale, color, alpha);
    }

    private static void drawArrowPointer(GuiGraphicsExtractor context, int centerX, int centerY, float directionAngle, float radius, float scale, Color color, int alpha) {
        // Arrow dimensions
        float arrowLength = 6.0f * scale;
        float arrowWidth = 4.5f * scale;

        // Position arrow at outer edge of arc
        float arrowBaseX = centerX + radius * Mth.sin(directionAngle);
        float arrowBaseY = centerY - radius * Mth.cos(directionAngle);

        // Calculate arrow tip pointing OUTWARD (away from center)
        float arrowTipX = centerX + (radius + arrowLength) * Mth.sin(directionAngle);
        float arrowTipY = centerY - (radius + arrowLength) * Mth.cos(directionAngle);

        // Calculate perpendicular angle for arrow wings
        float perpAngle = directionAngle + (float)Math.PI / 2.0f;

        // Arrow wing points
        float leftWingX = arrowBaseX + arrowWidth * Mth.sin(perpAngle);
        float leftWingY = arrowBaseY - arrowWidth * Mth.cos(perpAngle);
        float rightWingX = arrowBaseX - arrowWidth * Mth.sin(perpAngle);
        float rightWingY = arrowBaseY + arrowWidth * Mth.cos(perpAngle);

        // Create color with alpha
        int arrowColor = (alpha << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();

        // Draw arrow as a triangle
        float[] xs = {arrowTipX, leftWingX, rightWingX};
        float[] ys = {arrowTipY, leftWingY, rightWingY};
        sortVerticesByY(xs, ys);
        drawTriangle(context, xs, ys, arrowColor);
    }

    private static void drawQuad(GuiGraphicsExtractor context, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, int color) {
        // Draw quad as two triangles
        float[] tri1X = {x1, x2, x3};
        float[] tri1Y = {y1, y2, y3};
        float[] tri2X = {x1, x3, x4};
        float[] tri2Y = {y1, y3, y4};

        sortVerticesByY(tri1X, tri1Y);
        drawTriangle(context, tri1X, tri1Y, color);

        sortVerticesByY(tri2X, tri2Y);
        drawTriangle(context, tri2X, tri2Y, color);
    }

    // Helper: Sort vertices by Y coordinate (ascending)
    private static void sortVerticesByY(float[] xs, float[] ys) {
        if (ys[0] > ys[1]) {
            swap(xs, ys, 0, 1);
        }
        if (ys[0] > ys[2]) {
            swap(xs, ys, 0, 2);
        }
        if (ys[1] > ys[2]) {
            swap(xs, ys, 1, 2);
        }
    }

    // Helper: Swap two vertices in arrays
    private static void swap(float[] xs, float[] ys, int i, int j) {
        float tx = xs[i];
        xs[i] = xs[j];
        xs[j] = tx;
        float ty = ys[i];
        ys[i] = ys[j];
        ys[j] = ty;
    }

    // Helper: Draw a filled triangle using scanline algorithm
    private static void drawTriangle(GuiGraphicsExtractor context, float[] xs, float[] ys, int color) {
        float minY = ys[0];
        float midY = ys[1];
        float maxY = ys[2];

        if (minY == maxY) return; // Degenerate triangle

        // Iterate over each scanline
        for (int y = (int) Math.floor(minY); y <= (int) Math.ceil(maxY); y++) {
            if (y < minY || y > maxY) continue;

            boolean inSecondHalf = y > midY;
            float xa, xb;

            if (!inSecondHalf) {
                // Between minY and midY
                xa = interpolate(xs[0], ys[0], xs[1], ys[1], y);
                xb = interpolate(xs[0], ys[0], xs[2], ys[2], y);
            } else {
                // Between midY and maxY
                xa = interpolate(xs[1], ys[1], xs[2], ys[2], y);
                xb = interpolate(xs[0], ys[0], xs[2], ys[2], y);
            }

            // Ensure left to right order
            if (xa > xb) {
                float tmp = xa;
                xa = xb;
                xb = tmp;
            }

            // Draw horizontal line segment
            context.fill((int) xa, y, (int) xb + 1, y + 1, color);
        }
    }

    // Helper: Linear interpolation for edge scanning
    private static float interpolate(float x1, float y1, float x2, float y2, float y) {
        if (y1 == y2) return x1;
        return x1 + (x2 - x1) * (y - y1) / (y2 - y1);
    }
}