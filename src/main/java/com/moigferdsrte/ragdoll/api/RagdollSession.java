package com.moigferdsrte.ragdoll.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public interface RagdollSession {

   ServerPlayer player();

   // Current linear velocity of the torso body, in blocks/tick.
   Vec3 currentVelocity();

   long elapsedTicks();

   // Whether the player can manually exit the ragdoll right now.
   boolean isDismountLocked();

   // Lock or unlock manual dismount for this session. Overrides the global minDismountTicks while set.
   void setDismountLocked(boolean locked);

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
