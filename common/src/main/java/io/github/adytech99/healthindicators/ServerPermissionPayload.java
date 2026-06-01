package io.github.adytech99.healthindicators;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ServerPermissionPayload(boolean allowInvisiblePlayers) implements CustomPayload {

    public static final CustomPayload.Id<ServerPermissionPayload> ID =
            new CustomPayload.Id<>(Identifier.of(HealthIndicatorsCommon.MOD_ID, "server_permissions"));

    public static final PacketCodec<PacketByteBuf, ServerPermissionPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> buf.writeBoolean(value.allowInvisiblePlayers),
                    buf -> new ServerPermissionPayload(buf.readBoolean())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}