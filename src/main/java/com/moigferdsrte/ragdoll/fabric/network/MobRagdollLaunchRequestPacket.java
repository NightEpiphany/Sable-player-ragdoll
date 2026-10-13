package com.moigferdsrte.ragdoll.fabric.network;

import com.moigferdsrte.ragdoll.SablePlayerRagdoll;
import com.moigferdsrte.ragdoll.mob.client.MobRagdollClientExtractor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import com.moigferdsrte.ragdoll.fabric.network.FabricPayloadContext;

public record MobRagdollLaunchRequestPacket(int entityId, boolean suppressDeathEffects) implements CustomPacketPayload {
    public static final Type<MobRagdollLaunchRequestPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(SablePlayerRagdoll.MOD_ID, "mob_ragdoll_launch_request")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, MobRagdollLaunchRequestPacket> STREAM_CODEC = StreamCodec.of(
            MobRagdollLaunchRequestPacket::encode,
            MobRagdollLaunchRequestPacket::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, MobRagdollLaunchRequestPacket packet) {
        buffer.writeVarInt(packet.entityId());
        buffer.writeBoolean(packet.suppressDeathEffects());
    }

    private static MobRagdollLaunchRequestPacket decode(RegistryFriendlyByteBuf buffer) {
        return new MobRagdollLaunchRequestPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(MobRagdollLaunchRequestPacket packet, FabricPayloadContext context) {
        context.enqueueWork(() -> MobRagdollClientExtractor.extractAndSend(packet.entityId(), packet.suppressDeathEffects()));
    }
}
