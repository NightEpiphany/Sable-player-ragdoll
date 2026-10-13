package com.moigferdsrte.ragdoll.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

// Fired on the Fabric ragdoll event callbacks before a player ragdoll is assembled.
// Cancel to prevent the launch. Modify velocity to redirect or rescale the launch.
public class RagdollStartEvent {
   private final ServerPlayer player;
   private Vec3 velocity;
   private boolean canceled;

   public RagdollStartEvent(ServerPlayer player, Vec3 velocity) {
      this.player = player;
      this.velocity = velocity;
   }

   public ServerPlayer player() {
      return this.player;
   }

   public Vec3 velocity() {
      return this.velocity;
   }

   public void setVelocity(Vec3 velocity) {
      this.velocity = velocity;
   }

   public boolean isCanceled() {
      return canceled;
   }

   public void setCanceled(boolean canceled) {
      this.canceled = canceled;
   }
}
