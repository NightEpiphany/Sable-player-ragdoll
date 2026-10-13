package com.moigferdsrte.ragdoll.fabric.network;

import com.moigferdsrte.ragdoll.fabric.client.RagdollClientPoseCapture;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RagdollRequestPosePacket(long requestId) implements CustomPacketPayload {
   public static final Type<RagdollRequestPosePacket> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("sable_player_ragdoll", "request_pose")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, RagdollRequestPosePacket> STREAM_CODEC = StreamCodec.of(
      (buffer, packet) -> buffer.writeLong(packet.requestId()),
      buffer -> new RagdollRequestPosePacket(buffer.readLong())
   );

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handle(RagdollRequestPosePacket packet, FabricPayloadContext context) {
      context.enqueueWork(() -> {
         if (Minecraft.getInstance().player == null) return;
         FabricClientNetwork.sendToServer(new RagdollPoseResponsePacket(
            packet.requestId(),
            RagdollClientPoseCapture.capture(),
            Minecraft.getInstance().player.yBodyRot
         ));
      });
   }
}
