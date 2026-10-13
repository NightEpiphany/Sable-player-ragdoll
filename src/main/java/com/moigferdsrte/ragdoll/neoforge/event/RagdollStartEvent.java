package com.moigferdsrte.ragdoll.neoforge.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

@Deprecated(forRemoval = false)
public class RagdollStartEvent extends com.moigferdsrte.ragdoll.api.RagdollStartEvent {
   public RagdollStartEvent(ServerPlayer player, Vec3 velocity) {
      super(player, velocity);
   }
}
