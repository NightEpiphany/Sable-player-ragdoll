package com.moigferdsrte.ragdoll.fabric.network;

import com.moigferdsrte.ragdoll.SablePlayerRagdoll;
import com.moigferdsrte.ragdoll.mob.client.MobRagdollClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import com.moigferdsrte.ragdoll.fabric.network.FabricPayloadContext;

public record MobRagdollSourceStatePacket(int entityId, boolean hidden) implements CustomPacketPayload {
    public static final Type<MobRagdollSourceStatePacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(SablePlayerRagdoll.MOD_ID, "mob_ragdoll_source_state")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, MobRagdollSourceStatePacket> STREAM_CODEC = StreamCodec.of(
            MobRagdollSourceStatePacket::encode,
            MobRagdollSourceStatePacket::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, MobRagdollSourceStatePacket packet) {
        buffer.writeVarInt(packet.entityId());
        buffer.writeBoolean(packet.hidden());
    }

    private static MobRagdollSourceStatePacket decode(RegistryFriendlyByteBuf buffer) {
        return new MobRagdollSourceStatePacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(MobRagdollSourceStatePacket packet, FabricPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().level == null) {
                return;
            }
            Entity entity = Minecraft.getInstance().level.getEntity(packet.entityId());
            if (entity != null) {
                MobRagdollClientState.setHidden(entity, packet.hidden());
            }
        });
    }
}
