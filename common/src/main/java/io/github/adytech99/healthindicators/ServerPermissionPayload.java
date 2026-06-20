package io.github.adytech99.healthindicators;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ServerPermissionPayload(boolean allowInvisiblePlayers) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerPermissionPayload> ID =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(HealthIndicatorsCommon.MOD_ID, "server_permissions"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerPermissionPayload> CODEC =
            StreamCodec.of(
                    (buf, value) -> buf.writeBoolean(value.allowInvisiblePlayers),
                    buf -> new ServerPermissionPayload(buf.readBoolean())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
