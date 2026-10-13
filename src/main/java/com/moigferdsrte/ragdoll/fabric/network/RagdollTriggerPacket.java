package com.moigferdsrte.ragdoll.fabric.network;

import com.moigferdsrte.ragdoll.api.RagdollLimbConfig;
import com.moigferdsrte.ragdoll.api.RagdollLimbOptions;
import com.moigferdsrte.ragdoll.api.RagdollPoseSnapshot;
import com.moigferdsrte.ragdoll.block.entity.RagdollPartBlockEntity.BodyPart;
import com.moigferdsrte.ragdoll.physics.RagdollRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.moigferdsrte.ragdoll.fabric.network.FabricPayloadContext;

public record RagdollTriggerPacket(RagdollLimbOptions pose, float bodyYaw) implements CustomPacketPayload {
   public static final Type<RagdollTriggerPacket> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("sable_player_ragdoll", "trigger_ragdoll")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, RagdollTriggerPacket> STREAM_CODEC = StreamCodec.of(
      RagdollTriggerPacket::write,
      RagdollTriggerPacket::read
   );

   public RagdollTriggerPacket() {
      this(RagdollLimbOptions.defaults(), Float.NaN);
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handle(RagdollTriggerPacket packet, FabricPayloadContext context) {
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player) {
            RagdollRegistry.triggerManual(player, new RagdollPoseSnapshot(packet.pose(), packet.bodyYaw()));
         }
      });
   }

   private static void write(RegistryFriendlyByteBuf buffer, RagdollTriggerPacket packet) {
      buffer.writeFloat(packet.bodyYaw());
      for (BodyPart part : BodyPart.values()) {
         RagdollLimbConfig config = packet.pose().get(part);
         buffer.writeBoolean(config != null);
         if (config != null) {
            buffer.writeDouble(config.rightOffset().orElse(0.0));
            buffer.writeDouble(config.upOffset().orElse(0.0));
            buffer.writeDouble(config.forwardOffset().orElse(0.0));
            buffer.writeDouble(config.initialPitchDegrees().orElse(0.0));
            buffer.writeDouble(config.initialYawDegrees().orElse(0.0));
            buffer.writeDouble(config.initialRollDegrees().orElse(0.0));
         }
      }
   }

   private static RagdollTriggerPacket read(RegistryFriendlyByteBuf buffer) {
      float bodyYaw = buffer.readFloat();
      RagdollLimbOptions.Builder builder = RagdollLimbOptions.builder();
      for (BodyPart part : BodyPart.values()) {
         if (buffer.readBoolean()) {
            builder.limb(part, RagdollLimbConfig.builder()
               .offset(buffer.readDouble(), buffer.readDouble(), buffer.readDouble())
               .initialRotation(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
         }
      }
      return new RagdollTriggerPacket(builder.build(), bodyYaw);
   }
}
