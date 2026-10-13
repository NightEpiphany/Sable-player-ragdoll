package com.moigferdsrte.ragdoll.fabric.network;

import net.minecraft.server.level.ServerPlayer;

public final class RagdollNetworking {
   private RagdollNetworking() {
   }

   public static void register() { FabricNetwork.register(); }
   public static void registerClient() { FabricClientNetwork.register(); }

   public static void notifyReleased(ServerPlayer player) {
      player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player.getId(), player.getDeltaMovement()));
   }

   public static void notifyRequestPose(ServerPlayer player, long requestId) {
      FabricNetwork.send(player, new RagdollRequestPosePacket(requestId));
   }

   public static void notifyGrabStarted(ServerPlayer player) {
      var packet = new RagdollGrabSyncPacket(player.getUUID(), true);
      FabricNetwork.sendToTracking(player, packet);
      FabricNetwork.send(player, packet);
   }

   public static void notifyGrabEnded(ServerPlayer player) {
      var packet = new RagdollGrabSyncPacket(player.getUUID(), false);
      FabricNetwork.sendToTracking(player, packet);
      FabricNetwork.send(player, packet);
   }

}
