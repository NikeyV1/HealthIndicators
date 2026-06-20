package io.github.adytech99.healthindicators.enums;

import dev.isxander.yacl3.api.NameableEnum;
import net.minecraft.network.chat.Component;

public enum HealthDisplayTypeEnum implements NameableEnum {
    HEARTS("Hearts"),
    NUMBER("Number"),
    DYNAMIC("Dynamic");

    private final String displayName;
    HealthDisplayTypeEnum(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal(displayName);
    }
}
