package io.github.adytech99.healthindicators.enums;

import net.minecraft.resources.Identifier;

public enum ArmorTypeEnum {
    FULL("armor_full"),
    HALF("armor_half"),
    EMPTY("armor_empty");

    public final Identifier icon;
    public final Identifier vanillaIcon;

    ArmorTypeEnum(String armorIcon) {
        icon = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/" + armorIcon + ".png");
        vanillaIcon = Identifier.fromNamespaceAndPath("healthindicators", "textures/gui/armor/" + armorIcon + ".png");
    }
}