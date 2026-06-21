package io.github.adytech99.healthindicators;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PingPayload() implements CustomPacketPayload {

    // Wird zur Laufzeit mit der versionierten ID überschrieben
    public static CustomPacketPayload.Type<PingPayload> VERSIONED_ID;

    public static final StreamCodec<FriendlyByteBuf, PingPayload> CODEC =
            StreamCodec.unit(new PingPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return VERSIONED_ID;
    }
}
