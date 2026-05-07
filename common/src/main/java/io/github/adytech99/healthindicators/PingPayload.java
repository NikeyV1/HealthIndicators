package io.github.adytech99.healthindicators;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record PingPayload() implements CustomPayload {

    // Wird zur Laufzeit mit der versionierten ID überschrieben
    public static CustomPayload.Id<PingPayload> VERSIONED_ID;

    public static final PacketCodec<PacketByteBuf, PingPayload> CODEC =
            PacketCodec.unit(new PingPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return VERSIONED_ID;
    }
}