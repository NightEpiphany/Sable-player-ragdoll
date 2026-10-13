package com.moigferdsrte.ragdoll.api;

import java.util.UUID;
import net.minecraft.world.phys.Vec3;

public interface PlayerlessRagdollSession {

   UUID id();

   // Current linear velocity of the root body, in blocks/tick.
   Vec3 currentVelocity();

   long elapsedTicks();

   // Randomly retargets joint motors for a twitching/wailing motion.
   void applyWailing(RagdollWailingOptions options);

   default void applyWailing(double stiffness, int durationTicks, int intervalTicks) {
      applyWailing(RagdollWailingOptions.builder()
         .stiffness(stiffness)
         .durationTicks(durationTicks)
         .intervalTicks(intervalTicks)
         .build());
   }

   default void applyWailing(int durationTicks) {
      applyWailing(RagdollWailingOptions.builder().durationTicks(durationTicks).build());
   }

   void stopWailing();

   // Triggers an immediate clean release. No-op if the session is already ending.
   void release();
}
