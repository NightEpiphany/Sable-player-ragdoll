package com.moigferdsrte.ragdoll.fabric.network;

import com.moigferdsrte.ragdoll.RagdollGrabCallbacks;
import com.moigferdsrte.ragdoll.block.entity.RagdollPartBlockEntity;
import com.moigferdsrte.ragdoll.config.RagdollSettings;
import com.moigferdsrte.ragdoll.mob.block.entity.MobRagdollPartBlockEntity;
import com.moigferdsrte.ragdoll.physics.RagdollRegistry;
import com.moigferdsrte.ragdoll.physics.RagdollSessionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.moigferdsrte.ragdoll.fabric.network.FabricPayloadContext;

public record RagdollGrabPacket(BlockPos pos, boolean release) implements CustomPacketPayload {
   public static final Type<RagdollGrabPacket> TYPE = new Type<>(
      Identifier.fromNamespaceAndPath("sable_player_ragdoll", "grab")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, RagdollGrabPacket> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC, RagdollGrabPacket::pos,
      ByteBufCodecs.BOOL, RagdollGrabPacket::release,
      RagdollGrabPacket::new
   );

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handle(RagdollGrabPacket packet, FabricPayloadContext context) {
      context.enqueueWork(() -> {
         if (!(context.player() instanceof ServerPlayer player)) return;
         BlockEntity blockEntity = player.level().getBlockEntity(packet.pos());
         if (blockEntity instanceof RagdollPartBlockEntity ragdollPart) {
            if (packet.release()) {
               ragdollPart.stopGrab(player.getUUID());
               RagdollGrabCallbacks.notifyReleased(player);
            } else {
               if (!RagdollSettings.grabEnabled()) return;
               if (RagdollSessionManager.activeRagdollForPlayer(player.level(), player.getUUID()) != null) return;
               if (RagdollRegistry.isGrabDisabledAt(player.level(), packet.pos())) return;
               ragdollPart.startGrab(player.getUUID());
               RagdollGrabCallbacks.notifyGrabbed(player);
            }
         } else if (blockEntity instanceof MobRagdollPartBlockEntity mobRagdollPart) {
            if (packet.release()) {
               mobRagdollPart.stopGrab(player.getUUID());
               RagdollGrabCallbacks.notifyReleased(player);
            } else {
               if (!RagdollSettings.grabEnabled()) return;
               if (RagdollSessionManager.activeRagdollForPlayer(player.level(), player.getUUID()) != null) return;
               if (RagdollRegistry.isGrabDisabledAt(player.level(), packet.pos())) return;
               mobRagdollPart.startGrab(player.getUUID());
               RagdollGrabCallbacks.notifyGrabbed(player);
            }
         }
      });
   }
}
