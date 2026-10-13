package com.moigferdsrte.ragdoll.fabric.client;

import com.moigferdsrte.ragdoll.entity.RagdollSeatEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public final class RagdollSeatEntityRenderer extends EntityRenderer<RagdollSeatEntity> {
   public RagdollSeatEntityRenderer(Context context) {
      super(context);
   }

   @Override
   public boolean shouldRender(RagdollSeatEntity entity, Frustum frustum, double camX, double camY, double camZ) {
      return false;
   }

   @Override
   public @NotNull ResourceLocation getTextureLocation(RagdollSeatEntity entity) {
      return ResourceLocation.fromNamespaceAndPath("sable_player_ragdoll", "textures/block/empty.png");
   }
}
