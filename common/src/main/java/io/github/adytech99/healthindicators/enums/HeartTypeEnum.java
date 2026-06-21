package io.github.adytech99.healthindicators.enums;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;

public enum HeartTypeEnum {
    EMPTY("container"),
    RED_FULL("full"),
    RED_HALF("half"),
    YELLOW_FULL("absorbing_full"),
    YELLOW_HALF("absorbing_half");

    public final String icon;

    HeartTypeEnum(String heartIcon) {
        icon = heartIcon;
    }

    public static String addStatusIcon(LivingEntity livingEntity){
        if (livingEntity.hasEffect(MobEffects.WITHER)) return "withered_";
        if (livingEntity.hasEffect(MobEffects.POISON)) return "poisoned_";
        if (livingEntity.isFullyFrozen()) return "frozen_";
        else return "";
    }


    public static String addHardcoreIcon(LivingEntity livingEntity){
        if (livingEntity.level().getLevelData().isHardcore()) return "hardcore_";
        else return "";
    }
}