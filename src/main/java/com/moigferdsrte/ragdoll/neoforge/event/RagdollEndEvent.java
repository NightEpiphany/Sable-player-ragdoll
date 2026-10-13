package com.moigferdsrte.ragdoll.neoforge.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

@Deprecated(forRemoval = false)
public class RagdollEndEvent extends com.moigferdsrte.ragdoll.api.RagdollEndEvent {
   public RagdollEndEvent(ServerPlayer player, Vec3 exitVelocity, Reason reason) {
      super(player, exitVelocity, reason);
   }
}
