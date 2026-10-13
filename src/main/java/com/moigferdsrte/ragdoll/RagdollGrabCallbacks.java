package com.moigferdsrte.ragdoll;

import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;

public final class RagdollGrabCallbacks {
   private static Consumer<ServerPlayer> onGrabbed = player -> {};
   private static Consumer<ServerPlayer> onReleased = player -> {};

   private RagdollGrabCallbacks() {
   }

   public static void setOnGrabbed(Consumer<ServerPlayer> handler) {
      onGrabbed = handler != null ? handler : player -> {};
   }

   public static void setOnReleased(Consumer<ServerPlayer> handler) {
      onReleased = handler != null ? handler : player -> {};
   }

   public static void notifyGrabbed(ServerPlayer player) {
      onGrabbed.accept(player);
   }

   public static void notifyReleased(ServerPlayer player) {
      onReleased.accept(player);
   }
}
