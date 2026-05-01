package io.github.adytech99.healthindicators;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PingPayload(String modVersion) implements CustomPayload {
    public static final Id<PingPayload> ID =
            new Id<>(Identifier.of("healthindicators", "handshake"));

    public static final PacketCodec<PacketByteBuf, PingPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, PingPayload::modVersion,
                    PingPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}