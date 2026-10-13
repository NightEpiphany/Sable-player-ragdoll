package com.moigferdsrte.ragdoll.fabric.client;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RagdollGrabState {
   private static final Set<UUID> GRABBING = ConcurrentHashMap.newKeySet();

   private RagdollGrabState() {
   }

   public static void add(UUID playerId) {
      GRABBING.add(playerId);
   }

   public static void remove(UUID playerId) {
      GRABBING.remove(playerId);
   }

   public static boolean isGrabbing(UUID playerId) {
      return GRABBING.contains(playerId);
   }
}
