package io.github.adytech99.healthindicators.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class HeartJumpData {
    private static final Minecraft client = Minecraft.getInstance();

    private static boolean isHeartJumping = false;
    private static int whichHeartJumping = -1;

    public static int getWhichHeartJumping(LivingEntity livingEntity) {
        return (int) (livingEntity.tickCount % livingEntity.getMaxHealth());
    }

    public static boolean isHeartJumping() {
        return isHeartJumping;
    }

    public static void tick(Minecraft client){
        Player player = client.player;
        if(player == null) return;
        if(whichHeartJumping != -1){
            whichHeartJumping++;
            isHeartJumping = true;
            if(whichHeartJumping > player.getMaxHealth()/2){
                whichHeartJumping = -1;
                isHeartJumping = false;
            }
        } else if (player.tickCount % 16 == 0) {
            whichHeartJumping = 1;
            isHeartJumping = true;
        }
        else isHeartJumping = false;
        //client.player.sendMessage(Text.literal(String.valueOf(whichHeartJumping)), true);
    }
}
