package com.moigferdsrte.ragdoll.fabric.network;

import com.moigferdsrte.ragdoll.fabric.client.RagdollGrabClient;
import com.moigferdsrte.ragdoll.fabric.client.RagdollGrabState;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import java.util.UUID;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.moigferdsrte.ragdoll.fabric.network.FabricPayloadContext;

public record RagdollGrabSyncPacket(UUID playerId, boolean grabbing) implements CustomPacketPayload {
   public static final Type<RagdollGrabSyncPacket> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("sable_player_ragdoll", "grab_sync")
   );
   private static final StreamCodec<ByteBuf, UUID> UUID_CODEC = StreamCodec.of(
      (buf, uuid) -> { buf.writeLong(uuid.getMostSignificantBits()); buf.writeLong(uuid.getLeastSignificantBits()); },
      buf -> new UUID(buf.readLong(), buf.readLong())
   );
   public static final StreamCodec<ByteBuf, RagdollGrabSyncPacket> STREAM_CODEC = StreamCodec.composite(
      UUID_CODEC, RagdollGrabSyncPacket::playerId,
      ByteBufCodecs.BOOL, RagdollGrabSyncPacket::grabbing,
      RagdollGrabSyncPacket::new
   );

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handle(RagdollGrabSyncPacket packet, FabricPayloadContext context) {
      context.enqueueWork(() -> {
         if (packet.grabbing()) {
            RagdollGrabState.add(packet.playerId());
         } else {
            RagdollGrabState.remove(packet.playerId());
            if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.getUUID().equals(packet.playerId())) {
               RagdollGrabClient.clearActive();
            }
         }
      });
   }
}
