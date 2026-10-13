package com.moigferdsrte.ragdoll.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

// Fired on the Fabric ragdoll event callbacks after a player has been released from a ragdoll.
// exitVelocity is the velocity inherited by the player at the moment of release.
public class RagdollEndEvent {
   private final ServerPlayer player;
   private final Vec3 exitVelocity;
   private final Reason reason;

   public RagdollEndEvent(ServerPlayer player, Vec3 exitVelocity, Reason reason) {
      this.player = player;
      this.exitVelocity = exitVelocity;
      this.reason = reason;
   }

   public ServerPlayer player() {
      return this.player;
   }

   public Vec3 exitVelocity() {
      return this.exitVelocity;
   }

   public Reason reason() {
      return this.reason;
   }

   public enum Reason {
      EXPIRED,
      RELEASED,
      PLAYER_DEATH,
      PLAYER_LOGOUT
   }
}
