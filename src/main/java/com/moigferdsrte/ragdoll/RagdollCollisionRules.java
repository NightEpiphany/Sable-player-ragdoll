package com.moigferdsrte.ragdoll;

import java.util.function.BooleanSupplier;

public final class RagdollCollisionRules {
   private static BooleanSupplier localGrabActive = () -> false;

   private RagdollCollisionRules() {
   }

   public static void setLocalGrabActive(BooleanSupplier supplier) {
      localGrabActive = supplier != null ? supplier : () -> false;
   }

   public static boolean suppressLocalCollision() {
      return localGrabActive.getAsBoolean();
   }
}
