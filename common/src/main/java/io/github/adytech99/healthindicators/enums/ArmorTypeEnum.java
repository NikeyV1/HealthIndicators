package io.github.adytech99.healthindicators.enums;

import net.minecraft.resources.ResourceLocation;

public enum ArmorTypeEnum {
    FULL("armor_full"),
    HALF("armor_half"),
    EMPTY("armor_empty");

    public final ResourceLocation icon;
    public final ResourceLocation vanillaIcon;

    ArmorTypeEnum(String armorIcon) {
        icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/" + armorIcon + ".png");
        vanillaIcon = ResourceLocation.fromNamespaceAndPath("healthindicators", "textures/gui/armor/" + armorIcon + ".png");
    }
}
