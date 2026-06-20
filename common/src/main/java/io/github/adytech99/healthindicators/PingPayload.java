package io.github.adytech99.healthindicators;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PingPayload() implements CustomPacketPayload {

    // Overwritten at runtime with the versioned id (see HealthIndicatorsFabric / HANDSHAKE_CHANNEL).
    public static CustomPacketPayload.Type<PingPayload> VERSIONED_ID;

    public static final StreamCodec<RegistryFriendlyByteBuf, PingPayload> CODEC =
            StreamCodec.unit(new PingPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return VERSIONED_ID;
    }
}
