package io.github.adytech99.healthindicators.util;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;

public class Util {
    public static double truncate(double number, int places) {
        return Math.floor(number * Math.pow(10, places)) / Math.pow(10, places);
    }

    public static Entity getEntityFromName(ClientLevel world, String entity_name){
        for(Entity entity : world.entitiesForRendering()){
            if(entity.hasCustomName()) if(Objects.equals(entity.getCustomName().getString(), entity_name)) return entity;
            if(entity instanceof Player) if(Objects.equals(entity.getDisplayName().getString(), entity_name)) return entity;
        }
        return null;
    }
}
