package com.moigferdsrte.ragdoll.api;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class RagdollInteractEvent {
   private final ServerPlayer player;
   private final UUID rootId;
   private final UUID partId;
   private final BlockPos pos;
   private final ServerLevel level;
   private boolean canceled;

   public RagdollInteractEvent(ServerPlayer player, UUID rootId, UUID partId, BlockPos pos, ServerLevel level) {
      this.player = player;
      this.rootId = rootId;
      this.partId = partId;
      this.pos = pos;
      this.level = level;
   }

   public ServerPlayer player() {
      return this.player;
   }

   public UUID rootId() {
      return this.rootId;
   }

   // The sublevel UUID of the specific body part that was right-clicked
   public UUID partId() {
      return this.partId;
   }

   public BlockPos pos() {
      return this.pos;
   }

   public ServerLevel level() {
      return this.level;
   }

   public boolean isCanceled() {
      return canceled;
   }

   public void setCanceled(boolean canceled) {
      this.canceled = canceled;
   }
}
