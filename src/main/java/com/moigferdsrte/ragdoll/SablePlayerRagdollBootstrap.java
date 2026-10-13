package com.moigferdsrte.ragdoll;

import com.moigferdsrte.ragdoll.physics.RagdollRegistry;
import dev.ryanhcode.sable.platform.SableEventPlatform;

public final class SablePlayerRagdollBootstrap {
   private SablePlayerRagdollBootstrap() {
   }

   public static void init() {
      SableEventPlatform.INSTANCE.onPostPhysicsTick(RagdollRegistry::onPostPhysicsTick);
      SablePlayerRagdoll.LOGGER.info("[sable_player_ragdoll] post-physics ragdoll hook registered");
   }
}
